package org.sparkle.twilight;


import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.PublicKey;

import static org.junit.Assert.*;

/**
 * Created by kgoyo on 10-04-2017.
 */
public class KeyManagerTest {
    KeyManager man;
    private String message;
    private KeyPair pair;

    @Before
    public void setUp() {
        man = new KeyManager(2048);
        message = "hej Karl";
        pair = man.getKeys();
    }

    @Test
    public void testEncryptDecrypt() {
        byte[] cipherText = man.encrypt(message.getBytes(),pair.getPublic());
        byte[] clearText = man.decrypt(cipherText,pair.getPrivate());

        assertEquals("decryted message should be the same as original",message, new String(clearText));
        assertNotEquals("message must not be the same as encrypted message",cipherText, message.getBytes());
    }

    @Test
    public void testSignVerify() {
        byte[] signature = man.sign(message.getBytes(),pair.getPrivate());
        boolean b = man.verify(message.getBytes(), signature,pair.getPublic());

        assertTrue("should verify to true", b);
    }

    @Test
    public void encodeDecodePkey() {
        PublicKey pkey = pair.getPublic();
        String pkeyString = man.encodePublicKey(pkey);
        PublicKey pkey2 = man.decodePublicKey(pkeyString);
        assertEquals("keys should be the same", pkey, pkey2);
    }

}