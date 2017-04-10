package org.sparkle.twilight;


import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by kgoyo on 10-04-2017.
 */
public class KeyManagerTest {
    KeyManager man;

    @Before
    public void setUp() {
        man = new KeyManager(512);
    }

    @Test
    public void testEncryptDecrypt() {
        String message = "hej Karl";
        KeyPair pair = man.getKeys();
        byte[] cipherText = man.encrypt(message.getBytes(),pair.getPublic());
        byte[] clearText = man.decrypt(cipherText,pair.getPrivate());
        //print cipherText as string just for fun
        System.out.println(new String(cipherText));

        assertEquals("decryted message should be the same as original",message, new String(clearText));
        assertNotEquals("message must not be the same as encrypted message",cipherText, message.getBytes());

    }

}