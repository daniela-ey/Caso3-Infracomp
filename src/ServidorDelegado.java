import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import javax.crypto.Mac;
import javax.crypto.SecretKey;


public class ServidorDelegado extends Thread {
    private Map<String, String[]> tablaServicios;
    private PublicKey llavePublica;
    private PrivateKey llavePrivada;
    private Socket socket;

    public ServidorDelegado(Socket socket,Map<String, String[]> tablaServicios,  PublicKey llavePublica, PrivateKey llaveprivada) throws IOException {
        this.socket = socket;
        this.tablaServicios = tablaServicios;
        this.llavePublica = llavePublica;
        this.llavePrivada = llaveprivada;
     
    }


    private static String tablaToString(Map<String, String[]> tablaServicios) {
        StringBuilder sb = new StringBuilder();
    
        for (Map.Entry<String, String[]> entry : tablaServicios.entrySet()) {
            String id = entry.getKey();
            String[] datos = entry.getValue();
    
            // Concatenamos el ID + descripción + IP + puerto
            sb.append(id).append(",")
              .append(datos[0]).append(",")
              .append(datos[1]).append(",")
              .append(datos[2]).append("\n");
        }
    
        return sb.toString();
    }
    

 
    @Override
    public void run () {
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

           // Paso 6: recibir "OK" o "ERROR"
              String respuesta = (String) in.readObject();
                if ("ERROR".equals(respuesta)) {
                    socket.close();
                    System.out.println("Cliente rechazó el reto.");
                    return;
                }

           //Paso 7: Generar G,P y G^x
            GeneradorLlavesSesion dh = new GeneradorLlavesSesion();
            BigInteger G = dh.getG();
            out.writeObject(G);

            BigInteger P = dh.getP();
            out.writeObject(P);

            byte[] gx = dh.getClavePublicaCodificada();
            out.writeObject(gx);

            // Paso 8: Firmar (G, P, gx)

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(llavePrivada);
            sig.update(G.toByteArray());
            sig.update(P.toByteArray());
            sig.update(gx);
            byte[] firma = sig.sign();

            out.writeObject(firma);

        
            // Paso 10: recibir "OK" del cliente
            String ok = (String) in.readObject();
            if (!"OK".equals(ok)) {
                socket.close();
                System.out.println("Cliente rechazó la firma.");
                return;
            }

            // Paso 11: recibir G^y del cliente
            byte[] gy = (byte[]) in.readObject();

            //Paso 11b: Generar clave pública del cliente (G^y)^x = G^(xy)
            dh.procesarClaveRemota(gy);  // doPhase() usando clave pública del cliente
            byte[] llaveCompartida = dh.obtenerLlaveCompartida();

            // Derivar llaves de sesión
            SecretKey[] llavesSesion = dh.derivarLlaves(llaveCompartida);
            SecretKey k_AB1 = llavesSesion[0]; // AES para cifrado
            SecretKey k_AB2 = llavesSesion[1]; // HMAC para autenticidad

            // Paso 12b: recibir IV
            byte[] ivBytes = (byte[]) in.readObject();

            //Paso 13: Cifrar la tabla de servicios

            // Convertir tabla a texto plano
            byte[] datosTabla = tablaToString(tablaServicios).getBytes();

            // Cifrar
            byte[] tablaCifrada = Simetrico.cifrar(k_AB1, datosTabla, ivBytes);

            // Calcular HMAC
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(k_AB2);
            byte[] hmacTabla = mac.doFinal(tablaCifrada);

            // Enviar al cliente
            out.writeObject(tablaCifrada);
            out.writeObject(hmacTabla);    

            // Paso 15: Verificar HMAC de la tabla
            // 1. Recibir paqueteConsulta (IV + datos cifrados)
            byte[] paqueteConsulta = (byte[]) in.readObject();

            // 2. Recibir HMAC
            byte[] hmacRecibido = (byte[]) in.readObject();

            // 3. Verificar HMAC
            mac.init(k_AB2);
            byte[] hmacCalculado = mac.doFinal(paqueteConsulta);

            if (!Arrays.equals(hmacRecibido, hmacCalculado)) {
                System.out.println("Error: HMAC inválido en la consulta. Cerrando conexión.");
                socket.close();
                return;
            }

            // Paso 16: Descifrar consulta
            byte[] datosDescifrados = Simetrico.descifrar(k_AB1, paqueteConsulta);
            String consulta = new String(datosDescifrados);

            System.out.println("Consulta recibida: " + consulta);

            String[] partes = consulta.split(",");
            String idServicio = partes[0];
            String ipCliente = partes[1];

            // Buscar en la tabla de servicios el IP y puerto correspondiente
            String[] datosServicio = tablaServicios.get(idServicio); 

            if (datosServicio == null) {
                datosServicio = new String[]{"-1", "-1"}; // No encontrado
            }

            respuesta = datosServicio[1] + "," + datosServicio[2]; // ip_servidor,puerto_servidor
            byte[] paqueteRespuesta = Simetrico.cifrar(k_AB1, respuesta.getBytes(), ivBytes);

            // 3. Calcular HMAC de la respuesta cifrada
            mac.init(k_AB2);
            byte[] hmacRespuesta = mac.doFinal(paqueteRespuesta);

            // 4. Enviar ambos al cliente
            out.writeObject(paqueteRespuesta);
            out.writeObject(hmacRespuesta);



            // Paso final: esperar "OK" del cliente
            String finalOK = (String) in.readObject();
            System.out.println("Cliente finalizó con OK: " + finalOK);
            socket.close();

        } catch (Exception e) {
            System.err.println("Error en delegado: " + e.getMessage());
        }
    }
}
