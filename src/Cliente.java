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

    private int cantidadClientes;
    private PublicKey llavePublicaServidorA;

    public Cliente(PublicKey llavePublicaServidorA, int cantidadClientes) {
        this.llavePublicaServidorA = llavePublicaServidorA;
        this.cantidadClientes = cantidadClientes;
    }

    // Método para escenario (i): Cliente iterativo (32 consultas)
    public void iniciarIterativo() {
        System.out.println("Iniciando cliente iterativo...");
        ClienteIterativo cliente = new ClienteIterativo(llavePublicaServidorA);
        cliente.start();
    }

    // Método para escenario (ii): Clientes concurrentes
    public void iniciarConcurrente() {
        System.out.println("Iniciando clientes concurrentes...");
        for (int i = 0; i < cantidadClientes; i++) {
            System.out.println("Cliente concurrente " + (i + 1) + " iniciado.");
            ClienteDelegado cliente = new ClienteDelegado(llavePublicaServidorA);
            cliente.start(); // .start() para que sea concurrente
        }
    }
}



