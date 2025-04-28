import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Seleccione una opción:");
        System.out.println("1. Un servidor de consulta y un cliente iterativo");
        System.out.println("2. Servidor y clientes concurrentes");
        int opcion = scanner.nextInt();

        if (opcion == 1) {
            new Thread(() -> {
                try {
                    ServidorPrincipal servidor = new ServidorPrincipal(5000);
                    servidor.iniciar();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            try {
                Thread.sleep(1000); // Espera a que el servidor inicie
                Cliente cliente = new Cliente("localhost", 5000);
                cliente.iniciarIterativo(32);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (opcion == 2) {
            System.out.println("Ingrese el número de servidores/clientes (4, 16, 32, 64):");
            int num = scanner.nextInt();
            int[] puertos = new int[num];
        
            // Crear servidores
            for (int i = 0; i < num; i++) {
                int puerto = 5000 + i; // usamos puertos consecutivos: 5000, 5001, 5002, 5003...
                puertos[i] = puerto;
                int finalPuerto = puerto;
                new Thread(() -> {
                    try {
                        ServidorPrincipal servidor = new ServidorPrincipal(finalPuerto);
                        servidor.iniciar();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        
            try {
                Thread.sleep(2000); // Espera a que los servidores inicien
        
                // Crear clientes conectando a los servidores correctos
                for (int i = 0; i < num; i++) {
                    int finalPuerto = puertos[i];
                    new Thread(() -> {
                        try {
                            Cliente cliente = new Cliente("localhost", finalPuerto);
                            cliente.iniciarUnaConsulta();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
         else {
            System.out.println("Opción inválida.");
        }

        scanner.close();
    }
}
