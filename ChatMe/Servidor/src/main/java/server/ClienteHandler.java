package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ClienteHandler implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private String nombreUsuario;
        private String direccionIP;
        private InputStream inputStream;
        private OutputStream outputStream;
        private int puertoLlamada;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
            this.direccionIP = socket.getInetAddress().getHostAddress();
        }
        
        public Socket getSocket() {
            return socket;
        }
        
        public int getPuertoLlamada() {
            return puertoLlamada;
        }

        @Override
        public void run() {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                entrada = new BufferedReader(new InputStreamReader(inputStream));
                salida = new PrintWriter(outputStream, true);

                nombreUsuario = entrada.readLine();
                Server.getClientesConectados().put(nombreUsuario, this);
                System.out.println("Usuario conectado: " + nombreUsuario + " (" + direccionIP + ")");

                String linea;
                while ((linea = entrada.readLine()) != null) {
                    if (linea.equalsIgnoreCase("exit")) break;

                    // Comandos de configuración
                    if (linea.startsWith("@config|")) {
                        manejarConfiguracion(linea);
                        continue;
                    }

                    // Comandos de llamada
                    if (linea.startsWith("@llamada|")) {
                        manejarComandoLlamada(linea);
                        continue;
                    }

                    // Comandos de grupo
                    if (linea.startsWith("@grupo|")) {
                        manejarComandoGrupo(linea);
                        continue;
                    }

                    // Mensajes de voz
                    if (linea.startsWith("@voz|")) {
                        manejarVozPrivada(linea);
                        continue;
                    }

                    if (linea.startsWith("@vozgrupo|")) {
                        manejarVozGrupo(linea);
                        continue;
                    }

                    // Mensaje privado normal
                    String[] partes = linea.split("\\|", 2);
                    if (partes.length < 2) continue;

                    String destino = partes[0];
                    String mensaje = partes[1];
                    String registro = "[" + nombreUsuario + " -> " + destino + "] " + mensaje;

                    System.out.println(registro);
                    Server.guardarHistorial(registro);

                    Server.enviarMensaje(destino, "De " + nombreUsuario + ": " + mensaje);
                }

            } catch (IOException e) {
                System.err.println("Error con el cliente " + nombreUsuario + ": " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }

        private void manejarConfiguracion(String comando) {
            String[] partes = comando.split("\\|");
            if (partes.length < 3) return;
            
            String tipo = partes[1];
            String valor = partes[2];
            
            if (tipo.equals("puerto_llamada")) {
                try {
                    puertoLlamada = Integer.parseInt(valor);
                    System.out.println("Usuario " + nombreUsuario + " configurado para llamadas en puerto: " + puertoLlamada);
                } catch (NumberFormatException e) {
                    System.err.println("Puerto de llamada inválido: " + valor);
                }
            }
        }

        private void manejarComandoLlamada(String comando) {
            String[] partes = comando.split("\\|");
            if (partes.length < 3) return;

            String accion = partes[1];
            String destino = partes[2];

            switch (accion.toLowerCase()) {
                case "solicitar" -> Server.manejarSolicitudLlamada(nombreUsuario, destino);
                case "aceptar" -> Server.manejarAceptacionLlamada(destino, nombreUsuario);
                case "rechazar" -> Server.manejarRechazoLlamada(destino, nombreUsuario);
            }
        }

        private void manejarComandoGrupo(String comando) {
            String[] partes = comando.split("\\|");
            if (partes.length < 3) return;

            String accion = partes[1];
            String nombreGrupo = partes[2];

            switch (accion.toLowerCase()) {
                case "crear" -> {
                    Server.getGrupos().putIfAbsent(nombreGrupo, ConcurrentHashMap.newKeySet());
                    Server.getGrupos().get(nombreGrupo).add(nombreUsuario);
                    salida.println("Grupo '" + nombreGrupo + "' creado y unido correctamente.");
                }
                case "unir" -> {
                    Server.getGrupos().putIfAbsent(nombreGrupo, ConcurrentHashMap.newKeySet());
                    Server.getGrupos().get(nombreGrupo).add(nombreUsuario);
                    salida.println("Te uniste al grupo '" + nombreGrupo + "'.");
                }
                case "salir" -> {
                    Set<String> miembros = Server.getGrupos().get(nombreGrupo);
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
                    Server.enviarAGrupo(nombreGrupo, mensaje, nombreUsuario);
                    Server.guardarHistorial("[Grupo " + nombreGrupo + "] " + nombreUsuario + ": " + mensaje);
                }
                default -> salida.println("Comando de grupo no reconocido.");
            }
        }

        private void manejarVozPrivada(String metadata) {
            try {
                String[] partes = metadata.split("\\|");
                String destino = partes[1];
                int tamanoAudio = Integer.parseInt(partes[2]);

                byte[] audioData = new byte[tamanoAudio];
                int bytesRead = 0;
                while (bytesRead < tamanoAudio) {
                    int result = inputStream.read(audioData, bytesRead, tamanoAudio - bytesRead);
                    if (result == -1) break;
                    bytesRead += result;
                }

                Server.enviarAudio(destino, audioData, "@voz|" + nombreUsuario + "|" + audioData.length);
                Server.guardarHistorial("[Voz] " + nombreUsuario + " -> " + destino);
                System.out.println("Nota de voz enviada de " + nombreUsuario + " a " + destino);
        
            } catch (IOException e) {
                System.err.println("Error al manejar voz privada: " + e.getMessage());
            }
        }

        private void manejarVozGrupo(String metadata) {
            try {
                String[] partes = metadata.split("\\|");
                String grupo = partes[1];
                int tamanoAudio = Integer.parseInt(partes[2]);

                byte[] audioData = new byte[tamanoAudio];
                int bytesRead = 0;
                while (bytesRead < tamanoAudio) {
                    int result = inputStream.read(audioData, bytesRead, tamanoAudio - bytesRead);
                    if (result == -1) break;
                    bytesRead += result;
                }

                Server.enviarAudioAGrupo(grupo, audioData, nombreUsuario);
                Server.guardarHistorial("[Voz-Grupo " + grupo + "] " + nombreUsuario);
                System.out.println("Nota de voz enviada al grupo " + grupo + " por " + nombreUsuario);
            } catch (IOException e) {
                System.err.println("Error al manejar voz grupal: " + e.getMessage());
            }
        }

        public void enviar(String mensaje) {
            salida.println(mensaje);
        }

        public void enviarAudio(String destino, byte[] audioData, String metadata) {
            try {
                salida.println(metadata);
                outputStream.write(audioData);
                outputStream.flush();
            } catch (IOException e) {
                System.err.println("Error al enviar audio: " + e.getMessage());
            }
        }

        private void cerrarConexion() {
            try {
                if (nombreUsuario != null) {
                    System.out.println("Usuario desconectado: " + nombreUsuario);
                }
                socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }