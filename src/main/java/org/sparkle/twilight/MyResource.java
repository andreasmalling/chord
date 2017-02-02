package org.sparkle.twilight;

import org.glassfish.jersey.server.mvc.Template;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path(value = "/")
@Singleton
public class MyResource
{
    private Node n;

    public MyResource() {
        this.n = new Node();
    }

    @Template(name = "/index.mustache")
    @GET
    public Context getStatus() {
        return new Context(n.get());
    }

    public static class Context {
        public Integer value;

        public Context(final Integer value) {
            this.value = value;
        }
    }
}