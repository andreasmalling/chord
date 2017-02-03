package org.sparkle.twilight;

public interface ChordNode {

    public int getID();

    /**
     * Return address of Successor
     * @return address of Successor
     */
    public String getSuccessor();

    /**
     * Return address of Predecessor
     * @return address of Predecessor
     */
    public String getPredecessor();

    /**
     * Set nodes Successor address
     * @param successor address of Successor
     */
    public void setSuccessor(String successor);

    /**
     * Set nodes Predecessor address
     * @param predecessor address of Successor
     */
    public void setPredecessor(String predecessor);

    /**
     *
     * @param address of to be Successor
     */
    public void joinRing(String address);

    public void leaveRing();

    /**
     *
     * @param id The id of search
     * @param initiator Address of node asking for key
     */
    public void lookup(int id, String initiator);

    public boolean isInNetwork();
}
