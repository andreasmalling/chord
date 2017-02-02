package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.glassfish.jersey.server.mvc.Template;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path(value = "/")
@Singleton
public class MyResource
{
    private Node n;

    public MyResource() {
        long id = Integer.decode("0x" + DigestUtils.sha1Hex(Main.BASE_URI).substring(0,6));
        this.n = new Node(id);
    }

    @Template(name = "/index.mustache")
    @GET
    public Context getStatus() {
        return new Context(n.getID());
    }

    public static class Context {
        public Long value;

        public Context(final Long value) {
            this.value = value;
        }
    }
}