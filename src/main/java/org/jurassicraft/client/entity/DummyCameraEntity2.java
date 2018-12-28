package org.jurassicraft.client.entity;

import javax.vecmath.Vector3d;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.World;

public class DummyCameraEntity2 extends Entity {

	public static DummyCameraEntity2 instance;

	public DummyCameraEntity2(World world) {
		super(world);
		if (instance == null) {
			instance = this;
		}
	}

	public static void onUnloadWorld() {
		if (instance != null) {
			instance.setDead();
			instance = null;
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		this.rotationYaw++;
		if (rotationYaw == 365) {
			rotationYaw = 0;
		}
	}

	@Override
	protected void entityInit() {

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {

	}
}
