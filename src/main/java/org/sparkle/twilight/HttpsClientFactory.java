package org.sparkle.twilight;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class HttpsClientFactory {
    private static CloseableHttpClient client;

    public HttpClient getHttpsClient() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {

        if (client != null) {
            return client;
        }
        //SSLContext sslcontext = getSSLContext();
        // Added sslcontext dependant on empty trustmanager
        SSLContext sslcontext = SSLContexts.custom().useSSL().build();
        sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());

        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        client = HttpClients.custom().setSSLSocketFactory(factory).build();

        return client;
    }

    public static void releaseInstance() {
        client = null;
    }

    private SSLContext getSSLContext() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
        char[] pass = "Pa$$w0rd".toCharArray();
        /*SSLContextConfigurator sslCon = new SSLContextConfigurator(false);
        sslCon.setKeyStoreType("jceks");
        sslCon.setKeyStoreFile("keystore.jceks"); // contains server keypair
        sslCon.setKeyPass(pass);
        return sslCon.createSSLContext();*/
        KeyStore trustStore = KeyStore.getInstance("jceks");
        FileInputStream instream = new FileInputStream(new File("keystore.jceks"));
        try {
            trustStore.load(instream, pass);
        } finally {
            instream.close();
        }
        return SSLContexts.custom()
                .loadTrustMaterial(trustStore)
                .build();
    }
}
