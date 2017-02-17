package org.sparkle.twilight;

import org.json.simple.JSONObject;

public class Instruction {
    public enum Method {GET, POST, PUT, DELETE}

    private Method method;
    private JSONObject body;
    private String target;

    public Instruction(Method method, JSONObject body, String target) {
        this.method = method;
        this.body = body;
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public JSONObject getBody() {
        return body;
    }

    public String getTarget() {
        return target;
    }
}
