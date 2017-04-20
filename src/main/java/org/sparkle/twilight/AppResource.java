package org.sparkle.twilight;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.inject.Singleton;
import javax.ws.rs.*;

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

    @Path("posttopic")
    @POST
    public void postTopicForm(@FormParam("title") String title, @FormParam("message") String message) {
        app.postTopic(title,message);
    }

    @Template(name = "/appTopic.mustache")
    @GET
    @Path("{id}")
    @Produces("text/html")
    public TopicContext getTopicHtml(@PathParam("id") String id) {
        JSONObject topic = app.getTopic(id);
        return new TopicContext(topic, id);
    }

    @GET
    @Path("{id}")
    @Produces(JSONFormat.JSON)
    public String getTopicJson(@PathParam("id") String id) {
        return "";
    }

    @Path("{id}/reply")
    @POST
    public void replyToTopic(@PathParam("id") String id, @FormParam("message") String message) {
        app.replyToTopic(id,message);
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
        public String id;
        public String title;
        public String message;
        public JSONArray replies;

        public TopicContext(JSONObject topic, String id) {
            title = (String) topic.get(JSONFormat.TITLE);
            message = (String) topic.get(JSONFormat.MESSAGE);
            replies = (JSONArray) topic.get(JSONFormat.REPLIES);
            this.id = id;
        }
    }
}
