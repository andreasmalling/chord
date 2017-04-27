package org.sparkle.twilight;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

import static org.sparkle.twilight.Main.httpUtil;

public class App {

    private final ConcurrentHashMap<String, String> updateMap;
    private final ConcurrentHashMap<String, Long> timeStampViewMap;
    private Node node;
    private static final String INDEXKEY = "0";
    private static String NULLADDR = "";
    private final int RETRIES = 5;

    public App(Node node) {
        this.node = node;
        updateMap = node.getUpdateMap(); //K=id, V=address
        timeStampViewMap = new ConcurrentHashMap<>();
    }

    public JSONArray getIndex() {
        ChordStorage storage = node.getStorage();
        Object index = storage.getObject(INDEXKEY);
        return (JSONArray) index;
    }

    public JSONObject getTopic(String id) {
        ChordStorage storage = node.getStorage();
        Object topic = storage.getObject(id);
        return (JSONObject) topic;
    }

    public void postTopic(String title, String message) {
        int messageId = node.generateHash(title); //TODO maybe salt with public key

        //generate json for index
        JSONObject indexJson = new JSONObject();
        indexJson.put(JSONFormat.TITLE,title);
        indexJson.put(JSONFormat.ID,messageId); //pretty sure we store this as int
        addNewTopicToIndex(indexJson);

        //generate json for topic
        JSONObject topicJson = new JSONObject();
        topicJson.put(JSONFormat.TITLE,title);
        topicJson.put(JSONFormat.MESSAGE,message);
        topicJson.put(JSONFormat.REPLIES,new JSONArray());
        topicJson.put(JSONFormat.TIMESTAMP,0L);
        addTopic(messageId + "",topicJson);
    }

    private void addTopic(String key, JSONObject topicJson) {
        System.out.println("handling topic with key: " + key);
        Integer intKey = Integer.parseInt(key);
        if (node.isMyKey(intKey)) {
            ChordStorage storage = node.getStorage();
            storage.putObject(key, topicJson);
        } else {
            System.out.println("REDIRECTED");
            JSONObject packedJson = new JSONObject();
            packedJson.put(JSONFormat.VALUE,topicJson);
            //TODO make proxy handle this
            node.lookup(intKey, node.address,new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST,packedJson,"database/" + key);
            node.addInstruction(intKey,inst);
        }
    }

    public void addNewTopicToIndex(JSONObject topic) {
        System.out.println("handling index");
        int intIndexKey = Integer.parseInt(INDEXKEY);
        if (node.isMyKey(intIndexKey)) {
            System.out.println("adding: " + topic.toJSONString() + " to index");
            ChordStorage storage = node.getStorage();
            Object index = storage.getObject(INDEXKEY);
            if (index== null) {
                index = new JSONArray(); //create index array if it doesn't exist
            }
            ((JSONArray) index).add(topic);
            storage.putObject(INDEXKEY, index);
        } else {
            System.out.println("REDIRECTED");
            //TODO make proxy handle this
            node.lookup(intIndexKey, node.address, new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST,topic, "app/");
            node.addInstruction(intIndexKey,inst);
        }
    }


    public void replyToTopic(String id, String message, long view) {
        JSONObject messageJson = new JSONObject();
        messageJson.put(JSONFormat.MESSAGE,message);
        messageJson.put(JSONFormat.VIEW,view);
        int intKey = Integer.parseInt(id);
        if (node.isMyKey(intKey)) {
            ChordStorage storage = node.getStorage();
            JSONObject topic = (JSONObject) storage.getObject(id);
            long timestamp = (long) topic.get(JSONFormat.TIMESTAMP);
            timestamp++;
            topic.put(JSONFormat.TIMESTAMP,timestamp);
            JSONArray replyList = (JSONArray) topic.get(JSONFormat.REPLIES);
            messageJson.put(JSONFormat.TIMESTAMP,timestamp);
            replyList.add(messageJson);
            storage.putObject(id, topic);
        } else {
            //TODO make proxy handle this
            node.lookup(intKey, node.address, new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST,messageJson,"app/"+id+"/reply");
            node.addInstruction(intKey,inst);
        }
    }

    public void updateIndex() {
        int intIndexKey = Integer.parseInt(INDEXKEY);
        if (node.isMyKey(intIndexKey)) {
            System.out.println("index update skipped...");
            return;
        }
        updateMap.put(INDEXKEY,"");
        //TODO make proxy handle this
        node.lookup(intIndexKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID,INDEXKEY);;
        Instruction inst = new Instruction(Instruction.Method.GET,jsonId,"app/");
        node.addInstruction(intIndexKey,inst);

        //hacky async get
        waitForUpdate(INDEXKEY);
        try {
            String address = updateMap.get(INDEXKEY);
            if (address.equals(NULLADDR)) {
                System.out.println("%%%failed to update index");
                return;
            }
            JSONObject response = httpUtil.httpGetRequest(address);
            JSONArray updatedIndex = (JSONArray) response.get(JSONFormat.VALUE);
            if (updatedIndex==null) {
                System.out.println("***failed to update index");
                return;
            }
            ChordStorage storage = node.getStorage();
            storage.putObject(INDEXKEY, updatedIndex);
            //TODO mabye timestamp index???????
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }

    //TODO refactor less copypasta
    public void updateTopic(String id) {
        int intKey = Integer.parseInt(id);
        if (node.isMyKey(intKey)) {
            System.out.println("topic " + id + " update skipped...");
            JSONObject topicJson = getTopic(id);
            long view = (long) topicJson.get(JSONFormat.TIMESTAMP);
            timeStampViewMap.put(id,view);
            return;
        }
        updateMap.put(id,"");
        //TODO make proxy handle this
        node.lookup(intKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID,id);;
        Instruction inst = new Instruction(Instruction.Method.GET,jsonId,"app/" + id);
        node.addInstruction(intKey,inst);

        //hacky async get
        waitForUpdate(id);
        try {
            String address = updateMap.get(id);
            if (address.equals(NULLADDR)) {
                System.out.println("%%%failed to update topic " + id);
                return;
            }
            JSONObject topicJson = httpUtil.httpGetRequest(address);
            if (topicJson==null) {
                System.out.println("***failed to update topic " + id);
                return;
            }
            ChordStorage storage = node.getStorage();
            storage.putObject(id, topicJson);
            long view = (long) topicJson.get(JSONFormat.TIMESTAMP);
            timeStampViewMap.put(id,view);
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }

    public long getView(String key) {
        Object oView = timeStampViewMap.get(key);
        if (oView==null) {
            return 0;
        } else {
            return (long) oView;
        }
    }

    private void waitForUpdate(String key) {
        for (int i=0; i<=RETRIES;i++) {
            try {
                Thread.sleep((long) (100 * Math.pow(2,i))); //increasing sleep time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!updateMap.get(key).equals(NULLADDR)) {
                System.out.println("updating...");
                break;
            }
            System.out.println("failed to update index, trying again");
        }
    }

}
