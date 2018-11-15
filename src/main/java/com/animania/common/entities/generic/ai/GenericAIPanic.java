package com.animania.common.entities.generic.ai;

import com.animania.common.entities.cows.EntityBullBase;
import com.animania.common.entities.cows.EntityCowBase;
import com.animania.common.entities.interfaces.ISleeping;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIPanic;

public class GenericAIPanic<T extends EntityCreature> extends EntityAIPanic
{
	public GenericAIPanic(T creature, double speedIn)
	{
		super(creature, speedIn);
	}


	public boolean shouldExecute()
	{

		if (creature instanceof EntityCowBase || creature instanceof EntityBullBase) {
			return false;
		}

		if (creature.isBurning())
		{
			ISleeping s = (ISleeping)creature;
			if(s.getSleeping())
				s.setSleeping(false);
		}

		return super.shouldExecute();
	}
}