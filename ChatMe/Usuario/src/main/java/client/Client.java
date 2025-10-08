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
    private DataInputStream dataInput;
    private OutputStream outputStream;

    public Client(String nombreUsuario, String direccionServidor, int puerto) {
        this.nombreUsuario = nombreUsuario;
        this.contactos = new HashMap<>();

        try {
            socket = new Socket(direccionServidor, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            this.dataInput = new DataInputStream(socket.getInputStream());
            this.outputStream = socket.getOutputStream();
            System.out.println("Conectado al servidor en " + direccionServidor + ":" + puerto);

            // Enviar nombre de usuario al servidor
            salida.println(nombreUsuario);

            // Escuchar mensajes entrantes en un hilo aparte
            new Thread(new Receptor()).start();

            // Iniciar la interfaz de linea de comandos
            menuPrincipal();

        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    // Menu principal
    private void menuPrincipal() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== Menu de Chat ===");
            System.out.println("1. Enviar mensaje privado");
            System.out.println("2. Ver contactos");
            System.out.println("3. Agregar contacto");
            System.out.println("4. Grupos");
            System.out.println("5. Enviar nota de voz");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            
            try {
                String input = sc.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Por favor, ingresa un numero de opcion.");
                    continue;
                }
                
                int opcion = Integer.parseInt(input);

                switch (opcion) {
                    case 1 -> enviarMensaje(sc);
                    case 2 -> verContactos();
                    case 3 -> agregarContacto(sc);
                    case 4 -> menuGrupos(sc);
                    case 5 -> menuVoz(sc);
                    case 6 -> { cerrarConexion(); return; }
                    default -> System.out.println("Opcion invalida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Por favor ingresa un numero valido (1-6).");
            }
        }
    }

    // Submenu de grupos
    private void menuGrupos(Scanner sc) {
        while (true) {
            System.out.println("\n=== Gestion de Grupos ===");
            System.out.println("1. Crear grupo");
            System.out.println("2. Unirse a grupo");
            System.out.println("3. Enviar mensaje a grupo");
            System.out.println("4. Enviar nota de voz a grupo");
            System.out.println("5. Salir de grupo");
            System.out.println("6. Volver al menu principal");
            System.out.print("Opcion: ");
            
            try {
                String input = sc.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("Por favor, ingresa un numero de opcion.");
                    continue;
                }
                
                int opcion = Integer.parseInt(input);

                switch (opcion) {
                    case 1 -> crearGrupo(sc);
                    case 2 -> unirseGrupo(sc);
                    case 3 -> enviarMensajeGrupo(sc);
                    case 4 -> enviarMensajeVoz(sc, true);
                    case 5 -> salirGrupo(sc);
                    case 6 -> { return; }
                    default -> System.out.println("Opcion invalida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Por favor ingresa un numero valido (1-6).");
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
        System.out.print("Direccion IP del contacto: ");
        String ip = sc.nextLine();
        contactos.put(nombre, ip);
        System.out.println("Contacto agregado correctamente.");
    }

    private void verContactos() {
        System.out.println("\n--- Contactos guardados ---");
        if (contactos.isEmpty()) {
            System.out.println("(No hay contactos aun)");
        } else {
            contactos.forEach((nombre, ip) -> System.out.println(nombre + " -> " + ip));
        }
    }

    // Enviar mensaje privado
    private void enviarMensaje(Scanner sc) {
        System.out.print("A quien deseas enviar el mensaje? (nombre o IP): ");
        String destino = sc.nextLine();

        // Buscar IP si el usuario escribio un nombre
        String ipDestino = contactos.getOrDefault(destino, destino);

        System.out.print("Mensaje: ");
        String mensaje = sc.nextLine();

        // Formato: DESTINO_IP|MENSAJE
        salida.println(ipDestino + "|" + mensaje);
    }

    // Cerrar conexion
    private void cerrarConexion() {
        try {
            salida.println("exit");
            socket.close();
            System.out.println("Conexion cerrada.");
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error al cerrar conexion: " + e.getMessage());
        }
    }

    // Metodos para manejar audio
    private void menuVoz(Scanner sc) {
        System.out.println("\n=== Enviar Nota de Voz ===");
        System.out.println("1. Enviar a un usuario");
        System.out.println("2. Enviar a un grupo");
        System.out.print("Opcion: ");
        
        try {
            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Opcion invalida. Volviendo al menu principal.");
                return;
            }
            
            int opcion = Integer.parseInt(input);
            
            switch (opcion) {
                case 1 -> enviarMensajeVoz(sc, false);
                case 2 -> enviarMensajeVoz(sc, true);
                default -> System.out.println("Opcion invalida.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Por favor ingresa 1 o 2.");
        }
    }

    private void enviarMensajeVoz(Scanner sc, boolean esGrupo) {
        try {
            System.out.print("Duracion de la nota de voz (segundos, maximo 30): ");
            int duracion = Integer.parseInt(sc.nextLine());
            duracion = Math.min(duracion, 30);

            String destino;
            if (esGrupo) {
                System.out.print("Nombre del grupo: ");
                destino = sc.nextLine();
            } else {
                System.out.print("A quien deseas enviar la nota de voz? (nombre o IP): ");
                destino = sc.nextLine();
                destino = contactos.getOrDefault(destino, destino);
            }

            byte[] audioData = grabarAudio(duracion);
            if (audioData.length == 0) {
                System.err.println("No se pudo grabar el audio.");
                return;
            }

            // Enviar metadata primero
            if (esGrupo) {
                salida.println("@vozgrupo|" + destino + "|" + audioData.length);
            } else {
                salida.println("@voz|" + destino + "|" + audioData.length);
            }
            
            // Pequena pausa para sincronizacion
            Thread.sleep(50);
            
            // Enviar datos de audio
            outputStream.write(audioData);
            outputStream.flush();
            
            System.out.println("Nota de voz enviada correctamente a: " + destino);
            
        } catch (Exception e) {
            System.err.println("Error al grabar/enviar audio: " + e.getMessage());
        }
    }

    private byte[] grabarAudio(int duracionSegundos) {
        TargetDataLine microphone = null;
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true); // little-endian
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Línea de audio no soportada. Probando formato alternativo...");
                format = new AudioFormat(16000.0f, 16, 1, true, true);
                info = new DataLine.Info(TargetDataLine.class, format);
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            
            System.out.println("Grabando... (durante " + duracionSegundos + " segundos)");
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (duracionSegundos * 1000);
            
            // Barra de progreso
            Thread progressThread = new Thread(() -> {
                try {
                    int progress = 0;
                    while (progress < 100) {
                        long currentTime = System.currentTimeMillis();
                        progress = (int) ((currentTime - startTime) * 100 / (duracionSegundos * 1000));
                        progress = Math.min(100, progress);
                        
                        int bars = progress / 5;
                        String bar = "[" + "=".repeat(bars) + " ".repeat(20 - bars) + "]";
                        System.out.print("\rProgreso: " + bar + " " + progress + "%");
                        
                        Thread.sleep(100);
                    }
                    System.out.println("\rProgreso: [====================] 100% Listo");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            progressThread.start();
            
            while (System.currentTimeMillis() < endTime) {
                int bytesRead = microphone.read(buffer, 0, Math.min(buffer.length, microphone.available()));
                if (bytesRead > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            
            microphone.stop();
            microphone.close();
            progressThread.interrupt(); // Interrumpir el hilo de progreso por si acaso
            
            byte[] audioData = baos.toByteArray();
            System.out.println("Audio grabado: " + audioData.length + " bytes");
            
            return audioData;
            
        } catch (Exception e) {
            System.err.println("Error al grabar audio: " + e.getMessage());
            return new byte[0];
        } finally {
            if (microphone != null) {
                microphone.close();
            }
        }
    }

    // Método mejorado para reproducir audio
    private void recibirYReproducirAudio(byte[] audioData) {
        try {
            // Usar el mismo formato que en la grabación
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true); // little-endian
            
            // Crear un SourceDataLine para mejor control
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            // Reproducir los datos
            int bufferSize = 4096;
            int offset = 0;
            
            while (offset < audioData.length) {
                int remaining = audioData.length - offset;
                int chunkSize = Math.min(bufferSize, remaining);
                line.write(audioData, offset, chunkSize);
                offset += chunkSize;
            }
            
            // Esperar a que termine la reproducción
            line.drain();
            line.close();
            
            System.out.println("Audio reproducido exitosamente");
            
        } catch (Exception e) {
            System.err.println("Error al reproducir el audio: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: intentar con un formato más básico
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(audioData));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                
                // Esperar a que termine
                while (clip.isRunning()) {
                    Thread.sleep(100);
                }
                clip.close();
                
                System.out.println("Audio reproducido (fallback)");
            } catch (Exception ex) {
                System.err.println("Error también en fallback: " + ex.getMessage());
            }
        }
    }

    // Hilo receptor mejorado
    private class Receptor implements Runnable {
        @Override
        public void run() {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    if (mensaje.startsWith("@voz|")) {
                        manejarAudioRecibido(mensaje, false);
                    } else if (mensaje.startsWith("@vozgrupo|")) {
                        manejarAudioRecibido(mensaje, true);
                    } else {
                        System.out.println("\n" + mensaje);
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexion terminada: " + e.getMessage());
            }
        }
        
        private void manejarAudioRecibido(String metadata, boolean esGrupo) {
            try {
                String[] partes = metadata.split("\\|");
                if (partes.length < (esGrupo ? 4 : 3)) {
                    System.err.println("Mensaje de audio mal formado: " + metadata);
                    return;
                }
                String remitente = esGrupo ? partes[2] : partes[1];
                String grupo = esGrupo ? partes[1] : null;
                int tamanoAudio = Integer.parseInt(partes[esGrupo ? 3 : 2]);

                String contexto = esGrupo ? "en grupo " + grupo + " de " : "de ";
                System.out.println("\nRecibiendo nota de voz " + contexto + remitente + "...");

                // Leer datos de audio de manera robusta
                byte[] audioData = new byte[tamanoAudio];
                int totalLeido = 0;
                
                while (totalLeido < tamanoAudio) {
                    int bytesLeidos = dataInput.read(audioData, totalLeido, tamanoAudio - totalLeido);
                    if (bytesLeidos == -1) {
                        throw new EOFException("Conexion interrumpida durante recepcion de audio");
                    }
                    totalLeido += bytesLeidos;
                }

                System.out.println("Audio recibido completamente: " + totalLeido + " bytes");
                
                // Reproducir en un hilo separado con un pequeño delay para evitar conflictos
                new Thread(() -> {
                    try {
                        Thread.sleep(100); // Pequeño delay para estabilizar
                        System.out.println("Reproduciendo nota de voz...");
                        recibirYReproducirAudio(audioData);
                    } catch (Exception e) {
                        System.err.println("Error en hilo de reproducción: " + e.getMessage());
                    }
                }).start();
                
            } catch (Exception e) {
                System.err.println("Error al recibir audio: " + e.getMessage());
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
            System.out.print("Direccion del servidor [localhost]: ");
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