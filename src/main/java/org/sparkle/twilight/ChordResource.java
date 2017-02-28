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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public static final String RESOURCEPATH = "resource";
    public static final String DATABASE = "database";

    private Node n;
    private final JSONParser parser = new JSONParser();
    private final int lookupListSize = 5;

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

        } catch (ParseException e) {
            e.printStackTrace();
            return Response.status(400).build(); //Code 400: Bad Request due to malformed JSON
        }
        return Response.ok().build();
    }

    @Path("database/{type}") //join if not in network, otherwise receive address for key lookup
    @POST
    @Consumes(JSONFormat.JSON)
    public Response updateDatabase(String request, @PathParam("type") String type) {
        JSONParser parser = new JSONParser();
        try {
        JSONObject jRequest = (JSONObject) parser.parse(request);
            if(type.equals(ChordStorage.DATA_KEY)){
                Object value = parser.parse(jRequest.get(JSONFormat.VALUE).toString());
                if (value instanceof JSONArray) {
                    ArrayList<String> data = new ArrayList<String>((JSONArray) value);
                    n.overwriteData(data);
                } else {
                    n.upsertDatabase(type, value.toString(), true);
                }
            } else {
                String value = jRequest.get(JSONFormat.VALUE).toString();
                n.upsertDatabase(type, value, true);
            }
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

    @Path(RESOURCEPATH)
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response putResourceWeb(@FormParam("id") String id, @FormParam("token") String token) {

        JSONObject jRequest = new JSONObject();
        jRequest.put(JSONFormat.ID,id);
        jRequest.put(JSONFormat.ACCESSTOKEN,token);
        Runnable putResourceThread = () -> n.handlePutResource(jRequest);
        new Thread(putResourceThread).start();
        return Response.ok().build();
    }


    @Path(RESOURCEPATH)
    @PUT
    @Consumes(JSONFormat.JSON)
    public Response putResource(String request) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jRequest = (JSONObject) parser.parse(request);
            Runnable putResourceThread = () -> n.handlePutResource(jRequest);
            new Thread(putResourceThread).start();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @Path(RESOURCEPATH)
    @GET
    @Produces(JSONFormat.JSON)
    public String getResource() {
        DataSource d = n.getDataSource();
        JSONObject json = new JSONObject();
        if (d == null) {
            json.put(JSONFormat.HASDATA, false);
            json.put(JSONFormat.DATA, null);
        } else {
            String data = d.getData();
            json.put(JSONFormat.HASDATA, true);
            json.put(JSONFormat.DATA, data);
        }
        return json.toJSONString();
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
        public List<String> successors;
        public List<Finger> fingerTable;
        public boolean hasData = false;
        public String data = "";
        public String getseturl = "";
        public boolean hasDataForm;

        public Context(final Node node) {
            this.id = node.getID() + "";
            this.succ = node.getSuccessor();
            this.pred = node.getPredecessor();
            this.addresses = node.getAddresses();
            this.successors = node.getSuccessorList();
            this.fingerTable = node.getFingerTable();
            this.data = node.getData();
            if (!this.data.equals(DataSource.DATA_NOT_AVAILABLE)) {
                this.hasData = true;
                DataSource dataSource = node.getDataSource();
                if (dataSource != null) {
                    this.hasDataForm = true;
                    this.getseturl = dataSource.getSetUrl();
                }
            }
        }
    }
}