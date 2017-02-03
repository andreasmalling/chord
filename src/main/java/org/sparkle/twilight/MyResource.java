package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.glassfish.jersey.server.mvc.Template;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path(value = "/")
@Singleton
public class MyResource {
    public static final String LOOKUPPATH = "/lookup";
    public static final String RECEIVEPATH = "/receive";

    private Node n;

    public MyResource() {
        this.n = new Node();
    }

    @Template(name = "/index.mustache")
    @GET
    public Context getStatus() {
        return new Context(n.getID() + "", n.getSuccessor() + "");
    }

    @Path(LOOKUPPATH)
    @POST
    public Response lookup( String request ) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jreqeust = (JSONObject) parser.parse(request);
            int key = (int) jreqeust.get(JSONformat.KEY);
            String address = (String) jreqeust.get(JSONformat.ADDRESS);

            n.lookup(key, address);

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
    @Consumes("application/json")
    public Response response(String request){
        JSONParser parser = new JSONParser();
        try {
            JSONObject jreqeust = (JSONObject) parser.parse(request);
            int key = (int) jreqeust.get(JSONformat.KEY);
            String address = (String) jreqeust.get(JSONformat.ADDRESS);
            if (n.isInNetwork()) {
                //TODO key lookup call
            } else {
                n.joinRing(address);
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

        public Context(final String id, final String succ) {
            this.id = id;
            this.succ = succ;
        }
    }
}