package org.sparkle.twilight;

import org.junit.Test;

import java.security.cert.Certificate;

/**
 * Created by Root on 11/4/2017.
 */
public class SecurityManagerTest {
    @Test
    public void getCert() throws Exception {
        SecurityManager CM = new SecurityManager();
        Certificate cert = CM.getCert();
        System.out.println(cert.toString());
    }

}