package com.animania.common.entities.generic.ai;

import com.animania.common.entities.interfaces.ISleeping;
import com.animania.config.AnimaniaConfig;

import net.minecraft.entity.EntityCreature;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GenericAISearchShelter<T extends EntityCreature & ISleeping> extends GenericAISearchBlock
{
	public GenericAISearchShelter(T creature, double speedIn)
	{
		super(creature, speedIn, AnimaniaConfig.gameRules.aiBlockSearchRange, false, EnumFacing.UP);
	}

	@Override
	protected boolean shouldMoveTo(World world, BlockPos pos)
	{
		return !world.canSeeSky(pos.up());
	}

	@Override
	public boolean shouldExecute()
	{
		return world.isRainingAt(creature.getPosition()) && !((ISleeping)creature).getSleeping() ? super.shouldExecute() : false;
	}
}
