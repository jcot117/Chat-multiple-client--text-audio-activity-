package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.time.LocalDateTime;

public class ChatServer {
    private static final int TEXT_PORT = 5000;
    private static final int AUDIO_PORT = 5001;
    private ExecutorService pool;
    private ConcurrentHashMap<String, ClientHandler> clients;
    private ConcurrentHashMap<String, CopyOnWriteArrayList<ClientHandler>> groups;
    private Semaphore clientsSemaphore;
    private Semaphore groupsSemaphore;
    private Semaphore audioSemaphore;
    private File historyFile;

    public ChatServer() {
        this.pool = Executors.newFixedThreadPool(10);
        this.clients = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.clientsSemaphore = new Semaphore(1);
        this.groupsSemaphore = new Semaphore(3);
        this.audioSemaphore = new Semaphore(5);
        this.historyFile = new File("historical_chat.txt");
        
        // Crear archivo de historial si no existe
        try {
            if (!historyFile.exists()) {
                historyFile.createNewFile();
            }
        } catch (IOException e) {
            System.out.println(" Error creando archivo de historial");
        }
    }

    public void start() {
        System.out.println(" === Servidor de Chat Iniciado ===");
        System.out.println(" Puerto texto: " + TEXT_PORT);
        System.out.println(" Puerto audio: " + AUDIO_PORT);
        System.out.println(" Hora de inicio: " + LocalDateTime.now());
        
        new Thread(this::startTextServer).start();
        new Thread(this::startAudioServer).start();
    }

    private void startTextServer() {
        try (ServerSocket serverSocket = new ServerSocket(TEXT_PORT)) {
            System.out.println(" Servidor de texto escuchando en puerto " + TEXT_PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(" Nueva conexión texto: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.out.println(" Error en servidor de texto: " + e.getMessage());
        }
    }

    private void startAudioServer() {
        try (ServerSocket audioSocket = new ServerSocket(AUDIO_PORT)) {
            System.out.println(" Servidor de audio escuchando en puerto " + AUDIO_PORT);
            
            while (true) {
                Socket audioClientSocket = audioSocket.accept();
                System.out.println("🎵 Nueva conexión audio: " + audioClientSocket.getInetAddress());
                AudioHandler audioHandler = new AudioHandler(audioClientSocket, this);
                pool.execute(audioHandler);
            }
        } catch (IOException e) {
            System.out.println(" Error en servidor de audio: " + e.getMessage());
        }
    }

    public void addClient(String nombreUsuario, ClientHandler handler) {
        try {
            clientsSemaphore.acquire();
            if (!clients.containsKey(nombreUsuario)) {
                clients.put(nombreUsuario, handler);
                System.out.println(" Usuario conectado: " + nombreUsuario);
                System.out.println(" Total de usuarios: " + clients.size());
                broadcastContactsList();
            } else {
                handler.sendMessage("ERROR@Usuario ya existe");
            }
        } catch (InterruptedException e) {
            System.out.println(" Cliente " + nombreUsuario + " fue interrumpido");
            Thread.currentThread().interrupt();
        } finally {
            clientsSemaphore.release();
        }
    }

    public void removeClient(String nombreUsuario) {
        try {
            clientsSemaphore.acquire();
            ClientHandler handler = clients.remove(nombreUsuario);
            if (handler != null) {
                System.out.println(" Usuario desconectado: " + nombreUsuario);
                System.out.println(" Total de usuarios: " + clients.size());
                
                // Remover usuario de todos los grupos
                removeUserFromAllGroups(nombreUsuario);
                broadcastContactsList();
            }
        } catch (InterruptedException e) {
            System.out.println(" Error al remover cliente " + nombreUsuario);
            Thread.currentThread().interrupt();
        } finally {
            clientsSemaphore.release();
        }
    }

    private void removeUserFromAllGroups(String nombreUsuario) {
        try {
            groupsSemaphore.acquire();
            for (String groupName : groups.keySet()) {
                CopyOnWriteArrayList<ClientHandler> groupMembers = groups.get(groupName);
                if (groupMembers != null) {
                    groupMembers.removeIf(member -> member.getUsername().equals(nombreUsuario));
                    // Notificar al grupo
                    broadcastToGroup(groupName, "USER_LEFT@" + nombreUsuario + "@" + groupName);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            groupsSemaphore.release();
        }
    }

    public void sendMessageToUser(String from, String to, String message) {
        ClientHandler recipient = clients.get(to);
        if (recipient != null) {
            recipient.sendMessage("MSG@" + from + "@" + message);
            saveToHistory(from, to, message, "TEXT");
        } else {
            // Notificar al remitente que el usuario no está disponible
            ClientHandler sender = clients.get(from);
            if (sender != null) {
                sender.sendMessage("ERROR@Usuario " + to + " no está disponible");
            }
        }
    }

    public void sendAudioToUser(String from, String to, byte[] audioData) {
        try {
            audioSemaphore.acquire();
            ClientHandler recipient = clients.get(to);
            if (recipient != null) {
                recipient.sendAudioNotification(from, audioData.length);
                saveToHistory(from, to, "Nota de voz", "AUDIO");
            }
        } catch (InterruptedException e) {
            System.out.println(" Audio interrumpido para: " + to);
            Thread.currentThread().interrupt();
        } finally {
            audioSemaphore.release();
        }
    }

    public void sendAudioToGroup(String from, String groupName, byte[] audioData) {
        try {
            audioSemaphore.acquire();
            groupsSemaphore.acquire();
            
            CopyOnWriteArrayList<ClientHandler> groupMembers = groups.get(groupName);
            if (groupMembers != null) {
                for (ClientHandler member : groupMembers) {
                    if (!member.getUsername().equals(from)) {
                        member.sendAudioNotification(from + "@" + groupName, audioData.length);
                    }
                }
                saveToHistory(from, groupName, "Nota de voz grupal", "GROUP_AUDIO");
            }
        } catch (InterruptedException e) {
            System.out.println(" Audio grupal interrumpido: " + groupName);
            Thread.currentThread().interrupt();
        } finally {
            groupsSemaphore.release();
            audioSemaphore.release();
        }
    }

    public void createGroup(String groupName, String creator) {
        try {
            groupsSemaphore.acquire();
            if (!groups.containsKey(groupName)) {
                groups.put(groupName, new CopyOnWriteArrayList<>());
                addUserToGroup(creator, groupName);
                System.out.println(" Grupo creado: " + groupName + " por " + creator);
                
                // Notificar al creador
                ClientHandler creatorHandler = clients.get(creator);
                if (creatorHandler != null) {
                    creatorHandler.sendMessage("GROUP_CREATED@" + groupName);
                }
            } else {
                // Notificar que el grupo ya existe
                ClientHandler creatorHandler = clients.get(creator);
                if (creatorHandler != null) {
                    creatorHandler.sendMessage("ERROR@El grupo " + groupName + " ya existe");
                }
            }
        } catch (InterruptedException e) {
            System.out.println(" Error creando grupo: " + groupName);
            Thread.currentThread().interrupt();
        } finally {
            groupsSemaphore.release();
        }
    }

    public void addUserToGroup(String nombreUsuario, String groupName) {
        CopyOnWriteArrayList<ClientHandler> group = groups.get(groupName);
        if (group != null) {
            ClientHandler user = clients.get(nombreUsuario);
            if (user != null && !group.contains(user)) {
                group.add(user);
                user.sendMessage("GROUP_JOIN@" + groupName);
                broadcastToGroup(groupName, "USER_JOINED@" + nombreUsuario + "@" + groupName);
                System.out.println(" Usuario " + nombreUsuario + " unido al grupo " + groupName);
            }
        } else {
            // Notificar que el grupo no existe
            ClientHandler user = clients.get(nombreUsuario);
            if (user != null) {
                user.sendMessage("ERROR@El grupo " + groupName + " no existe");
            }
        }
    }

    private void broadcastContactsList() {
        StringBuilder contactsList = new StringBuilder("CONTACTS@");
        for (String contact : clients.keySet()) {
            contactsList.append(contact).append(",");
        }
        
        String contactsMsg = contactsList.toString();
        for (ClientHandler client : clients.values()) {
            client.sendMessage(contactsMsg);
        }
    }

    private void broadcastToGroup(String groupName, String message) {
        CopyOnWriteArrayList<ClientHandler> groupMembers = groups.get(groupName);
        if (groupMembers != null) {
            for (ClientHandler member : groupMembers) {
                member.sendMessage(message);
            }
        }
    }

    private void saveToHistory(String from, String to, String content, String type) {
        try (FileWriter writer = new FileWriter(historyFile, true)) {
            String timestamp = LocalDateTime.now().toString();
            String entry = String.format("[%s] %s -> %s (%s): %s\n", 
                timestamp, from, to, type, content);
            writer.write(entry);
        } catch (IOException e) {
            System.out.println(" Error guardando en historial: " + e.getMessage());
        }
    }

    public ConcurrentHashMap<String, ClientHandler> getClients() {
        return clients;
    }

    public ConcurrentHashMap<String, CopyOnWriteArrayList<ClientHandler>> getGroups() {
        return groups;
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}