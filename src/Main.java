import java.nio.file.*;
import java.security.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            // Crear llaves
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            KeyPair keyPair = generator.generateKeyPair();
            PublicKey llavePublica = keyPair.getPublic();
            PrivateKey llavePrivada = keyPair.getPrivate();

            Files.write(Paths.get("public_key.der"), llavePublica.getEncoded());
            Files.write(Paths.get("private_key.der"), llavePrivada.getEncoded());

            // Arrancar servidor
            ServidorPrincipal servidor = new ServidorPrincipal(llavePublica, llavePrivada);
            new Thread(() -> {
                try {
                    servidor.atenderClientes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            Thread.sleep(1000); // Esperar que el servidor arranque

            // Menú
            Scanner scanner = new Scanner(System.in);
            System.out.println("=== Escenarios de prueba ===");
            System.out.println("1. Escenario Iterativo (un cliente, 32 consultas)");
            System.out.println("2. Escenario Concurrente (varios clientes concurrentes)");
            System.out.print("Opción: ");
            int opcion = scanner.nextInt();

            if (opcion == 1) {
                Cliente cliente = new Cliente(llavePublica, 1); // solo 1 cliente para 32 consultas
                cliente.iniciarIterativo();
            } 
            else if (opcion == 2) {
                System.out.println("Selecciona cantidad de clientes concurrentes (4, 16, 32, 64): ");
                int cantidad = scanner.nextInt();
                if (cantidad == 4 || cantidad == 16 || cantidad == 32 || cantidad == 64) {
                    Cliente cliente = new Cliente(llavePublica, cantidad);
                    cliente.iniciarConcurrente();
                } else {
                    System.out.println("Cantidad inválida. Debe ser 4, 16, 32 o 64.");
                }
            } 
            else {
                System.out.println("Opción inválida. Terminando programa.");
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



