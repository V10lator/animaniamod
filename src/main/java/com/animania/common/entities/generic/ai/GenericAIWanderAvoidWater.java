package com.animania.common.entities.generic.ai;


import com.animania.common.entities.interfaces.ISleeping;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.util.math.BlockPos;

public class GenericAIWanderAvoidWater extends EntityAIWanderAvoidWater
{
	public GenericAIWanderAvoidWater(EntityCreature p_i47301_1_, double p_i47301_2_)
	{
		super(p_i47301_1_, p_i47301_2_, 0.001F);
	}

	public boolean shouldExecute()
	{
		if(((ISleeping) entity).getSleeping())
    		return false;

		boolean foundTarget = super.shouldExecute();
		if(foundTarget && entity.world.isRaining() && entity.world.canSeeSky(new BlockPos(this.x, this.y, this.z)))
		{
			foundTarget = false;
			this.mustUpdate = true;
		}

		return foundTarget;
	}
}