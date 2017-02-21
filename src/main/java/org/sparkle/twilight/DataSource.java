package org.sparkle.twilight;

import org.json.simple.JSONObject;

import static org.sparkle.twilight.Main.httpUtil;

/**
 * Created by kgoyo on 17-02-2017.
 */
public class DataSource {
    private String url;
    private String data = "DATA NOT AVAILABLE";

    public String getData() {
        return data;
    }

    //should only be used for exceptions
    public void setData(String data) {
        this.data = data;
    }

    public DataSource(String url) {
        this.url = url;
    }

    public void updateData() throws NodeOfflineException {
        JSONObject json = httpUtil.httpGetRequest(url);
        if (json.containsKey("result")) {
            data = json.get("result").toString();
        }
    }
}
