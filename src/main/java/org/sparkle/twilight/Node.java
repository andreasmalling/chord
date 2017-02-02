package org.sparkle.twilight;

import javax.inject.Singleton;

/**
 * Created by Root on 2/2/2017.
 */
public class Node {
    private int value=0;

    public int get() {
        value++;
        return value;
    }

    public Node() {
    }
}
