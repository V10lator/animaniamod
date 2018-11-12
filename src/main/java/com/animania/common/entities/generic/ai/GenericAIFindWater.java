package com.animania.common.entities.generic.ai;

import javax.annotation.Nullable;

import com.animania.common.entities.interfaces.IFoodEating;
import com.animania.common.entities.interfaces.ISleeping;
import com.animania.common.handler.BlockHandler;
import com.animania.common.helper.WeakBlockState;
import com.animania.common.tileentities.TileEntityTrough;
import com.animania.config.AnimaniaConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public class GenericAIFindWater<T extends EntityCreature & IFoodEating> extends GenericAISearchBlock
{

	private final T entity;
	private final double speed;
	private int waterFindTimer;
	private boolean isRunning;
	private EntityAIBase eatAI;
	private Class parentClass;
	private boolean halfAmount;

	public GenericAIFindWater(T entity, double speedIn, @Nullable EntityAIBase eatAI, Class parentClass, boolean halfAmount)
	{
		super(entity, speedIn, 16, EnumFacing.UP);
		this.entity = entity;
		this.speed = speedIn;
		this.setMutexBits(3);
		this.waterFindTimer = 0;
		this.eatAI = eatAI;
		this.parentClass = parentClass;
		this.halfAmount = halfAmount;
	}

	public GenericAIFindWater(T entity, double speedIn, @Nullable EntityAIBase eatAI, Class parentClass)
	{
		this(entity, speedIn, eatAI, parentClass, false);
	}

	@Override
	public boolean shouldExecute()
	{
		waterFindTimer++;

		if (this.waterFindTimer <= AnimaniaConfig.gameRules.ticksBetweenAIFirings)
		{
			return false;
		}
		else if (waterFindTimer > AnimaniaConfig.gameRules.ticksBetweenAIFirings)
		{

			if (entity.getWatered())
			{
				this.waterFindTimer = 0;
				return false;
			}

			if (entity instanceof ISleeping)
			{
				if (((ISleeping) entity).getSleeping())
				{
					this.waterFindTimer = 0;
					return false;
				}
			}

			if (entity.getRNG().nextInt(3) == 0)
				return super.shouldExecute();
		}

		return false;

	}
	
	@Override
	public boolean shouldContinueExecuting()
	{
		return super.shouldContinueExecuting() && !entity.getWatered();
	}
	
	@Override
	public void updateTask()
	{
		super.updateTask();
		
		if (this.isAtDestination())
		{
			this.creature.getLookHelper().setLookPosition((double) this.seekingBlockPos.getX() + 0.5D, (double) (this.seekingBlockPos.getY()), (double) this.seekingBlockPos.getZ() + 0.5D, 10.0F, (float) this.creature.getVerticalFaceSpeed());

			IBlockState state = world.getBlockState(seekingBlockPos);
			Block block = state.getBlock();

			if (block == BlockHandler.blockTrough)
			{
				TileEntityTrough trough = (TileEntityTrough) world.getTileEntity(seekingBlockPos);
				if (trough != null)
				{
					if(trough.canConsume(new FluidStack(FluidRegistry.WATER, this.halfAmount ? 50 : 100), null))
					{
						trough.consumeLiquid(halfAmount ? 50 : 100);
						
						entity.world.updateComparatorOutputLevel(seekingBlockPos, block);

						if (eatAI != null)
							eatAI.startExecuting();
						entity.setWatered(true);

						this.waterFindTimer = 0;
					}	
				}
			}
			
			if (block == Blocks.WATER)
			{
				if (eatAI != null)
					eatAI.startExecuting();
				entity.setWatered(true);

				if (AnimaniaConfig.gameRules.waterRemovedAfterDrinking)
				{
					if (!halfAmount)
						this.entity.world.setBlockToAir(seekingBlockPos);
				}

				this.waterFindTimer = 0;
			}
			
		}
	}
	

	@Override
	protected boolean shouldMoveTo(World worldIn, BlockPos pos)
	{
		WeakBlockState ws = GenericAISearchBlock.getWeakState(worldIn, pos);
		
		if(ws.block == BlockHandler.blockTrough)
		{
			TileEntityTrough trough = (TileEntityTrough) world.getTileEntity(pos);
			if (trough != null)
			{
				if(trough.canConsume(new FluidStack(FluidRegistry.WATER, this.halfAmount ? 50 : 100), null))
					return true;
			}
		}
		
		return ws.block == Blocks.WATER && !BiomeDictionary.hasType(ws.biome, Type.OCEAN) && !BiomeDictionary.hasType(ws.biome, Type.BEACH);
	}

}
