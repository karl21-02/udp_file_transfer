package com.kangwon.global.entity;

import java.io.Serializable;

/**
 * ~패킷 헤더
 */
public class Packet implements Serializable {
    private int type; // data=0, ack=1, eot=2
    private int seqNum; // from 0
    private int ackNum;
    private int length; // from 0 to 1000, 0 for ACK or EOT
    byte[] data;

    public Packet(int type, int seqNum, byte[] data) {
        this.type = type;
        this.seqNum = seqNum;
        this.data = data;
        this.length = (data == null) ? 0 : data.length;
        this.ackNum = -1;
    }

    public Packet(int type, int seqNum) {
        this.type = type;
        this.seqNum = seqNum;
        this.ackNum = seqNum;
        this.length = 0;
        this.data = new byte[0];
    }

    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }

    public int getSeqNum() {
        return seqNum;
    }
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public int getAckNum() {
        return ackNum;
    }
    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
}
