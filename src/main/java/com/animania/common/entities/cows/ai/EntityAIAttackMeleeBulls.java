package com.animania.common.entities.cows.ai;

import com.animania.common.entities.cows.EntityAnimaniaCow;
import com.animania.common.entities.cows.EntityBullBase;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntityAIAttackMeleeBulls extends EntityAIBase
{
	World                    worldObj;
	protected EntityCreature attacker;
	protected int            attackTick;
	double                   speedTowardsTarget;
	boolean                  longMemory;
	Path                     entityPathEntity;
	private int              delayCounter;
	private double           targetX;
	private double           targetY;
	private double           targetZ;
	protected final int      attackInterval           = 20;
	private int              failedPathFindingPenalty = 0;
	private boolean          canPenalize              = false;

	public EntityAIAttackMeleeBulls(EntityAnimaniaCow creature, double speedIn, boolean useLongMemory) {
		this.attacker = creature;
		this.worldObj = creature.world;
		this.speedTowardsTarget = speedIn;
		this.longMemory = useLongMemory;
		this.setMutexBits(3);
	}

	@Override
	public boolean shouldExecute() {
		EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();

		if (entitylivingbase == null)
			return false;
		else if (!entitylivingbase.isEntityAlive() || entitylivingbase instanceof EntitySkeleton)
			return false;
		else {

			if (this.attacker instanceof EntityBullBase) {

				EntityBullBase eb = (EntityBullBase) this.attacker;
				if (eb.getSleeping()) {
					eb.setSleeping(false);
				} else {
					eb.setFighting(true);
				}
			}
			this.entityPathEntity = this.attacker.getNavigator().getPathToEntityLiving(entitylivingbase);
			return this.entityPathEntity != null;

		}
	}

	@Override
	public boolean shouldContinueExecuting() {
		EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();
		return entitylivingbase == null ? false
				: !entitylivingbase.isEntityAlive() ? false
						: !this.longMemory ? !this.attacker.getNavigator().noPath()
								: !this.attacker.isWithinHomeDistanceFromPosition(new BlockPos(entitylivingbase)) ? false
										: !(entitylivingbase instanceof EntityPlayer) || !((EntityPlayer) entitylivingbase).isSpectator()
										&& !((EntityPlayer) entitylivingbase).isCreative();
	}

	@Override
	public void startExecuting() {
		this.attacker.getNavigator().setPath(this.entityPathEntity, this.speedTowardsTarget);
		this.delayCounter = 0;
	}

	@Override
	public void resetTask() {
		EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();

		if (entitylivingbase instanceof EntityPlayer
				&& (((EntityPlayer) entitylivingbase).isSpectator() || ((EntityPlayer) entitylivingbase).isCreative()))
			this.attacker.setAttackTarget((EntityLivingBase) null);

		this.attacker.getNavigator().clearPath();
		if (this.attacker instanceof EntityBullBase) {
			EntityBullBase eb = (EntityBullBase) this.attacker;
			eb.setFighting(false);
		}
	}

	@Override
	public void updateTask() {
		EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();

		if (entitylivingbase != null) {

			this.attacker.getLookHelper().setLookPositionWithEntity(entitylivingbase, 20.0F, 20.0F);
			double d0 = this.attacker.getDistanceSq(entitylivingbase.posX, entitylivingbase.getEntityBoundingBox().minY, entitylivingbase.posZ);
			--this.delayCounter;

			if ((this.longMemory || this.attacker.getEntitySenses().canSee(entitylivingbase)) && this.delayCounter <= 0
					&& (this.targetX == 0.0D && this.targetY == 0.0D && this.targetZ == 0.0D
					|| entitylivingbase.getDistanceSq(this.targetX, this.targetY, this.targetZ) >= 1.0D
					|| this.attacker.getRNG().nextFloat() < 0.05F)) {
				this.targetX = entitylivingbase.posX;
				this.targetY = entitylivingbase.getEntityBoundingBox().minY;
				this.targetZ = entitylivingbase.posZ;
				this.delayCounter = 4 + this.attacker.getRNG().nextInt(7);

				if (this.canPenalize) {
					this.delayCounter += this.failedPathFindingPenalty;
					if (this.attacker.getNavigator().getPath() != null) {
						net.minecraft.pathfinding.PathPoint finalPathPoint = this.attacker.getNavigator().getPath().getFinalPathPoint();
						if (finalPathPoint != null
								&& entitylivingbase.getDistanceSq(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
							this.failedPathFindingPenalty = 0;
						else
							this.failedPathFindingPenalty += 10;
					}
					else
						this.failedPathFindingPenalty += 10;
				}

				if (d0 > 1024.0D)
					this.delayCounter += 10;
				else if (d0 > 256.0D)
					this.delayCounter += 5;

				if (!this.attacker.getNavigator().tryMoveToEntityLiving(entitylivingbase, this.speedTowardsTarget))
					this.delayCounter += 15;
			}

			this.attackTick = Math.max(this.attackTick - 1, 0);
			this.func_190102_a(entitylivingbase, d0);
		}

	}

	protected void func_190102_a(EntityLivingBase p_190102_1_, double p_190102_2_) {
		double d0 = this.getAttackReachSqr(p_190102_1_);

		if (p_190102_2_ <= d0 && this.attackTick <= 0) {
			this.attackTick = 20;
			this.attacker.swingArm(EnumHand.MAIN_HAND);
			this.attacker.attackEntityAsMob(p_190102_1_);
		}
	}

	protected double getAttackReachSqr(EntityLivingBase attackTarget) {
		return this.attacker.width * 2.0F * this.attacker.width * 2.0F + attackTarget.width;
	}
}