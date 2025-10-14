package server;

import java.io.*;
import java.net.Socket;

public class AudioHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private String nombreUsuario;

    public AudioHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Leer username primero
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.nombreUsuario = reader.readLine();
            System.out.println("🎵 Cliente audio conectado: " + nombreUsuario);

            // Procesar stream de audio (similar al ejercicio Audio)
            InputStream audioStream = socket.getInputStream();
            processAudioStream(audioStream);
            
        } catch (IOException e) {
            System.out.println(" Error en AudioHandler: " + e.getMessage());
        }
    }

    private void processAudioStream(InputStream audioStream) {
        // Implementación similar al servidor del ejercicio Audio
        // Para reenvío de audio a otros clientes
        System.out.println("Procesando stream de audio para: " + nombreUsuario);
        
        // Aquí iría la lógica para recibir y retransmitir audio
        // usando UDP como en los ejercicios
    }
}