package org.sparkle.twilight;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path(value = "/app/")
@Singleton
public class AppResource {

    private App app;

    public AppResource() {
        this.app = new App(ChordResource.getNode());
    }

    @Template(name = "/appIndex.mustache")
    @GET
    @Produces("text/html")
    public IndexContext getIndexHtml() {
        JSONArray index = app.getIndex();
        return new IndexContext(index);
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
        JSONObject topic = app.getTopic(id);
        return new TopicContext(topic);
    }

    @GET
    @Path("{id}")
    @Produces(JSONFormat.JSON)
    public String getTopicJson(@PathParam("id") String id) {
        return "";
    }

    private class IndexContext {

        public JSONArray index;
        public String myUrl;

        public IndexContext(JSONArray index) {
            this.index = index;
            this.myUrl = Main.BASE_URI;
        }
    }

    private class TopicContext {
        public String title;
        public String message;
        public JSONArray replies;

        public TopicContext(JSONObject topic) {
            title = (String) topic.get(JSONFormat.TITLE);
            message = (String) topic.get(JSONFormat.MESSAGE);
            replies = (JSONArray) topic.get(JSONFormat.REPLIES);
        }
    }
}
