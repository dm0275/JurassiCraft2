package org.jurassicraft.server.entity.vehicle;

import net.minecraft.block.BlockAir;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.model.BlockStateLoader;
import net.minecraftforge.client.model.b3d.B3DModel;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.server.permission.context.ContextKeys;
import org.lwjgl.input.Keyboard;

import com.google.common.collect.TreeMultiset;
import com.ibm.icu.impl.ICUService.Key;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.proxy.ClientProxy;
import org.jurassicraft.client.render.RenderingHandler;
import org.jurassicraft.client.render.entity.ThirdPersonViewRenderer;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElement;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementAltimeter;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementArtificialHorizon;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementStatsDisplay;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudOverlay;
import org.jurassicraft.client.render.overlay.HelicopterHUDRenderer.HudElementTachometer;
import org.jurassicraft.client.sound.EntitySound;
import org.jurassicraft.server.entity.ai.util.InterpValue;
import org.jurassicraft.server.entity.vehicle.VehicleEntity.Seat;
import org.jurassicraft.server.entity.vehicle.VehicleEntity.Speed;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.message.CarEntityPlayRecord;
import org.jurassicraft.server.message.UpdateVehicleControlMessage;
import org.jurassicraft.server.util.MutableVec3;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;

public class HelicopterEntity extends VehicleEntity {

	private static final byte UPWARD = 0b010000;
	private static final byte DOWNWARD = 0b100000;
	public float gearLift;
	public boolean shouldGearLift = true;
	private final InterpValue rotationYawInterp = new InterpValue(this, 4f);
	public boolean isFlying;
	public final InterpValue interpRotationPitch = new InterpValue(this, 0.25D);
	public final InterpValue interpRotationRoll = new InterpValue(this, 0.25D);
	private MutableVec3 direction;
	public float pitch;
	public float roll;
	private final int MAX_MOVEMENT_ROTATION = 15;
	private boolean shouldFallDamage;
	public double rotAmount = 0D;
	private Vec3d prevInAirPos;
	private float damageAmount;
	private MutableBlockPos mb = new MutableBlockPos();
	private boolean lockOn;
	@SideOnly(Side.CLIENT)
	private HudOverlay hud;

	private float currentEngineSpeed = 0;
	private float torque;

	/*
	 * Technical specifications
	 */
	private int enginePower; // In PS
	private int engineSpeed; // In rotations per minute(recommended range: 1 - 300)
	private int rotorLength; // In blocks
	private int weight; // In kilograms
	private float physicalWidth; // In blocks
	private float physicalHeight; // In blocks
	private float physicalDepth; // In blocks
	protected float qualityGrade = 0.75f;
	private boolean simpleControle;

	/*
	 * =============================================================================
	 * Note: The helicopters physics do not work for an helicopter flying upside
	 * down
	 * =============================================================================
	 * Disclaimer: Do not take the formulas from this code for an physical project,
	 * they do not fulfill the academical standard you need and are only working for
	 * hobby helicopter constructors(as they came out of model helicopter
	 * constructing)
	 * =============================================================================
	 */

	// Later Superclass Constructor
	/**
	 * widthIn, heightIn, depthIn in blocks; enginePower in PS; engineSpeedIn in
	 * rotations per minute; weightIn in kilograms; rotorLengthIn in blocks
	 */
	public HelicopterEntity(World worldIn, int widthIn, int heightIn, int depthIn, int enginePowerIn, int engineSpeedIn, int weightIn,
			int rotorLengthIn) {
		super(worldIn);

		this.physicalWidth = widthIn;
		this.physicalHeight = heightIn;
		this.physicalDepth = depthIn;

		this.enginePower = (int) ((float) enginePowerIn * 735.5f);
		this.engineSpeed = engineSpeedIn;
		this.weight = weightIn;
		this.rotorLength = rotorLengthIn;
		this.torque = computeTorque();

		this.setEntityBoundingBox(new AxisAlignedBB(0, 0, 0, this.physicalWidth, this.physicalHeight, this.physicalDepth));
		this.setSize((float) this.physicalWidth, (float) this.physicalHeight);
		this.speedModifier = 1.5f;
		this.isFlying = false;
		this.direction = new MutableVec3(0, 1, 0);

		this.simpleControle = true;
		if (this.world.isRemote) {
			this.hud = new HudOverlay();
		}
	}

	public HelicopterEntity(World worldIn) {
		super(worldIn);
		double w = 5f; // width in blocks
		this.physicalHeight = 3.5f; // height in blocks
		double d = 8f; // depth in blocks
		this.setEntityBoundingBox(new AxisAlignedBB(0, 0, 0, w, this.physicalHeight, d));
		this.setSize((float) w, (float) this.physicalHeight);
		this.speedModifier = 1.5f;
		this.isFlying = false;
		this.direction = new MutableVec3(0, 1, 0);

		// this.enginePower = (int) (3392.0 * 735.5);
		this.enginePower = (int) (3992.0 * 735.5);
		// this.enginePower = (int) (2600.0 * 735.5);
		this.engineSpeed = 300;
		this.weight = 6838;
		this.rotorLength = 5;
		this.simpleControle = true;
		this.torque = computeTorque();
		if (this.world.isRemote) {
			this.hud = new HudOverlay();
		}
		this.lockOn = true;
		this.addHudOverlayElement(HudElementAltimeter.class);
		this.addHudOverlayElement(HudElementArtificialHorizon.class);
		this.addHudOverlayElement(HudElementTachometer.class);
		this.addHudOverlayElement(HudElementStatsDisplay.class);
	}

	public boolean upward() {
		return this.getStateBit(UPWARD);
	}

	public boolean downward() {
		return this.getStateBit(DOWNWARD);
	}

	@Override
	public void startSound() {
		ClientProxy.playHelicopterSound(this);
	}

	public void upward(boolean upward) {
		this.setStateBit(UPWARD, upward);

	}

	public void downward(boolean downward) {
		this.setStateBit(DOWNWARD, downward);
	}

	@Override
	public void dropItems() {
		this.dropItem(ItemHandler.HELICOPTER, 1);
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
	protected boolean shouldStopUpdates() {
		return false;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
	}

	@Override
	protected void doBlockCollisions() {
		super.doBlockCollisions();
	}

	public boolean isController(EntityPlayer e) {
		if ((!this.getIfExists(0, false).equals("") && this.getIfExists(0, false).equals(Integer.toString(e.getEntityId())))) {
			return true;
		}
		return false;
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (!world.isRemote) {
			if (!player.isSneaking() && !(player.getRidingEntity() == this)) {
				player.startRiding(this);
			}
		}
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	protected void handleControl() {

		if (isController(Minecraft.getMinecraft().player)) {
			if (this.isInWater()) {
				this.upward(false);
				this.downward(false);
			} else {
				this.upward(ClientProxy.getKeyHandler().HELICOPTER_UP.isKeyDown());
				this.downward(ClientProxy.getKeyHandler().HELICOPTER_DOWN.isKeyDown());
				this.increaseThirdPersonViewDistance(ClientProxy.getKeyHandler().HELICOPTER_THIRD_PERSON_VIEW_ZOOM_OUT.isKeyDown());
				this.decreaseThirdPersonViewDistance(ClientProxy.getKeyHandler().HELICOPTER_THIRD_PERSON_VIEW_ZOOM_IN.isKeyDown());
				this.handleKeyEnableAutoPilot(ClientProxy.getKeyHandler().HELICOPTER_AUTOPILOT.isPressed());
				this.handleKeyLock(ClientProxy.getKeyHandler().HELICOPTER_LOCK.isPressed());
			}
			super.handleControl();
		}
	}

	@Override
	public void fall(float distance, float damageMultiplier) {
		if (!world.isRemote && !isFlying) {
			float damage = MathHelper.ceil((distance - 3F) * damageMultiplier);
			if (damage > 0) {

				this.setHealth(this.getHealth() - (float) (damage * 1.25F));

				if (this.getHealth() <= 0) {
					this.setDead();
					if (this.world.getGameRules().getBoolean("doEntityDrops")) {
						this.dropItems();
					}
				}
			}
		}

		if (this.world.isRemote && !isFlying) {
			float damage = MathHelper.ceil((distance - 3F) * damageMultiplier);
			if (damage > 0) {
				float tmp = this.getHealth() - (float) (damage * 1.25F);
				if (tmp <= 0) {
					this.playHelicopterExplosion();
				}
			}
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		for (int i = 0; i < this.seats.length; i++) {
			if (passenger.equals(this.getEntityInSeat(i))) {
				passenger.noClip = false;
				break;
			}
		}
		if (this.world.isRemote) {
			resetThirdPersonViewDistance();
		}
	}

	@Override
	public void onEntityUpdate() {
		if (world.isRemote) {
			this.isFlying = this.hasNoGravity();
		}
		super.onEntityUpdate();

		if (!this.isInWater()) {
			float dist = this.getDistanceToGround();
			if (!world.isRemote) {
				for (int i = 0; i < this.seats.length; i++) {
					Entity e = this.getEntityInSeat(i);
					if (e != null) {
						e.fallDistance = 0;
					}
				}
			}
			if (forward() && this.isFlying) {
				this.pitch += this.computeThrottleUpDown();
			} else if (this.backward() && this.isFlying) {
				this.pitch -= this.computeThrottleUpDown();
			} else if (this.simpleControle && this.isFlying && this.lockOn) {
				if (Math.abs(this.pitch) > 0 && Math.abs(this.pitch) < 1.0f) {
					this.pitch = 0;
					// this.setCurrentEngineSpeed(this.computeRequiredEngineSpeedForHover());
				} else if (this.pitch < 0f) {
					this.pitch += this.computeThrottleUpDown();
					// this.changeCurrentEngineSpeed(this.getCurrentEngineSpeed() -
					// this.computeRequiredEngineSpeedForHover());
				} else if (this.pitch > 0f) {
					this.pitch -= this.computeThrottleUpDown();
					// this.changeCurrentEngineSpeed(this.getCurrentEngineSpeed()
					// -this.computeRequiredEngineSpeedForHover());
				}
			}
			if (this.left() && !Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && this.isFlying) {
				this.roll += this.computeThrottleUpDown();
			} else if (this.right() && this.isFlying) {
				this.roll -= this.computeThrottleUpDown();
			} else if (this.simpleControle && this.isFlying && this.lockOn) {
				if (Math.abs(this.roll) > 0 && Math.abs(this.roll) < 1.0f) {
					this.roll = 0;
					// this.setCurrentEngineSpeed(this.computeRequiredEngineSpeedForHover());
				} else if (this.roll < 0f) {
					this.roll += this.computeThrottleUpDown();
					// this.changeCurrentEngineSpeed(this.getCurrentEngineSpeed()
					// -this.computeRequiredEngineSpeedForHover());
				} else if (this.roll > 0f) {
					this.roll -= this.computeThrottleUpDown();
					// this.changeCurrentEngineSpeed(this.getCurrentEngineSpeed()
					// -this.computeRequiredEngineSpeedForHover());
				}
			}
			// if (this.pitch >= computeMaxMovementRotation(dist)) {
			// this.pitch = computeMaxMovementRotation(dist);
			// }
			// if (this.pitch <= -computeMaxMovementRotation(dist)) {
			// this.pitch = -computeMaxMovementRotation(dist);
			// }
			if (this.pitch > 180) {
				this.pitch = -180 + (this.pitch - 180);
			}
			if (this.pitch < -180) {
				this.pitch = this.pitch + 360;
			}
			if (this.pitch > this.computeMaxMovementRotation(dist)) {
				this.pitch -= this.computeThrottleUpDown();
			} else if (this.pitch < -this.computeMaxMovementRotation(dist)) {
				this.pitch += this.computeThrottleUpDown();
			}
			if (this.roll >= this.computeMaxMovementRotation(dist)) {
				this.roll = this.computeMaxMovementRotation(dist);
			}
			if (this.roll <= -this.computeMaxMovementRotation(dist)) {
				this.roll = -this.computeMaxMovementRotation(dist);
			}

			// if ((this.forward() || this.backward() || this.left() || this.right()) &&
			// this.isFlying && this.simpleControle) {
			// this.changeCurrentEngineSpeed((this.computeRequiredEngineSpeedForHover() -
			// this.getCurrentEngineSpeed()));
			// } else if (this.isFlying && this.simpleControle && !(this.upward() ||
			// this.downward())) {
			// this.changeCurrentEngineSpeed(this.computeRequiredEngineSpeedForHover() -
			// this.getCurrentEngineSpeed());
			// // System.out.println(this.computeRequiredEngineSpeedForHover() -
			// // this.getCurrentEngineSpeed());
			// }

			float requiredSpeedForHovering = this.computeRequiredEngineSpeedForHover();
			if ((this.shouldAdjustEngineSpeedByHorizontalControls(requiredSpeedForHovering)
					|| this.shouldAdjustEngineSpeedWithoutHorizontalControls(requiredSpeedForHovering)) && this.simpleControle
					&& this.isFlying && this.getControllingPassenger() != null
					&& Math.abs(this.getCurrentEngineSpeed() - requiredSpeedForHovering) <= 8f) {
				if (Math.abs(this.getCurrentEngineSpeed() - requiredSpeedForHovering) <= 2) {
					this.setCurrentEngineSpeed(requiredSpeedForHovering);
				} else if (this.getCurrentEngineSpeed() > requiredSpeedForHovering) {
					this.changeCurrentEngineSpeed(this.computeRequiredEngineSpeedForHover() - this.getCurrentEngineSpeed());
				} else if (this.getCurrentEngineSpeed() < requiredSpeedForHovering) {
					this.changeCurrentEngineSpeed(this.computeRequiredEngineSpeedForHover() - this.getCurrentEngineSpeed());
				}
			}

			rotationYawInterp.reset(this.rotationYaw - 180D);
			this.interpRotationPitch.setTarget(this.direction.zCoord * -30D);
			this.interpRotationRoll.setTarget(this.direction.xCoord * 20D);
			if ((this.getControllingPassenger() != null)) {
				if (this.upward()) {
					this.changeCurrentEngineSpeed(this.computeThrottleUpDown());
					// if (!this.isFlying) {
					this.setFlying();
					// }
				} else if (this.downward() && this.isFlying) {
					this.shouldFallDamage = false;
					this.changeCurrentEngineSpeed(-this.computeThrottleUpDown());
				} else {
					if (!this.isFlying) {
						this.setNoGravity(false);
						for (int i = 0; i < this.seats.length; i++) {
							Entity e = this.getEntityInSeat(i);
							if (e != null) {
								e.setNoGravity(false);
							}
						}
						if (this.simpleControle) {
							if (this.getCurrentEngineSpeed() > 0) {
								this.changeCurrentEngineSpeed(-1);
							}
						}
					} else {
						if (this.simpleControle) {
							if (Math.abs(this.getCurrentEngineSpeed() - requiredSpeedForHovering) <= 2) {
								this.setCurrentEngineSpeed(requiredSpeedForHovering);
							} else if (this.getCurrentEngineSpeed() > requiredSpeedForHovering) {
								this.changeCurrentEngineSpeed(-1.5f);
							} else if (this.getCurrentEngineSpeed() < requiredSpeedForHovering) {
								this.changeCurrentEngineSpeed(1.5f);
							}
						}
					}
				}
			} else if (this.getCurrentEngineSpeed() > 0) {
				this.changeCurrentEngineSpeed(-1);
			}

			if (!this.isFlying) {
				this.speedModifier = -0.75f;
			} else {
				this.speedModifier = 1.5f;
			}
			if (this.onGround == true) {
				this.isFlying = false;
				if ((this.pitch > 30 || this.pitch < -30)) {
					this.setHealth(-3);
					if (this.world.isRemote) {
						this.playHelicopterExplosion();
					}
				}
			}
			if (world.isRemote) {
				if (!this.shouldGearLift) {
					this.gearLift += 0.02f;
				} else {
					this.gearLift -= 0.02f;
				}

				// if(this.getPosition().getY() - 10 <
				// world.getChunkFromBlockCoords(this.getPosition()).getPrecipitationHeight(this.getPosition()).getY()){
				if (dist < 10) {
					this.shouldGearLift = false;
				} else {
					this.shouldGearLift = true;
				}
			}
			if (this.getControllingPassenger() == null) {
				this.setNoGravity(false);
			}
			if (this.onGround && this.shouldFallDamage) {
				this.damageAmount = (float) this.prevInAirPos.y - (float) this.getPositionVector().y;
				this.setHealth(this.getHealth() - (float) Math.floor((double) (this.damageAmount / 3)));
				this.shouldFallDamage = false;
			}
			if (world.isRemote) {
				if (this.gearLift < -0.5f) {
					this.gearLift = -0.5f;
				}
				if (this.gearLift > 0) {
					this.gearLift = 0f;
				}
			}
			this.rotAmount += this.getCurrentEngineSpeed() * 0.00666666666 / 2d;

			if (this.getCurrentEngineSpeed() >= 1 && !this.isRotorAreaFree()) {
				this.setHealth(this.getHealth() - 1);
			}
		} else {
			this.setNoGravity(false);
			this.wheelRotateAmount = 0;
			this.setCurrentEngineSpeed(1);
		}

		if (this.world.isRemote) {
			this.updateHudOverlay();
		}
	}

	@Override
	protected void applyMovement() {
		float moveAmount = 0.0f;

		// A(side) = this.depth * this.height;
		float surfaceFront = this.physicalWidth * this.physicalHeight;
		float surfaceTop = this.physicalWidth * this.physicalDepth;
		float horizontalSpeed = (float) Math.abs(Math.sqrt(Math.pow(this.motionX, 2) + Math.pow(this.motionZ, 2)) * 20);
		float verticalSpeed = (float) Math.abs(this.motionY * 20);
		float flowResistanceFront = (float) (2 * surfaceFront * 0.5f * 1.2f * Math.pow(horizontalSpeed, 2));
		float flowResistanceTop = (float) (2 * surfaceTop * 0.5f * 1.2f * Math.pow(verticalSpeed, 2));

		moveAmount = ((computeHorizontalForceFrontBack() - flowResistanceFront) / this.weight) / 20;
		// moveAmount *= 0.5;
		this.motionY += ((computeVerticalForce() - flowResistanceTop) / this.weight - 9.81) / 20;

		if (this.left()) {
			this.rotationDelta -= 20.0F * moveAmount;
		} else if (this.right()) {
			this.rotationDelta += 20.0F * moveAmount;
		}

		this.rotationDelta = MathHelper.clamp(this.rotationDelta, -30 * 0.1F, 30 * 0.1F);
		this.rotationYaw += this.rotationDelta;
		float rotYaw = this.rotationYaw;
		if (this.left() && !this.forward() && !this.backward() && !this.right()) {
			rotYaw -= 90;
			moveAmount = ((computeHorizontalForceLeftRight() - flowResistanceFront) / this.weight) / 20;
		} else if (!this.left() && !this.forward() && !this.backward() && this.right()) {
			rotYaw -= 90;
			moveAmount = ((computeHorizontalForceLeftRight() - flowResistanceFront) / this.weight) / 20;
		}
		this.motionX += MathHelper.sin(-rotYaw * 0.017453292F) * moveAmount;
		this.motionZ += MathHelper.cos(rotYaw * 0.017453292F) * moveAmount;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
	}

	@Override
	public float getSoundVolume() {
		return this.getCurrentEngineSpeed() > 0
				? (Math.abs(this.getCurrentEngineSpeed()) + 0.001F) / (this.sound == null || this.sound.isDonePlaying() ? 2f : 4f)
				: (Math.abs(this.wheelRotateAmount) + 0.001F) / (this.sound == null || this.sound.isDonePlaying() ? 2f : 4f);
	}

	@Nonnull
	@Override
	public EnumFacing getAdjustedHorizontalFacing() {
		return super.getAdjustedHorizontalFacing();
	}

	@Override
	protected WheelData createWheels() {
		return new WheelData(1, 2, -1, -2.2);
	}

	@Override
	protected boolean shouldTyresRender() {
		return false;
	}

	@Override
	public void updatePassenger(Entity passenger) {
		if (this.isPassenger(passenger)) {
			Seat seat = null;
			if (getSeatForEntity(passenger) != -1)
				seat = this.seats[getSeatForEntity(passenger)];

			Vec3d pos;
			if (seat == null) {
				pos = new Vec3d(this.posX, this.posY + this.physicalHeight, this.posZ);
			} else {
				pos = seat.getPos();
			}
			passenger.setPosition(pos.x, pos.y + this.interpRotationPitch.getCurrent() / 75D, pos.z);
			passenger.rotationYaw += this.rotationDelta;
			passenger.setRotationYawHead(passenger.getRotationYawHead() + this.rotationDelta);
			if (passenger instanceof EntityLivingBase) {
				EntityLivingBase living = (EntityLivingBase) passenger;
				living.renderYawOffset += (living.rotationYaw - living.renderYawOffset) * 0.6F;
			}
		}
	}

	private void playHelicopterExplosion() {
		this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.posX, this.posY, this.posZ, 0.1, 0.1, 0.1);
		this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 4.0F,
				(1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F, false);
	}

	private float computeMaxMovementRotation(float dist) {
		return (dist <= 3) ? (dist / 3.0f) * 90f : (this.lockOn) ? MAX_MOVEMENT_ROTATION : 180f;
	}

	private float getDistanceToGround() {
		boolean found = false;
		float dist = -1;
		mb.setPos(this.getPosition());
		while (!found) {
			if (this.posY < 0) {
				break;
			}
			if (world.isAirBlock(mb)) {
				mb = mb.setPos(mb.getX(), mb.getY() - 1, mb.getZ());
			} else {
				found = true;
				dist = (float) (this.posY - mb.getY() - 1);
			}
		}
		return dist;
	}

	@SideOnly(Side.CLIENT)
	private void increaseThirdPersonViewDistance(boolean shouldIncrease) {
		if (shouldIncrease) {
			RenderingHandler.INSTANCE.setThirdPersonViewDistance(RenderingHandler.INSTANCE.getThirdPersonViewDistance() + 1);
		}
	}

	@SideOnly(Side.CLIENT)
	private void decreaseThirdPersonViewDistance(boolean shouldDecrease) {
		if (shouldDecrease) {
			RenderingHandler.INSTANCE.setThirdPersonViewDistance(RenderingHandler.INSTANCE.getThirdPersonViewDistance() - 1);
		}
	}

	@SideOnly(Side.CLIENT)
	private void resetThirdPersonViewDistance() {
		RenderingHandler.INSTANCE.resetThirdPersonViewDistance();
	}

	@Override
	public void setDead() {
		if (!world.isRemote) {
			this.playHelicopterExplosion();
		}
		super.setDead();
	}

	@Override
	public float getCollisionBorderSize() {
		return 2.25f;
	}

	private boolean isRotorAreaFree() {
		boolean isFree = true;
		for (int x = -this.rotorLength; x < this.rotorLength && isFree; x++) {
			for (int z = -this.rotorLength; z < this.rotorLength && isFree; z++) {
				if (!(world.getBlockState(new BlockPos(this.posX + x, this.posY + this.physicalHeight, this.posZ + z))
						.getBlock() instanceof BlockAir)) {
					isFree = false;
				}
			}
		}
		return isFree;
	}

	protected void setFlying() {
		this.isFlying = true;
		this.shouldFallDamage = true;
		this.prevInAirPos = this.getPositionVector();
		this.setNoGravity(true);
		for (int i = 0; i < this.seats.length; i++) {
			Entity e = this.getEntityInSeat(i);
			if (e != null) {
				e.setNoGravity(false);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public HudOverlay getHudOverlay() {
		return this.hud;
	}

	protected void addHudOverlayElement(Class element) {
		if (this.world.isRemote) {
			this.getHudOverlay().enableHudElement(element);
		}
	}

	@SideOnly(Side.CLIENT)
	protected void updateHudOverlay() {
		if (this.getControllingPassenger() == Minecraft.getMinecraft().player) {
			this.getHudOverlay().updateHudElement(HudElementArtificialHorizon.class, this.roll, this.pitch);
			this.getHudOverlay().updateHudElement(HudElementTachometer.class, this.getCurrentEngineSpeed(),
					this.getCurrentEngineSpeed() / (float) this.engineSpeed,
					this.computeRequiredEngineSpeedForHover() / (float) this.engineSpeed);
			this.getHudOverlay().updateHudElement(HudElementAltimeter.class, (float) (this.posY), this.getDistanceToGround());
			this.getHudOverlay().updateHudElement(HudElementStatsDisplay.class, this.simpleControle, this.lockOn);
		}
	}

	private boolean shouldAdjustEngineSpeedByHorizontalControls(float requiredSpeedForHovering) {
		return (this.forward() || this.backward() || this.left() || this.right())
				&& !(this.getCurrentEngineSpeed() > requiredSpeedForHovering && this.upward())
				&& !(this.getCurrentEngineSpeed() < requiredSpeedForHovering && this.downward());
	}

	private boolean shouldAdjustEngineSpeedWithoutHorizontalControls(float requiredSpeedForHovering) {
		return !(this.forward() || this.backward() || this.left() || this.right())
				&& !(this.getCurrentEngineSpeed() > requiredSpeedForHovering && this.upward())
				&& !(this.getCurrentEngineSpeed() < requiredSpeedForHovering && this.downward());
	}

	protected void handleKeyEnableAutoPilot(boolean shouldChange) {
		if (shouldChange) {
			if (this.simpleControle) {
				this.simpleControle = false;
			} else {
				this.simpleControle = true;
			}
		}
	}

	protected void handleKeyLock(boolean shouldChange) {
		if (shouldChange) {
			if (this.lockOn) {
				this.lockOn = false;
			} else {
				this.lockOn = true;
			}
		}
	}

	// Physics

	protected void changeCurrentEngineSpeed(float changeSpeed) {
		this.setCurrentEngineSpeed(this.getCurrentEngineSpeed() + changeSpeed);
	}

	protected void setCurrentEngineSpeed(float speed) {
		this.currentEngineSpeed = speed;
		if (this.currentEngineSpeed > this.engineSpeed) {
			this.currentEngineSpeed = this.engineSpeed;
		} else if (this.currentEngineSpeed < 0) {
			this.currentEngineSpeed = 0;
		}
	}

	// F
	protected float computeRotorSweptArea() {
		return (float) (Math.PI * Math.pow(this.rotorLength, 2));
	}

	// P
	protected float computeShaftPower() {
		// M * n * (Math.PI / 30)
		// M * this.getCurrentEngineSpeed() * (Math.PI / 30)
		return (float) (this.torque * this.getCurrentEngineSpeed() * (Math.PI / 30));
	}

	protected float computeRotorForce() {
		// (2 * p * F * (ζ * P) ^ 2) ^ (1 / 3)
		return (float) Math.pow(2 * 1.2f * this.computeRotorSweptArea() * Math.pow(this.qualityGrade * this.computeShaftPower(), 2),
				(1.0 / 3.0));
	}

	protected float computeVerticalForce() {
		return ((1.0f - Math.abs(this.pitch) / 90) * (1 - Math.abs(this.roll) / 90)) * this.computeRotorForce();
	}

	protected float computeHorizontalForceFrontBack() {
		if (this.pitch <= 90.0f && this.pitch >= -90.0f) {
			return ((this.pitch / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f))
					* (((1.0f - Math.abs(this.pitch) / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f)) * 5f) * this.computeRotorForce();
		} else {
			if (this.pitch < 0) {
				return ((1.0f + (this.pitch + 90.0f) / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f))
						* (((Math.abs(this.pitch + 90) / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f)) * 5f) * this.computeRotorForce();
			} else {
				return ((1.0f - (this.pitch - 90.0f) / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f))
						* (((Math.abs(this.pitch - 90) / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f)) * 5f) * this.computeRotorForce();
			}
		}
	}

	protected float computeHorizontalForceLeftRight() {
		return ((this.roll / 90.0f) * (1.0f - this.pitch / 90.0f)) * this.computeRotorForce();
	}

	protected float getCurrentEngineSpeed() {
		return this.currentEngineSpeed;
	}

	protected float computeRequiredEngineSpeedForHover() {
		float S = (9.81f * this.weight) / ((1 - Math.abs(this.pitch) / 90.0f) * (1.0f - Math.abs(this.roll) / 90.0f));
		return (float) (((1 / this.qualityGrade) * Math.sqrt(Math.pow(S, 3) / (2 * 1.2f * this.computeRotorSweptArea())))
				/ (this.torque * (Math.PI / 30)));
	}

	protected float computeThrottleUpDown() {
		return ((float) this.enginePower / 735.5f) * 0.00029481132f;
	}

	protected float computeTorque() {
		return (float) (enginePower / (engineSpeed * (Math.PI / 30)));
	}

}