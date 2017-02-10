package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node implements ChordNode {
    public final String address = Main.BASE_URI;
    private int id;
    private List<String> successorList;
    private String predecessor;
    private HttpClient client;
    private boolean inNetwork = false;
    private int predecessorId;
    private List<String> addresses;

    private final int successorListLength = 5;

    public Node() {
        initializeNode();
        // Create Initial Ring
        setSuccessor(address);
        setPredecessor(address);
        inNetwork = true;
    }

    public Node(String entryPoint) {
        initializeNode();
        // Join Ring
        performLookup(entryPoint, id, address);
        //   Lookup new successor --> Set as successor
        //   Lookup Successor's predecessor --> Set pred's Successor to this node.
    }

    private void initializeNode() {
        id = generateHash(address);
        client = HttpClientBuilder.create().build();
        addresses = new CopyOnWriteArrayList();
        successorList = new CopyOnWriteArrayList();
    }

    private int generateHash(String address) {
        return Integer.decode("0x" + DigestUtils.sha1Hex(address).substring(0, 2));
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public List<String> getSuccessorList() {
        return successorList;
    }

    public String getSuccessor() {
        return successorList.get(0);
    }

    @Override
    public String getPredecessor() {
        return predecessor;
    }

    @Override
    public void setSuccessor(String successor) {
        successorList.add(0, successor);
        succListSizeConstrainer();
    }

    private void succListSizeConstrainer() {
        while (successorList.size() > successorListLength) {
            successorList.remove(successorListLength - 1);
        }
    }

    public void bulkInsertSuccessors(List<String> successors) {
        successorList.addAll(1, successors);
        succListSizeConstrainer();
    }

    @Override
    public void setPredecessor(String predecessor) {
        this.predecessorId = generateHash(predecessor);
        this.predecessor = predecessor;
    }

    @Override
    public void joinRing(String address) {
        setSuccessor(address);
        JSONObject predJson = httpGetRequest(getSuccessor() + ChordResource.PREDECESSORPATH);
        String predurl = predJson.get(JSONFormat.VALUE).toString();
        setPredecessor(predurl);
        //update pred.succ
        updateNeighbor(JSONFormat.SUCCESSOR, this.address, getPredecessor() + ChordResource.SUCCESSORPATH);
        //update succ.pred
        updateNeighbor(JSONFormat.PREDECESSOR, this.address, getSuccessor() + ChordResource.PREDECESSORPATH);

        updateSuccessorList();
        inNetwork = true;
    }

    private void updateSuccessorList() {
        //Add successors successorlist as this node's (after the first successor)
        //TODO should make list correctly
        JSONObject succListJson = httpGetRequest(getSuccessor() + ChordResource.SUCCESSORLISTPATH);
        ArrayList succList = new ArrayList((JSONArray) succListJson.get(JSONFormat.VALUE));
        bulkInsertSuccessors(succList);
    }

    private void updateNeighbor(String type, String value, String address) {
        JSONObject postjson = new JSONObject();
        postjson.put(JSONFormat.TYPE, type);
        postjson.put(JSONFormat.VALUE, value);
        httpPostRequest(address, postjson);
    }

    @Override
    public void leaveRing() {
        // Find predecessor and set its Successor to this node's Successor
        updateNeighbor(JSONFormat.SUCCESSOR, getSuccessor(), getPredecessor() + ChordResource.SUCCESSORPATH);
        // and set pred of succ to this node's pred
        updateNeighbor(JSONFormat.PREDECESSOR, getPredecessor(), getSuccessor() + ChordResource.PREDECESSORPATH);
        Main.server.shutdownNow();
    }

    @Override
    public void killNode() {
        System.exit(0);
    }

    //TODO validate lookup for nodes pushed out from ring
    @Override
    public void lookup(int key, String initiator) {
        if (predecessorId == id) {
            //only one in chord
            performQueryRepsonse(initiator, key);
        } else if (id >= key && predecessorId < key) {
            performQueryRepsonse(initiator, key);
        } else if (predecessorId > id && (key <= id || key > predecessorId)) {
            //the node has the lowest id, and the pred has the highest (first and last in ring)
            performQueryRepsonse(initiator, key);
        } else {
            performLookup(getSuccessor(), key, initiator);
        }
    }

    private void performLookup(String receiver, int id, String address) {
        String url = receiver + ChordResource.LOOKUPPATH;

        JSONObject json = new JSONObject();
        json.put(JSONFormat.KEY, id);
        json.put(JSONFormat.ADDRESS, address);
        httpPostRequest(url, json);
    }

    private void performQueryRepsonse(String receiver, int key) {
        String url = receiver + ChordResource.RECEIVEPATH;

        JSONObject json = new JSONObject();
        json.put(JSONFormat.KEY, key); //the search key is returned to sender in case they are doing multiple queries
        json.put(JSONFormat.ADDRESS, address);
        httpPostRequest(url, json);

    }

    private void httpPostRequest(String url, JSONObject body) {
        HttpPost postMsg = new HttpPost(url);
        try {
            StringEntity params = new StringEntity(body.toJSONString());
            postMsg.addHeader("content-type", JSONFormat.JSON);
            postMsg.setEntity(params);
            HttpResponse response = client.execute(postMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println(response.getStatusLine().getStatusCode());
                System.out.println("ERROR ERROR " + url);     //TODO ??
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

    private JSONObject httpGetRequest(String url) {
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
            while ((line = reader.readLine()) != null) {
                jsonstring += line;
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

    public List<String> getAddresses() {
        return addresses;
    }
}

