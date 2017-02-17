package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node {
    private static final int hashTruncation = 3;
    private static final int IDSPACE = (int) Math.pow(16, hashTruncation);
    public final String address = Main.BASE_URI;
    private int id;
    private List<String> successorList;
    private String predecessor;
    private HttpClient client;
    private boolean inNetwork = false;
    private int predecessorId;
    private List<String> addresses;
    private List<Finger> fingerTable;
    private DataSource dataSource;
    private ConcurrentHashMap<Integer, Instruction> instructionMap;

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
        try {
            performLookup(entryPoint, id, address, new JSONProperties("linear", 0, false));
        } catch (NodeOfflineException e) {
            System.out.println("The node we tried to connect to is offline");
        }
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
        instructionMap = new ConcurrentHashMap<>();

        upsertFingerTable(true);
    }

    //TODO THIS SHOULD NOT BE HERE, MOVE TO HELPER CLASS
    private int generateHash(String address) {
        return Integer.decode("0x" + DigestUtils.sha1Hex(address).substring(0, hashTruncation));
    }

    public int getID() {
        return id;
    }

    public List<String> getSuccessorList() {
        return successorList;
    }

    public String getSuccessor() {
        return successorList.get(0);
    }

    public String getPredecessor() {
        return predecessor;
    }

    public void setSuccessor(String successor) {
        successorList.add(0, successor);
        succListSizeConstrainer();
    }

    private void succListSizeConstrainer() {
        while (successorList.size() > successorListLength) {
            successorList.remove(successorList.size() - 1);
        }
    }

    public void setPredecessor(String predecessor) {
        this.predecessorId = generateHash(predecessor);
        this.predecessor = predecessor;
    }

    public void joinRing(String address) {
        setSuccessor(address);
        JSONObject predJson;
        try {
            predJson = httpGetRequest(getSuccessor() + ChordResource.PREDECESSORPATH);
            String predurl = predJson.get(JSONFormat.VALUE).toString();
            setPredecessor(predurl);
            //update pred.succ
            updateNeighbor(JSONFormat.SUCCESSOR, this.address, getPredecessor() + ChordResource.SUCCESSORPATH);
            //update succ.pred
            updateNeighbor(JSONFormat.PREDECESSOR, this.address, getSuccessor() + ChordResource.PREDECESSORPATH);

            upsertSuccessorList();
            fingerTable.set(0, new Finger(id + 1, getSuccessor()));
            inNetwork = true;

            Runnable successorListMaintainerThread = () -> maintainLists();
            new Thread(successorListMaintainerThread).start();
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }

    private void maintainLists() {
        while (inNetwork) {
            int tenSeconds = 10000;
            double random = Math.random() * tenSeconds;
            try {
                System.out.println("I'm sleeping for " + (tenSeconds + random) / 1000 + " seconds... Zzz");
                Thread.sleep((long) (tenSeconds + random));
                upsertFingerTable(false);
                System.out.println("Done upserting fingerblasters bois, back to sleep for " + (tenSeconds + random) / 1000 + " seconds... Zzz");
                Thread.sleep((long) (tenSeconds + random));
                System.out.println("I'm awake! Let's update some successors!");
                upsertSuccessorList();
                System.out.println("Done upserting succs bois, back to sleep.");
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
                } catch (NodeOfflineException e) {
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
                try {
                    performLookup(getSuccessor(), lookupID, address, new JSONProperties("linear", 0, false));
                } catch (NodeOfflineException e) {
                    System.out.println("upsertFingerTable failed. " + getSuccessor() + " is offline");
                }
            }
        }
    }

    public void updateFingerTable(int key, String address) {
        //check if key is part of fingertable
        for (int i = 0; i < fingerTable.size(); i++) {
            if (key == fingerTable.get(i).getId()) {
                fingerTable.set(i, new Finger(key, address));
            }
        }
    }

    private void updateNeighbor(String type, String value, String address) {
        try {
            JSONObject postjson = new JSONObject();
            postjson.put(JSONFormat.TYPE, type);
            postjson.put(JSONFormat.VALUE, value);
            httpPostRequest(address, postjson);
        } catch (NodeOfflineException e) {
            System.out.println("Failed updateNeighbor, " + address + " is offline");

        }
    }

    public void leaveRing() {
        // Find predecessor and set its Successor to this node's Successor
        updateNeighbor(JSONFormat.SUCCESSOR, getSuccessor(), getPredecessor() + ChordResource.SUCCESSORPATH);
        // and set pred of succ to this node's pred
        updateNeighbor(JSONFormat.PREDECESSOR, getPredecessor(), getSuccessor() + ChordResource.PREDECESSORPATH);
        Main.server.shutdownNow();
    }

    public void killNode() {
        System.exit(0);
    }

    public void lookup(int key, String initiator, JSONProperties jsonProperties) {
        boolean linear = true;
        if (jsonProperties.method.equals("finger")) {
            linear = false;
        }
        if (isMyKey(key)) {
            performQueryResponse(initiator, key, jsonProperties);
        } else {
            //do it the old way
            if (linear) {
                try {
                    performLookup(getSuccessor(), key, initiator, jsonProperties);
                } catch (NodeOfflineException e) {
                    System.out.println("Linear node lookup on " + getSuccessor() + " failed. initiator: " + initiator + " currentSender: " + address);
                }
            } else {
                //finger table lookup
                int lookupModKey = (key - id) % IDSPACE;
                Finger lookupFinger = fingerTable.get(0);
                for (Finger finger : fingerTable) {
                    int fingerModID = (finger.getId() - id) % IDSPACE;
                    if (fingerModID >= lookupModKey) {
                        break;
                    }
                    lookupFinger = finger;
                }
                try {
                    performLookup(lookupFinger.getAddress(), key, initiator, jsonProperties);
                } catch (NodeOfflineException e) {
                    System.out.println("Fingertable node lookup on " + getSuccessor() + " failed. initiator: " + initiator + " currentSender: " + address);
                }

            }
        }
    }

    private boolean isMyKey(int key) {
        if (predecessorId == id) {
            //only one in chord
            return true;
        } else if (id >= key && predecessorId < key) {
            return true;
        } else if (predecessorId > id && (key <= id || key > predecessorId)) {
            //the node has the lowest id, and the pred has the highest (first and last in ring)
            return true;
        } else {
            return false;
        }
    }

    private void performLookup(String receiver, int id, String address, JSONProperties jsonProperties) throws NodeOfflineException {
        String url = receiver + ChordResource.LOOKUPPATH + "/" + jsonProperties.method;

        JSONObject json = new JSONObject();
        json.put(JSONFormat.KEY, id);
        json.put(JSONFormat.ADDRESS, address);
        json.put(JSONFormat.HOPS, jsonProperties.hops + 1);
        json.put(JSONFormat.SHOWINCONSOLE, jsonProperties.showInConsole);
        httpPostRequest(url, json);
    }

    private void performQueryResponse(String receiver, int key, JSONProperties jsonProperties) {
        try {
            String url = receiver + ChordResource.RECEIVEPATH;
            JSONObject json = new JSONObject();
            json.put(JSONFormat.KEY, key); //the search key is returned to sender in case they are doing multiple queries
            json.put(JSONFormat.ADDRESS, address);
            json.put(JSONFormat.HOPS, jsonProperties.hops);
            json.put(JSONFormat.SHOWINCONSOLE, jsonProperties.showInConsole);
            httpPostRequest(url, json);
        } catch (NodeOfflineException e) {
            System.out.println(address + " tried to connect to: " + receiver + "  but failed. Key: " + key + " (performQueryResponse)");
        }

    }

    private void httpPostRequest(String url, JSONObject body) throws NodeOfflineException {
        HttpPost postMsg = new HttpPost(url);
        try {
            StringEntity params = new StringEntity(body.toJSONString());
            postMsg.addHeader("content-type", JSONFormat.JSON);
            postMsg.setEntity(params);
            HttpResponse response = client.execute(postMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println(response.getStatusLine().getStatusCode());
                System.out.println("ERROR ERROR " + url);     //TODO ??
            }
        } catch (HttpHostConnectException | ConnectTimeoutException | SocketTimeoutException e) {
            throw new NodeOfflineException();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void httpPutRequest(String url, JSONObject body) throws NodeOfflineException {
        HttpPut putMsg = new HttpPut(url);
        try {
            StringEntity params = new StringEntity(body.toJSONString());
            putMsg.addHeader("content-type", JSONFormat.JSON);
            putMsg.setEntity(params);
            HttpResponse response = client.execute(putMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println(response.getStatusLine().getStatusCode());
                System.out.println("ERROR ERROR " + url);     //TODO ??
            }
        } catch (HttpHostConnectException | ConnectTimeoutException | SocketTimeoutException e) {
            throw new NodeOfflineException();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject httpGetRequest(String url) throws NodeOfflineException {
        HttpGet getMsg = new HttpGet(url);
        try {
            HttpResponse response = client.execute(getMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("ERROR ERROR: Could not get");     //TODO ??
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
            throw new NodeOfflineException();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null; //TODO fix mabye?
    }

    public boolean isInNetwork() {
        return inNetwork;
    }


    public List<String> getAddresses() {
        return addresses;
    }

    public List<Finger> getFingerTable() {
        return fingerTable;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ConcurrentHashMap<Integer, Instruction> getInstructionMap() {
        return instructionMap;
    }

    public void handlePutResource(JSONObject json) {

        String address = json.get(JSONFormat.ADDRESS).toString();
        int key = generateHash(address);
        if (isMyKey(key)) {
            dataSource = new DataSource(address);
            Runnable dataUpdateThread = () -> dataSource.updateLoop();
            new Thread(dataUpdateThread).start();
        } else {
            //create instruction to be executed later when we receive the response on /receive
            Instruction inst = new Instruction(Instruction.Method.PUT, json, ChordResource.RESOURCEPATH);
            instructionMap.put(key, inst);
            lookup(key, this.address, new JSONProperties("finger", 0, false)); //should never return query response
        }
    }

    public void executeInstruction(Instruction inst, String address) {
        String url = address + inst.getTarget();
        JSONObject body = inst.getBody();
        Instruction.Method method = inst.getMethod();
        try {
            switch (method) {
                case GET:
                    //not handled and doesn't make sense to do here because you cant get the response
                    break;
                case POST:
                    httpPostRequest(url, body);
                    break;
                case PUT:
                    httpPutRequest(url, body);
                    break;
                case DELETE:
                    //not handled
                    break;
            }
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }
}

