package org.sparkle.twilight;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
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
    private DataSource dataSource;

    private ConcurrentHashMap<Integer, Instruction> instructionMap;
    private final int successorListLength = 5;
    private ChordStorage storage;
    private Thread dataUpdateThread;

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
        LOGGER.addHandler(LoggerUtil.getFh());
        LOGGER.info("Initializing Node");
        id = generateHash(address);
        addresses = new CopyOnWriteArrayList<>();
        successorList = new CopyOnWriteArrayList<>();
        fingerTable = new CopyOnWriteArrayList<>();
        instructionMap = new ConcurrentHashMap<>();
        storage = new ChordStorage(String.valueOf(id));

        upsertFingerTable(true);
    }

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

        // If we own a datasource, the predecessor (if responsible) should own source
        try {
            String dataSourceID = storage.getValue(ChordStorage.ID_KEY);
            String dataSourceToken = storage.getValue(ChordStorage.TOKEN_KEY);

            if (dataSource != null) {
                if (predecessorId > generateHash(dataSourceID)) {
                    JSONObject json = new JSONObject();
                    json.put(JSONFormat.ID, dataSourceID);
                    json.put(JSONFormat.ACCESSTOKEN, dataSourceToken);
                    try {
                        ArrayList<String> urllist = new ArrayList<>();
                        urllist.add(0, this.predecessor);
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.addAll(storage.getDataList());
                        String data = jsonArray.toJSONString();
                        pushDataToNodes(urllist, data);
                        httpUtil.httpPutRequest(predecessor + "/" + ChordResource.RESOURCEPATH, json);
                        dataUpdateThread.interrupt();
                    } catch (NodeOfflineException e) {
                        //TODO if node is offline try next successor
                        LOGGER.severe("Node is offline: " + e.getMessage());
                    }
                }
            }

            // Should we take responsiblity for resource, due to improper leave?
            if (isMyKey(generateHash(dataSourceID))) {
                if (dataSource == null) {
                    System.out.println("Take on responsibility of resource");
                    initDataSource(dataSourceID, dataSourceToken);
                }
            }
        } catch (NoValueException e) {
            LOGGER.warning("DataSource have 'No Value'");
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
        LOGGER.info("Upserting Successor List");
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

        if (updated && dataSource != null) {
            try {
                pushResourceToSuccessors(storage.getValue(ChordStorage.ID_KEY), storage.getValue(ChordStorage.TOKEN_KEY));
            } catch (NoValueException e) {
                LOGGER.warning("DataSource have 'No Value'");
            }
        }
        updated = false;

        String freshSucc = tempSuccList.get(0);
        if (!getSuccessor().equals(freshSucc)) {
            //update succ.pred
            updateNeighbor(JSONFormat.PREDECESSOR, this.address, freshSucc + ChordResource.PREDECESSORPATH);
        }

        if (!successorList.equals(tempSuccList)) {
            // Create new list with updated successors
            ArrayList<String> broadcastDataSetList = new ArrayList<>(tempSuccList);
            broadcastDataSetList.removeAll(successorList);

            JSONArray jsonArray = new JSONArray();
            jsonArray.addAll(storage.getDataList());
            String data = jsonArray.toJSONString();
            pushDataToNodes(broadcastDataSetList, data);

            // Set new successorlist
            successorList = tempSuccList;
        }
    }

    private void upsertFingerTable(boolean first) {
        LOGGER.info("Upserting Fingertable");
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
        //pass on the resource responsibillity
        if (dataSource != null) {
            JSONObject json = new JSONObject();
            try {
                json.put(JSONFormat.ID, storage.getValue(ChordStorage.ID_KEY));
                json.put(JSONFormat.ACCESSTOKEN, storage.getValue(ChordStorage.TOKEN_KEY));
                httpUtil.httpPutRequest(successorList.get(0) + "/" + ChordResource.RESOURCEPATH, json);
            } catch (NodeOfflineException e) {
                //TODO if node is offline try next successor
                LOGGER.severe("Node Offline: " + e.getMessage());
            } catch (NoValueException e) {
                // Should never happen
                LOGGER.severe("Data Source 'No Value'" + e.getMessage());
            }
        }
        storage.shutdown();
        dataUpdateThread.interrupt();
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

    public DataSource getDataSource() {
        return dataSource;
    }

    public ConcurrentHashMap<Integer, Instruction> getInstructionMap() {
        return instructionMap;
    }

    public void handlePutResource(JSONObject json) {
        if (dataSource == null) {
            String id = json.get(JSONFormat.ID).toString();
            int key = generateHash(id);
            if (isMyKey(key)) {
                String accesstoken = json.get(JSONFormat.ACCESSTOKEN).toString();
                initDataSource(id, accesstoken);
            } else {
                //create instruction to be executed later when we receive the response on /receive
                Instruction inst = new Instruction(Instruction.Method.PUT, json, ChordResource.RESOURCEPATH);
                instructionMap.put(key, inst);
                lookup(key, this.address, new JSONProperties("finger", 0, false)); //should never return query response
            }
        }
    }

    private void initDataSource(String id, String accesstoken) {
        dataSource = new DataSource(id, accesstoken);
        upsertDatabase(ChordStorage.ID_KEY, id, false);
        upsertDatabase(ChordStorage.TOKEN_KEY, accesstoken, true);
        pushResourceToSuccessors(id, accesstoken);
        Runnable dataUpdateThread = () -> startDataSourceUpdateLoop();
        this.dataUpdateThread = new Thread(dataUpdateThread);
        this.dataUpdateThread.start();
    }

    private void startDataSourceUpdateLoop() {
        while (!Thread.interrupted()) {
            try {
                if (dataSource != null) {
                    dataSource.updateData();
                    String data = dataSource.getData();
                    storage.putData(data);
                    pushDataToNodes(successorList, data);
                    int sleepTime = 2000;
                    double random = Math.random() * sleepTime;
                    System.out.println("Data was updated with new value: " + data + ". Now I sleep for " + (sleepTime + random) / 1000 + " seconds. Zzz...");
                    Thread.sleep((long) (sleepTime + random));
                } else {
                    return;
                }
            } catch (InterruptedException e) {
                dataSource = null;
            } catch (NodeOfflineException e) {
                dataSource.setData(DataSource.DATA_NOT_AVAILABLE);
            }
        }
        dataSource = null;
    }

    private void pushResourceToSuccessors(String id, String token) {
        for (String succAddress : successorList) {
            String url = succAddress + ChordResource.DATABASE + "/id";
            JSONObject bodyID = new JSONObject();
            bodyID.put(JSONFormat.VALUE, id);

            String urlToken = succAddress + ChordResource.DATABASE + "/token";
            JSONObject bodyToken = new JSONObject();
            bodyToken.put(JSONFormat.VALUE, token);
            try {
                httpUtil.httpPostRequest(url, bodyID);
                httpUtil.httpPostRequest(urlToken, bodyToken);
            } catch (NodeOfflineException e) {
                System.out.println("Tried to push data to " + url + ", but failed.");
                upsertSuccessorList();
            }
        }
    }

    private void pushDataToNodes(List<String> successorList, String data) {
        for (String succAddress : successorList) {
            if (succAddress.equals(address)) {
                break;
            }
            String url = succAddress + ChordResource.DATABASE + "/data";
            JSONObject body = new JSONObject();
            body.put(JSONFormat.VALUE, data);
            try {
                httpUtil.httpPostRequest(url, body);
            } catch (NodeOfflineException e) {
                System.out.println("Tried to push data to " + url + ", but failed.");
            }
        }
    }


    public void upsertDatabase(String type, String value, boolean commit) {
        storage.putValue(type, value, commit);
    }

    public void overwriteData(ArrayList<String> value) {
        storage.putData(value);
    }

    public String getData() {
        return storage.getData();
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
}

