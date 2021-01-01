package com.ttf.gourd.protocol;

import java.io.*;

class CreatureState implements Serializable {
    public String campType;
    public int creatureId;
    public int creatureState;
    public long gapTime;
}

public class CreatureStateMsg implements Msg {
    private static final int msgType = Msg.CREATURE_STATE_MSG;
    private CreatureState creatureState = new CreatureState();

    public CreatureStateMsg() {
    }

    public CreatureStateMsg(String campType, int creatureId, int creatureState, long gapTime) {
        this.creatureState.campType = campType;
        this.creatureState.creatureId = creatureId;
        this.creatureState.creatureState = creatureState;
        this.creatureState.gapTime = gapTime;
    }

    @Override
    public void sendMsg(ObjectOutputStream outStream) throws IOException {
        outStream.writeInt(msgType);
        outStream.writeObject(creatureState);
        outStream.flush();
    }

    @Override
    public void parseMsg(ObjectInputStream inStream) throws IOException, ClassNotFoundException {
        creatureState = (CreatureState) inStream.readObject();
    }

    public String getCampType() {
        return creatureState.campType;
    }

    public int getCreatureId() {
        return creatureState.creatureId;
    }

    public int getCreatureState() {
        return creatureState.creatureState;
    }

    public long getGapTime() {
        return creatureState.gapTime;
    }
}
