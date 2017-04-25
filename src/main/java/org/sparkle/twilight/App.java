package org.sparkle.twilight;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

import static org.sparkle.twilight.Main.httpUtil;

public class App {

    private final ConcurrentHashMap<String, String> updateMap;
    private Node node;
    private static final String INDEXKEY = "0";
    private final int RETRIES = 5;

    public App(Node node) {
        this.node = node;
        updateMap = node.getUpdateMap(); //K=id, V=address
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


    public void replyToTopic(String id, String message) {
        JSONObject messageJson = new JSONObject();
        messageJson.put(JSONFormat.MESSAGE,message);

        //TODO add check if responsible otherwise redirect
        ChordStorage storage = node.getStorage();
        JSONObject topic = (JSONObject) storage.getObject(id);
        JSONArray replyList = (JSONArray) topic.get(JSONFormat.REPLIES);
        replyList.add(messageJson);
        storage.putObject(id,topic);
    }

    public void updateIndex() {
        updateMap.put(INDEXKEY,null);
        int intIndexKey = Integer.parseInt(INDEXKEY);
        //TODO make proxy handle this
        node.lookup(intIndexKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID,INDEXKEY);;
        Instruction inst = new Instruction(Instruction.Method.GET,jsonId,"app/");
        node.addInstruction(intIndexKey,inst);

        //hacky async get
        for (int i=0; i<=RETRIES;i++) {
            try {
                Thread.sleep((long) (10 * Math.pow(2,i))); //increasing sleep time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!updateMap.get(INDEXKEY).equals(null)) {
                System.out.println("index updated");
                break;
            }
            System.out.println("failed to update index, trying again");
        }
        try {
            JSONObject updatedIndex = httpUtil.httpGetRequest(updateMap.get(INDEXKEY));
            if (updatedIndex==null) {
                System.out.println("failed to update index");
                return;
            }
            ChordStorage storage = node.getStorage();
            storage.putObject(INDEXKEY, updatedIndex);
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
        updateMap.remove(INDEXKEY);

    }

    public void updateTopic(String id) {
        //updateMap.put(id,null);
    }
}
