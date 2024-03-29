package org.sparkle.twilight;

import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
    public static final String SUCCESSORLISTPATH = "successor/list";
    public static final String DATABASE = "database";
    private static Node n;
    private final JSONParser parser = new JSONParser();
    private final int lookupListSize = 5;
    private static final Logger LOGGER = Logger.getLogger(ChordResource.class.getName());

    public ChordResource() {
        LoggerHandlers.addHandlers(LOGGER);
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

        JSONObject jRequest;
        try {
            jRequest = (JSONObject) parser.parse(jsonstring);
            String type = jRequest.get(JSONFormat.TYPE).toString();
            String url = jRequest.get(JSONFormat.VALUE).toString();
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

        JSONObject jRequest;
        try {
            jRequest = (JSONObject) parser.parse(jsonstring);
            String type = jRequest.get(JSONFormat.TYPE).toString();
            String url = jRequest.get(JSONFormat.VALUE).toString();
            n.setSuccessor(url);
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(403).build();
        }
        return Response.ok().build();
    }

    @Path(SUCCESSORLISTPATH)
    @GET
    @Produces(JSONFormat.JSON)
    public String getSuccessorList() {
        JSONObject json = new JSONObject();
        json.put(JSONFormat.TYPE, JSONFormat.SUCCESSORLIST);
        json.put(JSONFormat.VALUE, n.getSuccessorList());

        return json.toJSONString();
    }

    @Path("lookup/{method}")
    @POST
    @Consumes(JSONFormat.JSON)
    public Response lookup(String request, @PathParam("method") String method) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            int key = Integer.parseInt(jRequest.get(JSONFormat.KEY).toString());
            String address = jRequest.get(JSONFormat.ADDRESS).toString();
            int hops = Integer.parseInt(jRequest.get(JSONFormat.HOPS).toString());
            boolean showInConsole = Boolean.parseBoolean(jRequest.get(JSONFormat.SHOWINCONSOLE).toString());
            Runnable lookupThread = () -> n.lookup(key, address, new JSONProperties(method, hops, showInConsole));
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

    @Path("lookup/{method}")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response lookupForm(@FormParam("key") String key, @PathParam("method") String method) {
        //should be finger
        Runnable lookupThread = () -> n.lookup(Integer.parseInt(key), n.address, new JSONProperties(method, 0, true));
        new Thread(lookupThread).start();
        return Response.ok().build();
    }

    @Path(RECEIVEPATH) //join if not in network, otherwise receive address for key lookup
    @POST
    @Consumes(JSONFormat.JSON)
    public Response response(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            int key = Integer.parseInt(jRequest.get(JSONFormat.KEY).toString());
            int hops = Integer.parseInt(jRequest.get(JSONFormat.HOPS).toString());
            String address = jRequest.get(JSONFormat.ADDRESS).toString();
            boolean showInConsole = Boolean.parseBoolean(jRequest.get(JSONFormat.SHOWINCONSOLE).toString());
            Runnable handleThread = () -> handleReceive(showInConsole, address, key, hops);
            new Thread(handleThread).start();
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }
        return Response.ok().build();
    }

    private void handleReceive(boolean showInConsole, String address, int key, int hops) {
        LOGGER.info("hops to " + address + " value: " + hops);
        if (n.isInNetwork()) {
            if (showInConsole) {
                while (n.getAddresses().size() > lookupListSize) {
                    n.getAddresses().remove(n.getAddresses().size() - 1);
                }
                n.getAddresses().add(0, "The address responsible for key: " + key + " is: " + address + " (took " + hops + " hops)");
            }
            n.updateFingerTable(key, address);

            Map<Integer, Instruction> instMap = n.getInstructionMap();
            Instruction inst = instMap.get(key);
            if (inst != null) {
                instMap.remove(key);
                Runnable execThread = () -> n.executeInstruction(inst, address);
                new Thread(execThread).start();
            }

        } else {
            Runnable joinThread = () -> n.joinRing(address);
            new Thread(joinThread).start();
        }
    }

    @Path("database/{key}")
    @POST
    @Consumes(JSONFormat.JSON)
    public Response updateDatabase(@PathParam("key") String key, String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            Object value = jRequest.get(JSONFormat.VALUE);
            n.putObjectInStorage(key, value);
            Runnable replicateThread = () -> n.replicateData(key, jRequest);
            new Thread(replicateThread).start();
        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }
        return Response.ok().build();
    }

    @Path(DATABASE)
    @GET
    @Produces("text/plain")
    public String dumpDatabase() {
        return n.getDatabaseAsString();
    }

    @Path(DATABASE)
    @POST
    @Consumes(JSONFormat.JSON)
    public Response bulkInsertDatabase(String json) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(json);
            JSONArray jsonArray = (JSONArray) jRequest.get(JSONFormat.DATA);

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                Object key = jsonObject.get(JSONFormat.KEY);
                Object value = jsonObject.get(JSONFormat.VALUE);
                n.putObjectInStorage(key, value);
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

    public static Node getNode() {
        return n;
    }

    public static class Context {
        public String id;
        public String succ;
        public String pred;
        public List<String> addresses;
        public List<String> successors;
        public List<Finger> fingerTable;

        public Context(final Node node) {
            this.id = node.getID() + "";
            this.succ = node.getSuccessor();
            this.pred = node.getPredecessor();
            this.addresses = node.getAddresses();
            this.successors = node.getSuccessorList();
            this.fingerTable = node.getFingerTable();
        }
    }
}