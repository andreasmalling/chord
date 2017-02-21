package org.sparkle.twilight;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;

/**
 * Created by kgoyo on 17-02-2017.
 */
public class DataSource {
    private String getUrl;
    private String setUrl;
    private HttpClient client;
    private String data;

    public String getData() {
        System.out.println("getting data");
        return data;
    }

    public DataSource(String id,String accesstoken) {
        //https://api.particle.io/v1/devices/300028000147353138383138/analogvalue?access_token=b4cf943eeb88b98ef17cafd76d86b557704c5c2f
        getUrl = "https://api.particle.io/v1/devices/" + id + "/analogvalue?access_token=" + accesstoken;

        //https://api.particle.io/v1/devices/300028000147353138383138/led?access_token=b4cf943eeb88b98ef17cafd76d86b557704c5c2f
        setUrl = "https://api.particle.io/v1/devices/" + id + "/led?access_token=" + accesstoken;


        //TODO THIS SHOULD NOT BE HERE AT RELEASE!!!!!
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000).build();
        builder.setDefaultRequestConfig(config);
        client = builder.build();
    }

    private void updateData() throws DataSourceNotAvailableException {
        try {
            JSONObject json = httpGetRequest(getUrl);
            if (json.containsKey("result")) {
                data = json.get("result").toString();
            } else {
                throw new DataSourceNotAvailableException();
            }
        } catch (NodeOfflineException e) {
            throw new DataSourceNotAvailableException();
        }
    }


    //TODO THIS SHOULD NOT BE HERE AT RELEASE!!!!!
    private JSONObject httpGetRequest(String url) throws NodeOfflineException {
        HttpGet getMsg = new HttpGet(url);
        try {
            HttpResponse response = client.execute(getMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("ERROR ERROR: Could not get");     //TODO ??
                throw new IOException();
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent()));
            String jsonstring = "";
            String line;
            while ((line = reader.readLine()) != null) {
                jsonstring += line;
            }
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonstring);
            return json;
        } catch (HttpHostConnectException | ConnectTimeoutException | SocketTimeoutException e) {
            throw new NodeOfflineException();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null; //TODO fix mabye?
    }

    public void updateLoop() {
        data = "DATA NOT AVAILABLE";
        while(true) {
            try {
            updateData();
            //System.out.println("data is updated with new value: " + data);
            int seconds = 1000;
            double random = Math.random() * seconds;
                Thread.sleep((long) (seconds + random));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (DataSourceNotAvailableException e) {
                data = "DATA NOT AVAILABLE";
            }
        }
    }

    public String getSetUrl() {
        return setUrl;
    }

}
