import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Cliente {

    private String host;
    private int puerto;

    public Cliente(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    public void iniciarIterativo(int consultas) {
        try (Socket socket = new Socket(host, puerto)) {
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            long tiempoFirmaTotal = 0;
            long tiempoCifradoTotal = 0;
            long tiempoVerificacionTotal = 0;

            for (int i = 0; i < consultas; i++) {
                long inicioFirma = System.nanoTime();
                salida.writeObject("CONSULTA " + i);
                salida.flush();
                long finFirma = System.nanoTime();

                long inicioVerificacion = System.nanoTime();
                Object respuesta = entrada.readObject();
                long finVerificacion = System.nanoTime();

                // Aquí podrías agregar descifrado real si fuera necesario

                tiempoFirmaTotal += (finFirma - inicioFirma);
                tiempoVerificacionTotal += (finVerificacion - inicioVerificacion);
            }

            System.out.println("\n--- Reporte Iterativo ---");
            System.out.println("Tiempo total de firma: " + (tiempoFirmaTotal / 1_000_000) + " ms");
            System.out.println("Tiempo total de verificación: " + (tiempoVerificacionTotal / 1_000_000) + " ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void iniciarUnaConsulta() {
        try (Socket socket = new Socket(host, puerto)) {
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            long inicioFirma = System.nanoTime();
            salida.writeObject("CONSULTA_UNICA");
            salida.flush();
            long finFirma = System.nanoTime();

            long inicioVerificacion = System.nanoTime();
            Object respuesta = entrada.readObject();
            long finVerificacion = System.nanoTime();

            // Descifrado si fuera necesario

            System.out.println("\n--- Reporte Cliente Único ---");
            System.out.println("Tiempo firma: " + ((finFirma - inicioFirma) / 1_000_000) + " ms");
            System.out.println("Tiempo verificación: " + ((finVerificacion - inicioVerificacion) / 1_000_000) + " ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
