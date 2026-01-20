import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Sala {
    public String nombreSala;
    public int aforoMaximo;
    public List<String> listaNicks = new ArrayList<>();
    public List<PrintWriter> listaConexiones = new ArrayList<>();

    public Sala(String nombre, int aforo) {
        this.nombreSala = nombre;
        this.aforoMaximo = aforo;
    }
    
    public synchronized boolean añadirUsuario(String nickUsuario, PrintWriter flujoSalida) {
        if (listaNicks.size() < aforoMaximo) {
            listaNicks.add(nickUsuario);
            listaConexiones.add(flujoSalida);
            return true;
        }
        return false;
    }
}
