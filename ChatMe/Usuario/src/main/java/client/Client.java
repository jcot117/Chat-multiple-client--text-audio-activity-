package client;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private String nombreUsuario;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Map<String, String> contactos;

    public Client(String nombreUsuario, String direccionServidor, int puerto) {
        this.nombreUsuario = nombreUsuario;
        this.contactos = new HashMap<>();

        try {
            socket = new Socket(direccionServidor, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
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

    // Menú interactivo en consola
    private void menuPrincipal() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== Menú de Chat ===");
            System.out.println("1. Enviar mensaje");
            System.out.println("2. Ver contactos");
            System.out.println("3. Agregar contacto");
            System.out.println("4. Salir");
            System.out.print("Opción: ");
            int opcion = Integer.parseInt(sc.nextLine());

            switch (opcion) {
                case 1 -> enviarMensaje(sc);
                case 2 -> verContactos();
                case 3 -> agregarContacto(sc);
                case 4 -> cerrarConexion();
                default -> System.out.println("Opción inválida.");
            }
        }
    }

    // Agregar un contacto a la lista local
    private void agregarContacto(Scanner sc) {
        System.out.print("Nombre del contacto: ");
        String nombre = sc.nextLine();
        System.out.print("Dirección IP del contacto: ");
        String ip = sc.nextLine();
        contactos.put(nombre, ip);
        System.out.println("Contacto agregado correctamente ");
    }

    private void verContactos() {
        System.out.println("\n--- Contactos guardados ---");
        if (contactos.isEmpty()) {
            System.out.println("(No hay contactos aún)");
        } else {
            contactos.forEach((nombre, ip) -> System.out.println(nombre + " -> " + ip)); //Generado por ChatGPT
        }
    }

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

    // Hilo que escucha mensajes entrantes del servidor
    private class Receptor implements Runnable {
        @Override
        public void run() {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    System.out.println("\nNuevo mensaje: " + mensaje);
                }
            } catch (IOException e) {
                System.err.println("Conexión terminada: " + e.getMessage());
            }
        }
    }

    //Main
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Tu nombre de usuario: ");
        String nombre = sc.nextLine();
        System.out.print("Dirección del servidor: ");
        String direccion = sc.nextLine();
        System.out.print("Puerto: ");
        int puerto = Integer.parseInt(sc.nextLine());

        new Client(nombre, direccion, puerto);
    }
}
