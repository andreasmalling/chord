package org.sparkle.twilight;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.server.mvc.Template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path(value = "/app/")
@Immediate
public class AppResource {
    @Template(name = "/appIndex.mustache")
    @GET
    @Produces("text/html")
    public IndexContext getIndexHtml() {
        return new IndexContext();
    }

    @GET
    @Produces(JSONFormat.JSON)
    public String getIndexJson() {
        return "";
    }

    @Template(name = "/appTopic.mustache")
    @GET
    @Path("{id}")
    @Produces("text/html")
    public TopicContext getTopicHtml(@PathParam("id") String id) {
        return new TopicContext();
    }

    @GET
    @Path("{id}")
    @Produces(JSONFormat.JSON)
    public String getTopicJson(@PathParam("id") String id) {
        return null;
    }

    private class IndexContext {

    }

    private class TopicContext {
    }
}
