package com.animania.common.handler;

import com.animania.common.ModSoundEvents;
import com.animania.common.events.BlockChangeHandler;
import com.animania.common.events.CapabilityLoadHandler;
import com.animania.common.events.CarryRenderer;
import com.animania.common.events.EggThrowHandler;
import com.animania.common.events.EntityEventHandler;
import com.animania.common.events.EventBeehiveDecorator;
import com.animania.common.events.EventMudDamageCanceller;
import com.animania.common.events.EventReplaceSpawnAnimals;
import com.animania.common.events.InteractHandler;
import com.animania.common.events.LoginEventHandler;
import com.animania.common.events.RemoveVanillaSpawns;

import net.minecraftforge.common.MinecraftForge;

public class EventsHandler
{

    public static void preInit() {
        ModSoundEvents.registerSounds();
        MinecraftForge.EVENT_BUS.register(new LoginEventHandler());
        MinecraftForge.EVENT_BUS.register(new InteractHandler());
        MinecraftForge.EVENT_BUS.register(new EggThrowHandler());
        MinecraftForge.EVENT_BUS.register(new RemoveVanillaSpawns());
        MinecraftForge.EVENT_BUS.register(new EventReplaceSpawnAnimals());
        MinecraftForge.EVENT_BUS.register(new EventMudDamageCanceller());
        MinecraftForge.EVENT_BUS.register(new EntityEventHandler());
        MinecraftForge.EVENT_BUS.register(new CapabilityLoadHandler());
        MinecraftForge.EVENT_BUS.register(new CarryRenderer());
        MinecraftForge.TERRAIN_GEN_BUS.register(new EventBeehiveDecorator());
        MinecraftForge.EVENT_BUS.register(new BlockHandler());
        MinecraftForge.EVENT_BUS.register(new BlockChangeHandler());

    }
}
