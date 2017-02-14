package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node implements ChordNode {
    private static final int IDSPACE = 256;
    public final String address = Main.BASE_URI;
    private int id;
    private List<String> successorList;
    private String predecessor;
    private HttpClient client;
    private boolean inNetwork = false;
    private int predecessorId;
    private List<String> addresses;
    private List<Finger> fingerTable;

    private final int successorListLength = 5;
    private final int connectionTimeout = 3000;

    public Node() {
        initializeNode();
        // Create Initial Ring
        setSuccessor(address);
        setPredecessor(address);
        inNetwork = true;
        Runnable successorListMaintainerThread = () -> maintainLists();
        new Thread(successorListMaintainerThread).start();
    }

    public Node(String entryPoint) {
        initializeNode();
        // Join Ring
        performLookup(entryPoint, id, address);
        //   Lookup new successor --> Set as successor
        //   Lookup Successor's predecessor --> Set pred's Successor to this node.
    }

    private void initializeNode() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(connectionTimeout).build();
        builder.setDefaultRequestConfig(config);
        client = builder.build();

        id = generateHash(address);
        addresses = new CopyOnWriteArrayList<>();
        successorList = new CopyOnWriteArrayList<>();
        fingerTable = new CopyOnWriteArrayList<>();

        upsertFingerTable(true);
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
            successorList.remove(successorList.size() - 1);
        }
    }

    @Override
    public void setPredecessor(String predecessor) {
        this.predecessorId = generateHash(predecessor);
        this.predecessor = predecessor;
    }

    @Override
    public void joinRing(String address) {
        setSuccessor(address);
        JSONObject predJson = null;
        try {
            predJson = httpGetRequest(getSuccessor() + ChordResource.PREDECESSORPATH);
            String predurl = predJson.get(JSONFormat.VALUE).toString();
            setPredecessor(predurl);
            //update pred.succ
            updateNeighbor(JSONFormat.SUCCESSOR, this.address, getPredecessor() + ChordResource.SUCCESSORPATH);
            //update succ.pred
            updateNeighbor(JSONFormat.PREDECESSOR, this.address, getSuccessor() + ChordResource.PREDECESSORPATH);

            upsertSuccessorList();
            inNetwork = true;

            Runnable successorListMaintainerThread = () -> maintainLists();
            new Thread(successorListMaintainerThread).start();
        } catch (ChordOfflineException e) {
            e.printStackTrace();
        }
    }

    private void maintainLists() {
        while (inNetwork) {
            double random1 = Math.random() * 10000;
            double random2 = Math.random() * 10000;
            try {
                System.out.println("I'm sleeping for " + (10000 + random1) / 1000 + " seconds... Zzz");
                Thread.sleep((long) (10000 + random1));
                System.out.println("I'm awake! Let's update some successors!");
                upsertSuccessorList();
                System.out.println("Done upserting succs bois, back to sleep.");
                System.out.println("I'm sleeping for " + (10000 + random2) / 1000 + " seconds... Zzz");
                Thread.sleep((long) (10000 + random2));
                upsertFingerTable(false);
                System.out.println("Done upserting fingerblasters bois, back to sleep.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void upsertSuccessorList() {
        //Sorry
        //Add successors successorlist as this node's (after the first successor)

        //If we only have one node in chord, we do nothing
        if (successorList.get(0).equals(this.address)) {
            return;
        }
        CopyOnWriteArrayList<String> tempSuccList = new CopyOnWriteArrayList<>();
        boolean self = false;
        List<String> succList = successorList;
        for (int i = 0; i < successorListLength; i++) {
            if (self) {
                break;
            }
            for (int j = 0; j < succList.size(); j++) {
                String succCandidate = succList.get(j);
                if (address.equals(succCandidate)) {
                    //If node is included in successorlist, it does not continue update
                    self = true;
                    break;
                }
                try {
                    JSONObject candidateSuccListJson = httpGetRequest(succCandidate + ChordResource.SUCCESSORLISTPATH);
                    succList = new ArrayList((JSONArray) candidateSuccListJson.get(JSONFormat.VALUE));
                    tempSuccList.add(succCandidate);
                    break;
                } catch (ChordOfflineException e) {
                    //Node does not respond, so we connect to the next in the successor list
                    continue;
                }
            }
        }
        String freshSucc = tempSuccList.get(0);
        if (!getSuccessor().equals(freshSucc)) {
            //update succ.pred
            updateNeighbor(JSONFormat.PREDECESSOR, this.address, freshSucc + ChordResource.PREDECESSORPATH);
        }
        successorList = tempSuccList;
    }

    private void upsertFingerTable(boolean first) {
        int fingerTableSize = (int) (Math.log(IDSPACE) / Math.log(2));
        for (int i = 0; i < fingerTableSize; i++) {
            int lookupID = (id + (int) (Math.pow(2, i))) % IDSPACE;
            if (first) {
                fingerTable.add(new Finger(lookupID, null));
            } else {
                // TODO: Czech successors
                performLookup(getSuccessor(), lookupID, address);
            }
        }
    }

    @Override
    public void updateFingerTable(int key, String address) {
        //check if key is part of fingertable
        for (int i = 0; i < fingerTable.size(); i++) {
            if (key == fingerTable.get(i).getId()) {
                fingerTable.set(i, new Finger(key, address));
            }
        }
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

    private JSONObject httpGetRequest(String url) throws ChordOfflineException {
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
        } catch (HttpHostConnectException | ConnectTimeoutException | SocketTimeoutException e) {
            throw new ChordOfflineException();
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

    public List<Finger> getFingerTable() {
        return fingerTable;
    }

}

