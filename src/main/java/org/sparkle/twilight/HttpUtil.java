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
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class HttpUtil {
    protected HttpClient client;
    private final int connectionTimeout = 3000;
    protected static Logger LOGGER;

    public HttpUtil() {
        LOGGER = Logger.getLogger(HttpUtil.class.getName());
        LoggerHandlers.addHandlers(LOGGER);
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
            StringEntity params = new StringEntity(body.toJSONString(), StandardCharsets.UTF_8);
            msgType.addHeader("content-type", JSONFormat.JSON);
            msgType.setEntity(params);
            LOGGER.finer("Send " + msgType + " message to " + url);
            long start = System.currentTimeMillis();
            HttpResponse response = client.execute(msgType);
            LOGGER.info("Time to get response: " + (System.currentTimeMillis() - start));
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.warning("HTTP response is: " + response.getStatusLine().getStatusCode());
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
            LOGGER.finer("Send GET message to " + url);
            long start = System.currentTimeMillis();
            HttpResponse response = client.execute(getMsg);
            LOGGER.info("Time to get response: " + (System.currentTimeMillis() - start));
            int sc = response.getStatusLine().getStatusCode();
            if (sc != 200) {
                LOGGER.severe("Get request failed with status code " + sc);
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent(), StandardCharsets.UTF_8));
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
        LOGGER.severe("TODO fix mabye?");
        return null;
    }
}
