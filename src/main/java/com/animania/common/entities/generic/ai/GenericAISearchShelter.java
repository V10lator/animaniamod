package com.animania.common.entities.generic.ai;

import com.animania.common.entities.interfaces.ISleeping;
import com.animania.config.AnimaniaConfig;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GenericAISearchShelter<T extends EntityCreature & ISleeping> extends GenericAISearchBlock
{
	private T creature;
	private int delay;

	public GenericAISearchShelter(T creature, double speedIn)
	{
		super(creature, speedIn, AnimaniaConfig.gameRules.aiBlockSearchRange, false, EnumFacing.UP);
		this.creature = creature;
		this.delay = 0;
	}

	@Override
	protected boolean shouldMoveTo(World world, BlockPos pos)
	{
		if(!world.canSeeSky(pos.up()))
		{
			Material mat = world.getBlockState(pos).getMaterial();
			return mat != Material.WATER && mat != Material.LAVA;
		}
		return false;
	}

	@Override
	public boolean shouldExecute()
	{
		if (++delay <= AnimaniaConfig.gameRules.ticksBetweenAIFirings)
			return false;

		if (creature.getSleeping() || !world.isRainingAt(creature.getPosition()))
		{
			delay = 0;
			return false;
		}

		if (creature.getRNG().nextInt(3) != 0)
			return false;

		delay = 0;
		return super.shouldExecute();
	}
}
