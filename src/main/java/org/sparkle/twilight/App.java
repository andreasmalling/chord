package org.sparkle.twilight;


import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.sparkle.twilight.Main.httpUtil;

public class App {

    private ConcurrentHashMap<String, String> updateMap = null;
    private final ConcurrentHashMap<String, Long> timeStampViewMap;
    private final KeyManager keyman;
    private KeyPair keys;
    private Node node;
    private static final String INDEXKEY = "0";
    private static String NULLADDR = "";
    private final int RETRIES = 5; //configuration parameter, depends on the speed of the network
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());


    public App(Node node) {
        this.node = node;

        //this is necessary for unit tests of app where node is null
        if (node != null) {
            updateMap = node.getUpdateMap(); //K=id, V=address
        }
        LoggerHandlers.addHandlers(LOGGER);
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
        Integer intKey = Integer.parseInt(key);
        if (node.isMyKey(intKey)) {
            ChordStorage storage = node.getStorage();
            storage.putObject(key, topicJson);
            LOGGER.info("created topic with id: " + key);
        } else {
            JSONObject packedJson = new JSONObject();
            packedJson.put(JSONFormat.VALUE, topicJson);
            //TODO make proxy handle this
            node.lookup(intKey, node.address, new JSONProperties());
            Instruction inst = new Instruction(Instruction.Method.POST, packedJson, "database/" + key);
            node.addInstruction(intKey, inst);
        }
    }

    public void addNewTopicToIndex(JSONObject topic) {
        int intIndexKey = Integer.parseInt(INDEXKEY);
        if (node.isMyKey(intIndexKey)) {
            ChordStorage storage = node.getStorage();
            Object index = storage.getObject(INDEXKEY);
            if (index == null) {
                index = new JSONArray(); //create index array if it doesn't exist
            }
            ((JSONArray) index).add(topic);
            storage.putObject(INDEXKEY, index);
            LOGGER.info("Inserted new topic to index");
        } else {
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
                LOGGER.severe("reply to topic with is not valid");
                return;
            }
            long timestamp = (long) topic.get(JSONFormat.TIMESTAMP);
            timestamp++;
            topic.put(JSONFormat.TIMESTAMP, timestamp);
            JSONArray replyList = (JSONArray) topic.get(JSONFormat.REPLIES);
            message.put(JSONFormat.TIMESTAMP, timestamp);
            replyList.add(message);
            node.getStorage().putObject(id, topic);
            LOGGER.info("added reply to topic");
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
            LOGGER.info("updating local copy of index skipped");
            return;
        }
        updateMap.put(INDEXKEY, NULLADDR);
        //TODO make proxy handle this
        node.lookup(intIndexKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID, INDEXKEY);
        Instruction inst = new Instruction(Instruction.Method.GET, jsonId, "app/");
        node.addInstruction(intIndexKey, inst);

        //hacky async get
        waitForUpdate(INDEXKEY);
        try {
            String address = updateMap.get(INDEXKEY);
            if (address.equals(NULLADDR)) {
                LOGGER.severe("failed to update index, no response");
                return;
            }
            JSONObject response = httpUtil.httpGetRequest(address);
            JSONArray updatedIndex = (JSONArray) response.get(JSONFormat.VALUE);
            if (updatedIndex == null) {
                LOGGER.severe("failed to update index, null response");
                return;
            }
            ChordStorage storage = node.getStorage();
            storage.putObject(INDEXKEY, updatedIndex);
            LOGGER.info("updated local copy of index");
        } catch (NodeOfflineException e) {
            e.printStackTrace();
        }
    }

    //TODO refactor less copypasta
    public void updateTopic(String id) {
        int intKey = Integer.parseInt(id);
        if (node.isMyKey(intKey)) {
            JSONObject topicJson = getTopic(id);
            long view = (long) topicJson.get(JSONFormat.TIMESTAMP);
            timeStampViewMap.put(id, view);
            LOGGER.info("update topic timestamp");
            return;
        }
        updateMap.put(id, NULLADDR);
        //TODO make proxy handle this
        node.lookup(intKey, node.address, new JSONProperties());
        JSONObject jsonId = new JSONObject();
        jsonId.put(JSONFormat.ID, id);
        Instruction inst = new Instruction(Instruction.Method.GET, jsonId, "app/" + id);
        node.addInstruction(intKey, inst);

        //hacky async get
        waitForUpdate(id);
        try {
            String address = updateMap.get(id);
            if (address.equals(NULLADDR)) {
                LOGGER.severe("failed to update topic " + id + " no response");
                return;
            }
            JSONObject topicJson = httpUtil.httpGetRequest(address);
            if (topicJson == null) {
                LOGGER.severe("failed to update topic " + id + " null response");
                return;
            }
            ChordStorage storage = node.getStorage();
            storage.putObject(id, topicJson);
            LOGGER.info("updated local copy of topic");
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
                break;
            }
            LOGGER.warning("failed to update index on try number: " + (i + 1));
        }
    }
    public String getPublicKeyString() {
        return keyman.encodePublicKey(keys.getPublic());
    }

    public String signMessage(String message, JSONObject topicView, long view) {
        byte[] viewSig = getViewSig(topicView, view);
        byte[] cryptoMessage = createCryptoMessage(viewSig, message, keys.getPublic());
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
        String message = (String) reply.get(JSONFormat.MESSAGE);
        PublicKey pKey = keyman.decodePublicKey((String) reply.get(JSONFormat.PUBLICKEY));
        byte[] cryptoMessage = createCryptoMessage(viewSig, message, pKey);
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
