package com.ttf.gourd.protocol;

import java.io.*;

class BulletBuild implements Serializable {
    public int bulletKey;
    public String sourceCamp;
    public int sourceCreatureId;
    public String targetCamp;
    public int targetCreatureId;
    public int bulletType;
    public int bulletState;
}

public class BulletBuildMsg implements Msg {
    private static final int msgType = Msg.BULLET_BUILD_MSG;
    BulletBuild bulletBuild = new BulletBuild();

    public BulletBuildMsg() {
    }

    public BulletBuildMsg(int bulletKey, String sourceCamp, int sourceCreatureId,
                          String targetCamp, int targetCreatureId, int bulletType, int bulletState) {
        bulletBuild.bulletKey = bulletKey;
        bulletBuild.sourceCamp = sourceCamp;
        bulletBuild.sourceCreatureId = sourceCreatureId;
        bulletBuild.targetCamp = targetCamp;
        bulletBuild.targetCreatureId = targetCreatureId;
        bulletBuild.bulletType = bulletType;
        bulletBuild.bulletState = bulletState;
    }

    @Override
    public void sendMsg(ObjectOutputStream outStream) throws IOException {
        outStream.writeInt(msgType);
        outStream.writeObject(bulletBuild);
        outStream.flush();
    }

    @Override
    public void parseMsg(ObjectInputStream inStream) throws IOException, ClassNotFoundException {
        bulletBuild = (BulletBuild) inStream.readObject();
    }

    public int getBulletKey() {
        return bulletBuild.bulletKey;
    }

    public String getSourceCamp() {
        return bulletBuild.sourceCamp;
    }

    public int getSourceCreatureId() {
        return bulletBuild.sourceCreatureId;
    }

    public String getTargetCamp() {
        return bulletBuild.targetCamp;
    }

    public int getTargetCreatureId() {
        return bulletBuild.targetCreatureId;
    }

    public int getBulletType() {
        return bulletBuild.bulletType;
    }

    public int getBulletState() {
        return bulletBuild.bulletState;
    }
}
