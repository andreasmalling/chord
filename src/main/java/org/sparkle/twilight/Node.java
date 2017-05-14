package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.sparkle.twilight.Main.httpUtil;

public class Node {
    private static final int hashTruncation = 3;
    private static final int IDSPACE = (int) Math.pow(16, hashTruncation);
    public final String address = Main.BASE_URI;
    private int id;
    private List<String> successorList;
    private String predecessor;

    private boolean inNetwork = false;
    private int predecessorId;
    private List<String> addresses;
    private List<Finger> fingerTable;

    private ConcurrentHashMap<Integer, Instruction> instructionMap;
    private ConcurrentHashMap<String, String> updateMap;
    private final int successorListLength = 5;

    private ChordStorage storage;

    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());

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
            LOGGER.severe("The node we tried to connect to is offline. " + e.getMessage());
        }
        //   Lookup new successor --> Set as successor
        //   Lookup Successor's predecessor --> Set pred's Successor to this node.
    }

    private void initializeNode() {
        LoggerHandlers.addHandlers(LOGGER);
        LOGGER.info("Initializing Node");
        id = generateHash(address);
        addresses = new CopyOnWriteArrayList<>();
        successorList = new CopyOnWriteArrayList<>();
        fingerTable = new CopyOnWriteArrayList<>();
        instructionMap = new ConcurrentHashMap<>();
        storage = new ChordStorage(String.valueOf(id));
        updateMap = new ConcurrentHashMap<>();

        upsertFingerTable(true);
    }

    public int generateHash(String address) {
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

        if (predecessorId == id) {
            return;
        }

        JSONArray topicsToPred = new JSONArray();

        storage.dumpKeySet().stream().filter(key -> !isMyKey(Integer.parseInt(key.toString()))).forEach(key -> {
            packTopicsAsJSON(topicsToPred, key);
        });

        try {
            JSONObject json = new JSONObject();
            json.put(JSONFormat.DATA, topicsToPred);
            httpUtil.httpPostRequest(predecessor + ChordResource.DATABASE, json);
        } catch (NodeOfflineException e) {
            LOGGER.severe("Node is offline: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void joinRing(String address) {
        setSuccessor(address);
        JSONObject predJson;
        try {
            predJson = httpUtil.httpGetRequest(getSuccessor() + ChordResource.PREDECESSORPATH);
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
            LOGGER.severe("Node Offline: " + e.getMessage());
        }
    }

    private void maintainLists() {
        while (inNetwork) {
            int sleepTime = 5000;
            double random = Math.random() * sleepTime;
            try {
                LOGGER.finest("Sleeping for " + (sleepTime + random) + " ms");
                Thread.sleep((long) (sleepTime + random));
                upsertFingerTable(false);
                LOGGER.finest("Sleeping for " + (sleepTime + random) + " ms");
                Thread.sleep((long) (sleepTime + random));
                upsertSuccessorList();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void upsertSuccessorList() {
        LOGGER.fine("Upserting Successor List");
        //Sorry
        //Add successors successorlist as this node's (after the first successor)

        //If we only have one node in chord, we do nothing
        if (successorList.get(0).equals(this.address)) {
            return;
        }
        CopyOnWriteArrayList<String> tempSuccList = new CopyOnWriteArrayList<>();
        boolean self = false;
        boolean updated = false;

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
                    JSONObject candidateSuccListJson = httpUtil.httpGetRequest(succCandidate + ChordResource.SUCCESSORLISTPATH);
                    succList = new ArrayList((JSONArray) candidateSuccListJson.get(JSONFormat.VALUE));
                    tempSuccList.add(succCandidate);
                    break;
                } catch (NodeOfflineException e) {
                    //Node does not respond, so we connect to the next in the successor list
                    updated = true;
                    continue;
                }
            }
        }

        String freshSucc = tempSuccList.get(0);
        if (!getSuccessor().equals(freshSucc)) {
            //update succ.pred
            updateNeighbor(JSONFormat.PREDECESSOR, this.address, freshSucc + ChordResource.PREDECESSORPATH);
        }
        //set new succlist
        if (!successorList.equals(tempSuccList)) {
            successorList = tempSuccList;
        }

        //TODO - should replicate responsible topics to successors
        if (updated) {
            JSONArray topicsToSuccs = new JSONArray();
            storage.dumpKeySet().stream().filter(key -> isMyKey((int) key)).forEach(key -> {
                packTopicsAsJSON(topicsToSuccs, key);
            });
            for (String succAddress : successorList) {
                String url = succAddress + ChordResource.DATABASE;
                JSONObject json = new JSONObject();
                json.put(JSONFormat.DATA, topicsToSuccs);
                try {
                    httpUtil.httpPostRequest(url, json);
                } catch (NodeOfflineException e) {
                    System.out.println("Tried to push data to " + url + ", but failed.");
                    upsertSuccessorList();
                }
            }
        }
    }

    private void packTopicsAsJSON(JSONArray topicsToSend, Object key) {
        JSONAware topic = (JSONAware) storage.getObject(key);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSONFormat.KEY, key);
        jsonObject.put(JSONFormat.VALUE, topic);
        topicsToSend.add(jsonObject);
    }

    //TODO - could probably be smarter than linear
    private void upsertFingerTable(boolean first) {
        LOGGER.fine("Upserting Fingertable");
        int fingerTableSize = (int) (Math.log(IDSPACE) / Math.log(2));
        for (int i = 0; i < fingerTableSize; i++) {
            int lookupID = (id + (int) (Math.pow(2, i))) % IDSPACE;
            if (first) {
                fingerTable.add(new Finger(lookupID, null));
            } else {
                try {
                    performLookup(getSuccessor(), lookupID, address, new JSONProperties("linear", 0, false));
                } catch (NodeOfflineException e) {
                    LOGGER.severe("upsertFingerTable failed. " + getSuccessor() + " is offline");
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
            httpUtil.httpPostRequest(address, postjson);
        } catch (NodeOfflineException e) {
            LOGGER.warning("Failed updateNeighbor, " + address + " is offline");

        }
    }

    public void leaveRing() {
        LOGGER.info("Node is leaving ring.");
        // Find predecessor and set its Successor to this node's Successor
        updateNeighbor(JSONFormat.SUCCESSOR, getSuccessor(), getPredecessor() + ChordResource.SUCCESSORPATH);
        // and set pred of succ to this node's pred
        updateNeighbor(JSONFormat.PREDECESSOR, getPredecessor(), getSuccessor() + ChordResource.PREDECESSORPATH);
        storage.shutdown();
        Main.server.shutdownNow();
        System.exit(0);
    }

    public void killNode() {
        LOGGER.info("Kill node");
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
                    LOGGER.info("Linear node lookup on " + getSuccessor() + " failed. initiator: " + initiator + " currentSender: " + address);
                    upsertSuccessorList();
                }
            } else {
                //finger table lookup
                int lookupModKey = (key - id) % IDSPACE;
                Finger lookupFinger = fingerTable.get(0);
                for (Finger finger : fingerTable) {
                    int fingerModID = (finger.getId() - id) % IDSPACE;
                    if (fingerModID > lookupModKey) {
                        break;
                    }
                    lookupFinger = finger;
                }
                try {
                    performLookup(lookupFinger.getAddress(), key, initiator, jsonProperties);
                } catch (NodeOfflineException e) {
                    LOGGER.info("Fingertable node lookup on " + lookupFinger.getAddress() + " failed. initiator: " + initiator + " currentSender: " + address);
                    upsertFingerTable(false);
                }
            }
        }
    }

    public boolean isMyKey(int key) {
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
        httpUtil.httpPostRequest(url, json);
    }

    private void performQueryResponse(String receiver, int key, JSONProperties jsonProperties) {
        try {
            String url = receiver + ChordResource.RECEIVEPATH;
            JSONObject json = new JSONObject();
            json.put(JSONFormat.KEY, key); //the search key is returned to sender in case they are doing multiple queries
            json.put(JSONFormat.ADDRESS, address);
            json.put(JSONFormat.HOPS, jsonProperties.hops);
            json.put(JSONFormat.SHOWINCONSOLE, jsonProperties.showInConsole);
            httpUtil.httpPostRequest(url, json);
        } catch (NodeOfflineException e) {
            System.out.println(address + " tried to connect to: " + receiver + " but failed. Key: " + key + " (performQueryResponse)");
        }
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

    public ConcurrentHashMap<Integer, Instruction> getInstructionMap() {
        return instructionMap;
    }

    public void addInstruction(Integer key, Instruction inst) {
        instructionMap.put(key, inst);
    }

    public void executeInstruction(Instruction inst, String address) {
        String url = address + inst.getTarget();
        JSONObject body = inst.getBody();
        Instruction.Method method = inst.getMethod();
        System.out.println("executing instruction:\nURL: " + url + "\nmethod: " + method + "\nbody:\n" + body.toJSONString());
        try {
            switch (method) {
                case GET:
                    //not handled and doesn't make sense to do here because you cant get the response
                    //TODO hacky fix so that we can do async gets, Kresten plz fix
                    updateMap.put((String) body.get(JSONFormat.ID), url); //store lookup id in the body ex: {id: 45}
                    break;
                case POST:
                    httpUtil.httpPostRequest(url, body);
                    break;
                case PUT:
                    httpUtil.httpPutRequest(url, body);
                    break;
                case DELETE:
                    //not handled
                    break;
            }
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }

    public String getDatabaseAsString() {
        return storage.toString();
    }

    public ChordStorage getStorage() {
        return storage;
    }

    public void putObjectInStorage(Object key, Object value) {
        storage.putObject(key, value);
    }

    public void replicateData(String key, JSONObject jRequest) {
        if (!isMyKey(Integer.parseInt(key))) {
            return;
        }

        for (String succ : successorList) {
            try {
                httpUtil.httpPostRequest(succ + ChordResource.DATABASE + "/" + key, jRequest);
            } catch (NodeOfflineException e) {
                e.printStackTrace();
            }
        }
    }

    public ConcurrentHashMap<String, String> getUpdateMap() {
        return updateMap;
    }

}
