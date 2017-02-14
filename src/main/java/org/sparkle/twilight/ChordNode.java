package org.sparkle.twilight;

import java.util.List;

public interface ChordNode {

    int getID();

    /**
     * Return addresses of Successors
     *
     * @return addresses of Successors in a list
     */
    List<String> getSuccessorList();

    /**
     * Return address of Predecessor
     *
     * @return address of Predecessor
     */
    String getPredecessor();

    /**
     * Set nodes Successor address
     *
     * @param successor address of Successor
     */
    void setSuccessor(String successor);

    /**
     * Set nodes Predecessor address
     *
     * @param predecessor address of Successor
     */
    void setPredecessor(String predecessor);

    /**
     * @param address of to be Successor
     */
    void joinRing(String address);

    /**
     * Leaves the ring peacefully
     */
    void leaveRing();

    /**
     * Brutally kills the node, without considering its neighbors (merciless)
     */
    void killNode();

    /**
     * @param id        The id of search
     * @param initiator Address of node asking for key
     */
    void lookup(int id, String initiator);

    boolean isInNetwork();

    void updateFingerTable(int key, String address);
}
