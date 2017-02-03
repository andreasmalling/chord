package org.sparkle.twilight;

public interface ChordNode {

    public int getID();

    /**
     * Return address of Successor
     * @return address of Successor
     */
    public String getSuccessor();

    /**
     * Set nodes Successor address
     * @param successor address of Successor
     */
    public void setSuccessor(String successor);

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
     * @return Address of node responsible of key
     */
    public String lookup(int id, String initiator);


}
