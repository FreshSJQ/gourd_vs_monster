package com.ttf.gourd.ai;

import com.ttf.gourd.bullet.Bullet;
import com.ttf.gourd.creature.Creature;

import java.util.HashMap;

//ai接口,需要实现观测,移动,攻击
public interface AiInterface {
    //观测
    public Creature observe(Creature myCreature, HashMap<Integer,Creature> enemies);
    //移动方式
    public void moveMod(Creature myCreature, HashMap<Integer,Creature> enemies);
    //攻击模式
    public Bullet aiAttack(Creature myCreature, HashMap<Integer,Creature> enemies);
}
