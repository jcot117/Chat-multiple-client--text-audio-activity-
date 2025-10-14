package server;
import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;


// Clase  para cada cliente
public class ClientHandler implements Runnable{
    private Socket socket;
    private ChatServer server;
    private String nombreUsuario;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Semaphore messageSemaphore; // Semaforo para enviar mensajes

    private String direccionIP;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.messageSemaphore = new Semaphore(1); // Un permiso para enviar mensajes
        this.direccionIP = socket.getInetAddress().getHostAddress();
        try {
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.salida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    @Override
    public void run() {
        try {
            String message;
            while ((message = entrada.readLine()) != null) {
                System.out.println("Mensaje recibido de: " + (nombreUsuario != null ? nombreUsuario : "desconocido") + ": " + message);
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("🔌 Cliente desconectado: " + nombreUsuario);
        } finally {
            if (nombreUsuario != null) {
                server.removeClient(nombreUsuario);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

        private void processMessage(String message) {
        String[] parts = message.split("@", 3);
        if (parts.length < 2) return;

        String command = parts[0];
        String param1 = parts[1];
        String param2 = parts.length > 2 ? parts[2] : "";

        switch (command) {
            case "LOGIN":
                handleLogin(param1);
                break;
            case "MSG":
                handleTextMessage(param1, param2);
                break;
            case "AUDIO":
                handleAudioMessage(param1);
                break;
            case "GROUP_AUDIO":
                handleGroupAudioMessage(param1);
                break;
            case "CREATE_GROUP":
                handleCreateGroup(param1);
                break;
            case "JOIN_GROUP":
                handleJoinGroup(param1);
                break;
            case "GET_CONTACTS":
                handleGetContacts();
                break;
            case "GET_GROUPS":
                handleGetGroups();
                break;
            default:
                System.out.println("Comando desconocido: " + command);
        }
    }

    private void handleLogin(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
        server.addClient(nombreUsuario, this);
        sendMessage("LOGIN_SUCCESS@" + nombreUsuario);
        System.out.println(" Usuario " + nombreUsuario + " ha iniciado sesión");
    }

    private void handleTextMessage(String toUser, String message) {
        server.sendMessageToUser(nombreUsuario, toUser, message);
        // Echo al remitente
        sendMessage("MSG_SENT@" + toUser + "@" + message);
    }

    private void handleAudioMessage(String toUser) {
        // Notificar que se enviará audio (el audio real va por UDP)
        server.sendAudioToUser(nombreUsuario, toUser, new byte[0]);
        sendMessage("AUDIO_SENT@" + toUser);
    }

    private void handleGroupAudioMessage(String groupName) {
        // Notificar audio grupal
        server.sendAudioToGroup(nombreUsuario, groupName, new byte[0]);
        sendMessage("GROUP_AUDIO_SENT@" + groupName);
    }

    private void handleCreateGroup(String groupName) {
        server.createGroup(groupName, nombreUsuario);
        sendMessage("GROUP_CREATED@" + groupName);
    }

    private void handleJoinGroup(String groupName) {
        server.addUserToGroup(nombreUsuario, groupName);
        // La confirmación se envía cuando el grupo actualiza la lista
    }

    private void handleGetContacts() {
        // Enviar lista de contactos actualizada
        StringBuilder contacts = new StringBuilder("CONTACTS@");
        server.getClients().keySet().stream()
            .filter(contact -> !contact.equals(nombreUsuario))
            .forEach(contact -> contacts.append(contact).append(","));
        sendMessage(contacts.toString());
    }

    private void handleGetGroups() {
        // Enviar lista de grupos disponibles
        StringBuilder groups = new StringBuilder("GROUPS@");
        server.getGroups().keySet().forEach(group -> groups.append(group).append(","));
        sendMessage(groups.toString());
    }

    public void sendMessage(String message) {
        try {
            messageSemaphore.acquire(); // Proteger el envío de mensajes
            entrada.println(message);
            System.out.println("📤 Mensaje enviado a " + nombreUsuario + ": " + message);
        } catch (InterruptedException e) {
            System.out.println(" Error enviando mensaje a " + nombreUsuario);
            Thread.currentThread().interrupt();
        } finally {
            messageSemaphore.release();
        }
    }

    public void sendAudioNotification(String from, int audioSize) {
        String notification = "AUDIO_NOTIFY@" + from + "@" + audioSize;
        sendMessage(notification);
    }

    public void sendGroupNotification(String groupName, String action) {
        String notification = "GROUP_UPDATE@" + groupName + "@" + action;
        sendMessage(notification);
    }

    public String getUsername() {
        return nombreUsuario;
    }

    // Método para verificar si la conexión está activa
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    // Método para desconexión limpia
    public void disconnect() {
        try {
            if (entrada != null) {
                entrada.close();
            }
            if (salida != null) {
                salida.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error cerrando conexión de " + nombreUsuario);
        }
    }
}

