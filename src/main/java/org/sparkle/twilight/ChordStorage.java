package org.sparkle.twilight;


import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class ChordStorage {

    private DB db;
    private ConcurrentMap map;

    public ChordStorage(String node_id) {
        File file = new File("db/" + node_id);
        file.getParentFile().mkdirs();
        db = DBMaker.fileDB(file)
                .transactionEnable()
                .make();
        map = db.hashMap("map").createOrOpen();
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
