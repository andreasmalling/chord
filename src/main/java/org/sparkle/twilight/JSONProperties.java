package org.sparkle.twilight;

/**
 * Created by Kresten on 17-02-2017.
 */
public class JSONProperties {
    public String method;
    public int hops;
    public boolean showInConsole;

    public JSONProperties(String method, int hops, boolean showInConsole) {
        this.method = method;
        this.hops = hops;
        this.showInConsole = showInConsole;
    }

    public JSONProperties() {
        method = "finger";
        hops = 0;
        showInConsole = false;
    }
}
