package org.sparkle.twilight;


import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import static org.sparkle.twilight.Main.httpUtil;

public class App {

    private ConcurrentHashMap<String, String> updateMap = null;
    private final ConcurrentHashMap<String, Long> timeStampViewMap;
    private final KeyManager keyman;
    private KeyPair keys;
    private Node node;
    private static final String INDEXKEY = "0";
    private static String NULLADDR = "";
    private final int RETRIES = 5;

    public App(Node node) {
        this.node = node;

        //this is necessary for unit tests of app where node is null
        if (node != null) {
            updateMap = node.getUpdateMap(); //K=id, V=address
        }
        timeStampViewMap = new ConcurrentHashMap<>();
        keyman = new KeyManager(512);
        keys = keyman.getKeys();
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
        indexJson.put(JSONFormat.TITLE, title);
        indexJson.put(JSONFormat.ID, messageId);
        addNewTopicToIndex(indexJson);

        //generate json for topic
        JSONObject topicJson = new JSONObject();
        topicJson.put(JSONFormat.TITLE, title);
        topicJson.put(JSONFormat.MESSAGE, message);
        topicJson.put(JSONFormat.REPLIES, new JSONArray());
        topicJson.put(JSONFormat.TIMESTAMP, 0L);
        addTopic(messageId + "", topicJson);
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
            packedJson.put(JSONFormat.VALUE, topicJson);
            //TODO make proxy handle this
            node.lookup(intKey, node.address, new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST, packedJson, "database/" + key);
            node.addInstruction(intKey, inst);
        }
    }

    public void addNewTopicToIndex(JSONObject topic) {
        System.out.println("handling index");
        int intIndexKey = Integer.parseInt(INDEXKEY);
        if (node.isMyKey(intIndexKey)) {
            System.out.println("adding: " + topic.toJSONString() + " to index");
            ChordStorage storage = node.getStorage();
            Object index = storage.getObject(INDEXKEY);
            if (index == null) {
                index = new JSONArray(); //create index array if it doesn't exist
            }
            ((JSONArray) index).add(topic);
            storage.putObject(INDEXKEY, index);
        } else {
            System.out.println("REDIRECTED");
            //TODO make proxy handle this
            node.lookup(intIndexKey, node.address, new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST, topic, "app/");
            node.addInstruction(intIndexKey, inst);
        }
    }


    public void replyToTopic(String id, JSONObject message) {
        int intKey = Integer.parseInt(id);
        if (node.isMyKey(intKey)) {
            JSONObject topic = getTopic(id);
            if (!validateReply(message, topic)) {
                System.out.println("TopicID: " + id + " HAS BEEN HACKED! DON'T TRUST THIS!");
                return;
            }
            long timestamp = (long) topic.get(JSONFormat.TIMESTAMP);
            timestamp++;
            topic.put(JSONFormat.TIMESTAMP, timestamp);
            JSONArray replyList = (JSONArray) topic.get(JSONFormat.REPLIES);
            message.put(JSONFormat.TIMESTAMP, timestamp);
            replyList.add(message);
            node.getStorage().putObject(id, topic);
        } else {
            //TODO make proxy handle this
            node.lookup(intKey, node.address, new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST, message, "app/" + id + "/reply");
            node.addInstruction(intKey, inst);
        }
    }

    public void updateIndex() {
        int intIndexKey = Integer.parseInt(INDEXKEY);
        if (node.isMyKey(intIndexKey)) {
            System.out.println("index update skipped...");
            return;
        }
        updateMap.put(INDEXKEY, "");
        //TODO make proxy handle this
        node.lookup(intIndexKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID, INDEXKEY);
        ;
        Instruction inst = new Instruction(Instruction.Method.GET, jsonId, "app/");
        node.addInstruction(intIndexKey, inst);

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
            if (updatedIndex == null) {
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
            timeStampViewMap.put(id, view);
            return;
        }
        updateMap.put(id, "");
        //TODO make proxy handle this
        node.lookup(intKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID, id);
        ;
        Instruction inst = new Instruction(Instruction.Method.GET, jsonId, "app/" + id);
        node.addInstruction(intKey, inst);

        //hacky async get
        waitForUpdate(id);
        try {
            String address = updateMap.get(id);
            if (address.equals(NULLADDR)) {
                System.out.println("%%%failed to update topic " + id);
                return;
            }
            JSONObject topicJson = httpUtil.httpGetRequest(address);
            if (topicJson == null) {
                System.out.println("***failed to update topic " + id);
                return;
            }
            ChordStorage storage = node.getStorage();
            storage.putObject(id, topicJson);
            long view = (long) topicJson.get(JSONFormat.TIMESTAMP);
            timeStampViewMap.put(id, view);
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }

    public long getView(String key) {
        Object oView = timeStampViewMap.get(key);
        if (oView == null) {
            return 0;
        } else {
            return (long) oView;
        }
    }

    private void waitForUpdate(String key) {
        for (int i = 0; i <= RETRIES; i++) {
            try {
                Thread.sleep((long) (100 * Math.pow(2, i))); //increasing sleep time
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

    //TODO test the stuff below...


    //only used for debugging
    public String getPublicKeyString() {
        return keyman.encodePublicKey(keys.getPublic());
    }

    public String signMessage(String message, JSONObject topicView, long view) {
        byte[] viewSig = getViewSig(topicView, view);
        //System.out.println("signMessage: " + Base64.encodeBase64String(viewSig));
        byte[] cryptoMessage = createCryptoMessage(viewSig, message, keys.getPublic());
        System.out.println("sign crypto: " + new String(cryptoMessage));
        byte[] sigBytes = keyman.sign(cryptoMessage, keys.getPrivate());
        return Base64.encodeBase64String(sigBytes);
    }

    public String signOP(String title, String message) {
        byte[] cryptoMessage = createCryptoMessageOp(title, message, keys.getPublic());
        byte[] sigBytes = keyman.sign(cryptoMessage, keys.getPrivate());
        return Base64.encodeBase64String(sigBytes);
    }

    public boolean validateReply(JSONObject reply, JSONObject topicView) {
        long view = (long) reply.get(JSONFormat.VIEW);
        byte[] viewSig = getViewSig(topicView, view);
        //System.out.println("validate: " + Base64.encodeBase64String(viewSig));
        String message = (String) reply.get(JSONFormat.MESSAGE);
        PublicKey pKey = keyman.decodePublicKey((String) reply.get(JSONFormat.PUBLICKEY));
        byte[] cryptoMessage = createCryptoMessage(viewSig, message, pKey);
        System.out.println("veri crypto: " + new String(cryptoMessage));
        byte[] signature = Base64.decodeBase64((String) reply.get(JSONFormat.SIGNATURE));

        return keyman.verify(cryptoMessage, signature, pKey);
    }

    private byte[] createCryptoMessage(byte[] viewSig, String message, PublicKey pubkey) {
        String cryptoMessage = Base64.encodeBase64String(viewSig) + message + keyman.encodePublicKey(pubkey);
        return cryptoMessage.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createCryptoMessageOp(String title, String message, PublicKey pubkey) {
        String cryptoMessage = title + message + keyman.encodePublicKey(pubkey);
        return cryptoMessage.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] getViewSig(JSONObject topic, long view) {
        byte[] sig;
        if (view == 0) {
            //get sig of OP message
            sig = Base64.decodeBase64((String) topic.get(JSONFormat.SIGNATURE));
        } else {
            int index = (int) (view - 1);
            //get sig of reply with given view
            JSONArray array = (JSONArray) topic.get(JSONFormat.REPLIES);
            JSONObject latestReply = (JSONObject) array.get(index);
            sig = Base64.decodeBase64((String) latestReply.get(JSONFormat.SIGNATURE));
        }
        return sig;
    }
}
