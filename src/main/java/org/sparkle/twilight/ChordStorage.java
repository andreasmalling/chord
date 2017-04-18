package org.sparkle.twilight;


import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Kresten on 21-02-2017.
 */
public class ChordStorage {

    private DB db;
    private ConcurrentMap map;
    public static final int INDEX_KEY = 0;

    public ChordStorage(String node_id) {
        File file = new File("db/" + node_id);
        file.getParentFile().mkdirs();
        db = DBMaker.fileDB(file)
                .transactionEnable()
                .make();
        map = db.hashMap("map").createOrOpen();
    }

    /**
     * overwrite list in storage
     *
     * @param topics - a JSONArray with topic title and where to find it
     **/
    public void overwriteIndex(Object topics) {
        map.put(INDEX_KEY, topics);
        db.commit();
    }

    /**
     * get index list from storage
     *
     * @return List - an ArrayList with all topics, JSONObjects with titles and where to find it
     **/
    public Object getIndexList() {
        return map.get(INDEX_KEY);
    }

    /**
     * get all the keys (id's) in the database
     *
     * @return Set of keys
     **/
    public Set dumpKeySet() {
        return map.keySet();
    }

    /**
     * get all the keys (id's) in the database
     *
     * @return Set of keys
     **/
    public Object getObject(Object key) {
        return map.get(key);
    }

    /**
     * get all the keys (id's) in the database
     *
     * @return Set of keys
     **/
    public void putObject(Object key, Object value) {
        map.put(key, value);
        db.commit();
    }

    public void shutdown() {
        db.close();
    }

    @Override
    public String toString() {
        String res = "";
        for (Object k : map.keySet()) {
            res += k + ": " + ((JSONAware) map.get(k)).toJSONString() + "\n";
        }
        return res;
    }
}
