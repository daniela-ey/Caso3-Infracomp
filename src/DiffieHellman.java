import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.*;

public class DiffieHellman {

    private KeyPair         keyPair;
    private KeyAgreement    ka;
    private BigInteger      p, g;   // guardamos P y G por si llegan claves “crudas”

    /* ───────────────── 1. Constructor “servidor genera parámetros” ─────────────── */
    public DiffieHellman() throws Exception {
        AlgorithmParameterGenerator pg = AlgorithmParameterGenerator.getInstance("DH");
        pg.init(1024);
        DHParameterSpec spec = pg.generateParameters()
                                 .getParameterSpec(DHParameterSpec.class);
        inicializar(spec);
    }

    /* ───────────────── 2. Constructor “cliente recibe P y G del servidor” ──────── */
    public DiffieHellman(BigInteger G, BigInteger P) throws Exception {
        DHParameterSpec spec = new DHParameterSpec(P, G);   // orden: P, G
        inicializar(spec);
    }

    /* ───────────────── 3. Constructor “usa la clave pública remota para extraer P,G” */
    public DiffieHellman(PublicKey clavePubRemota) throws Exception {
        DHParameterSpec spec = ((DHPublicKey) clavePubRemota).getParams();
        inicializar(spec);
    }

    /* ─────────────────  Auxiliar común  ────────────────────────────────────────── */
    private void inicializar(DHParameterSpec spec) throws Exception {
        this.p = spec.getP();
        this.g = spec.getG();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(spec);
        keyPair = kpg.generateKeyPair();

        ka = KeyAgreement.getInstance("DH");
        ka.init(keyPair.getPrivate());
    }

    /* ───────────────── 4. Clave pública local en bytes (X.509) ─────────────────── */
    public byte[] getClavePublicaCodificada() {
        return keyPair.getPublic().getEncoded();
    }

    /* ───────────────── 5. Procesar clave remota (X.509 ó cruda) ────────────────── */
    public void procesarClaveRemota(byte[] remota) throws Exception {

        PublicKey pubRemota;

        /* Caso A: la otra parte envió la clave pública completa (X.509) */
        try {
            X509EncodedKeySpec x509 = new X509EncodedKeySpec(remota);
            pubRemota = KeyFactory.getInstance("DH").generatePublic(x509);
        } catch (Exception e) {
            /* Caso B: la otra parte envió sólo el número Y = G^x en bruto */
            BigInteger Y = new BigInteger(1, remota);
            DHPublicKeySpec dhSpec = new DHPublicKeySpec(Y, p, g);
            pubRemota = KeyFactory.getInstance("DH").generatePublic(dhSpec);
        }

        ka.doPhase(pubRemota, true);
    }

    /* ───────────────── 6. Obtener la llave secreta compartida ──────────────────── */
    public byte[] obtenerLlaveCompartida() {
        return ka.generateSecret();
    }
}


