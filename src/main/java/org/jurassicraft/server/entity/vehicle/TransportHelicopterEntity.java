package org.jurassicraft.server.entity.vehicle;

import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementAltimeter;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementArtificialHorizon;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementCompass;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementStatsDisplay;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementTachometer;
import org.jurassicraft.server.item.ItemHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class TransportHelicopterEntity extends HelicopterEntity {

	public TransportHelicopterEntity(World worldIn) {
		// super(worldIn, 5, 3.5f, 8, 3992, 300, 6838, 5);
		super(worldIn, 2, 3.5f, 5, 3992, 300, 6838, 5);

		if (this.world.isRemote) {
			this.addHudOverlayElement(HudElementAltimeter.class);
			this.addHudOverlayElement(HudElementArtificialHorizon.class);
			this.addHudOverlayElement(HudElementTachometer.class);
			this.addHudOverlayElement(HudElementCompass.class);
			this.addHudOverlayElement(HudElementStatsDisplay.class);
		}
	}

	@Override
	protected Seat[] createSeats() {
		Seat middle = new Seat(0F, -0.23F, 1.2F, 0.5F, 0.25F);
		Seat frontLeft = new Seat(-0.55F, -0.34F, 0.1F, 0.5F, 0.25F);
		Seat frontRight = new Seat(0.55F, -0.34F, 0.1F, 0.5F, 0.25F);
		Seat backLeft = new Seat(0.4F, 0.25F, -1F, 0.5F, 0.25F);
		Seat backReft = new Seat(-0.4F, 0.25F, -1F, 0.5F, 0.25F);
		return new Seat[] { middle, frontLeft, frontRight, backLeft, backReft };
	}

	@Override
	protected WheelData createWheels() {
		return new WheelData(1, 2, -1, -2.2);
	}
	
    @Override
    public void doPlayerRotations(EntityPlayer player, float partialTicks) {
    	double offsetY = this.getMountedYOffset() + player.getYOffset();
        GlStateManager.translate(0, offsetY, 0);
        GlStateManager.rotate(this.pitch, 1, 0, 0);
        GlStateManager.translate(0, -offsetY, 0);
    }

	@Override
	public void dropItems() {
		this.entityDropItem(new ItemStack(ItemHandler.VEHICLE_ITEM, 1, 2), 0.1f);
	}

}
