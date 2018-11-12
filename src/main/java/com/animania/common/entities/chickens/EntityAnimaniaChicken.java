package com.animania.common.entities.chickens;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.animania.Animania;
import com.animania.common.ModSoundEvents;
import com.animania.common.blocks.BlockSeeds;
import com.animania.common.entities.AnimalContainer;
import com.animania.common.entities.EntityGender;
import com.animania.common.entities.chickens.ai.EntityAIWatchClosestFromSide;
import com.animania.common.entities.generic.ai.GenericAIAvoidWater;
import com.animania.common.entities.generic.ai.GenericAIFindFood;
import com.animania.common.entities.generic.ai.GenericAIFindWater;
import com.animania.common.entities.generic.ai.GenericAILookIdle;
import com.animania.common.entities.generic.ai.GenericAIPanic;
import com.animania.common.entities.generic.ai.GenericAISleep;
import com.animania.common.entities.generic.ai.GenericAISwimmingSmallCreatures;
import com.animania.common.entities.generic.ai.GenericAITempt;
import com.animania.common.entities.generic.ai.GenericAIWanderAvoidWater;
import com.animania.common.entities.interfaces.IAnimaniaAnimalBase;
import com.animania.common.helper.AnimaniaHelper;
import com.animania.common.items.ItemEntityEgg;
import com.animania.config.AnimaniaConfig;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class EntityAnimaniaChicken extends EntityChicken implements IAnimaniaAnimalBase
{
	public static final Set<Item> TEMPTATION_ITEMS = Sets.newHashSet(AnimaniaHelper.getItemArray(AnimaniaConfig.careAndFeeding.chickenFood));
	protected static final DataParameter<Boolean> FED = EntityDataManager.<Boolean>createKey(EntityAnimaniaChicken.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> WATERED = EntityDataManager.<Boolean>createKey(EntityAnimaniaChicken.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Optional<UUID>> MATE_UNIQUE_ID = EntityDataManager.<Optional<UUID>>createKey(EntityAnimaniaChicken.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	protected static final DataParameter<Integer> AGE = EntityDataManager.<Integer>createKey(EntityAnimaniaChicken.class, DataSerializers.VARINT);
	protected static final DataParameter<Boolean> SLEEPING = EntityDataManager.<Boolean>createKey(EntityAnimaniaChicken.class, DataSerializers.BOOLEAN);
	protected static final DataParameter<Boolean> HANDFED = EntityDataManager.<Boolean>createKey(EntityAnimaniaChicken.class, DataSerializers.BOOLEAN);
	public boolean chickenJockey;
	protected ResourceLocation resourceLocation;
	protected ResourceLocation resourceLocationBlink;
	public float destPos;
	public float oFlapSpeed;
	public float oFlap;
	public float wingRotDelta = 1.0F;
	private int fedTimer;
	protected int wateredTimer;
	protected int happyTimer;
	public int blinkTimer;
	private int featherTimer;
	protected int damageTimer;
	public ChickenType type;
	public EntityGender gender;
	public int lidCol;
	
	public EntityAnimaniaChicken(World worldIn)
	{
		super(worldIn);
		this.tasks.taskEntries.clear();
		this.tasks.addTask(0, new GenericAISwimmingSmallCreatures(this));
		this.tasks.addTask(1, new GenericAIPanic(this, 1.4D));
		if (!AnimaniaConfig.gameRules.ambianceMode) {
			this.tasks.addTask(2, new GenericAIFindWater<EntityAnimaniaChicken>(this, 1.0D, null, EntityAnimaniaChicken.class, true));
			this.tasks.addTask(3, new GenericAIFindFood<EntityAnimaniaChicken>(this, 1.0D, null, true));
		}
		this.tasks.addTask(4, new GenericAITempt(this, 1.2D, false, EntityAnimaniaChicken.TEMPTATION_ITEMS));
		this.tasks.addTask(5, new GenericAIWanderAvoidWater(this, 1.0D));
		this.tasks.addTask(6, new EntityAIWatchClosestFromSide(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(8, new GenericAIAvoidWater(this));
		this.tasks.addTask(7, new GenericAILookIdle(this));
		if (AnimaniaConfig.gameRules.animalsSleep) {
			this.tasks.addTask(12, new GenericAISleep<EntityAnimaniaChicken>(this, 0.8, AnimaniaHelper.getBlock(AnimaniaConfig.careAndFeeding.chickenBed), AnimaniaHelper.getBlock(AnimaniaConfig.careAndFeeding.chickenBed2), EntityAnimaniaChicken.class));
		}
		this.targetTasks.addTask(13, new EntityAIHurtByTarget(this, false, new Class[0]));
		this.fedTimer = AnimaniaConfig.careAndFeeding.feedTimer + this.rand.nextInt(100);
		this.wateredTimer = AnimaniaConfig.careAndFeeding.waterTimer + this.rand.nextInt(100);
		this.happyTimer = 60;
		this.blinkTimer = 80 + this.rand.nextInt(80);
		this.featherTimer = AnimaniaConfig.careAndFeeding.featherTimer + rand.nextInt(1000);
		this.enablePersistence();
	}

	@Override
	protected void consumeItemFromStack(EntityPlayer player, ItemStack stack)
	{
		this.setFed(true);
		this.setHandFed(true);
		if (!player.capabilities.isCreativeMode)
			stack.setCount(stack.getCount() - 1);
	}

	@Override
	public void setPosition(double x, double y, double z)
	{
		super.setPosition(x, y, z);
	}


	@Override
	public void setInLove(EntityPlayer player)
	{
		this.world.setEntityState(this, (byte) 18);
	}

	public boolean isAIEnabled()
	{
		return true;
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		EntityPlayer entityplayer = player;

		if (stack != ItemStack.EMPTY && AnimaniaHelper.isWaterContainer(stack) && !this.getSleeping())
		{
			if(!player.isCreative())
			{
				ItemStack emptied = AnimaniaHelper.emptyContainer(stack);
				stack.shrink(1);
				AnimaniaHelper.addItem(player, emptied);
			}

			this.setWatered(true);
			this.setInLove(player);
			return true;
		}
		else
			return super.processInteract(player, hand);
	}

	public ResourceLocation getResourceLocation()
	{
		return this.resourceLocation;
	}

	public ResourceLocation getResourceLocationBlink()
	{
		return this.resourceLocationBlink;
	}

	@Override
	protected ResourceLocation getLootTable()
	{
		return this instanceof EntityChickBase ? null : this.type.isPrime ? new ResourceLocation(Animania.MODID, "chicken_prime") : new ResourceLocation(Animania.MODID, "chicken_regular");
	}
	
	@Override
	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(6.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.29D);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataManager.register(EntityAnimaniaChicken.FED, Boolean.valueOf(true));
		this.dataManager.register(EntityAnimaniaChicken.WATERED, Boolean.valueOf(true));
		this.dataManager.register(EntityAnimaniaChicken.AGE, Integer.valueOf(0));
		this.dataManager.register(EntityAnimaniaChicken.SLEEPING, Boolean.valueOf(false));
		this.dataManager.register(EntityAnimaniaChicken.HANDFED, Boolean.valueOf(false));
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setBoolean("IsChickenJockey", this.chickenJockey);
		nbttagcompound.setBoolean("Fed", this.getFed());
		nbttagcompound.setBoolean("Handfed", this.getHandFed());
		nbttagcompound.setBoolean("Watered", this.getWatered());
		nbttagcompound.setInteger("Age", this.getAge());
		nbttagcompound.setBoolean("Sleep", this.getSleeping());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readEntityFromNBT(nbttagcompound);

		this.chickenJockey = nbttagcompound.getBoolean("IsChickenJockey");

		this.setFed(nbttagcompound.getBoolean("Fed"));
		this.setHandFed(nbttagcompound.getBoolean("Handfed"));
		this.setWatered(nbttagcompound.getBoolean("Watered"));
		this.setAge(nbttagcompound.getInteger("Age"));
		this.setSleeping(nbttagcompound.getBoolean("Sleep"));
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
		this.dataManager.set(EntityAnimaniaChicken.AGE, Integer.valueOf(age));
	}

	@Override
	public void onLivingUpdate()
	{

		super.onLivingUpdate();

		if (this.getLeashed() && this.getSleeping())
			this.setSleeping(false);

		if (this.getAge() == 0) {
			this.setAge(1);
		}

		this.oFlap = this.wingRotation;
		this.oFlapSpeed = this.destPos;
		this.destPos = (float) (this.destPos + ((this.onGround || this.isRiding()) ? -1 : 4) * 0.3D);
		this.destPos = MathHelper.clamp(this.destPos, 0.0F, 1.0F);

		this.fallDistance = 0;

		if (!this.world.isRemote && !this.isChild() && AnimaniaConfig.gameRules.chickensDropFeathers && !this.isChickenJockey() && --this.featherTimer <= 0)
		{
			this.playSound(ModSoundEvents.chickenCluck2, 0.5F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
			this.dropItem(Items.FEATHER, 1);
			this.featherTimer = AnimaniaConfig.careAndFeeding.featherTimer + rand.nextInt(1000);
		}

		if (!this.onGround && !this.isRiding() && this.wingRotDelta < 1.0F)
			this.wingRotDelta = 1.0F;

		this.wingRotDelta = (float) (this.wingRotDelta * 0.9D);

		if (!this.onGround && !this.isRiding() && this.motionY < 0.0D)
			this.motionY *= 0.6D;

		this.wingRotation += this.wingRotDelta * 2.0F;

		if (this.blinkTimer > -1)
		{
			this.blinkTimer--;
			if (this.blinkTimer == 0)
				this.blinkTimer = 100 + this.rand.nextInt(100);
		}

		if (this.fedTimer > -1 && !AnimaniaConfig.gameRules.ambianceMode)
		{
			this.fedTimer--;

			if (this.fedTimer == 0)
				this.setFed(false);
		}

		if (this.wateredTimer > -1 && !AnimaniaConfig.gameRules.ambianceMode)
		{
			this.wateredTimer--;

			if (this.wateredTimer == 0)
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

		if (this.happyTimer > -1)
		{
			this.happyTimer--;
			if (this.happyTimer == 0)
			{
				this.happyTimer = 60;

				if (!this.getFed() && !this.getWatered() && !this.getSleeping() && AnimaniaConfig.gameRules.showUnhappyParticles)
				{
					double d = this.rand.nextGaussian() * 0.001D;
					double d1 = this.rand.nextGaussian() * 0.001D;
					double d2 = this.rand.nextGaussian() * 0.001D;
					this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX + this.rand.nextFloat() * this.width - this.width, this.posY + 1.5D + this.rand.nextFloat() * this.height, this.posZ + this.rand.nextFloat() * this.width - this.width, d, d1, d2);
				}
			}
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
			this.dataManager.set(EntityAnimaniaChicken.FED, Boolean.valueOf(true));
			this.fedTimer = AnimaniaConfig.careAndFeeding.feedTimer + this.rand.nextInt(100);
			this.setHealth(this.getHealth() + 1.0F);
		}
		else
			this.dataManager.set(EntityAnimaniaChicken.FED, Boolean.valueOf(false));
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
			this.dataManager.set(EntityAnimaniaChicken.WATERED, Boolean.valueOf(true));
			this.wateredTimer = AnimaniaConfig.careAndFeeding.waterTimer + this.rand.nextInt(100);
		}
		else
			this.dataManager.set(EntityAnimaniaChicken.WATERED, Boolean.valueOf(false));
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
			this.dataManager.set(EntityAnimaniaChicken.SLEEPING, Boolean.valueOf(true));
		}
		else
		{
			this.dataManager.set(EntityAnimaniaChicken.SLEEPING, Boolean.valueOf(false));
		}
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
		this.dataManager.set(EntityAnimaniaChicken.HANDFED, Boolean.valueOf(handfed));
	}

	protected void fall(float p_70069_1_)
	{
	}

	@Override
	protected SoundEvent getAmbientSound()
	{
		int happy = 0;
		int num = 1;

		if (this.getWatered())
			happy++;
		if (this.getFed())
			happy++;

		if (happy == 2)
			num = 6;
		else if (happy == 1)
			num = 12;
		else
			num = 24;

		int chooser = Animania.RANDOM.nextInt(num);

		if (chooser == 0)
			return ModSoundEvents.chickenCluck1;
		else if (chooser == 1)
			return ModSoundEvents.chickenCluck2;
		else if (chooser == 2)
			return ModSoundEvents.chickenCluck3;
		else if (chooser == 3)
			return ModSoundEvents.chickenCluck4;
		else if (chooser == 4)
			return ModSoundEvents.chickenCluck5;
		else
			return ModSoundEvents.chickenCluck6;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source)
	{
		return null;
	}

	@Override
	protected SoundEvent getDeathSound()
	{
		return null;
	}

	@Override
	protected void playStepSound(BlockPos pos, Block blockIn)
	{
		this.playSound(SoundEvents.ENTITY_CHICKEN_STEP, 0.10F, 1.4F);
	}

	@Override
	public void playSound(SoundEvent soundIn, float volume, float pitch)
	{
		if (!this.isSilent() && !this.getSleeping())
		{
			this.world.playSound((EntityPlayer)null, this.posX, this.posY, this.posZ, soundIn, this.getSoundCategory(), volume, pitch);
		}
	}

	@Override
	public boolean isBreedingItem(@Nullable ItemStack stack)
	{
		return stack != ItemStack.EMPTY && EntityAnimaniaChicken.TEMPTATION_ITEMS.contains(stack.getItem());
	}

	/**
	 * Get the experience points the entity currently has.
	 */
	@Override
	protected int getExperiencePoints(EntityPlayer player)
	{
		return this.isChickenJockey() ? 10 : super.getExperiencePoints(player);
	}

	@Override
	protected boolean canDespawn()
	{
		return false;
	}

	@Override
	public void updatePassenger(Entity passenger)
	{
		super.updatePassenger(passenger);
		float f = MathHelper.sin(this.renderYawOffset * 0.017453292F);
		float f1 = MathHelper.cos(this.renderYawOffset * 0.017453292F);
		float f2 = 0.1F;
		float f3 = 0.0F;
		passenger.setPosition(this.posX + 0.1F * f, this.posY + this.height * 0.5F + passenger.getYOffset() + 0.0D, this.posZ - 0.1F * f1);

		if (passenger instanceof EntityLivingBase)
			((EntityLivingBase) passenger).renderYawOffset = this.renderYawOffset;
	}

	public boolean isChickenJockey()
	{
		return this.chickenJockey;
	}

	public void setChickenJockey(boolean jockey)
	{
		this.chickenJockey = jockey;
	}

	@Override
	public EntityChicken createChild(EntityAgeable ageable)
	{
		return null;
	}

	@Override
	public Item getSpawnEgg()
	{
		return ItemEntityEgg.ANIMAL_EGGS.get(new AnimalContainer(this.type, this.gender));
	}

	@Override
	public ItemStack getPickedResult(RayTraceResult target)
	{
		return new ItemStack(getSpawnEgg());
	}

	@Override
	public int getPrimaryEggColor()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSecondaryEggColor()
	{
		// TODO Auto-generated method stub
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
	public int getBlinkTimer()
	{
		return blinkTimer;
	}

	@Override
	public Set<Item> getFoodItems()
	{
		return TEMPTATION_ITEMS;
	}

	@Override
	public void setSleepingPos(BlockPos pos)
	{
		
	}

	@Override
	public BlockPos getSleepingPos()
	{
		return null;
	}
	
	@Override
	public Class[] getFoodBlocks()
	{
		return new Class[]{BlockSeeds.class};
	}

	@Override
	public Float getSleepTimer()
	{
		return -100f;
	}

	@Override
	public void setSleepTimer(Float timer)
	{
		
	}

}
