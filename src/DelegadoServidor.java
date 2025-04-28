import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class DelegadoServidor implements Runnable {

    private Socket socket;

    public DelegadoServidor(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream())) {

            while (true) {
                try {
                    Object solicitud = entrada.readObject();
                    if (solicitud == null) {
                        break;
                    }

                    System.out.println("Solicitud recibida: " + solicitud);

                    long inicioCifrado = System.nanoTime();
                    salida.writeObject("RESPUESTA_SERVIDOR");
                    salida.flush();
                    long finCifrado = System.nanoTime();

                    System.out.println("Tiempo cifrado de respuesta: " + ((finCifrado - inicioCifrado) / 1_000_000) + " ms");

                } catch (java.io.EOFException eof) {
                    // El cliente cerró la conexión
                    System.out.println("Cliente desconectado.");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
