package com.animania.common.entities.cows;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.animania.Animania;
import com.animania.common.entities.AnimalContainer;
import com.animania.common.entities.EntityGender;
import com.animania.common.entities.generic.ai.GenericAIAvoidWater;
import com.animania.common.entities.generic.ai.GenericAIEatGrass;
import com.animania.common.entities.generic.ai.GenericAIFindFood;
import com.animania.common.entities.generic.ai.GenericAIFindSaltLick;
import com.animania.common.entities.generic.ai.GenericAIFindWater;
import com.animania.common.entities.generic.ai.GenericAILookIdle;
import com.animania.common.entities.generic.ai.GenericAIPanic;
import com.animania.common.entities.generic.ai.GenericAISleep;
import com.animania.common.entities.generic.ai.GenericAITempt;
import com.animania.common.entities.generic.ai.GenericAIWanderAvoidWater;
import com.animania.common.entities.generic.ai.GenericAIWatchClosest;
import com.animania.common.entities.interfaces.IAnimaniaAnimalBase;
import com.animania.common.helper.AnimaniaHelper;
import com.animania.common.items.ItemEntityEgg;
import com.animania.config.AnimaniaConfig;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityAnimaniaCow extends EntityCow implements IAnimaniaAnimalBase
{
	public static final Set<Item> TEMPTATION_ITEMS = Sets.newHashSet(AnimaniaHelper.getItemArray(AnimaniaConfig.careAndFeeding.cowFood));
	protected static final DataParameter<Integer> AGE = EntityDataManager.<Integer>createKey(EntityAnimaniaCow.class, DataSerializers.VARINT);
	protected static final DataParameter<Boolean> WATERED = EntityDataManager.<Boolean>createKey(EntityAnimaniaCow.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> FED = EntityDataManager.<Boolean>createKey(EntityAnimaniaCow.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> HANDFED = EntityDataManager.<Boolean>createKey(EntityAnimaniaCow.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> SLEEPING = EntityDataManager.<Boolean>createKey(EntityAnimaniaCow.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Float> SLEEPTIMER = EntityDataManager.<Float>createKey(EntityAnimaniaCow.class, DataSerializers.FLOAT);
	protected static final DataParameter<Optional<UUID>> MATE_UNIQUE_ID = EntityDataManager.<Optional<UUID>>createKey(EntityAnimaniaCow.class, DataSerializers.OPTIONAL_UNIQUE_ID);


	protected int happyTimer;
	public int blinkTimer;
	public int eatTimer;
	protected int fedTimer;
	protected int wateredTimer;
	protected int damageTimer;
	public GenericAIEatGrass entityAIEatGrass;
	public CowType cowType;
	protected boolean mateable = false;
	protected EntityGender gender;

	public EntityAnimaniaCow(World worldIn)
	{
		super(worldIn);
		this.tasks.taskEntries.clear();
		this.entityAIEatGrass = new GenericAIEatGrass(this);
		this.tasks.addTask(1, new GenericAIPanic(this, 2.0D));
		if (!AnimaniaConfig.gameRules.ambianceMode) {
			this.tasks.addTask(2, new GenericAIFindWater<EntityAnimaniaCow>(this, 1.0D, entityAIEatGrass, EntityAnimaniaCow.class));
			this.tasks.addTask(3, new GenericAIFindFood<EntityAnimaniaCow>(this, 1.0, entityAIEatGrass, true));
		}
		this.tasks.addTask(4, new GenericAIWanderAvoidWater(this, 1.0D));
		this.tasks.addTask(5, new EntityAISwimming(this));
		this.tasks.addTask(7, new GenericAITempt(this, 1.25D, false, EntityAnimaniaCow.TEMPTATION_ITEMS));
		this.tasks.addTask(6, new GenericAITempt(this, 1.25D, Item.getItemFromBlock(Blocks.YELLOW_FLOWER), false));
		this.tasks.addTask(6, new GenericAITempt(this, 1.25D, Item.getItemFromBlock(Blocks.RED_FLOWER), false));
		this.tasks.addTask(8, this.entityAIEatGrass);
		if (AnimaniaConfig.gameRules.animalsSleep) {
			this.tasks.addTask(9, new GenericAISleep<EntityAnimaniaCow>(this, 0.8, AnimaniaHelper.getBlock(AnimaniaConfig.careAndFeeding.cowBed), AnimaniaHelper.getBlock(AnimaniaConfig.careAndFeeding.cowBed2), EntityAnimaniaCow.class));
		}
		this.tasks.addTask(9, new GenericAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(10, new GenericAILookIdle(this));
		this.tasks.addTask(11, new GenericAIFindSaltLick(this, 1.0, entityAIEatGrass));
		this.tasks.addTask(13, new GenericAIAvoidWater(this));
		this.targetTasks.addTask(0, new EntityAIHurtByTarget(this, false, new Class[0]));
		if (AnimaniaConfig.gameRules.animalsCanAttackOthers) {
			this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, EntityPlayer.class));
		}
		this.fedTimer = AnimaniaConfig.careAndFeeding.feedTimer + this.rand.nextInt(100);
		this.wateredTimer = AnimaniaConfig.careAndFeeding.waterTimer + this.rand.nextInt(100);
		this.happyTimer = 60;
		this.blinkTimer = 100 + this.rand.nextInt(100);
		this.enablePersistence();
	}

	@Override
	protected boolean canDespawn()
	{
		return false;
	}

	@Override
	public void setPosition(double x, double y, double z)
	{
		super.setPosition(x, y, z);
	}


	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataManager.register(EntityAnimaniaCow.FED, Boolean.valueOf(true));
		this.dataManager.register(EntityAnimaniaCow.HANDFED, Boolean.valueOf(false));
		this.dataManager.register(EntityAnimaniaCow.WATERED, Boolean.valueOf(true));
		this.dataManager.register(EntityAnimaniaCow.SLEEPING, Boolean.valueOf(false));
		this.dataManager.register(EntityAnimaniaCow.SLEEPTIMER, Float.valueOf(0.0F));
		this.dataManager.register(EntityAnimaniaCow.MATE_UNIQUE_ID, Optional.<UUID>absent());
		this.dataManager.register(EntityAnimaniaCow.AGE, Integer.valueOf(0));

	}

	@Nullable
	public UUID getMateUniqueId()
	{
		if (mateable)
		{
			try
			{
				UUID id = (UUID) ((Optional) this.dataManager.get(EntityAnimaniaCow.MATE_UNIQUE_ID)).orNull();
				return id;
			}
			catch (Exception e)
			{
				return null;
			}
		}
		return null;
	}

	public void setMateUniqueId(@Nullable UUID uniqueId)
	{
		this.dataManager.set(EntityAnimaniaCow.MATE_UNIQUE_ID, Optional.fromNullable(uniqueId));
	}

	@Override
	protected void consumeItemFromStack(EntityPlayer player, ItemStack stack)
	{

		if (this instanceof EntityBullBase) {
			EntityBullBase ebb = (EntityBullBase) this;
			if (ebb.getFighting()) {
				return;
			}
		}

		if (!this.getSleeping()) {

			this.setFed(true);
			this.setHandFed(true);
			this.entityAIEatGrass.startExecuting();
			this.eatTimer = 80;

			if (!player.capabilities.isCreativeMode)
				stack.setCount(stack.getCount() - 1);
		}
	}

	public boolean getFed()
	{
		try {
			return (this.getBoolFromDataManager(FED));
		}
		catch (Exception e) {
			return false;
		}
	}

	public void setFed(boolean fed)
	{
		if (fed)
		{
			this.dataManager.set(EntityAnimaniaCow.FED, Boolean.valueOf(true));
			this.fedTimer = AnimaniaConfig.careAndFeeding.feedTimer + this.rand.nextInt(100);
			this.setHealth(this.getHealth() + 1.0F);
		}
		else
			this.dataManager.set(EntityAnimaniaCow.FED, Boolean.valueOf(false));
	}

	public boolean getHandFed()
	{
		try {
			return (this.getBoolFromDataManager(HANDFED));
		}
		catch (Exception e) {
			return false;
		}
	}

	public void setHandFed(boolean handfed)
	{
		this.dataManager.set(EntityAnimaniaCow.HANDFED, Boolean.valueOf(handfed));
	}

	public boolean getWatered()
	{
		try {
			return (this.getBoolFromDataManager(WATERED));
		}
		catch (Exception e) {
			return false;
		}
	}

	public void setWatered(boolean watered)
	{
		if (watered)
		{
			this.dataManager.set(EntityAnimaniaCow.WATERED, Boolean.valueOf(true));
			this.wateredTimer = AnimaniaConfig.careAndFeeding.waterTimer + this.rand.nextInt(100);
		}
		else
			this.dataManager.set(EntityAnimaniaCow.WATERED, Boolean.valueOf(false));
	}

	public boolean getSleeping()
	{
		try {
			return (this.getBoolFromDataManager(SLEEPING));
		}
		catch (Exception e) {
			return false;
		}
	}

	public void setSleeping(boolean flag)
	{
		if (flag)
		{
			this.dataManager.set(EntityAnimaniaCow.SLEEPING, Boolean.valueOf(true));
		}
		else
		{
			this.dataManager.set(EntityAnimaniaCow.SLEEPING, Boolean.valueOf(false));
		}
	}

	public Float getSleepTimer()
	{
		try
		{
			return (this.getFloatFromDataManager(SLEEPTIMER));
		}
		catch (Exception e)
		{
			return 0F;
		}
	}

	public void setSleepTimer(Float timer)
	{
		this.dataManager.set(EntityAnimaniaCow.SLEEPTIMER, Float.valueOf(timer));
	}


	public int getAge()
	{
		try {
			return (this.getIntFromDataManager(AGE));
		}
		catch (Exception e) {
			return 0;
		}
	}

	public void setAge(int age)
	{
		this.dataManager.set(EntityAnimaniaCow.AGE, Integer.valueOf(age));
	}

	@Override
	protected void updateAITasks()
	{
		this.eatTimer = this.entityAIEatGrass.getEatingGrassTimer();
		super.updateAITasks();
	}

	@Override
	public void playLivingSound()
	{
		SoundEvent soundevent = this.getAmbientSound();

		if (soundevent != null && !this.getSleeping())
			this.playSound(soundevent, this.getSoundVolume(), this.getSoundPitch() - .2F);
	}

	@Override
	protected float getSoundVolume()
	{
		return 0.4F;
	}
	
	@Override
	protected ResourceLocation getLootTable()
	{
		return this instanceof EntityCalfBase ? null : this.cowType.isPrime ? new ResourceLocation(Animania.MODID, "cow_prime") : new ResourceLocation(Animania.MODID, "cow_regular");
	}

	@Override
	public void onLivingUpdate()
	{
		if (this.world.isRemote)
			this.eatTimer = Math.max(0, this.eatTimer - 1);

		if (this.getLeashed() && this.getSleeping())
			this.setSleeping(false);

		if (this.getAge() == 0) {
			this.setAge(1);
		}

		if (this.fedTimer > -1 && !AnimaniaConfig.gameRules.ambianceMode)
		{
			this.fedTimer--;

			if (this.fedTimer == 0)
				this.setFed(false);
		}

		if (this.wateredTimer > -1)
		{
			this.wateredTimer--;

			if (this.wateredTimer == 0 && !AnimaniaConfig.gameRules.ambianceMode)
				this.setWatered(false);
		}


		boolean fed = this.getFed();
		boolean watered = this.getWatered();

		if (!fed && !watered)
		{
			this.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 2, 1, false, false));
			if (AnimaniaConfig.gameRules.animalsStarve)
			{
				if (this.damageTimer >= AnimaniaConfig.careAndFeeding.starvationTimer)
				{
					this.attackEntityFrom(DamageSource.STARVE, 4f);
					this.damageTimer = 0;
				}
				this.damageTimer++;
			}

		}
		else if (!fed || !watered)
			this.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 2, 0, false, false));

		if (this.getCustomNameTag().toLowerCase().trim().equals("purp") && (this instanceof EntityCowFriesian || this instanceof EntityBullFriesian || this instanceof EntityCowHolstein || this instanceof EntityBullHolstein || this instanceof EntityCalfFriesian || this instanceof EntityCalfHolstein)) {
			this.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 4, 2, false, false));
			if (!this.isWet() && !this.isInWater())
				this.setOnFireFromLava();
		}


		if (this.happyTimer > -1)
		{
			this.happyTimer--;
			if (this.happyTimer == 0)
			{
				this.happyTimer = 60;

				if (!this.getFed() && !this.getWatered() && !this.getSleeping() && AnimaniaConfig.gameRules.showUnhappyParticles && this.getHandFed())
				{
					double d = this.rand.nextGaussian() * 0.001D;
					double d1 = this.rand.nextGaussian() * 0.001D;
					double d2 = this.rand.nextGaussian() * 0.001D;
					this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX + this.rand.nextFloat() * this.width - this.width, this.posY + 1.5D + this.rand.nextFloat() * this.height, this.posZ + this.rand.nextFloat() * this.width - this.width, d, d1, d2);
				}
			}
		}

		super.onLivingUpdate();
	}
	
	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		EntityPlayer entityplayer = player;
		boolean fighting = false;

		if (this instanceof EntityBullBase) {
			EntityBullBase ebb = (EntityBullBase) this;
			if (ebb.getFighting()) {
				fighting = true;
			}
		}

		if (stack != ItemStack.EMPTY && AnimaniaHelper.isWaterContainer(stack) && fighting == false && !this.getSleeping())
		{
			if(!player.isCreative())
			{
				ItemStack emptied = AnimaniaHelper.emptyContainer(stack);
				stack.shrink(1);
				AnimaniaHelper.addItem(player, emptied);
			}

			this.eatTimer = 40;
			this.entityAIEatGrass.startExecuting();
			this.setWatered(true);
			this.setInLove(player);
			return true;
		}
		else if (stack != ItemStack.EMPTY && (this instanceof EntityCowMooshroom || this instanceof EntityBullMooshroom) && stack.getItem() == Items.SHEARS && this.getGrowingAge() >= 0) //Forge Disable, Moved to onSheared
		{
			this.setDead();
			this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.posX, this.posY + (double)(this.height / 2.0F), this.posZ, 0.0D, 0.0D, 0.0D, new int[0]);

			if (!this.world.isRemote)
			{

				if (this instanceof EntityCowMooshroom) {
					EntityCowFriesian entitycow = new EntityCowFriesian(this.world);
					entitycow.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
					entitycow.setHealth(this.getHealth());
					entitycow.renderYawOffset = this.renderYawOffset;
					if (this.hasCustomName())
					{
						entitycow.setCustomNameTag(this.getCustomNameTag());
					}
					this.world.spawnEntity(entitycow);
				} else {
					EntityBullFriesian entitycow = new EntityBullFriesian(this.world);
					entitycow.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
					entitycow.setHealth(this.getHealth());
					entitycow.renderYawOffset = this.renderYawOffset;
					if (this.hasCustomName())
					{
						entitycow.setCustomNameTag(this.getCustomNameTag());
					}
					this.world.spawnEntity(entitycow);
				}

				for (int i = 0; i < 5; ++i)
				{
					this.world.spawnEntity(new EntityItem(this.world, this.posX, this.posY + (double)this.height, this.posZ, new ItemStack(Blocks.RED_MUSHROOM)));
				}

				stack.damageItem(1, player);
				this.playSound(SoundEvents.ENTITY_MOOSHROOM_SHEAR, 1.0F, 1.0F);
			}

			return true;
		}
		else if (stack != ItemStack.EMPTY && AnimaniaHelper.isEmptyFluidContainer(stack))
		{
			return true;
		}
		else
			return super.processInteract(player, hand);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleStatusUpdate(byte id)
	{
		if (id == 10)
			this.eatTimer = 160;
		else
			super.handleStatusUpdate(id);
	}

	@Override
	public boolean isBreedingItem(@Nullable ItemStack stack)
	{
		return mateable && (stack != ItemStack.EMPTY && this.isCowBreedingItem(stack.getItem()));
	}

	private boolean isCowBreedingItem(Item itemIn)
	{
		return TEMPTATION_ITEMS.contains(itemIn) || itemIn == Item.getItemFromBlock(Blocks.YELLOW_FLOWER) || itemIn == Item.getItemFromBlock(Blocks.RED_FLOWER);
	}


	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		super.writeEntityToNBT(compound);
		UUID mate = this.getMateUniqueId();
		if (mate != null)
			if (this.getMateUniqueId() != null)
				compound.setString("MateUUID", this.getMateUniqueId().toString());

		compound.setBoolean("Fed", this.getFed());
		compound.setBoolean("Handfed", this.getHandFed());
		compound.setBoolean("Watered", this.getWatered());
		compound.setBoolean("Sleep", this.getSleeping());
		compound.setFloat("SleepTimer", this.getSleepTimer());
		compound.setInteger("Age", this.getAge());

	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound)
	{
		super.readEntityFromNBT(compound);

		String s;

		if (compound.hasKey("MateUUID", 8))
			s = compound.getString("MateUUID");
		else
		{
			String s1 = compound.getString("Mate");
			s = PreYggdrasilConverter.convertMobOwnerIfNeeded(this.getServer(), s1);
		}

		if (!s.isEmpty())
			this.setMateUniqueId(UUID.fromString(s));

		this.setFed(compound.getBoolean("Fed"));
		this.setHandFed(compound.getBoolean("Handfed"));
		this.setWatered(compound.getBoolean("Watered"));
		this.setSleeping(compound.getBoolean("Sleep"));
		this.setSleepTimer(compound.getFloat("SleepTimer"));
		this.setAge(compound.getInteger("Age"));


	}

	@Override
	public Item getSpawnEgg()
	{
		return ItemEntityEgg.ANIMAL_EGGS.get(new AnimalContainer(this.cowType, this.gender));
	}

	@Override
	public ItemStack getPickedResult(RayTraceResult target)
	{
		return new ItemStack(getSpawnEgg());
	}

	@Override
	public int getPrimaryEggColor()
	{
		return 0;
	}

	@Override
	public int getSecondaryEggColor()
	{
		return 0;
	}

	@Override
	public EntityGender getEntityGender()
	{
		return this.gender;
	}

	// ==================================================
	//     Data Manager Trapper (borrowed from Lycanites)
	// ==================================================

	public boolean getBoolFromDataManager(DataParameter<Boolean> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return false;
		}
	}

	public byte getByteFromDataManager(DataParameter<Byte> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return 0;
		}
	}

	public int getIntFromDataManager(DataParameter<Integer> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return 0;
		}
	}

	public float getFloatFromDataManager(DataParameter<Float> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return 0;
		}
	}

	public String getStringFromDataManager(DataParameter<String> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return null;
		}
	}

	public Optional<UUID> getUUIDFromDataManager(DataParameter<Optional<UUID>> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return null;
		}
	}

	public ItemStack getItemStackFromDataManager(DataParameter<ItemStack> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public Optional<BlockPos> getBlockPosFromDataManager(DataParameter<Optional<BlockPos>> key) {
		try {
			return this.getDataManager().get(key);
		}
		catch (Exception e) {
			return Optional.absent();
		}
	}

	@Override
	public void setSleepingPos(BlockPos pos)
	{
	}

	@Override
	public BlockPos getSleepingPos()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Item> getFoodItems()
	{
		return TEMPTATION_ITEMS;
	}

	@Override
	public int getBlinkTimer()
	{
		return blinkTimer;
	}

}
