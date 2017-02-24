package org.sparkle.twilight;

import org.json.simple.JSONObject;

import static org.sparkle.twilight.Main.httpUtil;

/**
 * Created by kgoyo on 17-02-2017.
 */
public class DataSource {
    private String url;
    private String data = "DATA NOT AVAILABLE";
    private String getUrl;
    private String setUrl;
    private HttpClient client;
    private String data;

    public DataSource(String id,String accesstoken) {
        //https://api.particle.io/v1/devices/300028000147353138383138/analogvalue?access_token=b4cf943eeb88b98ef17cafd76d86b557704c5c2f
        getUrl = "https://api.particle.io/v1/devices/" + id + "/analogvalue?access_token=" + accesstoken;

        //https://api.particle.io/v1/devices/300028000147353138383138/led?access_token=b4cf943eeb88b98ef17cafd76d86b557704c5c2f
        setUrl = "https://api.particle.io/v1/devices/" + id + "/led?access_token=" + accesstoken;

    }
    //should only be used for exceptions

    public String getData() {
        return data;
    }
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

    public String getSetUrl() {
        return setUrl;
    }

}
