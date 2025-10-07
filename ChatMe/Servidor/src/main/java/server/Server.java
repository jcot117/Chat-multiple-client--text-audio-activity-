package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private int puerto;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private Map<String, ClienteHandler> clientesConectados;
    private Map<String, Set<String>> grupos; // NUEVO: grupos de chat
    private File historialMensajes;

    public Server(int puerto) {
        this.puerto = puerto;
        this.clientesConectados = new ConcurrentHashMap<>();
        this.grupos = new ConcurrentHashMap<>();
        this.pool = Executors.newCachedThreadPool();
        this.historialMensajes = new File("historial_chat.txt");
    }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(puerto);
            System.out.println("Servidor de chat iniciado en el puerto " + puerto);

            while (true) {
                Socket socketCliente = serverSocket.accept();
                ClienteHandler handler = new ClienteHandler(socketCliente);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Enviar a cliente específico
    private void enviarMensaje(String destino, String mensaje) {
        ClienteHandler clienteDestino = clientesConectados.get(destino);
        if (clienteDestino != null) {
            clienteDestino.enviar(mensaje);
        } else {
            System.out.println("No se encontró el destino: " + destino);
        }
    }

    // NUEVO: enviar a todos los miembros de un grupo
    private void enviarAGrupo(String grupo, String mensaje, String remitente) {
        Set<String> miembros = grupos.get(grupo);
        if (miembros == null) {
            System.out.println("El grupo no existe: " + grupo);
            return;
        }

        for (String miembro : miembros) {
            if (!miembro.equals(remitente)) { // no se reenvía a sí mismo
                ClienteHandler destino = clientesConectados.get(miembro);
                if (destino != null) {
                    destino.enviar("[Grupo " + grupo + "] " + remitente + ": " + mensaje);
                }
            }
        }
    }

    // Guardar historial
    private synchronized void guardarHistorial(String registro) {
        try (FileWriter fw = new FileWriter(historialMensajes, true)) {
            fw.write(registro + "\n");
        } catch (IOException e) {
            System.err.println("Error al guardar historial: " + e.getMessage());
        }
    }

    // Clase interna para cada cliente
    private class ClienteHandler implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private String nombreUsuario;
        private String direccionIP;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
            this.direccionIP = socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                salida = new PrintWriter(socket.getOutputStream(), true);

                // Primer mensaje = nombre
                nombreUsuario = entrada.readLine();
                clientesConectados.put(nombreUsuario, this);
                System.out.println("Usuario conectado: " + nombreUsuario + " (" + direccionIP + ")");

                String linea;
                while ((linea = entrada.readLine()) != null) {
                    if (linea.equalsIgnoreCase("exit")) break;

                    // 🔹 Comandos de grupo
                    if (linea.startsWith("@grupo|")) {
                        manejarComandoGrupo(linea);
                        continue;
                    }

                    // 🔹 Mensajes de voz
                    if (linea.startsWith("@voz|")) {
                        manejarVozPrivada(linea);
                        continue;
                    }

                    if (linea.startsWith("@vozgrupo|")) {
                        manejarVozGrupo(linea);
                        continue;
                    }

                    // 🔹 Mensaje privado normal
                    String[] partes = linea.split("\\|", 2);
                    if (partes.length < 2) continue;

                    String destino = partes[0];
                    String mensaje = partes[1];
                    String registro = "[" + nombreUsuario + " -> " + destino + "] " + mensaje;

                    System.out.println(registro);
                    guardarHistorial(registro);

                    enviarMensaje(destino, "De " + nombreUsuario + ": " + mensaje);
                }

            } catch (IOException e) {
                System.err.println("Error con el cliente " + nombreUsuario + ": " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }

        // 🔹 Manejador de comandos de grupo
        private void manejarComandoGrupo(String comando) {
            String[] partes = comando.split("\\|");
            if (partes.length < 3) return;

            String accion = partes[1];
            String nombreGrupo = partes[2];

            switch (accion.toLowerCase()) {
                case "crear" -> {
                    grupos.putIfAbsent(nombreGrupo, ConcurrentHashMap.newKeySet());
                    grupos.get(nombreGrupo).add(nombreUsuario);
                    salida.println("Grupo '" + nombreGrupo + "' creado y unido correctamente.");
                }
                case "unir" -> {
                    grupos.putIfAbsent(nombreGrupo, ConcurrentHashMap.newKeySet());
                    grupos.get(nombreGrupo).add(nombreUsuario);
                    salida.println("Te uniste al grupo '" + nombreGrupo + "'.");
                }
                case "salir" -> {
                    Set<String> miembros = grupos.get(nombreGrupo);
                    if (miembros != null) {
                        miembros.remove(nombreUsuario);
                        salida.println("Saliste del grupo '" + nombreGrupo + "'.");
                    }
                }
                case "enviar" -> {
                    if (partes.length < 4) {
                        salida.println("Formato inválido. Usa: @grupo|enviar|nombreGrupo|mensaje");
                        return;
                    }
                    String mensaje = comando.split("\\|", 4)[3];
                    enviarAGrupo(nombreGrupo, mensaje, nombreUsuario);
                    guardarHistorial("[Grupo " + nombreGrupo + "] " + nombreUsuario + ": " + mensaje);
                }
                default -> salida.println("Comando de grupo no reconocido.");
            }
        }

        // 🔹 Manejador de voz privada
        private void manejarVozPrivada(String metadata) {
            try {
                String[] partes = metadata.split("\\|");
                String destino = partes[1];
                int tamañoAudio = Integer.parseInt(partes[2]);

                // Leer los bytes del audio
                byte[] audioData = new byte[tamañoAudio];
                InputStream inputStream = socket.getInputStream();
                int bytesRead = 0;
                while (bytesRead < tamañoAudio) {
                    int result = inputStream.read(audioData, bytesRead, tamañoAudio - bytesRead);
                    if (result == -1) break;
                    bytesRead += result;
                }

                // Enviar al destino
                ClienteHandler clienteDestino = clientesConectados.get(destino);
                if (clienteDestino != null) {
                    clienteDestino.salida.println("@voz|" + nombreUsuario + "|" + tamañoAudio);
                    OutputStream outputStreamDestino = clienteDestino.socket.getOutputStream();
                    outputStreamDestino.write(audioData);
                    outputStreamDestino.flush();
                    guardarHistorial("[Voz] " + nombreUsuario + " -> " + destino);
                } else {
                    System.out.println("Destino no encontrado para nota de voz: " + destino);
                }
            } catch (IOException e) {
                System.err.println("Error al manejar voz privada: " + e.getMessage());
            }
        }

        // 🔹 Manejador de voz grupal
        private void manejarVozGrupo(String metadata) {
            try {
                String[] partes = metadata.split("\\|");
                String grupo = partes[1];
                int tamañoAudio = Integer.parseInt(partes[2]);

                // Leer los bytes del audio
                byte[] audioData = new byte[tamañoAudio];
                InputStream inputStream = socket.getInputStream();
                int bytesRead = 0;
                while (bytesRead < tamañoAudio) {
                    int result = inputStream.read(audioData, bytesRead, tamañoAudio - bytesRead);
                    if (result == -1) break;
                    bytesRead += result;
                }

                // Enviar a todos los miembros del grupo
                Set<String> miembros = grupos.get(grupo);
                if (miembros != null) {
                    for (String miembro : miembros) {
                        if (!miembro.equals(nombreUsuario)) {
                            ClienteHandler clienteDestino = clientesConectados.get(miembro);
                            if (clienteDestino != null) {
                                clienteDestino.salida.println("@vozgrupo|" + grupo + "|" + nombreUsuario + "|" + tamañoAudio);
                                OutputStream outputStreamDestino = clienteDestino.socket.getOutputStream();
                                outputStreamDestino.write(audioData);
                                outputStreamDestino.flush();
                            }
                        }
                    }
                    guardarHistorial("[Voz-Grupo " + grupo + "] " + nombreUsuario);
                } else {
                    System.out.println("Grupo no encontrado: " + grupo);
                }
            } catch (IOException e) {
                System.err.println("Error al manejar voz grupal: " + e.getMessage());
            }
        }

        public void enviar(String mensaje) {
            salida.println(mensaje);
        }

        private void cerrarConexion() {
            try {
                if (nombreUsuario != null) {
                    clientesConectados.remove(nombreUsuario);
                    grupos.values().forEach(g -> g.remove(nombreUsuario));
                    System.out.println("Usuario desconectado: " + nombreUsuario);
                }
                socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    // Main
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Puerto del servidor: ");
        int puerto = Integer.parseInt(sc.nextLine());

        Server servidor = new Server(puerto);
        servidor.iniciar();
    }
}
