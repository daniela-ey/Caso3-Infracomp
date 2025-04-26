import java.security.Key;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;

public class Asimetrico {

    private static final String TRANSFORM = "RSA/ECB/PKCS1Padding";

    /* ───────── 1. Cifrar/descifrar arreglos de bytes ───────── */

    public static byte[] cifrar(Key llave, byte[] datosClaro) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.ENCRYPT_MODE, llave);
        return c.doFinal(datosClaro);
    }

    public static byte[] descifrar(Key llave, byte[] datosCif) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.DECRYPT_MODE, llave);
        return c.doFinal(datosCif);
    }

    /* ───────── 2. Sobrecargas de conveniencia para String ──── */

    public static byte[] cifrarDeString(Key llave, String textoClaro) throws Exception {
        return cifrar(llave, textoClaro.getBytes(StandardCharsets.UTF_8));
    }

    public static String descifrarAString(Key llave, byte[] datosCif) throws Exception {
        return new String(descifrar(llave, datosCif), StandardCharsets.UTF_8);
    }
}

