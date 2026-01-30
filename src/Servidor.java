import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Clase principal del servidor de Chat.
 * Se encarga de inicializar la configuración, gestionar conexiones y mantener el estado global.
 */
public class Servidor {
    private final static Map<String, Sala> registroSalas = new HashMap<>();
    private final static List<String> listaAdministradores = new ArrayList<>();
    private final static ArrayList<ManejadorCliente> clientesConectados = new ArrayList<>();
    private static String contrasenaAdministrador;
    private static String rutaArchivoLog;
    private static int aforoMaximoPermitido;
    private static int aforoInicialPorDefecto;


    public static Map<String, Sala> getRegistroSalas() {
        return registroSalas;
    }

    public static List<String> getListaAdministradores() {
        return listaAdministradores;
    }
    public static ArrayList<ManejadorCliente> getClientesConectados() {
        return clientesConectados;
    }

    public static String getContrasenaAdministrador() {
        return contrasenaAdministrador;
    }
    public static int getAforoMaximoPermitido() {
        return aforoMaximoPermitido;
    }

    public static int getAforoInicialPorDefecto() {
        return aforoInicialPorDefecto;
    }

    public static void main(String[] args) {
        Properties configuracion = new Properties();

        try (FileInputStream archivoConfig = new FileInputStream("chat.properties")) {
            configuracion.load(archivoConfig);

            int puertoServidor = Integer.parseInt(configuracion.getProperty("servidor.puerto", "1234"));
            int maximoHilosSimultaneos = Integer.parseInt(configuracion.getProperty("servidor.numero.maximo.threads", "100"));

            // Lectura de configuración de aforos
            aforoMaximoPermitido = Integer.parseInt(configuracion.getProperty("sala.aforo.maximo", "10"));
            aforoInicialPorDefecto = Integer.parseInt(configuracion.getProperty("sala.aforo.creacion", "5"));

            // Configuración general
            rutaArchivoLog = configuracion.getProperty("archivo.log", "servidor.log");
            contrasenaAdministrador = configuracion.getProperty("admin.password", "1234");

            // Carga de administradores desde el properties
            String adminsTexto = configuracion.getProperty("admins", "");
            if (!adminsTexto.isEmpty()) {
                String[] arrayAdmins = adminsTexto.split(",");
                for (String admin : arrayAdmins) listaAdministradores.add(admin.trim());
            }

            // Inicialización de salas base con el aforo inicial por defecto
            registroSalas.put("recepcion", new Sala("recepcion", aforoInicialPorDefecto));
            registroSalas.put("jiuston", new Sala("jiuston", aforoInicialPorDefecto));

            // Arranque del servidor
            try (ServerSocket socketServidor = new ServerSocket(puertoServidor);
                 ExecutorService gestorDeHilos = Executors.newFixedThreadPool(maximoHilosSimultaneos)) {

                System.out.println("Servidor iniciado en puerto " + puertoServidor);
                registrarLog("INICIO DEL SERVIDOR");

                while (!socketServidor.isClosed()) {

                    Socket socketCliente = socketServidor.accept();

                    ManejadorCliente nuevoCliente = new ManejadorCliente(socketCliente);

                    // Añadirlo a la lista segura de hilos
                    synchronized (clientesConectados) {
                        clientesConectados.add(nuevoCliente);
                    }

                    // Ejecutar el hilo
                    gestorDeHilos.execute(nuevoCliente);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica si un nombre de usuario ya está siendo usado en alguna sala.
     */
    public static synchronized boolean esNombreUsuarioDisponible(String nombreUsuario) {
        synchronized(registroSalas) {
            for (Sala sala : registroSalas.values()) {
                if (sala.getListaNicks().contains(nombreUsuario))
                    return false;
            }
        }
        return true;
    }

    /**
     * Escribe una línea en el archivo de registro (log) con fecha y hora.
     */
    public static synchronized void registrarLog(String mensaje) {
        try (PrintWriter escritor = new PrintWriter(new FileWriter(rutaArchivoLog, true))) {
            escritor.println("[" + new Date() + "] " + mensaje);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
