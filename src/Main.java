import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Main {
    public static void main(String[] args) {
        try {
            // Crear generador de llaves RSA
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);

            // Generar par de llaves
            KeyPair keyPair = generator.generateKeyPair();
            PublicKey llavePublica = keyPair.getPublic();
            PrivateKey llavePrivada = keyPair.getPrivate();

            // Guardar llaves
            Files.write(Paths.get("public_key.der"), llavePublica.getEncoded());
            Files.write(Paths.get("private_key.der"), llavePrivada.getEncoded());

            // Crear el servidor principal en un nuevo Thread
            ServidorPrincipal servidor = new ServidorPrincipal(llavePublica, llavePrivada);
            new Thread(() -> {
                try {
                    servidor.atenderClientes(); // Esto queda corriendo en segundo plano
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Esperar un pequeño tiempo para asegurarse que el servidor ya está escuchando
            Thread.sleep(1000);

            // Ahora sí: lanzar clientes
            Cliente cliente = new Cliente(llavePublica, 1); // 1 cliente
            cliente.iniciar();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

