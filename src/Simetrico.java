import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;

public class Simetrico {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    /** Genera un IV aleatorio de 16 bytes y cifra. Devuelve IV || textoCifrado */
    public static byte[] cifrar(SecretKey llave, byte[] datosClaro) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, llave, new IvParameterSpec(ivBytes));
        byte[] cifrado = cipher.doFinal(datosClaro);

        // concatenamos IV + cifrado para enviarlo junto
        byte[] salida = new byte[ivBytes.length + cifrado.length];
        System.arraycopy(ivBytes, 0, salida, 0, ivBytes.length);
        System.arraycopy(cifrado, 0, salida, ivBytes.length, cifrado.length);
        return salida;
    }

    /** Descifra asumiendo que el parámetro contiene IV || textoCifrado */
    public static byte[] descifrar(SecretKey llave, byte[] ivYcifrado) throws Exception {
        byte[] ivBytes = new byte[16];
        byte[] cifrado = new byte[ivYcifrado.length - 16];
        System.arraycopy(ivYcifrado, 0, ivBytes, 0, 16);
        System.arraycopy(ivYcifrado, 16, cifrado, 0, cifrado.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, llave, new IvParameterSpec(ivBytes));
        return cipher.doFinal(cifrado);
    }
}

