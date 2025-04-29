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
import java.security.PublicKey;



public class Cliente extends Thread {

    private int cantidadClientes;
    private PublicKey llavePublicaServidorA;

    public Cliente(PublicKey llavePublicaServidorA, int cantidadClientes) {
        this.llavePublicaServidorA= llavePublicaServidorA;
        this.cantidadClientes = cantidadClientes;

    }

    public void iniciar () {
        for (int i = 0; i < cantidadClientes; i++) {
            System.out.println("Cliente " + (i + 1) + " iniciado.");
            ClienteDelegado cliente = new ClienteDelegado(llavePublicaServidorA);
            cliente.start(); // .start() para que sea concurrente
        }
    }

  

   
}


