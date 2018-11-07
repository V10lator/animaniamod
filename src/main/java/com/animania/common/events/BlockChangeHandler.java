package com.animania.common.events;

import com.animania.common.entities.generic.ai.GenericAISearchBlock;
import com.animania.common.helper.WeakBlockState;

import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockChangeHandler
{
	private void internalOnBlockChange(BlockEvent event)
	{
		GenericAISearchBlock.stateCache.remove(WeakBlockState.getHash(event.getWorld().provider.getDimension(), event.getPos()));
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
	public void onBlockChange(BlockEvent.PlaceEvent event)
	{
		internalOnBlockChange(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
	public void onBlockChange(BlockEvent.BreakEvent event)
	{
		internalOnBlockChange(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockChange(BlockEvent.CreateFluidSourceEvent event)
	{
		if(event.getResult() == Result.ALLOW)
			internalOnBlockChange(event);
	}

	@SubscribeEvent
	public void onBlockChange(BlockEvent.CropGrowEvent.Post event)
	{
		internalOnBlockChange(event);
	}
	
	@SubscribeEvent
	public void onChunkUnload(ChunkEvent.Unload event)
	{
		int a = event.getChunk().x << 4;
		int b = event.getChunk().z << 4;
		World world = event.getWorld();
		int dim = world.provider.getDimension();

		for(int x = a; x < a + 16; x++)
			for(int y = 0; y < world.getHeight(); y++)
				for(int z = b; z < b + 16; z++)
					GenericAISearchBlock.stateCache.remove(WeakBlockState.getHash(dim, x, y, z));
	}
}
