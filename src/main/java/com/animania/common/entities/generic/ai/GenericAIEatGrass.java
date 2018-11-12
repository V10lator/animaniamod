package com.animania.common.entities.generic.ai;

import com.animania.common.entities.interfaces.ISleeping;
import com.animania.config.AnimaniaConfig;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIEatGrass;

public class GenericAIEatGrass extends EntityAIEatGrass
{
	private int timer = 0;
	private final EntityCreature entity;

	public GenericAIEatGrass(EntityCreature grassEaterEntityIn)
	{
		super(grassEaterEntityIn);
		this.entity = grassEaterEntityIn;
	}

	@Override
	public boolean shouldExecute()
	{
		if (++timer <= AnimaniaConfig.gameRules.ticksBetweenAIFirings)
			return false;
		timer = 0;
		if (entity instanceof ISleeping && ((ISleeping) entity).getSleeping())
			return false;
		return super.shouldExecute();
	}
}
