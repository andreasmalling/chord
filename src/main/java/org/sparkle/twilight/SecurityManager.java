package org.sparkle.twilight;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SecurityManager {

    private CertAndKeyGen keyGen;
    private X509Certificate cert;
    private KeyStore keyStore;

    public SecurityManager(){
        try{
            keyGen = new CertAndKeyGen("RSA", "SHA256WithRSA", null);
            keyGen.generate(2048);
            cert = keyGen.getSelfCertificate(new X500Name(""), 365 * 24 * 3600);
            keyStore = KeyStore.getInstance("jceks");
            Certificate[] certArray = {cert};
            keyStore.setKeyEntry("private_key", keyGen.getPrivateKey().getEncoded(),certArray);
            keyStore.setCertificateEntry("cert",cert);
            keyStore.store(null);
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
