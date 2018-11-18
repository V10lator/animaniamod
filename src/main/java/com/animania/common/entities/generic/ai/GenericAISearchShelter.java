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
		return !world.canSeeSky(pos) &&
				!world.canSeeSky(pos.north()) &&
				!world.canSeeSky(pos.east()) &&
				!world.canSeeSky(pos.south()) &&
				!world.canSeeSky(pos.west());
	}

	@Override
	public boolean shouldExecute()
	{
		if(world.isRainingAt(creature.getPosition()))
		{
			ISleeping sleeper = (ISleeping)creature;
			if(sleeper.getSleeping())
			{
				if(creature.getRNG().nextInt(10) < 8)
					return false;
				sleeper.setSleeping(false);
				sleeper.setSleepingPos(NO_POS);
			}
			return super.shouldExecute();
		}
		return false;
	}
}
