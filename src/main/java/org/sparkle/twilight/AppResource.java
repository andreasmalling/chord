package org.sparkle.twilight;

import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

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
        app.updateIndex();
        JSONArray index = app.getIndex();
        return new IndexContext(index);
    }

    @GET
    @Produces(JSONFormat.JSON)
    public String getIndexJson() {
        JSONObject packedJson = new JSONObject(); //pack the jsonarray so that our httpclient supports it
        packedJson.put(JSONFormat.VALUE, app.getIndex());
        return packedJson.toJSONString();
    }

    @POST
    @Consumes(JSONFormat.JSON)
    public Response appendToIndex(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            app.addNewTopicToIndex(jRequest);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }
        return Response.ok().build();
    }

    @Path("posttopic")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public void postTopicForm(@FormParam("title") String title, @FormParam("message") String message) {
        app.postTopic(title, message);
    }


    @Template(name = "/appTopic.mustache")
    @GET
    @Path("{id}")
    @Produces("text/html")
    public TopicContext getTopicHtml(@PathParam("id") String id) {
        app.updateTopic(id);
        JSONObject topic = app.getTopic(id);
        return new TopicContext(topic, id);
    }

    @GET
    @Path("{id}")
    @Produces(JSONFormat.JSON)
    public String getTopicJson(@PathParam("id") String id) {
        return app.getTopic(id).toJSONString();
    }

    @Path("{id}/reply")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public void replyToTopic(@PathParam("id") String id, @FormParam("message") String message) {
        long view = app.getView(id);
        JSONObject messageObj = new JSONObject();
        messageObj.put(JSONFormat.MESSAGE, message);
        messageObj.put(JSONFormat.VIEW, view);
        messageObj.put(JSONFormat.PUBLICKEY, app.getPublicKeyString());
        messageObj.put(JSONFormat.SIGNATURE, app.signMessage(message, app.getTopic(id), view));
        app.replyToTopic(id, messageObj);
    }

    @Path("{id}/reply")
    @POST
    @Consumes(JSONFormat.JSON)
    public Response postTopicReply(String request, @PathParam("id") String id) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject messageObj = (JSONObject) parser.parse(request);
            app.replyToTopic(id, messageObj);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }
        return Response.ok().build();
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
        public long timestamp;

        public TopicContext(JSONObject topic, String id) {
            title = (String) topic.get(JSONFormat.TITLE);
            message = (String) topic.get(JSONFormat.MESSAGE);
            replies = (JSONArray) topic.get(JSONFormat.REPLIES);
            timestamp = (long) topic.get(JSONFormat.TIMESTAMP);
            this.id = id;
        }
    }
}
