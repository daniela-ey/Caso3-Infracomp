import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServidorPrincipal {

    private static final int PUERTO = 3400;
    private static final int CLIENTES_ESPERADOS = 1; // Número de clientes que quieres atender

    private ServerSocket servidorSocket;
    private PrivateKey llavePrivada;
    private PublicKey llavePublica;
    private Map<String, String[]> tablaServicios;
    private long tiempoFirmaTotal;
    private long tiempoCifradoTotal;

    public ServidorPrincipal(PublicKey llavePublica, PrivateKey llavePrivada) throws Exception {
        // Inicializar servidor
        this.servidorSocket = new ServerSocket(PUERTO);

        // Guardar llaves
        this.llavePrivada = llavePrivada;
        this.llavePublica = llavePublica;

        // Cargar tabla de servicios
        this.tablaServicios = new HashMap<>();
        tablaServicios.put("1", new String[]{"Estado vuelo", "192.168.0.10", "9001"});
        tablaServicios.put("2", new String[]{"Disponibilidad vuelos", "192.168.0.11", "9002"});
        tablaServicios.put("3", new String[]{"Costo de un vuelo", "192.168.0.12", "9003"});
    }

    public void atenderClientes() throws Exception {
        System.out.println("Servidor principal escuchando en puerto " + PUERTO);

        List<ServidorDelegado> delegados = new ArrayList<>();
        int clientesAtendidos = 0;

        try {
            while (clientesAtendidos < CLIENTES_ESPERADOS) {
                Socket socketCliente = servidorSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());

                ServidorDelegado delegado = new ServidorDelegado(socketCliente, tablaServicios, llavePublica, llavePrivada);
                delegado.start();
                delegados.add(delegado);

                clientesAtendidos++;
            }
        } finally {
            System.out.println("Todos los clientes fueron aceptados. Esperando a que terminen...");

            // Primero esperar que todos los delegados terminen
            for (ServidorDelegado delegado : delegados) {
                delegado.join();
            }

            // Luego de que todos terminaron, sumar los tiempos
            for (ServidorDelegado delegado : delegados) {
                tiempoFirmaTotal += delegado.getTiempoFirma();
                tiempoCifradoTotal += delegado.getTiempoCifrado();
            }

            // Imprimir resultados
            System.out.println("Todos los clientes terminaron correctamente.");
            System.out.println("Tiempo total de firmas: " + tiempoFirmaTotal + " ms");
            System.out.println("Tiempo total de cifrados: " + tiempoCifradoTotal + " ms");

            servidorSocket.close();
            System.out.println("Servidor cerrado.");
        }
    }
}






