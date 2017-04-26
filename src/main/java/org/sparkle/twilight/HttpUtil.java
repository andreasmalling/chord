package org.sparkle.twilight;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

/**
 * Created by Kresten on 21-02-2017.
 */
public class HttpUtil {
    private HttpClient client;
    private final int connectionTimeout = 3000;

    public HttpUtil() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(connectionTimeout).build();
        builder.setDefaultRequestConfig(config);
        client = builder.build();
    }

    public void httpPostRequest(String url, JSONObject body) throws NodeOfflineException {
        HttpPost postMsg = new HttpPost(url);
        createAndExecuteRequest(url, body, postMsg);
    }

    public void httpPutRequest(String url, JSONObject body) throws NodeOfflineException {
        HttpPut putMsg = new HttpPut(url);
        createAndExecuteRequest(url, body, putMsg);
    }

    private void createAndExecuteRequest(String url, JSONObject body, HttpEntityEnclosingRequestBase msgType) throws NodeOfflineException {
        try {
            StringEntity params = new StringEntity(body.toJSONString());
            msgType.addHeader("content-type", JSONFormat.JSON);
            msgType.setEntity(params);
            HttpResponse response = client.execute(msgType);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println(response.getStatusLine().getStatusCode());
                System.out.println("ERROR ERROR " + url);
            }
        } catch (HttpHostConnectException | ConnectTimeoutException | SocketTimeoutException e) {
            throw new NodeOfflineException();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject httpGetRequest(String url) throws NodeOfflineException {
        HttpGet getMsg = new HttpGet(url);
        getMsg.addHeader("Accept", JSONFormat.JSON); //required if we want to do the M2M vs H2M rest stuff
        try {
            HttpResponse response = client.execute(getMsg);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("ERROR ERROR: Could not get");
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
