import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class GeneradorLlavesSesion {

    private KeyPair keyPair;
    private KeyAgreement agreement;
    private BigInteger p, g;

    public GeneradorLlavesSesion() throws Exception {
        // Generar parámetros DH (p, g)
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
        paramGen.init(1024);  // Longitud del primo
        AlgorithmParameters params = paramGen.generateParameters();
        DHParameterSpec dhSpec = params.getParameterSpec(DHParameterSpec.class);

        this.p = dhSpec.getP();
        this.g = dhSpec.getG();

        // Generar par de claves
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhSpec);
        this.keyPair = keyGen.generateKeyPair();

        // Inicializar el acuerdo DH
        this.agreement = KeyAgreement.getInstance("DH");
        this.agreement.init(keyPair.getPrivate());
    }

    public byte[] getClavePublicaCodificada() {
        return keyPair.getPublic().getEncoded();
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getG() {
        return g;
    }

    public void procesarClaveRemota(byte[] claveRemotaCodificada) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("DH");
        PublicKey claveRemota = kf.generatePublic(new X509EncodedKeySpec(claveRemotaCodificada));
        agreement.doPhase(claveRemota, true);
    }

    public byte[] obtenerLlaveCompartida() {
        return agreement.generateSecret();
    }

    public SecretKey[] derivarLlaves(byte[] llaveCompartida) throws Exception {
        byte[] hash = Digest.getDigest("SHA-512", llaveCompartida);
        SecretKey llaveCifrado = new SecretKeySpec(Arrays.copyOfRange(hash, 0, 32), "AES");
        SecretKey llaveHMAC = new SecretKeySpec(Arrays.copyOfRange(hash, 32, 64), "HmacSHA256");
        return new SecretKey[] { llaveCifrado, llaveHMAC };
    }
}



