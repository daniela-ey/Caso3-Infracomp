import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {

    public static byte[] getDigest(String algorithm, byte[] buffer) {
    try {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(buffer);
        return digest.digest();
    } catch (Exception e) {
        return null;
    }
}

    public static byte[] getDigestFile(String algorithm, String fileName) {
    MessageDigest md = null;
    try {
        md = MessageDigest.getInstance(algorithm);

        FileInputStream in = new FileInputStream(fileName);
        byte[] buffer = new byte[1024];

        int length;
        while ((length = in.read(buffer)) != -1) {
            md.update(buffer, 0, length);
        }
        in.close();
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }

    return md.digest();
}

    
}
