import java.net.ServerSocket;
import java.net.Socket;

public class ServidorPrincipal {

    private int puerto;

    public ServidorPrincipal(int puerto) {
        this.puerto = puerto;
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor escuchando en el puerto " + puerto);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socket.getInetAddress());

                // Cada conexión se maneja en un nuevo hilo
                new Thread(new DelegadoServidor(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
