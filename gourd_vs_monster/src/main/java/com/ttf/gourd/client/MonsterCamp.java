package com.ttf.gourd.client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

import com.ttf.gourd.constant.Constant;
import com.ttf.gourd.constant.CreatureId;
import com.ttf.gourd.creature.Creature;
import com.ttf.gourd.creature.CreatureFactory;
import com.ttf.gourd.protocol.Msg;
import com.ttf.gourd.protocol.NoParseMsg;
import com.ttf.gourd.protocol.PositionNotifyMsg;
import com.ttf.gourd.stage.SceneController;

import com.ttf.gourd.tool.PositionXY;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class MonsterCamp extends Camp{

    private Creature selectCreature = null;
    PositionXY beginPosition = new PositionXY(0, 0);

    public MonsterCamp(Socket socket, SceneController sceneController,
                       ObjectInputStream in, ObjectOutputStream out) {
        super(socket, sceneController, in, out);
        ArrayList<ImageView> gourdImageView = new ArrayList<>();
        ArrayList<ImageView> monsterImageView = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            ImageView imageView = new ImageView();
            imageView.setVisible(false);
            imageView.setDisable(true);
            sceneController.getFightScene().getChildren().add(imageView);
            monsterImageView.add(imageView);
        }
        for (int i = 0; i <= 20; i++) {
            ImageView imageView = new ImageView();
            imageView.setVisible(false);
            imageView.setDisable(true);
            sceneController.getMapPane().getChildren().add(imageView);
            gourdImageView.add(imageView);
        }

        try {
            CreatureFactory gourdFactory = new CreatureFactory(out, Constant.CampType.GOURD, Constant.Direction.RIGHT,
                    gourdImageView);
            CreatureFactory monsterFactory = new CreatureFactory(out, Constant.CampType.MONSTER, Constant.Direction.LEFT,
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
                sceneController.getMapPane().getChildren().add(creature.getHealthProgressBar());
                sceneController.getMapPane().getChildren().add(creature.getMagicProgressBar());
            }
            double layoutY = 10;
            for (Creature creature : monsterFamily.values()) {
                creature.setEnemyFamily(gourdFamily);
                creature.setMyFamily(monsterFamily);
                creature.setCreatureImagePos(1220, layoutY);
                layoutY += 120;
                sceneController.getMapPane().getChildren().add(creature.getHealthProgressBar());
                sceneController.getMapPane().getChildren().add(creature.getMagicProgressBar());
            }
            msgController = new MsgController(gourdFamily, monsterFamily);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sceneController.getMapPane().setLayoutX(Constant.SCENE_MARGIN_SIZE);
    }

    public void startGame() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                prepareForGame();
            }
        }).start();
    }

    public void bindDragEvent() {
//        System.out.println("MonsterCamp bindDragEvent");
        for(Creature creature : monsterFamily.values()) {
            ImageView tempImageView = creature.getCreatureImageView();
            tempImageView.setVisible(true);
            tempImageView.setDisable(false);
            tempImageView.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (selectCreature != null) {
                        selectCreature.flipControlled();
                        selectCreature.setCreatureImageView();
                    }
                    beginPosition.setPosition(event.getX(), event.getY());
                    creature.flipControlled();
                    creature.setCreatureImageView();
                    selectCreature = creature;
                    System.out.println(tempImageView.getParent());
                }
            });

            tempImageView.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    PositionXY currentPosition = new PositionXY(event.getSceneX(), event.getSceneY());
                    double deltaX = currentPosition.X - beginPosition.Y;
                    double deltaY = currentPosition.Y - beginPosition.Y;
                    if(deltaX < Constant.FIGHT_PANE_WIDTH / 2 + Constant.SCENE_MARGIN_SIZE)
                        deltaX = Constant.FIGHT_PANE_WIDTH / 2 + Constant.SCENE_MARGIN_SIZE;
                    deltaX = Math.min(Constant.FIGHT_PANE_WIDTH + Constant.SCENE_MARGIN_SIZE - creature.getWIDTH(), deltaX);
                    deltaY = Math.max(Constant.SCENE_MARGIN_SIZE, deltaY);
                    if(deltaY > Constant.FIGHT_PANE_HEIGHT - creature.getHEIGHT() + Constant.SCENE_MARGIN_SIZE)
                        deltaY = Constant.FIGHT_PANE_HEIGHT - creature.getHEIGHT() + Constant.SCENE_MARGIN_SIZE;
//                    System.out.println("deltaX: " + deltaX + " " + "deltaY: " + deltaY);
                    creature.setCreatureImagePos(deltaX, deltaY);
//                    System.out.println(tempImageView.getParent() == sceneController.getFightScene());
                }
            });
        }
    }

    public void notifyServerImagePosition() throws IOException {
//        System.out.println("monster notify server image position");
        for (Map.Entry<Integer, Creature> entry : monsterFamily.entrySet()) {
            int creatureId = entry.getKey();
            Creature creature = entry.getValue();
            ImageView tempImageView = creature.getCreatureImageView();
            tempImageView.setOnMouseDragged(null);
            tempImageView.setOnMousePressed(null);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    sceneController.getFightScene().getChildren().remove(tempImageView);
                    sceneController.getMapPane().getChildren().add(tempImageView);
                }
            });
            double layoutX = tempImageView.getLayoutX();
            double layoutY = tempImageView.getLayoutY();
            if(layoutX < Constant.FIGHT_PANE_WIDTH + Constant.SCENE_MARGIN_SIZE - creature.getWIDTH()) {
                layoutX -= Constant.SCENE_MARGIN_SIZE;
                layoutY -= Constant.SCENE_MARGIN_SIZE;
            } else {
                layoutX = randomNum.nextDouble() * (Constant.FIGHT_PANE_WIDTH / 2 - creature.getWIDTH()) + Constant.FIGHT_PANE_WIDTH / 2;
                layoutY = randomNum.nextDouble() * (Constant.FIGHT_PANE_HEIGHT - creature.getHEIGHT());
            }
            creature.setCreatureImagePos(layoutX, layoutY);
            new PositionNotifyMsg(Constant.CampType.MONSTER, creatureId, layoutX, layoutY).sendMsg(out);
        }
        new NoParseMsg(Msg.FINISH_FLAG_MSG).sendMsg(out);

        while(true) {
            try {
                int msgType = in.readInt();
                if(msgType == Msg.POSITION_NOTIFY_MSG) {
                    msgController.getMsgClass(msgType, in);
                } else if(msgType == Msg.START_GAME_MSG) {
//                    System.out.println("start game");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(Creature creature : monsterFamily.values()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    sceneController.getFightScene().getChildren().remove(creature.getCloseAttackImageView());
                    sceneController.getMapPane().getChildren().add(creature.getCloseAttackImageView());
                }
            });
        }
        for(Creature creature : monsterFamily.values()) {
            if(creature.isControlled()) {
                creature.flipControlled();
            }
        }
        new GameStartFight(socket, Constant.CampType.MONSTER, sceneController, in, out,
                monsterFamily, gourdFamily, equipmentFactory).start();
    }
}