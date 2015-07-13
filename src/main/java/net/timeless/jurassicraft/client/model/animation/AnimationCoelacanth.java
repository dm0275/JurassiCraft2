package net.timeless.jurassicraft.client.model.animation;

import net.ilexiconn.llibrary.client.model.entity.animation.IModelAnimator;
import net.ilexiconn.llibrary.client.model.modelbase.MowzieModelBase;
import net.ilexiconn.llibrary.client.model.modelbase.MowzieModelRenderer;
import net.ilexiconn.llibrary.client.model.tabula.ModelJson;
import net.minecraft.entity.Entity;
import net.timeless.jurassicraft.api.animation.Animator;

public class AnimationCoelacanth implements IModelAnimator
{
    private final Animator animator;

    public AnimationCoelacanth()
    {
        this.animator = new Animator();
    }

    @Override
    public void setRotationAngles(ModelJson model, float f, float f1, float rotation, float rotationYaw, float rotationPitch, float partialTicks, Entity entity)
    {
        float globalSpeed = 0.2F;
        float globalDegree = 0.77F;
        float globalHeight = 2F;

        f = entity.ticksExisted;
        f1 = 1F;

        //tail
        MowzieModelRenderer tail1 = model.getCube("Tail Section 1");
        MowzieModelRenderer tail2 = model.getCube("Tail Section 2");
        MowzieModelRenderer tail3 = model.getCube("Tail Section 3");

        //body
        MowzieModelRenderer body2 = model.getCube("Body Section 2");
        MowzieModelRenderer body3 = model.getCube("Body Section 3");

        //flipper
        MowzieModelRenderer leftFlipper = model.getCube("Left Front Flipper");
        MowzieModelRenderer rightFlipper = model.getCube("Right Front Flipper");

        MowzieModelRenderer[] tail = new MowzieModelRenderer[] { tail3, tail2, tail1, body3, body2 };

        model.chainSwing(tail, 0.5F * globalSpeed, -0.1F, 2, f, f1);

        int ticksExisted = entity.ticksExisted;

        model.chainSwing(tail, 0.15F, -0.1F, 3, ticksExisted, 1.0F);
    }
}
