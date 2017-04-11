package org.sparkle.twilight;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.mustache.MustacheMvcFeature;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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

    // Base URI the Grizzly HTTP server will listen on
    public static String BASE_URI;
    public static String ENTRY_POINT;
    public static HttpServer server;
    public static HttpUtil httpUtil;
    public static SecurityManager SM;

    @Before
    public void setUp() throws Exception {
        SM = new SecurityManager();
    }

    private TrustManager[] getTrustManager() {
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }
                }
        };
    }

    @Test
    public void httpsServer() throws Exception {
        BASE_URI = "https://localhost:8080/";

        /**
         * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
         * @return Grizzly HTTP server.
         */
        // create a resource config that scans for JAX-RS resources and providers
        // in org.sparkle.twilight package
        final ResourceConfig rc = new ResourceConfig().property(
                MustacheMvcFeature.TEMPLATE_BASE_PATH, "templates")
                .register(MustacheMvcFeature.class)
                .register(ImmediateFeature.class);


        SSLContextConfigurator sslCon = new SSLContextConfigurator(true);

        sslCon.setKeyStoreType("jceks");
        sslCon.setKeyStoreFile("keystore.jceks"); // contains server keypair
        SSLContext sslContext = sslCon.createSSLContext();
        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, getTrustManager(), new java.security.SecureRandom());
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc
                , true, new SSLEngineConfigurator(sslContext, false, false, true));

        server.start();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.shutdownNow();
    }
}