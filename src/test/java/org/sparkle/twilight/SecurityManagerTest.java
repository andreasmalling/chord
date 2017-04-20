package org.sparkle.twilight;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.mustache.MustacheMvcFeature;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by Root on 11/4/2017.
 */
public class SecurityManagerTest {
    @Test
    public void getCert() throws Exception {
        SecurityManager CM = new SecurityManager("test");
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
        SM = new SecurityManager("test");
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


        SSLContextConfigurator sslCon = new SSLContextConfigurator(false);
        char[] pass = SM.PASSWORD;
        sslCon.setKeyStoreType(SM.TYPE);
        sslCon.setKeyStoreFile(SM.KEYSTORE); // contains server keypair
        sslCon.setKeyPass(pass);
        //SSLContext sslContext = sslCon.createSSLContext();
        //sslContext = SSLContext.getInstance("TLSv1");
        //sslContext.init(null, getTrustManager(), new java.security.SecureRandom());
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc
                , true, new SSLEngineConfigurator(sslCon, false, false, true));

        server.start();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.shutdownNow();
    }

    @Test
    public void httpsClient(){
        try {
            CloseableHttpClient httpclient = (CloseableHttpClient) new HttpsClientFactory().getHttpsClient();

            HttpContext context = new BasicHttpContext();
            HttpGet test = new HttpGet("https://www.google.com");

            CloseableHttpResponse res = httpclient.execute(test, context);
            System.out.println(res.getStatusLine());
            System.out.println(convertStreamToString(res.getEntity().getContent()));

            ManagedHttpClientConnection man = (ManagedHttpClientConnection) context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
            Certificate[] certs = man.getSSLSession().getPeerCertificates();

            for(Certificate c : certs){
                System.out.println(c.toString());
            }
            /*
            httpclient.getHostConfiguration().setHost("www.whatever.com", 443, myhttps);
            GetMethod httpget = new GetMethod("/");
            try {
                httpclient.executeMethod(httpget);
                System.out.println(httpget.getStatusLine());
            } finally {
                httpget.releaseConnection();
            }*/

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}