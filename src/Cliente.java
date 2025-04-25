import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.*;
import javax.crypto.spec.*;


public class Cliente extends Thread {

    // ── Datos de conexión ───────────────────────────────────────────────────────────
    
    private static final String SERVIDOR_IP     = "localhost";
    private static final int    PUERTO          = 3400;
    private PublicKey llavePublicaServidor;

    public Cliente(PublicKey llavePublicaServidor){
        this.llavePublicaServidor = llavePublicaServidor;

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
            byte[] R = Asimetrico.descifrar(llavePublicaServidor, Rta);
            String respuesta = "ERROR";
            if (Arrays.equals(reto, R)) {
               respuesta = "ERROR";
            }

            //Pao 6:  (envía “OK” | “ERROR”)
            out.writeObject(respuesta); 
            
            //Paso 9: Recibir y verificar la respuesta del servidor
            BigInteger P = (BigInteger) in.readObject();
            BigInteger G = (BigInteger) in.readObject();
            byte[] gx = (byte[]) in.readObject();
            byte[] firma = (byte[]) in.readObject();

            /* 9. Verificar firma */
            Signature verDH = Signature.getInstance("SHA256withRSA");
            verDH.initVerify(pubSrv);
            verDH.update(G.toByteArray());
            verDH.update(P.toByteArray());
            verDH.update(gxBytes);
            boolean okFirmaDH = verDH.verify(firmaDH);

            out.writeObject(okFirmaDH ? "OK" : "ERROR");     // 10
            if (!okFirmaDH) {
                System.out.println(getName() + " → Firma sobre (G,P,Gx) inválida.");
                return;
            }

            /* =========================================================================
               11-18  Fase ya conocida  (DH, AES, HMAC, consulta, respuesta)
               ========================================================================= */

            /* 11a. Generar par DH propio usando mismos parámetros */
            DiffieHellman dh = new DiffieHellman(G, P);      //  ctor que acepta parámetros
            out.writeObject(dh.getClavePublicaCodificada()); // 11  (envía Gy)

            /* 11b. Derivar llaves */
            dh.procesarClaveRemota(gxBytes);                 // procesa Gx
            byte[] llaveCompartida = dh.obtenerLlaveCompartida();
            byte[] hash = Digest.getDigest("SHA-512", llaveCompartida);
            SecretKey k_AB1 = new SecretKeySpec(Arrays.copyOfRange(hash, 0, 32),  "AES");
            SecretKey k_AB2 = new SecretKeySpec(Arrays.copyOfRange(hash, 32, 64), "HmacSHA256");

            /* 12. Recibir IV generado por servidor + tabla cifrada + HMAC */
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
            byte[] consultaClaro = idServicio.getBytes();
            byte[] paqueteConsulta = Simetrico.cifrar(k_AB1, consultaClaro);
            mac.init(k_AB2);
            byte[] hmacConsulta = mac.doFinal(paqueteConsulta);

            out.writeObject(paqueteConsulta);
            out.writeObject(hmacConsulta);

            /* 15-17. Recibir respuesta, verificar HMAC y descifrar */
            byte[] paqueteResp = (byte[]) in.readObject();
            byte[] hmacResp    = (byte[]) in.readObject();
            mac.init(k_AB2);
            if (!Arrays.equals(hmacResp, mac.doFinal(paqueteResp))) {
                System.out.println(getName() + " → HMAC respuesta no válido.");
                return;
            }
            byte[] respClaro = Simetrico.descifrar(k_AB1, paqueteResp);
            System.out.println("[" + getName() + "] Respuesta: " + new String(respClaro));

            out.writeObject("OK");                           // 18  “OK” final (opcional)

        } catch (Exception e) {
            System.err.println("[" + getName() + "] Error: " + e.getMessage());
        }
    }

    /* ── Utilidad para leer llave pública RSA ─────────────────────────────────────── */
    private static PublicKey cargarLlavePublica(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return KeyFactory.getInstance("RSA")
                         .generatePublic(new X509EncodedKeySpec(bytes));
    }
}


