import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.*;
import javax.crypto.spec.*;

public class ServidorDelegado extends Thread {
    private final Socket socket;
    private final Map<String, String> servicios;
    private PublicKey llavePublica;
    private PrivateKey llavePrivada;

    public ServidorDelegado(Socket socket, Map<String, String> servicios, PublicKey llavePublica, PrivateKey llave) {
        this.llavePublica= llavePublica;
        this.llavePrivada = llave;
        this.socket = socket;
        this.servicios = servicios;
        
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Paso 1: recibir "HELLO"
            String saludo = (String) in.readObject();
            if (!"HELLO".equals(saludo)) {
                socket.close();
                return;
            }

           // Paso 3: Calcula el reto y envía al cliente
           byte[] reto = (byte[]) in.readObject();
           byte[] Rta = Asimetrico.cifrar(llavePrivada, reto);

           //Paso 4 : Enviar reto cifrado al cliente
           out.writeObject(Rta);


            // Paso 3: recibir reto cifrado y verificar con clave privada

            byte[] retoCifrado = (byte[]) in.readObject();
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, llavePrivada);
            byte[] retoClaro = rsa.doFinal(retoCifrado);

            if (!Arrays.equals(reto, retoClaro)) {
                out.writeObject("ERROR");
                socket.close();
                return;
            }
            out.writeObject("OK");

            // Paso 4: generar parámetros Diffie-Hellman
            DiffieHellman dh = new DiffieHellman();
            BigInteger G = dh.getG();
            BigInteger P = dh.getP();
            byte[] gx = dh.getClavePublicaCodificada();

            // Paso 5: firmar (G, P, gx)
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(llavePrivada);
            sig.update(G.toByteArray());
            sig.update(P.toByteArray());
            sig.update(gx);
            byte[] firma = sig.sign();

            out.writeObject(G);
            out.writeObject(P);
            out.writeObject(gx);
            out.writeObject(firma);

            // Paso 6: recibir "OK" del cliente
            String ok = (String) in.readObject();
            if (!"OK".equals(ok)) {
                socket.close();
                return;
            }

            // Paso 7: recibir Gy, calcular llave compartida
            byte[] gyBytes = (byte[]) in.readObject();
            dh.procesarClaveRemota(gyBytes);
            byte[] llaveCompartida = dh.obtenerLlaveCompartida();

            // Paso 8: derivar claves AES y HMAC
            byte[] hash = Digest.getDigest("SHA-512", llaveCompartida);
            SecretKey kAES = new SecretKeySpec(Arrays.copyOfRange(hash, 0, 32), "AES");
            SecretKey kHMAC = new SecretKeySpec(Arrays.copyOfRange(hash, 32, 64), "HmacSHA256");

            // Paso 9: enviar tabla cifrada con HMAC
            byte[] datosTabla = tablaToString(servicios).getBytes();
            byte[] paquete = Simetrico.cifrar(kAES, datosTabla);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(kHMAC);
            byte[] hmac = mac.doFinal(paquete);

            out.writeObject(paquete);
            out.writeObject(hmac);

            // Paso 10: recibir consulta cifrada y su HMAC
            byte[] paqueteConsulta = (byte[]) in.readObject();
            byte[] hmacConsulta = (byte[]) in.readObject();

            mac.init(kHMAC);
            if (!Arrays.equals(hmacConsulta, mac.doFinal(paqueteConsulta))) {
                socket.close();
                return;
            }

            byte[] consultaDescifrada = Simetrico.descifrar(kAES, paqueteConsulta);
            String id = new String(consultaDescifrada).trim();
            String respuesta = servicios.getOrDefault(id, "-1,-1");

            byte[] paqueteResp = Simetrico.cifrar(kAES, respuesta.getBytes());
            byte[] hmacResp = mac.doFinal(paqueteResp);

            out.writeObject(paqueteResp);
            out.writeObject(hmacResp);

            // Paso final: esperar "OK" del cliente
            String finalOK = (String) in.readObject();
            System.out.println("Cliente finalizó con OK: " + finalOK);
            socket.close();

        } catch (Exception e) {
            System.err.println("Error en delegado: " + e.getMessage());
        }
    }

    private String tablaToString(Map<String, String> tabla) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : tabla.entrySet()) {
            sb.append(entry.getKey()).append(" → ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
