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
    private File historialMensajes;

    public Server(int puerto) {
        this.puerto = puerto;
        this.clientesConectados = new ConcurrentHashMap<>();
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

    // Envía un mensaje a un cliente específico si está conectado
    private void enviarMensaje(String destino, String mensaje) {
        ClienteHandler clienteDestino = clientesConectados.get(destino);
        if (clienteDestino != null) {
            clienteDestino.enviar(mensaje);
        } else {
            System.out.println("No se encontró el destino: " + destino);
        }
    }

    // Guarda el mensaje en un archivo de texto
    private synchronized void guardarHistorial(String registro) {
        try (FileWriter fw = new FileWriter(historialMensajes, true)) {
            fw.write(registro + "\n");
        } catch (IOException e) {
            System.err.println("Error al guardar historial: " + e.getMessage());
        }
    }

    // Clase interna para manejar a cada cliente
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

                // Primer mensaje: el nombre del usuario
                nombreUsuario = entrada.readLine();
                clientesConectados.put(nombreUsuario, this);
                System.out.println("Usuario conectado: " + nombreUsuario + " (" + direccionIP + ")");

                String linea;
                while ((linea = entrada.readLine()) != null) {
                    if (linea.equalsIgnoreCase("exit")) break;

                    // Formato: DESTINO_IP|MENSAJE
                    String[] partes = linea.split("\\|", 2);
                    if (partes.length < 2) continue;

                    String destino = partes[0];
                    String mensaje = partes[1];
                    String registro = "[" + nombreUsuario + " -> " + destino + "] " + mensaje;

                    System.out.println("" + registro);
                    guardarHistorial(registro);

                    // Reenviar al destinatario (si está conectado por nombre)
                    enviarMensaje(destino, "De " + nombreUsuario + ": " + mensaje);
                }

            } catch (IOException e) {
                System.err.println("Error con el cliente " + nombreUsuario + ": " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }

        public void enviar(String mensaje) {
            salida.println(mensaje);
        }

        private void cerrarConexion() {
            try {
                if (nombreUsuario != null) {
                    clientesConectados.remove(nombreUsuario);
                    System.out.println("Usuario desconectado: " + nombreUsuario);
                }
                socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    // Método principal
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Puerto del servidor: ");
        int puerto = Integer.parseInt(sc.nextLine());

        Server servidor = new Server(puerto);
        servidor.iniciar();
    }
}
