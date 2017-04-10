package org.sparkle.twilight;

import javax.crypto.Cipher;
import java.security.*;

public class KeyManager {
    private KeyPairGenerator gen;
    private final String ALGORITHM = "RSA";
        public KeyManager(int keySize) {
            try {
                gen = KeyPairGenerator.getInstance(ALGORITHM);
                gen.initialize(keySize);
            } catch (NoSuchAlgorithmException e) {
                //we should never be here if we coded it correctly
                e.printStackTrace();
            }
        }

        public KeyPair getKeys() {
            return gen.generateKeyPair();
        }

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

}
