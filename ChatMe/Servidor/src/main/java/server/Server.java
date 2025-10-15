package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private int puerto;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private static Map<String, ClienteHandler> clientesConectados = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> grupos;
    private static File historialMensajes;
    private static Semaphore semaphore;

    public Server(int puerto) {
        this.semaphore= new Semaphore(2);
        this.puerto = puerto;
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
                semaphore.acquire();
                pool.execute(handler);
            }
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    public static void enviarMensaje(String destino, String mensaje) {
        ClienteHandler clienteDestino = clientesConectados.get(destino);
        if (clienteDestino != null) {
            clienteDestino.enviar(mensaje);
        } else {
            System.out.println("No se encontró el destino: " + destino);
        }
    }

    public static void enviarAudio(String destino, byte[] audioData, String metadata) {
        ClienteHandler clienteDestino = clientesConectados.get(destino);
        if (clienteDestino != null) {
            clienteDestino.enviarAudio(destino, audioData, metadata);
        } else {
            System.out.println("No se encontró el destino para audio: " + destino);
        }
    }

    public static void enviarAGrupo(String grupo, String mensaje, String remitente) {
        Set<String> miembros = grupos.get(grupo);
        if (miembros == null) {
            System.out.println("El grupo no existe: " + grupo);
            return;
        }

        for (String miembro : miembros) {
            if (!miembro.equals(remitente)) {
                ClienteHandler destino = clientesConectados.get(miembro);
                if (destino != null) {
                    destino.enviar("[Grupo " + grupo + "] " + remitente + ": " + mensaje);
                }
            }
        }
    }

    public static void enviarAudioAGrupo(String grupo, byte[] audioData, String remitente) {
        Set<String> miembros = grupos.get(grupo);
        if (miembros == null) {
            System.out.println("El grupo no existe: " + grupo);
            return;
        }

        for (String miembro : miembros) {
            if (!miembro.equals(remitente)) {
                ClienteHandler destino = clientesConectados.get(miembro);
                if (destino != null) {
                    destino.enviarAudio(miembro, audioData, "@vozgrupo|" + grupo + "|" + remitente + "|" + audioData.length);
                }
            }
        }
    }

    // Nuevo método para manejar llamadas
    public static void manejarSolicitudLlamada(String remitente, String destino) {
        ClienteHandler clienteDestino = clientesConectados.get(destino);
        ClienteHandler clienteRemitente = clientesConectados.get(remitente);
        
        if (clienteDestino == null) {
            clienteRemitente.enviar("@llamada|no_disponible|" + destino);
            return;
        }
        
        // Enviar solicitud al destino
        clienteDestino.enviar("@llamada|solicitud|" + remitente);
        System.out.println("Solicitud de llamada de " + remitente + " a " + destino);
    }
    
    public static void manejarAceptacionLlamada(String remitente, String destino) {
        ClienteHandler clienteDestino = clientesConectados.get(destino);
        ClienteHandler clienteRemitente = clientesConectados.get(remitente);
        
        if (clienteDestino != null && clienteRemitente != null) {
            // Enviar información de conexión directa
            String ipRemitente = clienteRemitente.getSocket().getInetAddress().getHostAddress();
            int puertoRemitente = clienteRemitente.getPuertoLlamada();
            
            String ipDestino = clienteDestino.getSocket().getInetAddress().getHostAddress();
            int puertoDestino = clienteDestino.getPuertoLlamada();
            
            // Conectar a remitente con destino
            clienteRemitente.enviar("@llamada|conectar|" + ipDestino + ":" + puertoDestino);
            clienteDestino.enviar("@llamada|conectar|" + ipRemitente + ":" + puertoRemitente);
            
            System.out.println("Llamada conectada: " + remitente + " <-> " + destino);
        }
    }
    
    public static void manejarRechazoLlamada(String remitente, String destino) {
        ClienteHandler clienteRemitente = clientesConectados.get(remitente);
        if (clienteRemitente != null) {
            clienteRemitente.enviar("@llamada|rechazada|" + destino);
        }
    }

    public synchronized static void guardarHistorial(String registro) {
        try (FileWriter fw = new FileWriter(historialMensajes, true)) {
            fw.write(registro + "\n");
        } catch (IOException e) {
            System.err.println("Error al guardar historial: " + e.getMessage());
        }
    }

    public static Map<String, ClienteHandler> getClientesConectados(){
        return clientesConectados;
    }
    
    

    public static Map<String, Set<String>> getGrupos() {
        return grupos;
    }

    public static void main(String[] args) {
        int puerto;

        if (args.length > 0) {
            puerto = Integer.parseInt(args[0]);
        } else {
            puerto = 8080;
            System.out.println("Usando puerto por defecto: " + puerto);
        }

        Server servidor = new Server(puerto);
        servidor.iniciar();
    }
}