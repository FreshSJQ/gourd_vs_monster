package com.ttf.gourd.creature;

import com.ttf.gourd.bullet.Bullet;
import com.ttf.gourd.bullet.BulletState;
import com.ttf.gourd.constant.Constant;
import com.ttf.gourd.constant.CreatureId;
import com.ttf.gourd.constant.ImageUrl;
import javafx.scene.image.ImageView;

import java.util.ArrayList;

public class FourthGourd extends Creature {

    FourthGourd(int faceDirection, ImageView imageView, ImageView closeAttackImageView) {
        super(Constant.CampType.GOURD, CreatureId.FOURTH_GOURD_ID, CreatureId.FOURTH_GOURD_NAME,
                3000, 150, 80, 40, 0.5, 12, 400.0,
                faceDirection, 70.0, false, Constant.ClawType.NONE_CLAW,
                imageView, closeAttackImageView, ImageUrl.gourdLeftImageMap.get(CreatureId.FOURTH_GOURD_ID),
                ImageUrl.gourdLeftSelectImageMap.get(CreatureId.FOURTH_GOURD_ID),
                ImageUrl.gourdRightImageMap.get(CreatureId.FOURTH_GOURD_ID),
                ImageUrl.gourdRightSelectImageMap.get(CreatureId.FOURTH_GOURD_ID));
    }

    @Override
    public ArrayList<Bullet> qAction() {
        //火焰之神的降临,给1.5射程范围内的所有敌军发射一颗带有火焰之子特效的子弹
        ArrayList<Bullet> bullets = new ArrayList<>();
        if (currentMagic < baseMagic)
            return bullets;
        currentMagic = 0;
        for (Creature creature : enemyFamily.values()) {
            if (this.imagePosition.getDistance(creature.imagePosition) < 1.5 * shootRange)
                bullets.add(new Bullet(this, creature,
                        Constant.REMOTE_BULLET_TYPE, BulletState.THE_SON_OF_FLAME));
        }
        return bullets;
    }
}
