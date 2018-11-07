package com.animania.common.helper;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class WeakBlockState
{
	public final int dim, x, y, z;
	public final AxisAlignedBB aabb;
	public final Block block;
	public final Biome biome;

	public WeakBlockState(int dim, BlockPos pos)
	{
		this.dim = dim;
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.aabb = null;
		this.block = null;
		this.biome = null;
	}

	public WeakBlockState(World world, BlockPos pos, IBlockState state)
	{
		this.dim = world.provider.getDimension();
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.aabb = state.getCollisionBoundingBox(world, pos);
		this.block = state.getBlock();
		this.biome = world.getBiome(pos);
	}

	public static int getHash(int dim, int x, int y, int z)
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + dim;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	public static int getHash(int dim, BlockPos pos)
	{
		return WeakBlockState.getHash(dim, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public int hashCode() {
		return WeakBlockState.getHash(this.dim, this.x, this.y, this.z);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof WeakBlockState))
			return false;
		WeakBlockState other = (WeakBlockState)obj;
		return this.dim == other.dim && this.x == other.x && this.y == other.y && this.z == other.z;
	}
}
