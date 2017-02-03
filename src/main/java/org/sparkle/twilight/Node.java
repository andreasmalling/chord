package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Node implements ChordNode {
    private int id;
    private String successor;
    private final String address  = Main.BASE_URI;
    private HttpClient client;
    private boolean inNetwork = false;

    public Node() {
        id = generateID();

        // Create Initial Ring
        setSuccessor(address);
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
    public void setSuccessor(String successor) {
        this.successor = successor;
    }

    @Override
    public void joinRing (String address) {
        successor = address;
        //TODO set succesors predessor to self
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
        httpRequest(url, json);
    }

    private void performQueryRepsonse(String receiver, int key) {
        String url = receiver + MyResource.RECEIVEPATH;

        JSONObject json = new JSONObject();
        json.put(JSONformat.KEY, key); //the search key is returned to sender in case they are doing multiple queries
        json.put(JSONformat.ADDRESS, address);
        httpRequest(url, json);

    }

    private void httpRequest(String url, JSONObject body) {
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

    @Override
    public boolean isInNetwork() {
        return inNetwork;
    }
}
