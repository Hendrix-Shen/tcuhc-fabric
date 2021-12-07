package me.fallenbreath.tcuhc.mixins.worldgen.ore;

import me.fallenbreath.tcuhc.gen.UhcFeatures;
import me.fallenbreath.tcuhc.options.Options;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(DefaultBiomeFeatures.class)
public abstract class DefaultBiomeFeaturesMixin
{
	/*
	 * TC-UHC 1.12 v1.4.6
	 *
	 * coalSize: 17 -> 12
	 * ironSize: 9 -> 6
	 * ironCount: 20 -> 4
	 * goldSize: 9 -> 4
	 * goldCount: 2 -> 1
	 * goldMaxHeight: 32 -> 20;
	 * redstoneSize: 8 -> 4;
	 * redstoneCount: 8 -> 4;
	 * diamondSize: 8 -> 5;
	 * lapisSize: 7 -> 4;
	 * lapisCenterHeight: 16 -> 20;
	 * lapisSpread: 16 -> 10;
	 *
	 * oreFrequency on ironCount, goldCount, diamondCount, lapisCount
	 */

	@Unique
	private static int calcOreCount(int baseCount)
	{
		int oreCnt = Options.instance.getIntegerOptionValue("oreFrequency");
		return baseCount * oreCnt;
	}
	
	@ModifyArgs(
			method = "addDefaultOres",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/biome/Biome;configureFeature(Lnet/minecraft/world/gen/feature/Feature;Lnet/minecraft/world/gen/feature/FeatureConfig;Lnet/minecraft/world/gen/decorator/Decorator;Lnet/minecraft/world/gen/decorator/DecoratorConfig;)Lnet/minecraft/world/gen/feature/ConfiguredFeature;"
			)
	)
	private static void modifyOreFeature(Args args)
	{
		if (UhcFeatures.shouldModifyToValuableOre(args.get(0), args.get(1)))
		{
			args.set(0, UhcFeatures.VALUABLE_ORE);
		}
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 17, ordinal = 0))
	private static int coalSize(int value)
	{
		return 12;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 9, ordinal = 0))
	private static int ironSize(int value)
	{
		return 6;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 20, ordinal = 1))
	private static int ironCount(int value)
	{
		return calcOreCount(4);
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 9, ordinal = 1))
	private static int goldSize(int value)
	{
		return 4;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 2, ordinal = 0))
	private static int goldCount(int value)
	{
		return calcOreCount(1);
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 32, ordinal = 0))
	private static int goldMaxHeight(int value)
	{
		return 20;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 8, ordinal = 0))
	private static int redstoneSize(int value)
	{
		return 4;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 8, ordinal = 2))
	private static int diamondSize(int value)
	{
		return 5;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 1, ordinal = 0))
	private static int diamondCount(int value)
	{
		return calcOreCount(1);
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 7, ordinal = 0))
	private static int lapisSize(int value)
	{
		return 4;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 1, ordinal = 1))
	private static int lapisCount(int value)
	{
		return calcOreCount(1);
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 16, ordinal = 2))
	private static int lapisCenterHeight(int value)
	{
		return 20;
	}

	@ModifyConstant(method = "addDefaultOres", constant = @Constant(intValue = 16, ordinal = 3))
	private static int lapisSpread(int value)
	{
		return 10;
	}
}
