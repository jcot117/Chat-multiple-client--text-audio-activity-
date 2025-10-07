package client;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.sound.sampled.*;

public class Client {
    private String nombreUsuario;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Map<String, String> contactos;
    private InputStream inputStream;

    public Client(String nombreUsuario, String direccionServidor, int puerto) {
        this.nombreUsuario = nombreUsuario;
        this.contactos = new HashMap<>();

        try {
            socket = new Socket(direccionServidor, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            this.inputStream = socket.getInputStream();
            System.out.println("Conectado al servidor en " + direccionServidor + ":" + puerto);

            // Enviar nombre de usuario al servidor
            salida.println(nombreUsuario);

            // Escuchar mensajes entrantes en un hilo aparte
            new Thread(new Receptor()).start();

            // Iniciar la interfaz de línea de comandos
            menuPrincipal();

        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    // Menú principal
    private void menuPrincipal() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== Menú de Chat ===");
            System.out.println("1. Enviar mensaje privado");
            System.out.println("2. Ver contactos");
            System.out.println("3. Agregar contacto");
            System.out.println("4. Grupos");
            System.out.println("5. Enviar nota de voz");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            int opcion = Integer.parseInt(sc.nextLine());

            switch (opcion) {
                case 1 -> enviarMensaje(sc);
                case 2 -> verContactos();
                case 3 -> agregarContacto(sc);
                case 4 -> menuGrupos(sc);
                case 5 -> menuVoz(sc);
                case 6 -> cerrarConexion();
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    // Submenú de grupos
    private void menuGrupos(Scanner sc) {
        while (true) {
            System.out.println("\n=== Gestión de Grupos ===");
            System.out.println("1. Crear grupo");
            System.out.println("2. Unirse a grupo");
            System.out.println("3. Enviar mensaje a grupo");
            System.out.println("4. Enviar nota de voz a grupo");
            System.out.println("5. Salir de grupo");
            System.out.println("6. Volver al menú principal");
            System.out.print("Opción: ");
            int opcion = Integer.parseInt(sc.nextLine());

            switch (opcion) {
                case 1 -> crearGrupo(sc);
                case 2 -> unirseGrupo(sc);
                case 3 -> enviarMensajeGrupo(sc);
                case 4 -> enviarMensajeVoz(sc, true); //Enviar voz a grupo
                case 5 -> salirGrupo(sc);
                case 6 -> { return; }
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    private void crearGrupo(Scanner sc) {
        System.out.print("Nombre del grupo a crear: ");
        String grupo = sc.nextLine();
        salida.println("@grupo|crear|" + grupo);
    }

    private void unirseGrupo(Scanner sc) {
        System.out.print("Nombre del grupo al que deseas unirte: ");
        String grupo = sc.nextLine();
        salida.println("@grupo|unir|" + grupo);
    }

    private void enviarMensajeGrupo(Scanner sc) {
        System.out.print("Nombre del grupo: ");
        String grupo = sc.nextLine();
        System.out.print("Mensaje: ");
        String mensaje = sc.nextLine();
        salida.println("@grupo|enviar|" + grupo + "|" + mensaje);
    }

    private void salirGrupo(Scanner sc) {
        System.out.print("Nombre del grupo del que deseas salir: ");
        String grupo = sc.nextLine();
        salida.println("@grupo|salir|" + grupo);
    }

    // Contactos
    private void agregarContacto(Scanner sc) {
        System.out.print("Nombre del contacto: ");
        String nombre = sc.nextLine();
        System.out.print("Dirección IP del contacto: ");
        String ip = sc.nextLine();
        contactos.put(nombre, ip);
        System.out.println("Contacto agregado correctamente.");
    }

    private void verContactos() {
        System.out.println("\n--- Contactos guardados ---");
        if (contactos.isEmpty()) {
            System.out.println("(No hay contactos aún)");
        } else {
            contactos.forEach((nombre, ip) -> System.out.println(nombre + " -> " + ip));
        }
    }

    // Enviar mensaje privado
    private void enviarMensaje(Scanner sc) {
        System.out.print("¿A quién deseas enviar el mensaje? (nombre o IP): ");
        String destino = sc.nextLine();

        // Buscar IP si el usuario escribió un nombre
        String ipDestino = contactos.getOrDefault(destino, destino);

        System.out.print("Mensaje: ");
        String mensaje = sc.nextLine();

        // Formato: DESTINO_IP|MENSAJE
        salida.println(ipDestino + "|" + mensaje);
    }

    // Cerrar conexión
    private void cerrarConexion() {
        try {
            salida.println("exit");
            socket.close();
            System.out.println("Conexión cerrada.");
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

    // Métodos para manejar audio
    private void menuVoz(Scanner sc) {
        System.out.println("\n=== Enviar Nota de Voz ===");
        System.out.println("1. Enviar a un usuario");
        System.out.println("2. Enviar a un grupo");
        System.out.print("Opción: ");
        int opcion = Integer.parseInt(sc.nextLine());
        switch (opcion) {
            case 1 -> enviarMensajeVoz(sc, false);
            case 2 -> enviarMensajeVoz(sc, true);
            default -> System.out.println("Opción inválida.");
        }
    }

    private void enviarMensajeVoz(Scanner sc,  boolean esGrupo) {
        try {
            System.out.print("Duración de la nota de voz (segundos, máximo 30): ");

            int duracion = Integer.parseInt(sc.nextLine());
            duracion = Math.min(duracion, 30); // Limitar a 30 segundos

            String destino;

            if (esGrupo) {
                System.out.print("Nombre del grupo: ");
                destino = sc.nextLine();
            } else {
                System.out.print("¿A quién deseas enviar la nota de voz? (nombre o IP): ");
                destino = sc.nextLine();
                destino = contactos.getOrDefault(destino, destino);
            }

            System.out.println("Grabando... (durante " + duracion + " segundos)");
            
            // Configurar formato de audio
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Micrófono no disponible o no soportado");
                return;
            }
            
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            
            // Grabar por el tiempo especificado
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < duracion * 1000) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                audioBuffer.write(buffer, 0, bytesRead);
            }
            
            microphone.stop();
            microphone.close();
            
            byte[] audioData = audioBuffer.toByteArray();
            
            // Enviar metadata primero
            if (esGrupo) {
                salida.println("@vozgrupo|" + destino + "|" + audioData.length);
            } else {
                salida.println("@voz|" + destino + "|" + audioData.length);
            }
            
            // Enviar datos de audio
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(audioData);
            outputStream.flush();
            
            System.out.println("Nota de voz enviada correctamente (" + audioData.length + " bytes)");
            
        } catch (Exception e) {
            System.err.println("Error al grabar/enviar audio: " + e.getMessage());
        }
    }


    // NUEVO: Método para reproducir audio recibido
    private void recibirYReproducirAudio(byte[] audioData) {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Altavoz no disponible para reproducir audio");
                return;
            }
            
            SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();
            
            speaker.write(audioData, 0, audioData.length);
            speaker.drain();
            speaker.close();
            
        } catch (Exception e) {
            System.err.println("Error al reproducir audio: " + e.getMessage());
        }
    }

    // Hilo receptor
    private class Receptor implements Runnable {
        @Override
        public void run() {
            try {
                InputStream inputStream = socket.getInputStream();
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    if (mensaje.startsWith("@voz|")) {
                        // Mensaje de voz privado
                        String[] partes = mensaje.split("\\|");
                        String remitente = partes[1];
                        int tamanoAudio = Integer.parseInt(partes[2]);

                        System.out.println("\nRecibiendo nota de voz de " + remitente + "...");

                        // Leer datos de audio
                        byte[] audioData = new byte[tamanoAudio];
                        int bytesRead = 0;
                        while (bytesRead < tamanoAudio) {
                            int result = inputStream.read(audioData, bytesRead, tamanoAudio - bytesRead);
                            if (result == -1) break;
                            bytesRead += result;
                        }
                        System.out.println("\nReproduciendo nota de voz de " + remitente + "...");
                        recibirYReproducirAudio(audioData);

                    } else if (mensaje.startsWith("@vozgrupo|")) {
                        // Manejar nota de voz grupal
                        String[] partes = mensaje.split("\\|");
                        String grupo = partes[1];
                        String remitente = partes[2];
                        int tamanoAudio = Integer.parseInt(partes[3]);

                        System.out.println("\nRecibiendo nota de voz en grupo " + grupo + " de " + remitente + "...");

                        // Leer datos de audio
                        byte[] audioData = new byte[tamanoAudio];
                        int bytesRead = 0;
                        while (bytesRead < tamanoAudio) {
                            int result = inputStream.read(audioData, bytesRead, tamanoAudio - bytesRead);
                            if (result == -1) break;
                            bytesRead += result;
                        }
                        System.out.println("\nReproduciendo nota de voz del grupo " + grupo + " de " + remitente + "...");
                        recibirYReproducirAudio(audioData);
                        
                    } else {
                        System.out.println("\n" + mensaje);
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexión terminada: " + e.getMessage());
            }
        }
    }

    // Main
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Tu nombre de usuario: ");
        String nombre = sc.nextLine();

        // Valores por defecto para evitar problemas con Gradle
        String direccion = "localhost";
        int puerto = 8080;

        try {
            System.out.print("Dirección del servidor [localhost]: ");
            String inputDir = sc.nextLine();
            if (!inputDir.trim().isEmpty()) {
                direccion = inputDir;
            }
        
            System.out.print("Puerto [8080]: ");
            String inputPuerto = sc.nextLine();
            if (!inputPuerto.trim().isEmpty()) {
                puerto = Integer.parseInt(inputPuerto);
            }
        } catch (Exception e) {
            System.out.println("Usando valores por defecto: localhost:8080");
        }

        new Client(nombre, direccion, puerto);
    }
}
