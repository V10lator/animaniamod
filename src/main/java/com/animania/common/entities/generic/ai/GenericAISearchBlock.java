package com.animania.common.entities.generic.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class GenericAISearchBlock extends EntityAIBase
{
	protected final EntityCreature creature;
	protected final double movementSpeed;
	protected BlockPos destinationBlock = NO_POS;
	private boolean isAtDestination;
	protected final int searchRange;
	protected World world;	
	protected List<EnumFacing> destinationOffset;
	protected BlockPos seekingBlockPos = NO_POS;

	protected BlockPos oldBlockPos = NO_POS;
	private boolean hasSecondary;
	private int walkTries = 0;
	private boolean isDone = false;
	private Set<BlockPos> nonValidPositions = new HashSet<BlockPos>();
	
	public static final BlockPos NO_POS = new BlockPos(-1, -1, -1);

	public GenericAISearchBlock(EntityCreature creature, double speedIn, int range, boolean hasSecondary, EnumFacing... destinationOffset)
	{
		this.creature = creature;
		this.movementSpeed = speedIn;
		this.searchRange = range;
		this.destinationOffset = new ArrayList<EnumFacing>();
		for (EnumFacing f : destinationOffset)
			this.destinationOffset.add(f);
		this.world = creature.world;
		this.hasSecondary = hasSecondary;
		// this.setMutexBits(5);
	}
	
	public GenericAISearchBlock(EntityCreature creature, double speedIn, int range, EnumFacing... destinationOffset)
	{
		this(creature, speedIn, range, false, destinationOffset);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute()
	{
		if (this.seekingBlockPos == NO_POS)
			return this.searchForDestination();
		
		return false;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean shouldContinueExecuting()
	{
		return destinationBlock != NO_POS && seekingBlockPos != NO_POS && !isDone && (this.shouldMoveTo(this.creature.world, this.seekingBlockPos) || (this.hasSecondary ? this.shouldMoveToSecondary(world, seekingBlockPos) : false));
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	public void startExecuting()
	{
		this.creature.getNavigator().tryMoveToXYZ((double) ((float) this.destinationBlock.getX()) + 0.5D, (double) (this.destinationBlock.getY()), (double) ((float) this.destinationBlock.getZ()) + 0.5D, this.movementSpeed);
		this.walkTries = 0;
	}

	@Override
	public void resetTask()
	{
		this.isAtDestination = false;
		this.destinationBlock = NO_POS;
		this.seekingBlockPos = NO_POS;
		this.walkTries = 0;
		this.isDone = false;
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void updateTask()
	{
		if (!shouldContinueExecuting())
			this.resetTask();
	
		if (!this.destinationBlock.equals(NO_POS))
		{
			double distance = this.creature.getDistanceSqToCenter(this.destinationBlock);

			if (distance > 1.95D)
			{
				this.isAtDestination = false;
				this.walkTries++;

				boolean isStandingStill = this.creature.prevPosX == this.creature.posX && this.creature.prevPosY == this.creature.posY && this.creature.prevPosZ == this.creature.posZ;
				
				if (this.walkTries % 40 == 0 || isStandingStill)
				{
					this.creature.getNavigator().tryMoveToXYZ((double) ((float) this.destinationBlock.getX()) + 0.5D, (double) (this.destinationBlock.getY()), (double) ((float) this.destinationBlock.getZ()) + 0.5D, this.movementSpeed);
					this.creature.getLookHelper().setLookPosition((double) this.seekingBlockPos.getX() + 0.5D, (double) (this.seekingBlockPos.getY()), (double) this.seekingBlockPos.getZ() + 0.5D, 10.0F, (float) this.creature.getVerticalFaceSpeed());
				}					
				
				if(isStandingStill && this.walkTries > 100) 
				{
					this.nonValidPositions.add(seekingBlockPos);
					this.resetTask();
					this.searchForDestination();
				}
			}
			else
			{
				this.isAtDestination = true;
				this.walkTries = 0;
			}
			
			if(this.isAtDestination)
			{
				this.nonValidPositions.clear();
				this.isDone = true;
			}
		}
	}

	protected boolean isAtDestination()
	{
		return this.isAtDestination;
	}
	
	public void onArriveAtDestination()
	{
		this.nonValidPositions.clear();
	}

	@Override
	public boolean isInterruptible()
	{
		return true;
	}

	/**
	 * Searches and sets new destination block and returns true if a suitable
	 * block (specified in
	 * {@link net.minecraft.entity.ai.EntityAIMoveToBlock#shouldMoveTo(World, BlockPos)
	 * EntityAIMoveToBlock#shouldMoveTo(World, BlockPos)}) can be found.
	 */
	protected boolean searchForDestination()
	{
		BlockPos blockpos = new BlockPos(this.creature);

		if (blockpos.equals(oldBlockPos))
			return false;
		oldBlockPos = blockpos;

		BlockPos secondaryDest = null;
		BlockPos secondarySeek = null;
		int ySearchRange = searchRange / 2;
		if(ySearchRange < 1)
			ySearchRange = 1;

		for (int range = 0; range < this.searchRange; ++range)
		{
			for (int y = 0; y <= ySearchRange; y = y > 0 ? -y : 1 - y)
			{
				for (int x = 0; x <= range; x = x > 0 ? -x : 1 -x)
				{
					for (int z = x < range && x > -range ? range : 0; z <= range; z = z > 0 ? -z : 1 - z)
					{
						BlockPos blockpos1 = blockpos.add(x, y - 1, z);

						if (this.shouldMoveTo(this.creature.world, blockpos1) && !this.nonValidPositions.contains(blockpos1))
						{
							Collections.shuffle(destinationOffset);

							for (EnumFacing facing : destinationOffset)
							{
								AxisAlignedBB aabb = world.getBlockState(blockpos1).getCollisionBoundingBox(world, blockpos1);

								BlockPos offsetPos = aabb == Block.NULL_AABB ? blockpos1 : blockpos1.offset(facing);

								if (this.creature.getNavigator().getPathToXYZ(offsetPos.getX() + 0.5, offsetPos.getY(), offsetPos.getZ() + 0.5) != null)
								{
									this.destinationBlock = offsetPos;
									this.seekingBlockPos = blockpos1;
									return true;
								}
							}
							
							if(destinationOffset.isEmpty())
							{
								if (this.creature.getNavigator().getPathToXYZ(blockpos1.getX() + 0.5, blockpos1.getY(), blockpos1.getZ() + 0.5) != null)
								{
									this.destinationBlock = blockpos1;
									this.seekingBlockPos = blockpos1;
									return true;
								}
							}
						}
						else if (this.hasSecondary && secondarySeek == null && this.shouldMoveToSecondary(this.creature.world, blockpos1) && !this.nonValidPositions.contains(blockpos1))
						{
							Collections.shuffle(destinationOffset);

							for (EnumFacing facing : destinationOffset)
							{
								AxisAlignedBB aabb = world.getBlockState(blockpos1).getCollisionBoundingBox(world, blockpos1);

								BlockPos offsetPos = aabb == Block.NULL_AABB ? blockpos1 : blockpos1.offset(facing);

								if (this.creature.getNavigator().getPathToXYZ(offsetPos.getX() + 0.5, offsetPos.getY(), offsetPos.getZ() + 0.5) != null)
								{
									secondaryDest = offsetPos;
									secondarySeek = blockpos1;
								}
							}
							
							if(destinationOffset.isEmpty())
							{
								if (this.creature.getNavigator().getPathToXYZ(blockpos1.getX() + 0.5, blockpos1.getY(), blockpos1.getZ() + 0.5) != null)
								{
									secondaryDest = blockpos1;
									secondarySeek = blockpos1;
								}
							}
						}
					}
				}
			}
		}

		if(secondarySeek != null)
		{
			this.destinationBlock = secondaryDest;
 			this.seekingBlockPos = secondarySeek;
 			return true;
		}

		return false;
	}

	/**
	 * Return true to set given position as destination
	 */
	protected abstract boolean shouldMoveTo(World worldIn, BlockPos pos);
	
	protected boolean shouldMoveToSecondary(World worldIn, BlockPos pos)
	{
		return false;
	}

}