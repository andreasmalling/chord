package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class Node implements ChordNode {
    private int id;
    private String successor;
    private String predecessor;
    private final String address  = Main.BASE_URI;
    private HttpClient client;
    private boolean inNetwork = false;

    public Node() {
        id = generateID();

        // Create Initial Ring
        setSuccessor(address);
        setPredecessor(address);

        inNetwork = true;
    }

    public Node( String entryPoint ){
        id = generateID();

        // Join Ring

        client = HttpClientBuilder.create().build();

        performLookup(entryPoint, id, address);

        //   Lookup new successor --> Set as successor
        //   Lookup Successor's predecessor --> Set pred's Successor to this node.
    }



    private int generateID( ){
        return Integer.decode("0x" + DigestUtils.sha1Hex(address).substring(0,2));
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getSuccessor() {
        return successor;
    }

    @Override
    public String getPredecessor() {
        return predecessor;
    }

    @Override
    public void setSuccessor(String successor) {
        this.successor = successor;
    }

    @Override
    public void setPredecessor(String predecessor) {
        this.predecessor = predecessor;
    }

    @Override
    public void joinRing (String address) {
        setSuccessor(address);
        //TODO set succesors predessor to self
        JSONObject json = httpGetRequest(getSuccessor()+MyResource.PREDECESSORPATH);
        String predurl = (String)json.get(JSONformat.URL);
        setPredecessor(predurl);

        //post to predeccessor of successor
        JSONObject postjson = new JSONObject();
        postjson.put(JSONformat.TYPE,JSONformat.SUCCESSOR);
        postjson.put(JSONformat.URL,this.address);
        httpPostRequest(predurl+MyResource.SUCCESSORPATH,postjson);

        //post to successor of self
        JSONObject postjson2 = new JSONObject();
        postjson2.put(JSONformat.TYPE,JSONformat.PREDECESSOR);
        postjson2.put(JSONformat.URL,this.address);
        httpPostRequest(getSuccessor()+MyResource.PREDECESSORPATH,postjson2);

        inNetwork = true;

    }

    @Override
    public void leaveRing() {
        // Find predecessor and set it's Successor to this node's Successor
    }

    @Override
    public void lookup(int key, String initiator) {
        if (id >= key) {
            performQueryRepsonse(initiator, key);
        } else {
            performLookup(successor, key, initiator);
        }
    }

    private void performLookup(String reciever, int id, String address) {
        String url = reciever + MyResource.LOOKUPPATH;

        JSONObject json = new JSONObject();
        json.put(JSONformat.KEY, id);
        json.put(JSONformat.ADDRESS, address);
        httpPostRequest(url, json);
    }

    private void performQueryRepsonse(String receiver, int key) {
        String url = receiver + MyResource.RECEIVEPATH;

        JSONObject json = new JSONObject();
        json.put(JSONformat.KEY, key); //the search key is returned to sender in case they are doing multiple queries
        json.put(JSONformat.ADDRESS, address);
        httpPostRequest(url, json);

    }

    private void httpPostRequest(String url, JSONObject body) {
        HttpPost postMsg = new HttpPost(url);
        try {
            StringEntity params = new StringEntity(body.toJSONString());
            postMsg.addHeader("content-type", JSONformat.JSON);
            postMsg.setEntity(params);
            HttpResponse response = client.execute(postMsg);

            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("ERROR ERROR");     //TODO ??
                throw new IOException();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject httpGetRequest(String url){
        HttpGet getMsg = new HttpGet(url);
        try {
            HttpResponse response = client.execute(getMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("ERROR ERROR: Could not get");     //TODO ??
                throw new IOException();
            }
            BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(
                                            response.getEntity().getContent()));
            String jsonstring = "";
            String line;
            while ((line = reader.readLine()) != null){
                jsonstring+=line;
            }
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonstring);
            return json;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null; //TODO fix mabye?
    }

    @Override
    public boolean isInNetwork() {
        return inNetwork;
    }
}
