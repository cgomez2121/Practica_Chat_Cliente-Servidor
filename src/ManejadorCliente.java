import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hilo encargado de gestionar la comunicación individual con un cliente conectado.
 */
public class ManejadorCliente implements Runnable {
    private final Socket socketCliente;
    private final long marcaTiempoConexion;
    private String nombreUsuario;
    private Sala salaActualUsuario;
    private boolean esUsuarioAdministrador;

    public ManejadorCliente(Socket socket) {
        this.socketCliente = socket;
        this.marcaTiempoConexion = System.currentTimeMillis();
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public Socket getSocketCliente() {
        return socketCliente;
    }

    public Sala getSalaActualUsuario() {
        return salaActualUsuario;
    }

    public long getMarcaTiempoConexion() {
        return marcaTiempoConexion;
    }

    public void setSalaActualUsuario(Sala nuevaSala) {
        this.salaActualUsuario = nuevaSala;
    }

    @Override
    public void run() {
        try (
                PrintWriter flujoSalida = new PrintWriter(socketCliente.getOutputStream(), true);
                BufferedReader flujoEntrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()))
        ) {
            // PROCESO DE LOGIN
            while (true) {
                flujoSalida.println("SISTEMA: Introduce tu nombre de usuario:");
                nombreUsuario = flujoEntrada.readLine();

                if (nombreUsuario == null)
                    return; // Cliente cerró la ventana o perdio la conexion antes de poner el nombre, desconectamos el hilo
                nombreUsuario = nombreUsuario.trim();

                if (!nombreUsuario.isEmpty() && Servidor.esNombreUsuarioDisponible(nombreUsuario)) {
                    break; // Nombre válido y libre, entra al chat
                }
                flujoSalida.println("ERROR: El nombre ya está en uso o es inválido.");
            }

            // VERIFICACIÓN DE ROL (ADMINISTRADOR)
            if (Servidor.getListaAdministradores().contains(nombreUsuario)) {
                flujoSalida.println("SISTEMA: Usuario Administrador detectado. Introduce contraseña:");
                String passwordIngresada = flujoEntrada.readLine();

                if (passwordIngresada != null && passwordIngresada.equals(Servidor.getContrasenaAdministrador())) {
                    esUsuarioAdministrador = true;
                    flujoSalida.println("SISTEMA: Acceso concedido como ADMINISTRADOR.");
                } else {
                    flujoSalida.println("ERROR: Contraseña incorrecta. Accediendo como usuario BÁSICO.");
                    esUsuarioAdministrador = false;
                }
            } else {
                esUsuarioAdministrador = false;
            }

            // ASIGNACIÓN DE SALA INICIAL
            String nombreSalaInicio = esUsuarioAdministrador ? "jiuston" : "recepcion";
            unirseASala(nombreSalaInicio, flujoSalida);
            Servidor.registrarLog("CONEXIÓN: " + nombreUsuario);

            // BUCLE PRINCIPAL DE COMANDOS
            String textoRecibido;
            while ((textoRecibido = flujoEntrada.readLine()) != null) {
                if (textoRecibido.startsWith("/")) {
                    procesarComando(textoRecibido, flujoSalida);
                    if (textoRecibido.equalsIgnoreCase("/abandona")) {
                        break;
                    }
                } else {
                    // Bloqueo de escritura directa (obligatorio usar /mensaje)
                    flujoSalida.println("Aviso: Usa /mensaje <texto> para hablar en la sala.");
                }
            }
        } catch (IOException e) {
            // Error de conexión o desconexión abrupta
            e.printStackTrace();
        } finally {
            desconectarCliente();
        }
    }

    /**
     * Analiza y ejecuta los comandos introducidos por el usuario.
     * Identifica el comando principal, separa los argumentos y llama a la lógica correspondiente.
     * lineaCompleta es la línea de texto íntegra recibida desde el cliente.
     * flujoSalida es el flujo de escritura para enviar la respuesta al cliente.
     */
    private void procesarComando(String lineaCompleta, PrintWriter flujoSalida) {
        String[] palabrasMensaje = lineaCompleta.split(" ");
        String comando = palabrasMensaje[0].toLowerCase();

        switch (comando) {
            case "/ayuda":
                flujoSalida.println("\n=== MANUAL DE USUARIO ===");
                flujoSalida.println("Uso: <parametro_obligatorio> [parametro_opcional]");
                flujoSalida.println("-------------------------------------------------");
                flujoSalida.println("/ayuda                  : Muestra este manual.");
                flujoSalida.println("/quien_soy              : Muestra información de tu sesión.");
                flujoSalida.println("/salas                  : Lista las salas disponibles.");
                flujoSalida.println("/usuarios <sala>        : Lista los usuarios conectados en una sala.");
                flujoSalida.println("/unirse <sala>          : Te mueve a la sala indicada.");
                flujoSalida.println("/ping <usuario>         : Comprueba si un usuario está en tu sala.");
                flujoSalida.println("/mensaje <texto>        : Envía un mensaje PÚBLICO a la sala.");
                flujoSalida.println("/mensaje <txt> <usu>    : Envía un mensaje PRIVADO al usuario.");
                flujoSalida.println("/abandona               : Cierra la sesión y desconecta.");

                if (esUsuarioAdministrador) {
                    flujoSalida.println("\n=== COMANDOS DE ADMINISTRADOR ===");
                    flujoSalida.println("/crea <sala> [aforo]    : Crea una sala nueva (Aforo por defecto: " + Servidor.getAforoInicialPorDefecto() + ").");
                    flujoSalida.println("/elimina <sala>         : Elimina una sala si está vacía.");
                    flujoSalida.println("/elimina_forzado <sala> : Elimina sala y mueve usuarios a Recepción.");
                    flujoSalida.println("/cambia_aforo <sala> <n>: Modifica la capacidad máxima (Max: " + Servidor.getAforoMaximoPermitido() + ").");
                    flujoSalida.println("/expulsa <usu> [motivo] : Expulsa a un usuario del servidor.");
                    flujoSalida.println("/info_sala <sala>       : Muestra datos técnicos de una sala.");
                    flujoSalida.println("/info_usuario <usu>     : Muestra datos técnicos de un usuario.");
                    flujoSalida.println("/mensaje_todos <txt>    : Envía un mensaje global a TODAS las salas.");
                    flujoSalida.println("/apaga                  : Apaga el servidor inmediatamente.");
                }
                flujoSalida.println("=================================================\n");
                break;

            case "/quien_soy":
                long segundosConectado = (System.currentTimeMillis() - marcaTiempoConexion) / 1000;
                flujoSalida.println("INFO: " + nombreUsuario + " | Rol: " + (esUsuarioAdministrador ? "Admin" : "Básico") +
                        " | Sala: " + salaActualUsuario.getNombre() + " | Tiempo: " + segundosConectado + "s");
                break;

            case "/salas":
                flujoSalida.println("Salas disponibles:");
                synchronized(Servidor.getRegistroSalas()) {
                    for (Sala sala : Servidor.getRegistroSalas().values()) {
                        if (!sala.getNombre().equals("jiuston") || esUsuarioAdministrador) {
                            flujoSalida.println("- " + sala.getNombre() + " (" + sala.getListaNicks().size() + " usu)");
                        }
                    }
                }
                break;

            case "/usuarios":
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <sala>.");
                } else {
                    Sala salaObjetivo = Servidor.getRegistroSalas().get(palabrasMensaje[1].toLowerCase());
                    if (salaObjetivo != null)
                        flujoSalida.println("Usuarios en " + palabrasMensaje[1] + ": " + salaObjetivo.getListaNicks());
                    else
                        flujoSalida.println("Aviso: La sala no existe.");
                }
                break;

            case "/unirse":
                if (palabrasMensaje.length < 2)
                    flujoSalida.println("Aviso: Falta <sala>.");
                else
                    unirseASala(palabrasMensaje[1], flujoSalida);
                break;

            case "/ping":
                if (palabrasMensaje.length < 2)
                    flujoSalida.println("Aviso: Falta <usuario>.");
                else if (salaActualUsuario.getListaNicks().contains(palabrasMensaje[1]))
                    flujoSalida.println("PONG: " + palabrasMensaje[1] + " está aquí.");
                else
                    flujoSalida.println("Aviso: El usuario no está en tu sala.");
                break;

            case "/mensaje":
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <mensaje>.");
                    break;
                }

                // Comprobar si la última palabra es un USUARIO REGISTRADO EN EL SERVIDOR
                String ultimaPalabra = palabrasMensaje[palabrasMensaje.length - 1];
                boolean esUsuarioRegistrado = !Servidor.esNombreUsuarioDisponible(ultimaPalabra);

                if (esUsuarioRegistrado) {
                    // MODO PRIVADO: La última palabra es un usuario conocido
                    String destinatario = ultimaPalabra;

                    if (destinatario.equals(nombreUsuario)) {
                        // No te mandes mensajes a ti mismo
                        flujoSalida.println("Aviso: No te escribas a ti mismo.");
                    }
                    else if (salaActualUsuario.getListaNicks().contains(destinatario)) {
                        // Usuario existe Y está en la sala -> Mensaje Privado

                        // Verificamos si hay texto antes del usuario
                        if (palabrasMensaje.length < 3) {
                            flujoSalida.println("Aviso: Falta <mensaje> para el usuario " + destinatario);
                        } else {
                            // Extraemos el mensaje (desde el comando hasta el usuario final)
                            String mensajePrivado = lineaCompleta.substring(comando.length(), lineaCompleta.lastIndexOf(destinatario)).trim();
                            salaActualUsuario.enviarPrivado(nombreUsuario, destinatario, mensajePrivado);
                        }
                    }
                    else {
                        // Si existe pero en otra sala -> "Se trata como si no existiera"
                        flujoSalida.println("Aviso: El usuario '" + destinatario + "' no existe en esta sala.");
                    }
                }
                else {
                    // MODO PÚBLICO: La última palabra NO es un usuario
                    // Se envía todo el contenido a la sala
                    String mensajePublico = lineaCompleta.substring(comando.length()).trim();
                    salaActualUsuario.difundirMensaje(nombreUsuario, mensajePublico);
                }
                break;

            case "/abandona":
                flujoSalida.println("Desconectando...");
                break;

            // --- COMANDOS EXCLUSIVOS DE ADMINISTRADOR ---

            case "/crea":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <sala>.");
                    break;
                }

                synchronized(Servidor.getRegistroSalas()) {
                    String nombreNuevaSala = palabrasMensaje[1].toLowerCase();
                    if (Servidor.getRegistroSalas().containsKey(nombreNuevaSala)) {
                        flujoSalida.println("Aviso: La sala ya existía.");
                    } else {
                        // Determinar aforo: Si se especifica se usa, si no, el defecto
                        int aforoSala = (palabrasMensaje.length > 2) ? Integer.parseInt(palabrasMensaje[2]) : Servidor.getAforoInicialPorDefecto();

                        // Validar límite máximo del servidor
                        if (aforoSala > Servidor.getAforoMaximoPermitido()) {
                            flujoSalida.println("Aviso: El aforo se ha limitado al máximo permitido (" + Servidor.getAforoMaximoPermitido() + ").");
                            aforoSala = Servidor.getAforoMaximoPermitido();
                        }

                        Servidor.getRegistroSalas().put(nombreNuevaSala, new Sala(nombreNuevaSala, aforoSala));
                        flujoSalida.println("SISTEMA: Sala creada con aforo " + aforoSala + ".");
                    }
                }
                break;

            case "/elimina":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <sala>.");
                    break;
                }
                if (palabrasMensaje[1].equals("recepcion") || palabrasMensaje[1].equals("jiuston")) {
                    flujoSalida.println("Aviso: Sala protegida.");
                    break;
                }

                synchronized(Servidor.getRegistroSalas()) {
                    Sala salaParaEliminar = Servidor.getRegistroSalas().get(palabrasMensaje[1].toLowerCase());
                    if (salaParaEliminar != null && salaParaEliminar.getListaNicks().isEmpty()) {
                        Servidor.getRegistroSalas().remove(palabrasMensaje[1].toLowerCase());
                        flujoSalida.println("SISTEMA: Sala eliminada.");
                    } else
                        flujoSalida.println("Aviso: Sala ocupada o inexistente.");
                }
                break;

            case "/cambia_aforo":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 3) {
                    flujoSalida.println("Aviso: Faltan datos.");
                    break;
                }

                Sala salaModificar = Servidor.getRegistroSalas().get(palabrasMensaje[1].toLowerCase());
                if (salaModificar != null) {
                    int nuevoAforo = Integer.parseInt(palabrasMensaje[2]);

                    if (nuevoAforo > Servidor.getAforoMaximoPermitido()) {
                        flujoSalida.println("Aviso: Limitado al máx (" + Servidor.getAforoMaximoPermitido() + ").");
                        nuevoAforo = Servidor.getAforoMaximoPermitido();
                    }

                    if (nuevoAforo > 0 && nuevoAforo >= salaModificar.getListaNicks().size()) {
                        salaModificar.setCapacidadAforo(nuevoAforo);
                        flujoSalida.println("SISTEMA: Aforo cambiado.");
                    } else
                        flujoSalida.println("Aviso: Aforo inválido o menor que usuarios actuales.");
                }
                break;

            case "/elimina_forzado":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <sala>.");
                    break;
                }

                synchronized(Servidor.getRegistroSalas()) {
                    Sala salaEliminar = Servidor.getRegistroSalas().get(palabrasMensaje[1].toLowerCase());
                    Sala salaRecepcion = Servidor.getRegistroSalas().get("recepcion");

                    if (salaEliminar != null && salaRecepcion != null && !palabrasMensaje[1].equals("recepcion")) {
                        if (salaRecepcion.getListaNicks().size() + salaEliminar.getListaNicks().size() <= salaRecepcion.getCapacidadAforo()) {
                            // Mover usuarios
                            List<String> usuariosMovidos = new ArrayList<>(salaEliminar.getListaNicks());
                            List<PrintWriter> flujosMovidos = new ArrayList<>(salaEliminar.getListaFlujos());

                            for(int i=0; i<usuariosMovidos.size(); i++) {
                                salaRecepcion.intentarEntrar(usuariosMovidos.get(i), flujosMovidos.get(i));
                                flujosMovidos.get(i).println("SISTEMA: Sala eliminada. Usuarios movidos a Recepción.");
                            }

                            // Actualizar referencias en los hilos de cliente
                            synchronized(Servidor.getClientesConectados()) {
                                for(ManejadorCliente cliente : Servidor.getClientesConectados()) {
                                    if (cliente.getSalaActualUsuario() != null && cliente.getSalaActualUsuario().getNombre().equals(salaEliminar.getNombre())) {
                                        cliente.setSalaActualUsuario(salaRecepcion);
                                    }
                                }
                            }
                            Servidor.getRegistroSalas().remove(palabrasMensaje[1].toLowerCase());
                            flujoSalida.println("SISTEMA: Borrado forzado OK.");
                        } else
                            flujoSalida.println("Aviso: No caben en recepción.");
                    }
                }
                break;

            case "/expulsa":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <usuario>.");
                    break;
                }

                String usuarioAExpulsar = palabrasMensaje[1];
                String motivoExpulsion = (palabrasMensaje.length > 2) ? lineaCompleta.substring(lineaCompleta.indexOf(palabrasMensaje[2])) : "Sin motivo especificado";
                boolean fueExpulsado = false;

                synchronized(Servidor.getClientesConectados()) {
                    for (ManejadorCliente cliente : Servidor.getClientesConectados()) {
                        if (cliente.getNombreUsuario().equals(usuarioAExpulsar) && !cliente.esUsuarioAdministrador) {
                            try {
                                PrintWriter salidaCliente = new PrintWriter(cliente.getSocketCliente().getOutputStream(), true);
                                salidaCliente.println("\nSISTEMA: Has sido expulsado. Motivo: " + motivoExpulsion);
                                cliente.getSocketCliente().close();
                                fueExpulsado = true;
                            } catch (IOException e) { e.printStackTrace(); }
                            break;
                        }
                    }
                }
                if (fueExpulsado) {
                    flujoSalida.println("SISTEMA: Has expulsado a " + usuarioAExpulsar);
                    Servidor.registrarLog("EXPULSIÓN: " + usuarioAExpulsar + " | Motivo: " + motivoExpulsion);
                } else {
                    flujoSalida.println("Aviso: Usuario no encontrado o es Admin.");
                }
                break;

            case "/info_sala":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <sala>.");
                    break;
                }
                Sala infoSala = Servidor.getRegistroSalas().get(palabrasMensaje[1].toLowerCase());
                if (infoSala != null)
                    flujoSalida.println(infoSala.getInfo());
                else
                    flujoSalida.println("Aviso: No existe.");
                break;

            case "/info_usuario":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                if (palabrasMensaje.length < 2) {
                    flujoSalida.println("Aviso: Falta <usuario>.");
                    break;
                }
                boolean usuarioEncontrado = false;
                synchronized(Servidor.getClientesConectados()) {
                    for (ManejadorCliente cliente : Servidor.getClientesConectados()) {
                        if (cliente.getNombreUsuario().equals(palabrasMensaje[1])) {
                            long duracion = (System.currentTimeMillis() - cliente.getMarcaTiempoConexion()) / 1000;
                            flujoSalida.println("INFO " + palabrasMensaje[1] + ": Sala=" + cliente.getSalaActualUsuario().getNombre() + ", Tiempo=" + duracion + "s");
                            usuarioEncontrado = true;
                            break;
                        }
                    }
                }
                if (!usuarioEncontrado)
                    flujoSalida.println("Aviso: No conectado.");
                break;

            case "/mensaje_todos":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break; }
                if (palabrasMensaje.length > 1) {
                    String mensajeGlobal = lineaCompleta.substring(comando.length()).trim();
                    synchronized(Servidor.getRegistroSalas()) {
                        for (Sala sala : Servidor.getRegistroSalas().values())
                            sala.difundirMensaje("ADMIN-GLOBAL", mensajeGlobal);
                    }
                }
                break;

            case "/apaga":
                if (!esUsuarioAdministrador) {
                    flujoSalida.println("ERROR: Solo admin.");
                    break;
                }
                System.out.println("SISTEMA: Apagando servidor...");
                System.exit(0);
                break;

            default:
                flujoSalida.println("Comando desconocido.");
                break;
        }
    }

    /**
     * Intenta mover al usuario actual a una sala específica.
     * Realiza validaciones de seguridad (permisos de administrador) y de capacidad (aforo).
     * Si el cambio es exitoso, actualiza la referencia de la sala en el usuario y notifica el cambio.
     * nombreSala es el nombre de la sala destino a la que se quiere entrar.
     * flujoSalida es el flujo de escritura para enviar mensajes de éxito o error al usuario.
     */
    private void unirseASala(String nombreSala, PrintWriter flujoSalida) {
        nombreSala = nombreSala.toLowerCase();
        if (esUsuarioAdministrador && !nombreSala.equals("jiuston")) {
            flujoSalida.println("Aviso: Los administradores no pueden abandonar Jiuston.");
            return;
        }
        if (!esUsuarioAdministrador && nombreSala.equals("jiuston")) {
            flujoSalida.println("Aviso: Sala reservada a administradores.");
            return;
        }

        Sala nuevaSala = Servidor.getRegistroSalas().get(nombreSala);
        if (nuevaSala != null && nuevaSala.intentarEntrar(nombreUsuario, flujoSalida)) {
            if (salaActualUsuario != null) salaActualUsuario.eliminarUsuario(nombreUsuario);
            salaActualUsuario = nuevaSala;
            flujoSalida.println("SISTEMA: Bienvenido a " + nombreSala);
        } else {
            flujoSalida.println("Aviso: Sala llena o inexistente.");
        }
    }

    /**
     * Gestiona el cierre ordenado de la sesión del cliente.
     * Se encarga de:
     * 1. Eliminar al usuario de la lista de ocupantes de su sala actual.
     * 2. Eliminar el hilo de la lista global de clientes conectados del servidor.
     * 3. Registrar la desconexión en el log del sistema.
     * 4. Cerrar el socket de red de forma segura.
     */
    private void desconectarCliente() {
        if (salaActualUsuario != null)
            salaActualUsuario.eliminarUsuario(nombreUsuario);
        synchronized(Servidor.getClientesConectados()) {
            Servidor.getClientesConectados().remove(this);
        }
        Servidor.registrarLog("DESCONEXIÓN: " + nombreUsuario);
        try {
            socketCliente.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
