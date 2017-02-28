package org.sparkle.twilight;



import org.json.simple.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Kresten on 21-02-2017.
 */
public class ChordStorage {

    private DB db;
    private ConcurrentMap map;
    public static final String DATA_KEY = "data";
    public static final String ID_KEY = "id";
    public static final String TOKEN_KEY = "token";

    public ChordStorage(String node_id) {
        File file = new File("db/" + node_id);
        file.getParentFile().mkdirs();
        db = DBMaker.fileDB(file)
                .transactionEnable()
                .make();
        map = db.hashMap("map").createOrOpen();
    }

    public void putData(String value) {
        ArrayList<String> data = getDataList();
        data.add(0, value);
        map.put(DATA_KEY, data);
        db.commit();
    }

    public void putData(ArrayList<String> input) {
        map.put(DATA_KEY, input);
        db.commit();
    }

    public void putValue(String key, String value, boolean commit) {
        if(key.equals(DATA_KEY)){
            putData(value);
            return;
        }
        map.put(key, value);
        if (commit) {
            db.commit();
        }
    }

    public String getData() {
        if (map.containsKey(DATA_KEY)) {
            ArrayList<String> temp = (ArrayList<String>) map.get(DATA_KEY);
            if (temp.size() > 0) {
                return temp.get(0);
            }
        }
        return DataSource.DATA_NOT_AVAILABLE;
    }

    public ArrayList<String> getDataList() {
        if (map.containsKey(DATA_KEY)) {
            return (ArrayList<String>) map.get(DATA_KEY);
        }
        return new ArrayList<>();
    }

    public String getValue(String key) throws NoValueException {

        if(map.containsKey(key))
            return map.get(key).toString();
        else
            throw new NoValueException();
    }

    public void shutdown() {
        db.close();
    }

    @Override
    public String toString(){
        String res = "";
        for (Object k: map.keySet()) {
            res += k + ": " + map.get(k) + "\n";
        }
        return res;
    }
}
