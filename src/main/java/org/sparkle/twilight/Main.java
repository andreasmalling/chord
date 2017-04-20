package org.sparkle.twilight;

import org.apache.http.client.HttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.mustache.MustacheMvcFeature;

import java.io.IOException;
import java.net.URI;

/**
 * Main class.
 *
 */
public class Main {
    private static SecurityManager SM = null;
    // Base URI the Grizzly HTTP server will listen on
    public static String BASE_URI;
    public static String BASE_PORT;
    public static String ENTRY_POINT;
    public static HttpServer server;
    public static HttpsUtil httpUtil;


    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in org.sparkle.twilight package
        final ResourceConfig rc = new ResourceConfig().property(
                MustacheMvcFeature.TEMPLATE_BASE_PATH, "templates")
                .register(MustacheMvcFeature.class)
                .packages("org.sparkle.twilight")
                .register(ImmediateFeature.class);
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static HttpServer startSecureServer(){
        SM = new SecurityManager(BASE_PORT);
        final ResourceConfig rc = new ResourceConfig().property(
                MustacheMvcFeature.TEMPLATE_BASE_PATH, "templates")
                .packages("org.sparkle.twilight")
                .register(MustacheMvcFeature.class)
                .register(ImmediateFeature.class);

        SSLContextConfigurator sslCon = new SSLContextConfigurator(false);
        sslCon.setKeyStoreType(SM.TYPE);
        sslCon.setKeyStoreFile(SM.KEYSTORE); // contains server keypair
        sslCon.setKeyPass(SM.PASSWORD);
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc
                , true, new SSLEngineConfigurator(sslCon, false, false, true));

        return server;
        }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            //Create chord ring at args 0
            BASE_PORT = args[0];
            BASE_URI = "https://localhost:" + BASE_PORT + "/";
        } else if (args.length == 2){
            //Join chord ring at args 1
            BASE_PORT = args[0];
            BASE_URI = "https://localhost:" + BASE_PORT + "/";
            ENTRY_POINT = "https://localhost:" + args[1] + "/";
        } else {
            BASE_PORT = args[0];
            //Create chord ring at port 8080
            BASE_URI = "https://localhost:" + BASE_PORT + "/";
        }
        httpUtil = new HttpsUtil();
        server = startSecureServer();

        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        for (String s: args)
            System.out.println(s);
        System.in.read();
        server.shutdownNow();
    }
}

