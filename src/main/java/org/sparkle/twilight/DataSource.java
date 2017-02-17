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
    private String url;
    private JSONParser parser;
    private HttpClient client;

    public DataSource(String url) {
        this.url = url;


        //TODO THIS SHOULD NOT BE HERE AT RELEASE!!!!!
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000).build();
        builder.setDefaultRequestConfig(config);
        client = builder.build();
    }

    public String getData() throws DataSourceNotAvailableException {
        try {
            JSONObject json = httpGetRequest(url);
            if (json.containsKey("result")) {
                return json.get("result").toString();
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
}
