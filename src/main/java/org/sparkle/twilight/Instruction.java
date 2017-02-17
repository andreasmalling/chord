package org.sparkle.twilight;

import org.json.simple.JSONObject;

public class Instruction {
    public enum Method {GET, POST, PUT, DELETE}

    private Method method;
    private String url;
    private JSONObject body;

    public Instruction(Method method, String url, JSONObject body) {
        this.method = method;
        this.url = url;
        this.body = body;
    }

    public Method getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public JSONObject getBody() {
        return body;
    }
}
