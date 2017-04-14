package org.sparkle.twilight;


import org.json.simple.JSONArray;

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
}
