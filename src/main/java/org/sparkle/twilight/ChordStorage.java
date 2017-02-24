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
    public static final String DATA_KEY = "data";
    public static final String RESOURCE_KEY = "resource";
    public static final String ID_KEY = "id";
    public static final String TOKEN_KEY = "token";

    public ChordStorage(String node_id) {
        File dir = new File("db/");
        dir.mkdirs();
        File file = new File("db/" + node_id);
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

    public void putValue(String key, String value, boolean commit) {
        map.put(key, value);
        if (commit) {
            db.commit();
        }
    }

    public String getData() {
        if (map.containsKey(DATA_KEY)) {
            return map.get(DATA_KEY).toString();
        } else
            return DataSource.DATA_NOT_AVAILABLE;
    }

    public String getResource(String key) {
        return map.get(RESOURCE_KEY).toString();
    }

    public void shutdown() {
        db.close();
    }
}
