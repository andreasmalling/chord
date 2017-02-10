package org.sparkle.twilight;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * Root resource
 */
@Path(value = "/")
@Immediate
public class ChordResource {
    public static final String LOOKUPPATH = "lookup";
    public static final String RECEIVEPATH = "receive";
    public static final String LEAVEPATH = "leave";
    public static final String PREDECESSORPATH = "predecessor";
    public static final String KILLPATH = "kill";
    public static final String SUCCESSORPATH = "successor";

    private Node n;
    private final JSONParser parser = new JSONParser();

    public ChordResource() {
        if (Main.ENTRY_POINT == null) {
            this.n = new Node();
        } else {
            this.n = new Node(Main.ENTRY_POINT);
        }
    }

    @Template(name = "/index.mustache")
    @GET
    public Context getStatus() {
        return new Context(n);
    }

    @Path(PREDECESSORPATH)
    @GET
    @Produces(JSONFormat.JSON)
    public String getPredecessor() {

        JSONObject json = new JSONObject();
        json.put(JSONFormat.TYPE, JSONFormat.PREDECESSOR);
        json.put(JSONFormat.VALUE, n.getPredecessor());

        return json.toJSONString();
    }

    @Path(PREDECESSORPATH)
    @POST
    @Consumes(JSONFormat.JSON)
    public Response setPredecessor(String jsonstring) {

        JSONObject jreqeust;
        try {
            jreqeust = (JSONObject) parser.parse(jsonstring);
            String type = jreqeust.get(JSONFormat.TYPE).toString();
            String url = jreqeust.get(JSONFormat.VALUE).toString();
            n.setPredecessor(url);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(403).build();
        }
        return Response.ok().build();
    }

    @Path(SUCCESSORPATH)
    @GET
    @Produces(JSONFormat.JSON)
    public String getSuccessor() {

        JSONObject json = new JSONObject();
        json.put(JSONFormat.TYPE, JSONFormat.SUCCESSOR);
        json.put(JSONFormat.VALUE, n.getSuccessor());

        return json.toJSONString();
    }

    @Path(SUCCESSORPATH)
    @POST
    @Consumes(JSONFormat.JSON)
    public Response setSuccessor(String jsonstring) {

        JSONObject jreqeust;
        try {
            jreqeust = (JSONObject) parser.parse(jsonstring);
            String type = jreqeust.get(JSONFormat.TYPE).toString();
            String url = jreqeust.get(JSONFormat.VALUE).toString();
            n.setSuccessor(url);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(403).build();
        }
        return Response.ok().build();
    }

    @Path(LOOKUPPATH)
    @POST
    @Consumes(JSONFormat.JSON)
    public Response lookup(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            int key = Integer.parseInt(jRequest.get(JSONFormat.KEY).toString());
            String address = jRequest.get(JSONFormat.ADDRESS).toString();
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

    @Path(LOOKUPPATH)
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response lookupForm(@FormParam("key") String key) {
        Runnable lookupThread = () -> n.lookup(Integer.parseInt(key), n.address);
        new Thread(lookupThread).start();
        return Response.ok().build();
    }

    @Path(RECEIVEPATH) //join if not in network, otherwise receive address for key lookup
    @POST
    @Consumes(JSONFormat.JSON)
    public Response response(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jreqeust = (JSONObject) parser.parse(request);
            int key = Integer.parseInt(jreqeust.get(JSONFormat.KEY).toString());
            String address = jreqeust.get(JSONFormat.ADDRESS).toString();
            if (n.isInNetwork()) {
                if (n.getAddresses().size() > 10) {
                    n.getAddresses().remove(n.getAddresses().size() - 1);
                }
                n.getAddresses().add(0, "The address responsible for key: " + key + " is: " + address);
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

    @Path(LEAVEPATH)
    @POST
    public Response leave() {
        Runnable leaveThread = () -> n.leaveRing();
        new Thread(leaveThread).start();
        return Response.seeOther(URI.create(n.getSuccessor())).build();
    }

    @Path(KILLPATH)
    @POST
    public void kill() {
        n.killNode();
    }

    public static class Context {
        public String id;
        public String succ;
        public String pred;
        public List<String> addresses;

        public Context(final Node node) {
            this.id = node.getID() + "";
            this.succ = node.getSuccessor();
            this.pred = node.getPredecessor();
            this.addresses = node.getAddresses();
        }
    }
}