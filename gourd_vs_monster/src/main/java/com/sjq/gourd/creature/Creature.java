package com.sjq.gourd.creature;

import com.sjq.gourd.ai.AiInterface;
import com.sjq.gourd.ai.FirstGenerationAi;
import com.sjq.gourd.bullet.Bullet;
import com.sjq.gourd.constant.Constant;
import com.sjq.gourd.equipment.Equipment;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Random;

public class Creature {
    //敌方和己方阵营,需要传参初始化
    protected HashMap<Integer, Creature> enemyFamily;
    protected HashMap<Integer, Creature> myFamily;

    protected DataInputStream inputStream;
    protected DataOutputStream outputStream;

    //campType表示阵营,Constant中有阵营信息
    //creatureId表示生物id,Constant中有每个生物的id
    //creatureName表示生物名字
    protected final String campType;
    protected final int creatureId;
    protected final String creatureName;

    //六大基本信息+射程
    protected final double baseHealth;
    protected final double baseMagic;
    protected final double baseAttack;
    protected final double baseDefense;
    protected final double baseAttackSpeed;
    protected final double baseMoveSpeed;
    double shootRange;

    //六大现有状态
    protected double currentHealth;
    protected double currentMagic;
    protected double currentAttack;
    protected double currentDefense;
    protected double currentAttackSpeed;
    protected double currentMoveSpeed;

    //上次攻击时间
    protected long lastAttackMillis = 0;
    //上次被别人近战攻击时间
    protected long lastCloseAttack = 0;
    //显示被攻击的时候图片上的抓痕的控件
    protected ImageView closeAttackImageView;
    private double closeAttackImageWidth;
    private double closeAttackImageHeight;
    //是否是近战类型
    protected boolean isCloseAttack;
    //自己的近战类型的抓痕
    protected int clawType = Constant.ClawType.NONE_CLAW;

    //上次被方向被设置的时间,用于锁定方向一段时间
    protected long lastDirectionSetTime = 0;

    //将要移动的方向
    protected int direction;
    //是否被控制
    protected boolean isControlled = false;
    //控件的位置
    protected ImagePosition imagePosition;
    //Ai的选择
    protected AiInterface aiInterface;
    //每次攻击能回复的蓝量
    protected double magicIncrementOnce = 10.0;
    //当前拥有的装备
    private Equipment equipment = null;

    //当前的血条和蓝条
    protected ProgressBar healthProgressBar = new ProgressBar();
    protected ProgressBar magicProgressBar = new ProgressBar();

    //生物图片存放的控件
    protected ImageView creatureImageView;

    //生物是否被选择和方向的图片
    protected Image creatureRightImage;
    protected Image selectCreatureRightImage;
    protected Image creatureLeftImage;
    protected Image selectCreatureLeftImage;

    //用于生成随机数
    private final Random randomNum = new Random(System.currentTimeMillis());

    //图片的宽度和高度
    private final double WIDTH;
    private final double HEIGHT;

    //玩家的攻击目标
    protected Creature playerAttackTarget = null;

    //四大标记位置,表示图片是否到了边缘
    protected boolean isHighest = false, isLowest = false, isLeftMost = false, isRightMost = false;
    //四大标记位置,表示被控制时,四个方向是否被按住
    protected boolean isUpPressOn = false, isDownPressOn = false, isLeftPressOn = false, isRightPressOn = false;

    public Creature(DataInputStream in, DataOutputStream out,
                    String campType, int creatureId, String creatureName,
                    double baseHealth, double baseMagic, double baseAttack, double baseDefense, double baseAttackSpeed,
                    double baseMoveSpeed, double shootRange, int faceDirection, double width, boolean isCloseAttack, int clawType,
                    ImageView imageView, ImageView closeAttackImageView,
                    Image creatureLeftImage, Image selectCreatureLeftImage,
                    Image creatureRightImage, Image selectCreatureRightImage) {
        //TODO 这个类里，尽量不要改，改也可以，你可以和我说你下，你要改哪些内容，可以多加函数。
        this.inputStream = in;
        this.outputStream = out;
        this.campType = campType;
        this.creatureId = creatureId;
        this.creatureName = creatureName;
        this.currentHealth = this.baseHealth = baseHealth;
        this.baseMagic = baseMagic;
        this.currentMagic = 0;
        this.currentAttack = this.baseAttack = baseAttack;
        this.currentDefense = this.baseDefense = baseDefense;
        this.currentAttackSpeed = this.baseAttackSpeed = baseAttackSpeed;
        this.currentMoveSpeed = this.baseMoveSpeed = baseMoveSpeed;
        this.shootRange = shootRange;
        this.direction = faceDirection;

        this.isCloseAttack = isCloseAttack;
        this.clawType = clawType;

        this.creatureLeftImage = creatureLeftImage;
        this.selectCreatureLeftImage = selectCreatureLeftImage;
        this.creatureRightImage = creatureRightImage;
        this.selectCreatureRightImage = selectCreatureRightImage;

        //把抓痕的长宽暂时设置为50 50
        //初始化时抓痕不可见,只有在被近战攻击时,抓痕才可见
        this.closeAttackImageView = closeAttackImageView;
        this.closeAttackImageView.setPreserveRatio(true);
        this.closeAttackImageView.setFitWidth(50);
        this.closeAttackImageView.setFitHeight(50);
        closeAttackImageWidth = closeAttackImageHeight = 50;
        closeAttackImageView.setVisible(false);

        creatureImageView = imageView;

        creatureImageView.setPreserveRatio(true);
        this.WIDTH = width;
        creatureImageView.setFitWidth(width);
        this.HEIGHT = creatureLeftImage.getHeight() / creatureLeftImage.getWidth() * width;
        creatureImageView.setFitHeight(HEIGHT);

        if (direction == Constant.Direction.LEFT)
            creatureImageView.setImage(creatureLeftImage);
        else
            creatureImageView.setImage(creatureRightImage);

        creatureImageView.setVisible(false);
        creatureImageView.setDisable(true);
        imagePosition = new ImagePosition(0, 0);


        healthProgressBar.setPrefWidth(Constant.CREATURE_IMAGE_WIDTH);
        healthProgressBar.setPrefHeight(Constant.BAR_HEIGHT);
        healthProgressBar.setMinHeight(Constant.BAR_HEIGHT);
        healthProgressBar.setStyle("-fx-accent: red;");
        healthProgressBar.setVisible(false);

        magicProgressBar.setPrefWidth(Constant.CREATURE_IMAGE_WIDTH);
        magicProgressBar.setPrefHeight(Constant.BAR_HEIGHT);
        magicProgressBar.setMinHeight(Constant.BAR_HEIGHT);
        magicProgressBar.setStyle("-fx-accent: blue;");
        magicProgressBar.setVisible(false);

        //ai
        aiInterface = new FirstGenerationAi(creatureId + System.currentTimeMillis());
    }


    public void setCreatureImageView() {
        if (isControlled()) {
            if (direction == Constant.Direction.LEFT)
                this.creatureImageView.setImage(selectCreatureLeftImage);
            else
                this.creatureImageView.setImage(selectCreatureRightImage);
        } else {
            if (direction == Constant.Direction.LEFT)
                this.creatureImageView.setImage(creatureLeftImage);
            else
                this.creatureImageView.setImage(creatureRightImage);
        }
    }

    //设置position并更新控件位置
    public void setCreatureImagePos(double layoutX, double layoutY) {
        imagePosition.setLayoutX(layoutX);
        imagePosition.setLayoutY(layoutY);
        creatureImageView.setLayoutX(layoutX);
        creatureImageView.setLayoutY(layoutY);
    }

    //设置移动方向
    public void setDirection(int direction) {
        if (isControlled()) {
            this.direction = direction;
        } else if (System.currentTimeMillis() - lastDirectionSetTime >= Constant.DIRECTION_LOCK_TIME) {
            this.direction = direction;
            lastDirectionSetTime = System.currentTimeMillis();
            setCreatureImageView();
        }
    }

    //判断是否下载可以攻击,收到攻速的限制
    public boolean canAttack() {
        return (System.currentTimeMillis() - lastAttackMillis >= 1000.0 / currentAttackSpeed);
    }

    //这个函数是发生位移的函数,每一帧只要没死,就发生一次位移
    public void move() {
        //死则返回
        if (!isAlive())
            return;
        //获取当前位置
        double x = imagePosition.getLayoutX();
        double y = imagePosition.getLayoutY();

        //根据方向进行位移
        switch (direction) {
            case Constant.Direction.UP: {
                y -= currentMoveSpeed;
                break;
            }
            case Constant.Direction.DOWN: {
                y += currentMoveSpeed;
                break;
            }
            case Constant.Direction.LEFT: {
                x -= currentMoveSpeed;
                break;
            }
            case Constant.Direction.RIGHT: {
                x += currentMoveSpeed;
                break;
            }
            default: {
                break;
            }
        }
        //超出范围则固定成最边界位置并更新四个边界标记位
        if (x <= 0) {
            x = 0;
            isLeftMost = true;
        } else
            isLeftMost = false;
        if (x >= Constant.FIGHT_PANE_WIDTH - WIDTH) {
            x = Constant.FIGHT_PANE_WIDTH - WIDTH;
            isRightMost = true;
        } else
            isRightMost = false;
        if (y <= 0) {
            y = 0;
            isHighest = true;
        } else
            isHighest = false;
        if (y >= Constant.FIGHT_PANE_HEIGHT - HEIGHT) {
            y = Constant.FIGHT_PANE_HEIGHT - HEIGHT;
            isLowest = true;
        } else isLowest = false;
        //立即根据方向和控制人更新图片状态
        if (isControlled()) {
            if (creatureImageView.getImage() != selectCreatureLeftImage
                    && direction == Constant.Direction.LEFT)
                creatureImageView.setImage(selectCreatureLeftImage);
            else if (creatureImageView.getImage() != selectCreatureRightImage
                    && direction == Constant.Direction.RIGHT)
                creatureImageView.setImage(selectCreatureRightImage);
        } else {
            if (creatureImageView.getImage() != creatureLeftImage
                    && direction == Constant.Direction.LEFT)
                creatureImageView.setImage(creatureLeftImage);
            else if (creatureImageView.getImage() != creatureRightImage
                    && direction == Constant.Direction.RIGHT)
                creatureImageView.setImage(creatureRightImage);
        }
        //最终
        setCreatureImagePos(x, y);
    }

    //判断是否还活着
    public boolean isAlive() {
        return currentHealth > 0;
    }

    //画血条
    public void drawBar() {
        Platform.runLater(() -> healthProgressBar.setVisible(true));
        healthProgressBar.setLayoutX(imagePosition.getLayoutX());
        healthProgressBar.setLayoutY(imagePosition.getLayoutY() - 2 * Constant.BAR_HEIGHT);
        double progressValue = (double) currentHealth / baseHealth;
        double finalProgressValue = progressValue;
        Platform.runLater(() -> healthProgressBar.setProgress(finalProgressValue));

        Platform.runLater(() -> magicProgressBar.setVisible(true));
        magicProgressBar.setLayoutX(imagePosition.getLayoutX());
        magicProgressBar.setLayoutY(imagePosition.getLayoutY() - Constant.BAR_HEIGHT);
        progressValue = (double) currentMagic / baseMagic;
        double finalProgressValue1 = progressValue;
        Platform.runLater(() -> magicProgressBar.setProgress(finalProgressValue1));
    }

    //根据新的生命值信息更新当前生命值,如果生命值<=0,则=0,如果超出最大生命值,则等于最大生命值
    public void setCurrentHealth(double healthVal) {
        if (healthVal > baseHealth)
            currentHealth = baseHealth;
        else if (healthVal < 0)
            currentHealth = 0;
        else
            currentHealth = healthVal;
    }

    //获得图片正中心
    public ImagePosition getCenterPos() {
        double x = imagePosition.getLayoutX();
        double y = imagePosition.getLayoutY();
        return new ImagePosition(x + WIDTH / 2, y + HEIGHT / 2);
    }

    public void drawCloseAttack() {
        if (System.currentTimeMillis() - lastCloseAttack <= Constant.CLAW_IMAGE_EXIST_TIME) {
            //显示近战攻击图片
            //正中心对齐
            ImagePosition pos = getCenterPos();
            double x = pos.getLayoutX() - closeAttackImageWidth / 2;
            double y = pos.getLayoutY() - closeAttackImageHeight / 2;
            closeAttackImageView.setLayoutX(x);
            closeAttackImageView.setLayoutY(y);
            closeAttackImageView.setVisible(true);
        } else {
            //否则不显示近战图片
            closeAttackImageView.setVisible(false);
        }
    }

    //每回合不管死没死都要调用draw
    public void draw() {
        drawCloseAttack();
        if (isAlive()) {
            creatureImageView.setVisible(true);
            creatureImageView.setDisable(false);
            drawBar();
            move();
        } else {
            creatureImageView.setVisible(false);
            creatureImageView.setDisable(true);
            healthProgressBar.setVisible(false);
            magicProgressBar.setVisible(false);
        }
    }

    //封装移动方式,画,攻击,返回子弹
    public Bullet update() {
        if (!isControlled()) {
            if (isAlive()) {
                aiInterface.moveMod(this);
                draw();
                Bullet bullet = aiInterface.aiAttack(this, enemyFamily);
                return bullet;
            } else {
                draw();
                return null;
            }
        } else {
            draw();
            Bullet bullet = playerAttack();
            setPlayerAttackTarget(null);
            return bullet;
        }
    }

    //翻转isControlled状态
    public void flipControlled() {
        isControlled = !isControlled;
    }

    public boolean isControlled() {
        return isControlled;
    }

    public void setEnemyFamily(HashMap<Integer, Creature> hashMap) {
        enemyFamily = hashMap;
    }

    public void setMyFamily(HashMap<Integer, Creature> myFamily) {
        this.myFamily = myFamily;
    }

    public String getCampType() {
        return campType;
    }

    public ImageView getCreatureImageView() {
        return creatureImageView;
    }

    public int getCreatureId() {
        return creatureId;
    }

    public ProgressBar getHealthProgressBar() {
        return healthProgressBar;
    }

    public ProgressBar getMagicProgressBar() {
        return magicProgressBar;
    }

    public ImagePosition getImagePos() {
        return imagePosition;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public double getCurrentAttack() {
        return currentAttack;
    }

    public double getCurrentDefense() {
        return currentDefense;
    }


    public double getShootRange() {
        return shootRange;
    }

    public void setLastAttackTimeMillis(long currentTimeMillis) {
        lastAttackMillis = currentTimeMillis;
    }

    public double getCurrentMoveSpeed() {
        return currentMoveSpeed;
    }

    public double getBaseHealth() {
        return baseHealth;
    }


    public double getCurrentMagic() {
        return currentMagic;
    }

    public void setCurrentMagic(double currentMagic) {
        if (currentMagic < 0)
            currentMagic = 0;
        if (currentMagic > baseMagic)
            currentMagic = baseMagic;
        this.currentMagic = currentMagic;
    }

    public double getMagicIncrementOnce() {
        return magicIncrementOnce;
    }

    public void setLastCloseAttack(long lastCloseAttack) {
        this.lastCloseAttack = lastCloseAttack;
    }


    public String getCreatureName() {
        return creatureName;
    }

    public HashMap<Integer, Creature> getEnemyFamily() {
        return enemyFamily;
    }

    public boolean isCloseAttack() {
        return isCloseAttack;
    }

    public int getClawType() {
        return clawType;
    }

    public ImageView getCloseAttackImageView() {
        return closeAttackImageView;
    }

    public boolean isLeftMost() {
        return isLeftMost;
    }

    public boolean isRightMost() {
        return isRightMost;
    }

    public boolean isLowest() {
        return isLowest;
    }

    public boolean isHighest() {
        return isHighest;
    }

    public boolean canChangeDirection() {
        return (System.currentTimeMillis() - lastDirectionSetTime >= Constant.DIRECTION_LOCK_TIME);
    }

    public void pickUpEquipment(Equipment equipment) {
        if (!equipment.getName().equals("TreasureBag")) {
            giveUpEquipment();
            this.equipment = equipment;
        }
        equipment.dispose();
        equipment.takeEffect(this);
    }

    private void giveUpEquipment() {
        //舍弃原来的装备
        if (equipment != null) {
            equipment.giveUpTakeEffect(this);
            equipment = null;
        }
    }

    public void setPlayerAttackTarget(Creature playerAttackTarget) {
        this.playerAttackTarget = playerAttackTarget;
    }

    public double getHEIGHT() {
        return HEIGHT;
    }

    public double getWIDTH() {
        return WIDTH;
    }

    private Bullet playerAttack() {
        if (!isAlive()) {
//            System.out.println("你已死亡");
            return null;
        }
        if (playerAttackTarget == null)
            return null;
//            System.out.println("没有目标");
        if (!playerAttackTarget.isAlive()) {
//            System.out.println("目标已死亡");
            return null;
        }
        if (!canAttack()) {
//            System.out.println("攻速过慢,还不能攻击");
            return null;
        }
        if (getImagePos().getDistance(playerAttackTarget.getImagePos()) > shootRange) {
//            System.out.println("攻击距离不够");
            return null;
        }

        setLastAttackTimeMillis(System.currentTimeMillis());
        if (isCloseAttack()) {
            System.out.println("我方近战已攻击");
            return new Bullet(this, playerAttackTarget,
                    new ImagePosition(imagePosition.getLayoutX(), imagePosition.getLayoutY()));
        } else {
            System.out.println("我方远程已攻击");
            return new Bullet(this, playerAttackTarget,
                    new ImagePosition(imagePosition.getLayoutX(), imagePosition.getLayoutY()), null);
        }

//        if (isAlive() || attackTarget == null || !attackTarget.isAlive())
//            return null;
//        if (!canAttack())
//            return null;
//        if (getImagePos().getDistance(attackTarget.getImagePos()) > shootRange)
//            return null;
//        if (isCloseAttack())
//            return new Bullet(this, attackTarget, new ImagePosition(imagePosition.getLayoutX(), imagePosition.getLayoutY()));
//        else
//            return new Bullet(this, attackTarget, new ImagePosition(imagePosition.getLayoutX(), imagePosition.getLayoutY()), null);
    }

    public void setCurrentMoveSpeed(double currentMoveSpeed) {
        this.currentMoveSpeed = currentMoveSpeed;
    }

    public void setCurrentAttack(double currentAttack) {
        this.currentAttack = currentAttack;
    }

    public void setCurrentDefense(double currentDefense) {
        this.currentDefense = currentDefense;
    }

    public void setCurrentAttackSpeed(double currentAttackSpeed) {
        this.currentAttackSpeed = currentAttackSpeed;
    }

    public void setShootRange(double shootRange) {
        this.shootRange = shootRange;
    }

    public double getBaseAttackSpeed() {
        return baseAttackSpeed;
    }

    public double getCurrentAttackSpeed() {
        return currentAttackSpeed;
    }
}