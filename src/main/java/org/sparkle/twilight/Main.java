package org.sparkle.twilight;

import org.glassfish.grizzly.http.server.HttpServer;
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
    // Base URI the Grizzly HTTP server will listen on
    public static String BASE_URI;
    public static String ENTRY_POINT;
    public static HttpServer server;

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

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            //Create chord ring at args 0
            BASE_URI = "http://localhost:" + args[0] + "/";
        } else if (args.length == 2){
            //Join chord ring at args 1
            BASE_URI = "http://localhost:" + args[0] + "/";
            ENTRY_POINT = "http://localhost:" + args[1] + "/";
        } else {
            //Create chord ring at port 8080
            BASE_URI = "http://localhost:8080/";
        }
        server = startServer();

        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        for (String s: args)
            System.out.println(s);
        System.in.read();
        server.shutdownNow();
    }
}

