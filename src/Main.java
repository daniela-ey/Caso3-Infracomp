import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Main {
    public static void main(String[] args) throws Exception {
       try {
            // Crear generador de llaves con algoritmo RSA
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024); // Tamaño de clave en bits

            // Generar el par de llaves
            KeyPair keyPair = generator.generateKeyPair();
            PublicKey llavePublica = keyPair.getPublic();
            PrivateKey llavePrivada = keyPair.getPrivate();

           

        } catch (Exception e) {
            e.printStackTrace();
        }
    
    }
}
