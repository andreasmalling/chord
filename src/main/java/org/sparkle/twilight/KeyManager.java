package org.sparkle.twilight;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeyManager {
    private KeyPairGenerator gen;
    private final String ALGORITHM = "RSA";
    private final String HASHPLUSALGORITHM = "SHA256with" + ALGORITHM;
        public KeyManager(int keySize) {
            try {
                gen = KeyPairGenerator.getInstance(ALGORITHM);
                gen.initialize(keySize);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        public KeyPair getKeys() {
            return gen.generateKeyPair();
        }

    /**
     *
     * @param clearText, may not exceed keysize/8-11 bytes
     * @param key
     * @return
     */
        public byte[] encrypt(byte[] clearText, PublicKey key) {
            byte[] cipherText = null;
            try {
                final Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                cipherText = cipher.doFinal(clearText);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cipherText;
        }

        public byte[] decrypt(byte[] cipherText, PrivateKey key) {
            byte[] dectyptedText = null;
            try {
                final Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key);
                dectyptedText = cipher.doFinal(cipherText);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return dectyptedText;
        }

        public byte[] sign(byte[] data, PrivateKey key) {
            byte[] signedData = null;
            try {
                Signature sig = Signature.getInstance(HASHPLUSALGORITHM);
                sig.initSign(key);
                sig.update(data);
                signedData = sig.sign();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                e.printStackTrace();
            }
            return signedData;
        }

    public boolean verify(byte[] data, byte[] signature, PublicKey key) {
        boolean judgment = false;
        try {
            Signature sig = Signature.getInstance(HASHPLUSALGORITHM);
            sig.initVerify(key);
            sig.update(data);
            judgment = sig.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return judgment;
    }

    public String encodePublicKey(PublicKey key) {
        return Base64.encodeBase64String(key.getEncoded());
    }

    public PublicKey decodePublicKey(String string) {
        PublicKey pubKey = null;
        byte[] publicBytes = Base64.decodeBase64(string);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(ALGORITHM);
            pubKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return pubKey;
    }

}
