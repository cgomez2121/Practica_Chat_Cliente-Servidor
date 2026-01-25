import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * Esta clase gestiona la parte del usuario:
 * Lee la configuración de conexión (IP y puerto) desde 'chat.properties'.
 * Intenta conectar con el servidor (con sistema de reintentos).
 * Crea dos hilos de ejecución paralelos:
 * Uno para ESCUCHAR mensajes del servidor
 * Otro para ENVIAR lo que escribe el usuario
 */
public class ClienteChat {

    /**
     * Método principal del cliente.
     * Inicia el proceso de conexión y lanza los hilos de comunicación.
     */
    public static void main(String[] args) {
        String host = null;
        int puerto = 0;

        // LEER ARCHIVO PROPIEDADES
        try (FileInputStream fis = new FileInputStream("chat.properties")) {
            Properties p = new Properties();
            p.load(fis);
            host = p.getProperty("servidor.ip");
            puerto = Integer.parseInt(p.getProperty("servidor.puerto"));
        } catch (IOException e) {
            System.err.println("ERROR: No se pudo leer chat.properties");
            return;
        }

        Socket s = null;
        boolean conectado = false;
        int intentos = 0;

        // BUCLE DE CONEXIÓN (5 intentos, 10s espera)
        while (!conectado && intentos < 5) {
            try {
                s = new Socket(host, puerto);
                conectado = true;
            } catch (IOException e) {
                intentos++;
                System.out.println("Fallo al conectar a " + host + ":" + puerto +
                        " (Intento " + intentos + "/5). Reintentando en 10s...");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    e.printStackTrace();
                }
            }
        }

        if (!conectado) {
            System.out.println("Imposible conectar tras 5 intentos. Cerrando.");
            System.exit(1);
            return;
        }

        // LOGICA DEL CHAT
        try (Socket socket = s) {
            // Hilo Lector: Se encarga de recibir mensajes del servidor y mostrarlos en pantalla, elaborado con expresión lambda
            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String mensajeRecibido;
                    while ((mensajeRecibido = in.readLine()) != null)
                        System.out.println(mensajeRecibido);
                } catch (IOException e) {
                    System.out.println("Conexión con el servidor finalizada.");
                }
            }).start();

            // Hilo Escritor: Se encarga de leer lo que escribe el usuario y mandarlo al servidor, es el propio main para aprovechar recursos
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in);
            while (true) {
                String entrada = sc.nextLine();
                out.println(entrada);
                if (entrada.equalsIgnoreCase("/abandona"))
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
