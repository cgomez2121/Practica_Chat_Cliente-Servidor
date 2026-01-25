import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa una sala de chat individual.
 * Se encarga de gestionar la lista de usuarios conectados a ella,
 * controlar el aforo máximo y difundir los mensajes (públicos o privados).
 */
public class Sala {
    private final String nombre;
    private int aforo;
    private final LocalDateTime fechaCreacion;
    private final List<String> nicks = new ArrayList<>();
    private final List<PrintWriter> flujos = new ArrayList<>();

    /**
     * Constructor de la clase Sala.
     * Inicializa la sala con un nombre y una capacidad máxima.
     * también registra la fecha y hora exacta de creación.
     * nombre es el nombre identificador de la sala (ej: "recepcion").
     * aforo es eñ número máximo de usuarios permitidos en una sala.
     */
    public Sala(String nombre, int aforo) {
        this.nombre = nombre;
        this.aforo = aforo;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Obtiene el nombre de la sala.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve la capacidad máxima actual de la sala.
     */
    public int getCapacidadAforo() {
        return aforo;
    }

    /**
     * Permite modificar el aforo máximo de la sala dinámicamente.
     * utilizado por el administrador con el comando /cambia_aforo.
     */
    public void setCapacidadAforo(int a) {
        this.aforo = a;
    }

    /**
     * Devuelve la lista de nombres de usuario (nicks) actualmente en la sala.
     */
    public List<String> getListaNicks() {
        return nicks;
    }

    /**
     * Devuelve la lista de flujos de escritura (PrintWriters) de los usuarios en la sala.
     */
    public List<PrintWriter> getListaFlujos() {
        return flujos;
    }

    /**
     * Intenta añadir un usuario a la sala.
     * Es synchronized para evitar conflictos si dos usuarios entran a la vez.
     * nick es el nombre del usuario que quiere entrar.
     * flujo es el flujo de salida para enviarle mensajes.
     * devuelve true si entró correctamente, false si la sala estaba llena.
     */
    public synchronized boolean intentarEntrar(String nick, PrintWriter flujo) {
        if (nicks.size() < aforo) {
            nicks.add(nick);
            flujos.add(flujo);
            return true;
        }
        return false;
    }

    /**
     * Elimina a un usuario de la sala y su flujo de comunicación.
     * nick es el nombre del usuario a eliminar.
     */
    public synchronized void eliminarUsuario(String nick) {
        int i = nicks.indexOf(nick);
        if (i != -1) {
            nicks.remove(i);
            flujos.remove(i);
        }
    }

    /**
     * Envía un mensaje público a todos los usuarios conectados a esta sala.
     * Recorre la lista de flujos y escribe el mensaje en cada uno.
     * emisor es quien envía el mensaje.
     * mensaje es el contenido del mensaje.
     */
    public synchronized void difundirMensaje(String emisor, String mensaje) {
        for (PrintWriter f : flujos) {
            f.println("[" + nombre + "] " + emisor + ": " + mensaje);
        }
    }

    /**
     * Envía un mensaje privado a un usuario específico dentro de esta sala.
     * Busca el índice del receptor y usa su flujo específico.
     * emisor es quien envía el mensaje.
     * receptor es quien debe recibirlo.
     * mensaje es el contenido del mensaje.
     */
    public synchronized void enviarPrivado(String emisor, String receptor, String mensaje) {
        int i = nicks.indexOf(receptor);
        if (i != -1) {
            flujos.get(i).println("[PRIVADO de " + emisor + "]: " + mensaje);
        }
    }

    /**
     * Genera una cadena de texto con información detallada de la sala.
     * Incluye nombre, fecha de creación y ocupación actual.
     */
    public String getInfo() {
        return "SALA: " + nombre + " | Creada: " +
                fechaCreacion.getDayOfMonth() + "/" + fechaCreacion.getMonthValue() + "/" + fechaCreacion.getYear() +  " | " +
                "Usuarios: " + nicks.size() + "/" + aforo;
    }
}

