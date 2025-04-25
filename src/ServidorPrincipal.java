import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class ServidorPrincipal {
    private static final int PUERTO = 9090;
    private static Map<String, String> servicios = new HashMap<>();
    private static PublicKey llavePublica;
    private static PrivateKey llavePrivada;

    public ServidorPrincipal(PublicKey llavePublica, PrivateKey llavePrivada) {
        this.llavePublica = llavePublica;       
        this.llavePrivada = llavePrivada;
        cargarServicios();
    }

    public static void main(String[] args) throws Exception {
        cargarServicios();
        ServerSocket servidor = new ServerSocket(PUERTO);
        System.out.println("Servidor esperando conexiones...");

        while (true) {
            
            Socket cliente = servidor.accept();
            
            new Thread(new ServidorDelegado(cliente, servicios, llavePublica, llavePrivada)).start();
           
        }
    }

    private static void cargarServicios() {
        servicios.put("1", "192.168.0.10:9001");
        servicios.put("2", "192.168.0.11:9002");
        // más servicios...
    }
}


