package org.sparkle.twilight;


import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Kresten on 21-02-2017.
 */
public class ChordStorage {

    private DB db;
    private ConcurrentMap map;
    private final String DATA_KEY = "data";
    private final String RESOURCE_KEY = "resource";

    public ChordStorage(String node_id) {
        File file = new File(node_id);
        db = DBMaker.fileDB(file)
                .transactionEnable()
                .make();
        //TODO INCLUDE DATA HISTORY, and not just latest in map
        map = db.hashMap("map").createOrOpen();
    }

    public void putData(String value) {
        map.put(DATA_KEY, value);
        db.commit();
    }

    public void putResource(String value) {
        map.put(RESOURCE_KEY, value);
        db.commit();
    }

    public void putValue(String type, String value) {
        map.put(type, value);
        db.commit();
    }

    public String getData() {
        if(map.containsKey(DATA_KEY)) {
            return map.get(DATA_KEY).toString();
        } else
            return "DATA NOT AVAILABLE";
    }

    public String getResource(String key) {
        return map.get(RESOURCE_KEY).toString();
    }

    public void shutdown(){
        db.close();
    }
}
