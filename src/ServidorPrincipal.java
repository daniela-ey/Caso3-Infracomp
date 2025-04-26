import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.HashMap;
import java.util.Map;

public class ServidorPrincipal {

    private static final int PUERTO = 3400;

    private ServerSocket servidorSocket;
    private PrivateKey llavePrivada;
    private PublicKey llavePublica;
    private Map<String, String[]> tablaServicios;

    public ServidorPrincipal(PublicKey llavePublica, PrivateKey llavePrivada ) throws Exception {
        // 1. Inicializar servidor
        this.servidorSocket = new ServerSocket(PUERTO);

        // 2. Cargar llaves
        this.llavePrivada =llavePrivada;
        this.llavePublica = llavePublica;

        // 3. Cargar tabla de servicios
        this.tablaServicios = new HashMap<>();
        tablaServicios.put("1", new String[]{"Estado vuelo", "192.168.0.10", "9001"});
        tablaServicios.put("2", new String[]{"Disponibilidad vuelos", "192.168.0.11", "9002"});
        tablaServicios.put("3", new String[]{"Costo de un vuelo", "192.168.0.12", "9003"});
    }

    // Método para escuchar conexiones
    public void atenderClientes() throws Exception {
        System.out.println("Servidor principal escuchando en puerto " + PUERTO);
        while (true) {
            Socket socketCliente = servidorSocket.accept();
            System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());

            ServidorDelegado delegado = new ServidorDelegado(socketCliente, tablaServicios, llavePublica, llavePrivada);
            delegado.start();
        }
    }


}





