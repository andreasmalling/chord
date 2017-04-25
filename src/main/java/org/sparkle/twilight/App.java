package org.sparkle.twilight;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class App {

    private Node node;
    private static final String INDEXKEY = "0";

    public App(Node node) {
        this.node = node;
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
}
