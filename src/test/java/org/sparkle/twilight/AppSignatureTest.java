package org.sparkle.twilight;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by kgoyo on 02-05-2017.
 */
public class AppSignatureTest {
    private App opApp;
    private App rApp1;
    private App rApp2;
    private JSONObject testTopic;
    private JSONObject reply1;
    private JSONObject reply2;

    @Before
    public void setup() {
        opApp = new App(null);
        rApp1 = new App(null);
        rApp2 = new App(null);
        testTopic = new JSONObject();
        String title = "TITLE";
        String message = "MESSAGE";
        testTopic.put(JSONFormat.TITLE, title);
        testTopic.put(JSONFormat.MESSAGE, message);
        testTopic.put(JSONFormat.TIMESTAMP, 0L);
        String opsig = opApp.signOP(title, message);
        testTopic.put(JSONFormat.SIGNATURE, opsig);
        testTopic.put(JSONFormat.PUBLICKEY, opApp.getPublicKeyString());
        JSONArray replies = new JSONArray();
        testTopic.put(JSONFormat.REPLIES, replies);

        //reply 1
        reply1 = new JSONObject();
        String rMsg1 = "first message";
        long rview1 = 0L;
        String rSig1 = rApp1.signMessage(rMsg1, testTopic, rview1);
        reply1.put(JSONFormat.MESSAGE, rMsg1);
        reply1.put(JSONFormat.TIMESTAMP, 1L);
        reply1.put(JSONFormat.VIEW, rview1);
        reply1.put(JSONFormat.PUBLICKEY, rApp1.getPublicKeyString());
        reply1.put(JSONFormat.SIGNATURE, rSig1);

        replies.add(reply1);

        //relply 2
        reply2 = new JSONObject();
        String rMsg2 = "second message";
        long rview2 = 1L;
        String rSig2 = rApp2.signMessage(rMsg2, testTopic, rview2);
        reply2.put(JSONFormat.MESSAGE, rMsg2);
        reply2.put(JSONFormat.TIMESTAMP, 2L);
        reply2.put(JSONFormat.VIEW, rview2);
        reply2.put(JSONFormat.PUBLICKEY, rApp2.getPublicKeyString());
        reply2.put(JSONFormat.SIGNATURE, rSig2);

        replies.add(reply2);
    }

    @Test
    public void keysAreDifferent() {
        assertNotEquals(opApp.getPublicKeyString(), rApp1.getPublicKeyString());
        assertNotEquals(opApp.getPublicKeyString(), rApp2.getPublicKeyString());
        assertNotEquals(rApp1.getPublicKeyString(), rApp2.getPublicKeyString());
    }

    @Test
    public void sigsAreDifferent() {
        String sig1 = (String) testTopic.get(JSONFormat.SIGNATURE);
        JSONArray replies = (JSONArray) testTopic.get(JSONFormat.REPLIES);
        String sig2 = (String) ((JSONObject) replies.get(0)).get(JSONFormat.SIGNATURE);
        String sig3 = (String) ((JSONObject) replies.get(1)).get(JSONFormat.SIGNATURE);
        assertNotEquals(sig1, sig2);
        assertNotEquals(sig1, sig3);
        assertNotEquals(sig2, sig3);
    }

    @Test
    public void repliesListShouldBeSize2() {
        JSONArray replies = (JSONArray) testTopic.get(JSONFormat.REPLIES);
        assertEquals("size should be 2", 2, replies.size());
    }

    @Test
    public void validateReplyToOp() {
        assertTrue("The first reply should be valid", opApp.validateReply(reply1, testTopic));
        assertTrue("The second reply should be valid", opApp.validateReply(reply2, testTopic));
    }
}