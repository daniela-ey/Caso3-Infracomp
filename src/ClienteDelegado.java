import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.*;
import javax.crypto.spec.*;


public class ClienteDelegado extends Thread {

    
    private static final String SERVIDOR_IP     = "localhost";
    private static final int    PUERTO          = 3400;
    private PublicKey llavePublicaServidorA;
    private SecretKey k_AB1, k_AB2;
    private long tiempoValidar;




    public ClienteDelegado(PublicKey llavePublicaServidorA){
        this.llavePublicaServidorA= llavePublicaServidorA;

    }

    @Override public void run() {
        try (Socket socket = new Socket(SERVIDOR_IP, PUERTO);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("HELLO");                        // Paso 1

            byte[] reto = new byte[16];
            new SecureRandom().nextBytes(reto);
            out.writeObject(reto);                        // 2  (envía reto)

            // Paso 5: recibir reto cifrado y verificar con clave privada

            byte[] Rta = (byte[]) in.readObject();       
            byte[] R = Asimetrico.descifrar(llavePublicaServidorA, Rta);
            String respuesta = "ERROR";
            if (Arrays.equals(reto, R)) {
               respuesta = "OK"; 
            }

            //Pao 6:  (envía “OK” | “ERROR”)
            out.writeObject(respuesta); 
            
            //Paso 9: Recibir y verificar la respuesta del servidor
            BigInteger G = (BigInteger) in.readObject();
            BigInteger P = (BigInteger) in.readObject();
            byte[] gx = (byte[]) in.readObject();
            byte[] firma = (byte[]) in.readObject();

            /* 9. Verificar firma */
            Signature verDH = Signature.getInstance("SHA256withRSA");
            verDH.initVerify(llavePublicaServidorA);
            verDH.update(G.toByteArray());
            verDH.update(P.toByteArray());
            verDH.update(gx);
            boolean okFirmaDH = verDH.verify(firma);

            out.writeObject(okFirmaDH ? "OK" : "ERROR");     
            if (!okFirmaDH) {
                System.out.println(getName() + " → Firma sobre (G,P,Gx) inválida.");
                return;
            }

            //Paso 11a: Calcular clave pública del cliente (G^y mod P)

           // Generar DH cliente
            DHParameterSpec dhSpec = new DHParameterSpec(P, G);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(dhSpec);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Enviar G^y
            byte [] gy = (byte[]) keyPair.getPublic().getEncoded();
            System.out.println(gy);
            out.writeObject(gy);

            // Hacer doPhase
            KeyAgreement ka = KeyAgreement.getInstance("DH");
            ka.init(keyPair.getPrivate());

            PublicKey claveServidor = KeyFactory.getInstance("DH").generatePublic(new X509EncodedKeySpec(gx));
            ka.doPhase(claveServidor, true);

            // Derivar llaves
            byte[] llaveCompartida = ka.generateSecret();
            byte[] digest = Digest.getDigest("SHA-512", llaveCompartida);

            k_AB1 = new SecretKeySpec(Arrays.copyOfRange(digest, 0, 32), "AES");
            k_AB2 = new SecretKeySpec(Arrays.copyOfRange(digest, 32, 64), "HmacSHA256");

            // Paso 12a: Generar IV
            byte[] ivBytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            out.writeObject(ivBytes);                  // 12b IV

            // Paso 13: recibir tabla cifrada y HMAC
            byte[] paqueteTabla = (byte[]) in.readObject();  // IV||tablaCif
            byte[] hmacTabla    = (byte[]) in.readObject();

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(k_AB2);
            if (!Arrays.equals(hmacTabla, mac.doFinal(paqueteTabla))) {
                System.out.println(getName() + " → HMAC tabla no válido.");
                return;
            }
            byte[] tablaClaro = Simetrico.descifrar(k_AB1, paqueteTabla);
            System.out.println("[" + getName() + "] Servicios:\n" + new String(tablaClaro));

            /* 14. Elegir servicio y enviar consulta cifrada */
            String idServicio = String.valueOf(new Random().nextInt(3) + 1);
            String ipCliente = socket.getLocalAddress().getHostAddress();
            String mensaje = idServicio + "," + ipCliente;
            byte[] mensajeBytes = mensaje.getBytes();
    
            byte[] paqueteConsulta = Simetrico.cifrar(k_AB1, mensajeBytes, ivBytes);    
            mac.init(k_AB2);
            byte[] hmacConsulta = mac.doFinal(paqueteConsulta);

            out.writeObject(paqueteConsulta);
            out.writeObject(hmacConsulta);

            /* 17. Recibir respuesta, verificar HMAC y descifrar */
            long inicio = System.nanoTime();
            byte[] paqueteResp = (byte[]) in.readObject();
            byte[] hmacResp    = (byte[]) in.readObject();
            mac.init(k_AB2);
            if (!Arrays.equals(hmacResp, mac.doFinal(paqueteResp))) {
                System.out.println(getName() + " → HMAC respuesta no válido.");
                out.writeObject("OK"); // 18  “OK” final (opcional)
                return;
            } else {
                out.writeObject("OK"); // 18  “OK” final (opcional)
            }
            byte[] respClaro = Simetrico.descifrar(k_AB1, paqueteResp);
            long fin = System.nanoTime();
            // Calcular tiempo de firma
            tiempoValidar = tiempoValidar +  (fin - inicio) / 1_000_000;
            System.out.println("[" + getName() + "] Respuesta: " + new String(respClaro));


        } catch (Exception e) {
            System.err.println("[" + getName() + "] Error: " + e.getMessage());
        }
    }

    public long getTiempoValidar() {
        return tiempoValidar;
    }

   
}
