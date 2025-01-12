/*
 From Gamepiaynmo: https://github.com/Gamepiaynmo/TC-UHC
 */

package me.fallenbreath.tcuhc.gen.feature;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import me.fallenbreath.tcuhc.UhcGameManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.util.registry.Registry;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BonusChestFeature extends Feature<DefaultFeatureConfig>
{
	public static final String BONUS_CHEST_NAME = "Bonus Chest";
	public static final String EMPTY_CHEST_NAME = "Empty Chest";

	private static Map<Biome.Category, Double> POSSIBILITY_MAP;
	private static final Enchantment[] POSSIBLE_ENCHANTMENTS = {
			Enchantments.POWER, Enchantments.SHARPNESS, Enchantments.UNBREAKING, Enchantments.EFFICIENCY,
			Enchantments.FIRE_ASPECT, Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION
	};
	private static final Enchantment[] POSSIBLE_MARINE_ENCHANTMENTS = {
			Enchantments.POWER, Enchantments.SHARPNESS, Enchantments.UNBREAKING, Enchantments.EFFICIENCY,
			Enchantments.FIRE_ASPECT, Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION,
			Enchantments.LOYALTY, Enchantments.RIPTIDE, Enchantments.IMPALING,
			Enchantments.FROST_WALKER, Enchantments.AQUA_AFFINITY, Enchantments.DEPTH_STRIDER
	};
	private static final Enchantment[] POSSIBLE_ICARUS_ENCHANTMENTS = {
			Enchantments.POWER, Enchantments.SHARPNESS, Enchantments.UNBREAKING, Enchantments.EFFICIENCY,
			Enchantments.FIRE_ASPECT, Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION,
			Enchantments.FEATHER_FALLING
	};
	private static final Random rand = new Random();

	private static final List<RandomItem> chestItemList = Lists.newArrayList();
	private static final List<RandomItem> valuableItemList = Lists.newArrayList();
	private static final List<RandomItem> emptyItemList = Lists.newArrayList();

	private static double chestChance;
	private static double emptyChestChance;
	private static double itemChance;

	private static boolean dataGenerated = false;

	public BonusChestFeature(Codec<DefaultFeatureConfig> configCodec)
	{
		super(configCodec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context)
	{
		StructureWorldAccess worldIn = context.getWorld();
		BlockPos position = context.getOrigin();
		Random rand = context.getRandom();
		if (!dataGenerated)
		{
			generateData();
			dataGenerated = true;
		}

		int chunkX = position.getX() >> 4;
		int chunkZ = position.getZ() >> 4;
		if (Math.abs(chunkX) <= 1 || Math.abs(chunkZ) <= 1)
			return false;
		int posX = rand.nextInt(32) + position.getX() - 16;
		int posZ = rand.nextInt(32) + position.getZ() - 16;
		int posY = worldIn.getTopY(Heightmap.Type.OCEAN_FLOOR, posX, posZ);
		position = new BlockPos(posX, posY, posZ);
		while (!worldIn.getBlockState(position).isSolidBlock(worldIn, position) && position.getY() > worldIn.getBottomY())
		{
			position = position.down();
		}
		if (position.getY() == worldIn.getBottomY())
		{
			return false;
		}
		Biome biome = worldIn.getBiome(position);
		if (!POSSIBILITY_MAP.containsKey(biome.getCategory()))
		{
			return false;
		}
		boolean hasWater = worldIn.getFluidState(position).isIn(FluidTags.WATER);
		if (rand.nextFloat() < POSSIBILITY_MAP.get(biome.getCategory()) * chestChance)
		{
			boolean isEmptyChest = rand.nextDouble() < emptyChestChance;
			Block chestBlock = isEmptyChest ? Blocks.TRAPPED_CHEST : Blocks.CHEST;
			worldIn.setBlockState(
					position,
					chestBlock.getDefaultState().
							rotate(BlockRotation.random(rand)).
							with(ChestBlock.WATERLOGGED, hasWater)
					, 3
			);
			if (worldIn.getBlockState(position.up()).getBlock() == Blocks.SNOW)
			{
				worldIn.setBlockState(position.up(), Blocks.AIR.getDefaultState(), 3);
			}
			BlockEntity tileentity = worldIn.getBlockEntity(position);
			if (!(tileentity instanceof ChestBlockEntity))
			{
				return false;
			}
			ChestBlockEntity chest = (ChestBlockEntity) tileentity;
			chest.setCustomName(new LiteralText(isEmptyChest ? EMPTY_CHEST_NAME : BONUS_CHEST_NAME));
			if (isEmptyChest)
			{
				this.genChestItem(chest, emptyItemList, false);
			}
			else
			{
				this.genChestItem(chest, chestItemList, false);
				this.genChestItem(chest, valuableItemList, true);
			}
		}
		return false;
	}

	private void genChestItem(ChestBlockEntity chest, List<RandomItem> itemList, boolean valuable)
	{
		for (RandomItem item : itemList)
		{
			Optional<ItemStack> itemstack = item.getItemStack();
			itemstack.ifPresent(stack -> chest.setStack(rand.nextInt(chest.size()), stack));
			if (valuable && itemstack.isPresent()) break;
		}
	}

	static class ItemSupplier implements Supplier<ItemStack>
	{
		Item item;

		public ItemSupplier(Item item)
		{
			this.item = item;
		}

		public ItemStack get()
		{
			return new ItemStack(item);
		}
	}

	static class MinMaxSupplier implements Supplier<ItemStack>
	{
		Item item;
		int min, max;

		public MinMaxSupplier(Item item, int min, int max)
		{
			this.item = item;
			this.min = min;
			this.max = max;
		}

		public ItemStack get()
		{
			return new ItemStack(item, BonusChestFeature.rand.nextInt(max - min + 1) + min);
		}
	}

	static class RandomItem
	{
		int chance;
		Supplier<ItemStack> stack;

		public RandomItem(int chance, Supplier<ItemStack> stack)
		{
			this.chance = chance;
			this.stack = stack;
		}

		public Optional<ItemStack> getItemStack()
		{
			return BonusChestFeature.rand.nextInt(chance) == 0 ? Optional.of(stack.get()) : Optional.empty();
		}
	}

	private static void generateData()
	{
		double forestChance = 0.12;
		double oceanChance = 0.0;
		if(UhcGameManager.getBattleType() == UhcGameManager.EnumBattleType.MARINE)
			oceanChance = 0.2;
		double desertChance = 0.06;
		double exHillsChance = 0.12;
		double plainChance = 0.06;
		double icePlainChance = 0.2;
		double iceMountainChance = 0.2;
		double jungleChance = 0.12;
		double mesaChance = 0.12;
		double mushroomChance = 0.1;
		double rforestChance = 0.12;
		double savannaChance = 0.12;
		double taigaChance = 0.12;
		double riverChance = 0.0;
		double beachChance = 0.0;
		double swamplandChance = 0.1;
		double miscChance = 0.0;

		POSSIBILITY_MAP = new ImmutableMap.Builder<Biome.Category, Double>().
				put(Biome.Category.NONE, miscChance).
				put(Biome.Category.TAIGA, taigaChance).
				put(Biome.Category.EXTREME_HILLS, exHillsChance).
				put(Biome.Category.JUNGLE, jungleChance).
				put(Biome.Category.MESA, mesaChance).
				put(Biome.Category.PLAINS, plainChance).
				put(Biome.Category.SAVANNA, savannaChance).
				put(Biome.Category.ICY, icePlainChance).
				put(Biome.Category.THEEND, miscChance).
				put(Biome.Category.BEACH, beachChance).
				put(Biome.Category.FOREST, forestChance).
				put(Biome.Category.OCEAN, oceanChance).
				put(Biome.Category.DESERT, desertChance).
				put(Biome.Category.RIVER, riverChance).
				put(Biome.Category.SWAMP, swamplandChance).
				put(Biome.Category.MUSHROOM, mushroomChance).
				put(Biome.Category.NETHER, miscChance).
				put(Biome.Category.UNDERGROUND, miscChance).
				put(Biome.Category.MOUNTAIN, exHillsChance).
				build();

		valuableItemList.add(new RandomItem(16, new ItemSupplier(Items.DIAMOND_SWORD)));
		valuableItemList.add(new RandomItem(24, new ItemSupplier(Items.DIAMOND_PICKAXE)));
		valuableItemList.add(new RandomItem(20, new ItemSupplier(Items.GOLDEN_APPLE)));
		valuableItemList.add(new RandomItem(8, new ItemSupplier(Items.DIAMOND)));
		valuableItemList.add(new RandomItem(16, () -> {
			ItemStack item = new ItemStack(Items.ENCHANTED_BOOK);
			switch (UhcGameManager.getBattleType()) {
				case NORMAL:
					EnchantedBookItem.addEnchantment(item, new EnchantmentLevelEntry(POSSIBLE_ENCHANTMENTS[rand.nextInt(POSSIBLE_ENCHANTMENTS.length)], rand.nextInt(4) == 0 ? 2 : 1));
					break;
				case MARINE:
					EnchantedBookItem.addEnchantment(item, new EnchantmentLevelEntry(POSSIBLE_MARINE_ENCHANTMENTS[rand.nextInt(POSSIBLE_MARINE_ENCHANTMENTS.length)], rand.nextInt(4) == 0 ? 2 : 1));
					break;
				case ICARUS:
					EnchantedBookItem.addEnchantment(item, new EnchantmentLevelEntry(POSSIBLE_ICARUS_ENCHANTMENTS[rand.nextInt(POSSIBLE_ICARUS_ENCHANTMENTS.length)], rand.nextInt(4) == 0 ? 2 : 1));
					break;
			}
			return item;
		}));

		chestItemList.add(new RandomItem(1, new ItemSupplier(Items.STICK)));
		chestItemList.add(new RandomItem(1, new ItemSupplier(Items.BONE)));
		chestItemList.add(new RandomItem(2, new ItemSupplier(Items.STRING)));
		chestItemList.add(new RandomItem(2, new MinMaxSupplier(Items.IRON_INGOT, 1, 2)));
		chestItemList.add(new RandomItem(3, new ItemSupplier(Items.GOLD_INGOT)));
		chestItemList.add(new RandomItem(3, new ItemSupplier(Items.CHORUS_FRUIT)));
		chestItemList.add(new RandomItem(5, new ItemSupplier(Items.LEATHER)));
		chestItemList.add(new RandomItem(5, new MinMaxSupplier(Items.EXPERIENCE_BOTTLE, 2, 4)));

		emptyItemList.add(new RandomItem(1, () -> new ItemStack(Blocks.DEAD_BUSH).setCustomName(new LiteralText("There should be something here, but ..."))));

		if (UhcGameManager.getBattleType() == UhcGameManager.EnumBattleType.MARINE) {
			valuableItemList.add(new RandomItem(8, () -> {
				return PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER_BREATHING);
			}));
			valuableItemList.add(new RandomItem(64, () -> {
				ItemStack item = new ItemStack(Items.ENCHANTED_BOOK);
				EnchantedBookItem.addEnchantment(item, new EnchantmentLevelEntry(Enchantments.CHANNELING, 1));
				return item;
			}));
			valuableItemList.add(new RandomItem(64, () -> {
				ItemStack item = new ItemStack(Items.ENCHANTED_BOOK);
				EnchantedBookItem.addEnchantment(item, new EnchantmentLevelEntry(Enchantments.RIPTIDE, 1));
				return item;
			}));
			valuableItemList.add(new RandomItem(16, new ItemSupplier(Items.TRIDENT)));
			valuableItemList.add(new RandomItem(8, new ItemSupplier(Items.APPLE)));
			chestItemList.add(new RandomItem(5, new ItemSupplier(Items.OAK_LOG)));
		} else if (UhcGameManager.getBattleType() == UhcGameManager.EnumBattleType.ICARUS) {
			valuableItemList.add(new RandomItem(16, new ItemSupplier(Items.GUNPOWDER)));
			valuableItemList.add(new RandomItem(32, () -> {
				ItemStack item = PotionUtil.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.LUCK);
				return item;
			}));
			valuableItemList.add(new RandomItem(8, () -> {
				List list = Registry.POTION.stream().filter(potion -> !potion.getEffects().isEmpty() && BrewingRecipeRegistry.isBrewable(potion)).collect(Collectors.toList());
				Potion potion = (Potion) list.get(rand.nextInt(list.size()));
				ItemStack item = PotionUtil.setPotion(new ItemStack(Items.TIPPED_ARROW), potion);
				return item;
			}));
			if(UhcGameManager.getLevelType() == UhcGameManager.EnumLevelType.AMPLIFIED)
				chestItemList.add(new RandomItem(1, new ItemSupplier(Items.FIREWORK_ROCKET)));
			else
				chestItemList.add(new RandomItem(3, new ItemSupplier(Items.FIREWORK_ROCKET)));
		}

		chestChance = UhcGameManager.instance.getOptions().getFloatOptionValue("chestFrequency");
		emptyChestChance = UhcGameManager.instance.getOptions().getFloatOptionValue("trappedChestFrequency");
		itemChance = UhcGameManager.instance.getOptions().getFloatOptionValue("chestItemFrequency");
	}
}
