package com.ttf.gourd.creature;

import com.ttf.gourd.bullet.Bullet;
import com.ttf.gourd.bullet.BulletState;
import com.ttf.gourd.constant.Constant;
import com.ttf.gourd.constant.CreatureId;
import com.ttf.gourd.constant.ImageUrl;
import javafx.scene.image.ImageView;

import java.util.ArrayList;

public class Grandpa extends Creature {

    Grandpa(int faceDirection, ImageView imageView, ImageView closeAttackImageView) {
        super(Constant.CampType.GOURD, CreatureId.GRANDPA_ID, CreatureId.GRANDPA_NAME,
                3000, 200, 75, 10, 0.5, 12, 300.0,
                faceDirection, 70.0, false, Constant.ClawType.NONE_CLAW,
                imageView, closeAttackImageView, ImageUrl.gourdLeftImageMap.get(CreatureId.GRANDPA_ID),
                ImageUrl.gourdLeftSelectImageMap.get(CreatureId.GRANDPA_ID),
                ImageUrl.gourdRightImageMap.get(CreatureId.GRANDPA_ID),
                ImageUrl.gourdRightSelectImageMap.get(CreatureId.GRANDPA_ID));
    }

    //不能去掉,否则爷爷打对面
    @Override
    public ArrayList<Bullet> update() {
        ArrayList<Bullet> bullets = new ArrayList<>();
        if (!isControlled()) {
            if (isAlive()) {
                aiInterface.moveMod(this, myFamily);
                Bullet bullet = aiInterface.aiAttack(this, myFamily);
                if (bullet != null) {
                    bullets.add(bullet);
                }
            }
        } else {
            Bullet bullet = playerAttack();
            if (bullet != null)
                bullets.add(bullet);
            if (qFlag && currentMagic >= baseMagic) {
                ArrayList<Bullet> bullets1 = qAction();
                currentMagic = 0;
                if (bullets1.size() > 0)
                    bullets.addAll(bullets1);
            }
            qFlag = false;
        }
        draw();
        return bullets;
    }

    @Override
    protected Bullet playerAttack() {
        if (playerAttackTarget == null)
            return null;
        if (campType.equals(playerAttackTarget.campType)) {
            if (!isAlive())
                return null;
            if (playerAttackTarget == null)
                return null;
            if (!playerAttackTarget.isAlive())
                return null;
            if (!canAttack())
                return null;
            if (getImagePos().getDistance(playerAttackTarget.getImagePos()) > shootRange)
                return null;
            setLastAttackTimeMillis(System.currentTimeMillis());

            return selectBullet(playerAttackTarget);
        }
        return null;
    }

    @Override
    public ArrayList<Bullet> qAction() {
        ArrayList<Bullet> bulletArrayList = new ArrayList<>();
        if (currentMagic < baseMagic)
            return bulletArrayList;
        currentMagic = 0;
        for (Creature creature : myFamily.values()) {
            if (creature.isAlive() && imagePosition.getDistance(creature.imagePosition) <= 1.5 * shootRange) {
                bulletArrayList.add(new Bullet(this, creature,
                        Constant.REMOTE_BULLET_TYPE, BulletState.THE_GOD_OF_HEALING_MAX));
            }
        }
        return bulletArrayList;
    }
}
