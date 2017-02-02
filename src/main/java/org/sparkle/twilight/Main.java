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

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in org.sparkle.twilight package
        final ResourceConfig rc = new ResourceConfig().property(
                MustacheMvcFeature.TEMPLATE_BASE_PATH, "templates"
        ).register(
                MustacheMvcFeature.class
        ).packages("org.sparkle.twilight");

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
        if (args.length == 2) {
            BASE_URI = "http://" + args[0] + ":" + args[1] + "/myapp/";
        } else {
            BASE_URI = "http://localhost:8080/myapp/";
        }
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        for (String s: args)
            System.out.println(s);
        System.out.println(MustacheMvcFeature.TEMPLATE_BASE_PATH);
        System.in.read();
        server.stop();
    }
}

