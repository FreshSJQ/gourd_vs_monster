package com.ttf.gourd.server;

import com.ttf.gourd.bullet.Bullet;
import com.ttf.gourd.collision.Collision;
import com.ttf.gourd.constant.Constant;
import com.ttf.gourd.constant.CreatureId;
import com.ttf.gourd.constant.ImageUrl;
import com.ttf.gourd.creature.Creature;
import com.ttf.gourd.creature.CreatureFactory;
import com.ttf.gourd.creature.ImagePosition;
import com.ttf.gourd.equipment.Equipment;
import com.ttf.gourd.equipment.EquipmentFactory;

import com.ttf.gourd.protocol.*;
import javafx.scene.image.ImageView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.RandomStringUtils;

public class ServerScene {
    private HashMap<Integer, Creature> gourdFamily = new HashMap<>();
    private HashMap<Integer, Creature> monsterFamily = new HashMap<>();

    private EquipmentFactory equipmentFactory = null;
    private HashMap<Integer, Equipment> equipmentList = new HashMap<>();
    private int equipmentKey = 0;

    private ObjectOutputStream outFile = null;

    private ServerSocket serverSocket;
    private Socket gourdSocket = null;
    private Socket monsterSocket = null;

    private ObjectInputStream inGourd;
    private ObjectOutputStream outGourd;
    private ObjectInputStream inMonster;
    private ObjectOutputStream outMonster;

    private MsgController gourdMsgController = null;
    private MsgController monsterMsgController = null;

    boolean gourdFinishFlag = false;
    boolean monsterFinishFlag = false;

    private ConcurrentHashMap<Integer, Bullet> bullets = new ConcurrentHashMap<>();

    private long gameOverTimeMillis = 0;

    private boolean gameOverFlag = false;
    private boolean abnormalDisconnect = false;

    public ServerScene(ServerSocket serverSocket, Socket gourdSocket, Socket monsterSocket,
                       ObjectInputStream inGourd, ObjectOutputStream outGourd,
                       ObjectInputStream inMonster, ObjectOutputStream outMonster) {
        this.serverSocket = serverSocket;
        this.gourdSocket = gourdSocket;
        this.monsterSocket = monsterSocket;
        this.inGourd = inGourd;
        this.outGourd = outGourd;
        this.inMonster = inMonster;
        this.outMonster = outMonster;
        try {
            String fileName = RandomStringUtils.random(6, "sjqxrhSJQXRHNUnu33418985460");
            outFile = new ObjectOutputStream(new FileOutputStream("../playbackFiles/" + fileName + ".back"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        initScene();
    }

    public void initScene() {
        ImageUrl.initImageUrl();

        ArrayList<ImageView> imageViews = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ImageView imageView = new ImageView();
            imageView.setVisible(false);
            imageView.setDisable(true);
            imageViews.add(imageView);
        }
        equipmentFactory = new EquipmentFactory(imageViews);

        ArrayList<ImageView> gourdImageView = new ArrayList<>();
        ArrayList<ImageView> monsterImageView = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            ImageView imageView = new ImageView();
            gourdImageView.add(imageView);
        }
        for (int i = 0; i <= 20; i++) {
            ImageView imageView = new ImageView();
            monsterImageView.add(imageView);
        }

        try {
            CreatureFactory gourdFactory = new CreatureFactory(outGourd, Constant.CampType.GOURD, Constant.Direction.RIGHT,
                    gourdImageView);
            CreatureFactory monsterFactory = new CreatureFactory(outMonster, Constant.CampType.MONSTER, Constant.Direction.LEFT,
                    monsterImageView);

            int id = CreatureId.MIN_GOURD_ID;
            while (gourdFactory.hasNext()) {
                Creature creature = gourdFactory.next();
                gourdFamily.put(id++, creature);
            }
            id = CreatureId.MIN_MONSTER_ID;
            while (monsterFactory.hasNext()) {
                Creature creature = monsterFactory.next();
                monsterFamily.put(id++, creature);
            }

            for (Creature creature : gourdFamily.values()) {
                creature.setEnemyFamily(monsterFamily);
                creature.setMyFamily(gourdFamily);
            }
            for (Creature creature : monsterFamily.values()) {
                creature.setEnemyFamily(gourdFamily);
                creature.setMyFamily(monsterFamily);
            }

            gourdMsgController = new MsgController(gourdFamily, monsterFamily);
            monsterMsgController = new MsgController(gourdFamily, monsterFamily);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGame() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int gourdMsgType = inGourd.readInt();
                        if (gourdMsgType == Msg.FINISH_FLAG_MSG) {
                            gourdFinishFlag = true;
                            break;
                        } else if (gourdMsgType == Msg.POSITION_NOTIFY_MSG) {
                            gourdMsgController.getMsgClass(gourdMsgType, inGourd);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int monsterMsgType = inMonster.readInt();
                        if (monsterMsgType == Msg.FINISH_FLAG_MSG) {
                            monsterFinishFlag = true;
                            break;
                        } else if (monsterMsgType == Msg.POSITION_NOTIFY_MSG) {
                            monsterMsgController.getMsgClass(monsterMsgType, inMonster);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        while (true) {
            if (gourdFinishFlag && monsterFinishFlag) {
                for(Map.Entry<Integer, Creature> entry : gourdFamily.entrySet()) {
                    int creatureId = entry.getKey();
                    Creature creature = entry.getValue();
                    ImageView tempImageView = creature.getCreatureImageView();
                    PositionNotifyMsg positionNotifyMsg = new PositionNotifyMsg(Constant.CampType.GOURD, creatureId,
                            tempImageView.getLayoutX(), tempImageView.getLayoutY());
                    positionNotifyMsg.sendMsg(outMonster);
                    positionNotifyMsg.sendMsg(outFile);
                }

                for(Map.Entry<Integer, Creature> entry : monsterFamily.entrySet()) {
                    int creatureId = entry.getKey();
                    Creature creature = entry.getValue();
                    ImageView tempImageView = creature.getCreatureImageView();
                    PositionNotifyMsg positionNotifyMsg = new PositionNotifyMsg(Constant.CampType.MONSTER, creatureId,
                            tempImageView.getLayoutX(), tempImageView.getLayoutY());
                    positionNotifyMsg.sendMsg(outGourd);
                    positionNotifyMsg.sendMsg(outFile);
                }
                new NoParseMsg(Msg.START_GAME_MSG).sendMsg(outGourd);
                new NoParseMsg(Msg.START_GAME_MSG).sendMsg(outMonster);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        gourdFinishFlag = false;
        monsterFinishFlag = false;
        startFight();
    }

    public void startFight() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int gourdMsgType = inGourd.readInt();
                        if (gourdMsgType == Msg.FINISH_FLAG_MSG) {
                            gourdFinishFlag = true;
                        } else if(gourdMsgType == Msg.SOCKET_DISCONNECT_MSG) {
                            Thread.sleep(3000);
                            inGourd.close();
                            outGourd.close();
                            gourdSocket.close();
                            if(!serverSocket.isClosed())
                                serverSocket.close();
                            break;
                        }
                        else {
                            gourdMsgController.getMsgClass(gourdMsgType, inGourd);
                        }
                    } catch (Exception e) {
                        try {
                            abnormalDisconnect = true;
                            inGourd.close();
                            outGourd.close();
                            gourdSocket.close();
                            inMonster.close();
                            outMonster.close();
                            monsterSocket.close();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int monsterMsgType = inMonster.readInt();
                        if (monsterMsgType == Msg.FINISH_FLAG_MSG) {
                            monsterFinishFlag = true;
                        } else if(monsterMsgType == Msg.SOCKET_DISCONNECT_MSG) {
                            Thread.sleep(3000);
                            inMonster.close();
                            outMonster.close();
                            monsterSocket.close();
                            if(!serverSocket.isClosed())
                                serverSocket.close();
                            break;
                        }
                        else {
                            monsterMsgController.getMsgClass(monsterMsgType, inMonster);
                        }
                    } catch (Exception e) {
                        try {
                            abnormalDisconnect = true;
                            inGourd.close();
                            outGourd.close();
                            gourdSocket.close();
                            inMonster.close();
                            outMonster.close();
                            monsterSocket.close();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                    }
                }
            }
        }).start();
        while (true) {
            if(abnormalDisconnect) {
                try {
                    outFile.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
            if(gameOverFlag) {
                new NoParseMsg(Msg.SOCKET_DISCONNECT_MSG).sendMsg(outGourd);
                new NoParseMsg(Msg.SOCKET_DISCONNECT_MSG).sendMsg(outMonster);
                break;
            }

            for(Creature gourdCreature : gourdFamily.values()) {
                gourdCreature.sendAllAttribute(outMonster);
                gourdCreature.sendAllAttribute(outFile);
            }

            for(Creature monsterCreature : monsterFamily.values()) {
                monsterCreature.sendAllAttribute(outGourd);
                monsterCreature.sendAllAttribute(outFile);
            }

            ArrayList<Bullet> closeBullets = gourdMsgController.getCloseBullets();
            closeBullets.addAll(monsterMsgController.getCloseBullets());
            for(Bullet tempBullet : closeBullets) {
                if(tempBullet.isValid()) {
                    Collision collision = tempBullet.update();
//                    collision.collisionEvent();
                    BulletCloseAttackMsg bulletCloseAttackMsg = new BulletCloseAttackMsg(tempBullet.getSourceCreature().getCreatureId(),
                            tempBullet.getTargetCreature().getCreatureId(), tempBullet.getBulletState().ordinal());
                    bulletCloseAttackMsg.sendMsg(outGourd);
                    bulletCloseAttackMsg.sendMsg(outMonster);
                    bulletCloseAttackMsg.sendMsg(outFile);
                }
            }


            HashMap<Integer, Bullet> buildBullets = gourdMsgController.getBuildBullets();
            if(buildBullets.size() != 0) {
                Iterator<Map.Entry<Integer, Bullet>> bulletMapIterator = buildBullets.entrySet().iterator();
                while(bulletMapIterator.hasNext()) {
                    Map.Entry<Integer, Bullet> bulletEntry = bulletMapIterator.next();
                    int bulletKey = bulletEntry.getKey();
                    Bullet bullet = bulletEntry.getValue();
                    bullets.put(bulletKey, bullet);
                    BulletBuildMsg bulletBuildMsg = new BulletBuildMsg(bulletKey,
                                bullet.getSourceCreature().getCampType(), bullet.getSourceCreature().getCreatureId(),
                                bullet.getTargetCreature().getCampType(), bullet.getTargetCreature().getCreatureId(),
                                bullet.getBulletType(), bullet.getBulletState().ordinal());
                    bulletBuildMsg.sendMsg(outMonster);
                    bulletBuildMsg.sendMsg(outFile);
                }
            }

            buildBullets = monsterMsgController.getBuildBullets();
            if(buildBullets.size() != 0) {
                Iterator<Map.Entry<Integer, Bullet>> bulletMapIterator = buildBullets.entrySet().iterator();
                while(bulletMapIterator.hasNext()) {
                    Map.Entry<Integer, Bullet> bulletEntry = bulletMapIterator.next();
                    int bulletKey = bulletEntry.getKey();
                    Bullet bullet = bulletEntry.getValue();
                    bullets.put(bulletKey, bullet);
                    BulletBuildMsg bulletBuildMsg = new BulletBuildMsg(bulletKey,
                            bullet.getSourceCreature().getCampType(), bullet.getSourceCreature().getCreatureId(),
                            bullet.getTargetCreature().getCampType(), bullet.getTargetCreature().getCreatureId(),
                            bullet.getBulletType(), bullet.getBulletState().ordinal());
                    bulletBuildMsg.sendMsg(outGourd);
                    bulletBuildMsg.sendMsg(outFile);
                }
            }

            ArrayList<CreatureStateGroup> creatureStateList = gourdMsgController.getCreatureStateGroupArrayList();
            for(CreatureStateGroup group : creatureStateList) {
                String campType = group.campType;
                int creatureId = group.creatureId;
                int creatureState = group.creatureState;
                long gapTime = group.gapTime;
                CreatureStateMsg creatureStateMsg = new CreatureStateMsg(campType, creatureId, creatureState, gapTime);
                creatureStateMsg.sendMsg(outGourd);
                creatureStateMsg.sendMsg(outMonster);
                creatureStateMsg.sendMsg(outFile);
            }

            HashMap<Creature, Double> sameDestinyHashMap = monsterMsgController.getSameDestinyHashMap();
            for(Map.Entry<Creature, Double> entry : sameDestinyHashMap.entrySet()) {
                Creature creature = entry.getKey();
                double deltaHealth = entry.getValue();
                new SameDestinyMsg(creature.getCampType(), creature.getCreatureId(), deltaHealth).sendMsg(outGourd);
            }

            Iterator<Map.Entry<Integer, Bullet>> hashMapIterator = bullets.entrySet().iterator();
            while(hashMapIterator.hasNext()) {
                Map.Entry<Integer, Bullet> mapEntry = hashMapIterator.next();
                int bulletKey = mapEntry.getKey();
                Bullet bullet = mapEntry.getValue();
                if(bullet.isValid()) {
                    Collision collision = bullet.update();
                    if(collision != null) {
                        hashMapIterator.remove();
                        bullet.setValid(false);
                        BulletDeleteMsg bulletDeleteMsg = new BulletDeleteMsg(bulletKey);
                        bulletDeleteMsg.sendMsg(outGourd);
                        bulletDeleteMsg.sendMsg(outMonster);
                        bulletDeleteMsg.sendMsg(outFile);

                    } else {
                        BulletMoveMsg bulletMoveMsg = new BulletMoveMsg(bulletKey, bullet.getImagePosition().getLayoutX(),
                                bullet.getImagePosition().getLayoutY());
                        bulletMoveMsg.sendMsg(outGourd);
                        bulletMoveMsg.sendMsg(outMonster);
                        bulletMoveMsg.sendMsg(outFile);
                    }
                }
            }
            if(equipmentFactory.hasNext()) {
                int randNum = equipmentFactory.nextInt();
                ImagePosition imagePosition = equipmentFactory.nextImagePosition();
                Equipment equipment = equipmentFactory.next(randNum, imagePosition);
                equipmentList.put(equipmentKey, equipment);
                EquipmentGenerateMsg equipmentGenerateMsg = new EquipmentGenerateMsg(equipmentKey, randNum,
                        imagePosition.getLayoutX(), imagePosition.getLayoutY());
                equipmentGenerateMsg.sendMsg(outMonster);
                equipmentGenerateMsg.sendMsg(outGourd);
                equipmentGenerateMsg.sendMsg(outFile);
                equipmentKey += 1;
            }

            HashMap<Creature, Integer> requestEquipment = gourdMsgController.getRequestEquipment();
            requestEquipment.putAll(monsterMsgController.getRequestEquipment());
            for(Map.Entry<Creature, Integer> entry : requestEquipment.entrySet()) {
                Creature creature = entry.getKey();
                int equipmentKey = entry.getValue();
                if(equipmentList.get(equipmentKey) != null) {
                    Equipment equipment = equipmentList.get(equipmentKey);
                    creature.pickUpEquipment(equipment);
                    EquipmentRequestMsg equipmentRequestMsg = new EquipmentRequestMsg(creature.getCampType(),
                            creature.getCreatureId(), equipmentKey);
                    equipmentRequestMsg.sendMsg(outGourd);
                    equipmentRequestMsg.sendMsg(outMonster);
                    equipmentRequestMsg.sendMsg(outFile);
                    equipmentList.remove(equipmentKey);
                }
            }

            for(Equipment equipment : equipmentList.values()) {
                equipment.draw();
            }

            try {
                new NoParseMsg(Msg.FRAME_FINISH_FLAG_MSG).sendMsg(outFile);
                int judge = judgeWin(gourdFamily, monsterFamily);
                if (judge != 2) {
                    FinishGameFlagMsg finishGameFlagMsg = null;
                    if(judge == -1)
                        finishGameFlagMsg = new FinishGameFlagMsg(Constant.CampType.MONSTER);
                    else if(judge == 1)
                        finishGameFlagMsg = new FinishGameFlagMsg(Constant.CampType.GOURD);
                    else {
                        new FinishGameFlagMsg(Constant.CampType.MONSTER).sendMsg(outMonster);
                        new FinishGameFlagMsg(Constant.CampType.GOURD).sendMsg(outGourd);
                        new FinishGameFlagMsg("").sendMsg(outFile);
                        outFile.close();
                        break;
                    }
                    finishGameFlagMsg.sendMsg(outGourd);
                    finishGameFlagMsg.sendMsg(outMonster);
                    finishGameFlagMsg.sendMsg(outFile);
                    outFile.close();
                    gameOverTimeMillis = System.currentTimeMillis();
                    gameOverFlag = true;
                }
                Thread.yield();
                Thread.sleep(Constant.FRAME_TIME);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //根据阵营以及两个family判断是谁获胜了,-1,0,1,2返回值只可能是这四种状态
    private int judgeWin(HashMap<Integer, Creature> myFamily, HashMap<Integer, Creature> enemyFamily) {
        //todo -1 0 1 2 分别代表妖精胜利,平局,葫芦娃胜利,还没结束
        int flag = 2;
        boolean allGourdDie = true, allMonsterDie = true;
        for (Creature creature : myFamily.values())
            if (creature.isAlive()) {
                allGourdDie = false;
                break;
            }

        for (Creature creature : enemyFamily.values())
            if (creature.isAlive()) {
                allMonsterDie = false;
                break;
            }

        if (allGourdDie && allMonsterDie)
            flag = 0;
        else if (allGourdDie)
            flag = -1;
        else if (allMonsterDie)
            flag = 1;
        return flag;
    }
}
