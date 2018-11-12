package com.animania.common.entities.generic.ai;


import com.animania.common.entities.interfaces.ISleeping;
import com.animania.common.entities.pigs.EntityAnimaniaPig;
import com.animania.common.handler.BlockHandler;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAILookIdle;

public class GenericAILookIdle extends EntityAILookIdle
{
	/** The entity that is looking idle. */
	private final EntityLiving entity;

	public GenericAILookIdle(EntityLiving entitylivingIn)
	{
		super(entitylivingIn);
		this.entity = entitylivingIn;
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute()
	{
		if ((this.entity instanceof ISleeping && ((ISleeping) this.entity).getSleeping()) ||
				(this.entity instanceof EntityAnimaniaPig && entity.world.getBlockState(entity.getPosition().down()).getBlock() == BlockHandler.blockMud))
			return false;

		return super.shouldExecute();
	}
}