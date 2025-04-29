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
        private static final int CLIENTES_ESPERADOS = 1; // <- Número de clientes que quieres atender

        private ServerSocket servidorSocket;
        private PrivateKey llavePrivada;
        private PublicKey llavePublica;
        private Map<String, String[]> tablaServicios;
        private long tiempoFirmaTotal;
        private long tiempoCifradoTotal;

        public ServidorPrincipal(PublicKey llavePublica, PrivateKey llavePrivada) throws Exception {
            // 1. Inicializar servidor
            this.servidorSocket = new ServerSocket(PUERTO);

            // 2. Guardar llaves
            this.llavePrivada = llavePrivada;
            this.llavePublica = llavePublica;

            // 3. Cargar tabla de servicios
            this.tablaServicios = new HashMap<>();
            tablaServicios.put("1", new String[]{"Estado vuelo", "192.168.0.10", "9001"});
            tablaServicios.put("2", new String[]{"Disponibilidad vuelos", "192.168.0.11", "9002"});
            tablaServicios.put("3", new String[]{"Costo de un vuelo", "192.168.0.12", "9003"});
        }

        // Método principal para atender conexiones
        public void atenderClientes() throws Exception {
            System.out.println("Servidor principal escuchando en puerto " + PUERTO);

            List<ServidorDelegado> delegados = new ArrayList<>();  // Para guardar los threads

            int clientesAtendidos = 0;

            try {
                while (clientesAtendidos < CLIENTES_ESPERADOS) {
                    Socket socketCliente = servidorSocket.accept();
                    System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());

                    ServidorDelegado delegado = new ServidorDelegado(socketCliente, tablaServicios, llavePublica, llavePrivada);
                    delegado.start();  // Iniciar hilo
                    delegados.add(delegado);  // Guardarlo para luego esperar su fin

                    clientesAtendidos++;

                    tiempoFirmaTotal += delegado.getTiempoFirma();  // Acumular tiempo de firma
                    tiempoCifradoTotal += delegado.getTiempoCifrado();  // Acumular tiempo de cifrado
                }
            } finally {
                // Después de aceptar todos los clientes
                System.out.println("Todos los clientes fueron aceptados. Esperando a que terminen...");

                for (ServidorDelegado delegado : delegados) {
                    delegado.join();  // Esperar que cada hilo termine
                }

                System.out.println("Todos los clientes terminaron correctamente. Cerrando servidor...");
                servidorSocket.close();
            }
        }
    }






