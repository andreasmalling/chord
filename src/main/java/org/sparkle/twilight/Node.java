package org.sparkle.twilight;

import javax.inject.Singleton;

/**
 * Created by Root on 2/2/2017.
 */
public class Node implements ChordNode {
    private int id;

    public Node(int id) {
        this.id = id;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getSuccessor() {
        return null;
    }

    @Override
    public void setSuccessor(String successor) {

    }

    @Override
    public void joinRing() {

    }

    @Override
    public void leaveRing() {

    }

    @Override
    public String lookup(int id, String initiator) {
        return null;
    }
}
