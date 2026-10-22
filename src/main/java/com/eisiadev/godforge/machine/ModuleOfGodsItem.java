package com.eisiadev.godforge.machine;

import com.eisiadev.godforge.structure.GodforgeModules;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModuleOfGodsItem extends MetaMachineItem {

    private static final double SEARCH_RANGE_SQR = 256.0 * 256.0;

    public ModuleOfGodsItem(IMachineBlock block, Properties properties) {
        super(block, properties);
    }

    public static boolean isHolding(Player player) {
        return isBlueprint(player.getMainHandItem()) || isBlueprint(player.getOffhandItem());
    }

    public static boolean isBlueprint(ItemStack stack) {
        return stack.getItem() instanceof ModuleOfGodsItem;
    }

    public record Target(ForgeOfGodsMachine forge, GodforgeModules.Site site) {}

    @Nullable
    public static Target findTarget(Level level, BlockPos pos) {
        for (ForgeOfGodsMachine forge : ForgeOfGodsMachine.loaded(level)) {
            if (!forge.isFormed() || forge.getPos().distSqr(pos) > SEARCH_RANGE_SQR) continue;
            for (GodforgeModules.Site site : GodforgeModules.sites(forge.moduleFrame())) {
                if (site.contains(pos)) return new Target(forge, site);
            }
        }
        return null;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {}

    @Override
    public String getDescriptionId() {
        return "item.godforge.module_of_the_gods_blueprint";
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        Target target = findTarget(level, context.getClickedPos());
        if (level.isClientSide) {
            return target == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
        }
        if (target == null) {
            player.displayClientMessage(Component.translatable("godforge.module.not_a_slot")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }

        if (GodforgeModules.status(level, target.site()) == GodforgeModules.Status.COMPLETE) {
            player.displayClientMessage(Component.translatable("godforge.module.already_complete")
                    .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }

        GodforgeModules.BuildResult result = GodforgeModules.build(level, player, target.site());
        ForgeOfGodsMachine forge = target.forge();
        forge.updateModules();

        if (GodforgeModules.status(level, target.site()) == GodforgeModules.Status.COMPLETE) {
            player.displayClientMessage(Component.translatable("godforge.module.completed",
                    forge.getModuleCount(), GodforgeModules.MAX_MODULES).withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.translatable("godforge.module.built", result.placed())
                .withStyle(ChatFormatting.AQUA), false);
        if (!result.missing().isEmpty()) {
            player.displayClientMessage(Component.translatable("godforge.module.missing")
                    .withStyle(ChatFormatting.RED), false);
            for (Map.Entry<Block, Integer> entry : result.missing().entrySet()) {
                player.displayClientMessage(Component.literal("  " + entry.getValue() + " x ")
                        .append(entry.getKey().getName()).withStyle(ChatFormatting.GRAY), false);
            }
        }
        if (result.obstructed() > 0) {
            player.displayClientMessage(Component.translatable("godforge.module.obstructed", result.obstructed())
                    .withStyle(ChatFormatting.RED), false);
        }
        return InteractionResult.CONSUME;
    }
}
