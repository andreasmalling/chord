package org.sparkle.twilight;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SecurityManager {

    public static String TYPE = "jceks";
    public static String KEYSTORE = null;
    public static char[] PASSWORD = "nopassword".toCharArray();


    private CertAndKeyGen keyGen;
    private X509Certificate cert;
    private KeyStore keyStore;

    public CertAndKeyGen getKeyGen() {
        return keyGen;
    }

    public void setKeyGen(CertAndKeyGen keyGen) {
        this.keyGen = keyGen;
    }

    public void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public SecurityManager(String basePort) {
        KEYSTORE = "keystores/" + basePort + "." + TYPE;
        try {
            keyGen = new CertAndKeyGen("RSA", "SHA256WithRSA", null);
            keyGen.generate(2048);
            cert = keyGen.getSelfCertificate(new X500Name("CN=localhost"), 365 * 24 * 3600);
            keyStore = KeyStore.getInstance(TYPE);
            keyStore.load(null, PASSWORD);
            Certificate[] certArray = {cert};
            System.out.println(keyGen.getPrivateKey().getFormat() + " @@ " + keyGen.getPrivateKey().getClass());
            keyStore.setKeyEntry("PKCS", keyGen.getPrivateKey(), PASSWORD, certArray);
            keyStore.setCertificateEntry("cert", cert);
            keyStore.store(new FileOutputStream(KEYSTORE), PASSWORD);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    public X509Certificate getCert() {
        return cert;
    }

    public PrivateKey getPrivateKey() {
        return keyGen.getPrivateKey();
    }

    public PublicKey getPublicKey() {
        return keyGen.getPublicKey();
    }
}
