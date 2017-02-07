package org.sparkle.twilight;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path(value = "/")
@Immediate
public class MyResource {
    public static final String LOOKUPPATH = "lookup";
    public static final String RECEIVEPATH = "receive";
    public static final String PREDECESSORPATH = "predecessor";
    public static final String SUCCESSORPATH = "successor";

    private Node n;
    private final JSONParser parser = new JSONParser();

    public MyResource() {
        if (Main.ENTRY_POINT == null) {
            this.n = new Node();
        } else {
            this.n = new Node(Main.ENTRY_POINT);
        }
    }

    @Template(name = "/index.mustache")
    @GET
    public Context getStatus() {
        return new Context(n.getID() + "", n.getSuccessor(), n.getPredecessor());
    }

    @Path(PREDECESSORPATH)
    @GET
    @Produces(JSONformat.JSON)
    public String getPredecessor() {

        JSONObject json = new JSONObject();
        json.put(JSONformat.TYPE, JSONformat.PREDECESSOR);
        json.put(JSONformat.URL, n.getPredecessor());

        return json.toJSONString();
    }

    @Path(PREDECESSORPATH)
    @POST
    @Consumes(JSONformat.JSON)
    public Response setPredecessor(String jsonstring) {

        JSONObject jreqeust;
        try {
            jreqeust = (JSONObject) parser.parse(jsonstring);
            String type = jreqeust.get(JSONformat.TYPE).toString();
            String url = jreqeust.get(JSONformat.URL).toString();
            n.setPredecessor(url);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(403).build();
        }
        return Response.ok().build();
    }

    @Path(SUCCESSORPATH)
    @GET
    @Produces(JSONformat.JSON)
    public String getSuccessor() {

        JSONObject json = new JSONObject();
        json.put(JSONformat.TYPE, JSONformat.SUCCESSOR);
        json.put(JSONformat.URL, n.getSuccessor());

        return json.toJSONString();
    }

    @Path(SUCCESSORPATH)
    @POST
    @Consumes(JSONformat.JSON)
    public Response setSuccessor(String jsonstring) {

        JSONObject jreqeust;
        try {
            jreqeust = (JSONObject) parser.parse(jsonstring);
            String type = jreqeust.get(JSONformat.TYPE).toString();
            String url = jreqeust.get(JSONformat.URL).toString();
            n.setSuccessor(url);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(403).build();
        }
        return Response.ok().build();
    }

    @Path(LOOKUPPATH)
    @POST
    public Response lookup(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            int key = Integer.parseInt(jRequest.get(JSONformat.KEY).toString());
            String address = jRequest.get(JSONformat.ADDRESS).toString();
            Runnable lookupThread = () -> n.lookup(key, address);
            new Thread(lookupThread).start();
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }
        return Response.ok().build();
    }

    /*
    JSON object for response:
    {
        "key": 312
        "address": "http://localhost:6543"
    }
     */

    @Path(RECEIVEPATH) //join if not entering network, otherwise receive address for key lookup
    @POST
    @Consumes(JSONformat.JSON)
    public Response response(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jreqeust = (JSONObject) parser.parse(request);
            int key = Integer.parseInt(jreqeust.get(JSONformat.KEY).toString());
            String address = jreqeust.get(JSONformat.ADDRESS).toString();
            if (n.isInNetwork()) {
                //TODO key lookup call
            } else {
                Runnable joinThread = () -> n.joinRing(address);
                new Thread(joinThread).start();
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }

        return Response.ok().build();
    }


    public static class Context {
        public String id;
        public String succ;
        public String pred;

        public Context(final String id, final String succ, final String pred) {
            this.id = id;
            this.succ = succ;
            this.pred = pred;
        }
    }
}