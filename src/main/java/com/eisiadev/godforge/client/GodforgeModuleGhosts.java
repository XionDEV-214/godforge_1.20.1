package com.eisiadev.godforge.client;

import com.eisiadev.godforge.machine.ForgeOfGodsMachine;
import com.eisiadev.godforge.machine.ModuleOfGodsItem;
import com.eisiadev.godforge.structure.GodforgeModules;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class GodforgeModuleGhosts {

    private GodforgeModuleGhosts() {}

    private static final double RANGE_SQR = 256.0 * 256.0;

    private record Anchor(BlockPos pos, int kind) {}

    private record Ghost(BlockPos pos, BlockState state) {}

    private static final List<Anchor> ANCHORS = new ArrayList<>();
    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final List<BlockPos> BLOCKED = new ArrayList<>();
    private static long lastScan = Long.MIN_VALUE;

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || !ModuleOfGodsItem.isHolding(player)) {
            lastScan = Long.MIN_VALUE;
            return;
        }

        long time = level.getGameTime();
        if (time != lastScan) {
            scan(mc, level, player, time);
            lastScan = time;
        }
        if (ANCHORS.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Anchor anchor : ANCHORS) {
            AABB box = new AABB(anchor.pos()).inflate(0.03);
            switch (anchor.kind()) {
                case 2 -> LevelRenderer.renderLineBox(pose, lines, box, 0.2f, 1f, 0.3f, 1f);
                case 1 -> LevelRenderer.renderLineBox(pose, lines, box.inflate(0.05), 0.2f, 0.9f, 1f, 1f);
                default -> LevelRenderer.renderLineBox(pose, lines, box, 1f, 0.85f, 0.1f, 1f);
            }
        }
        for (BlockPos pos : BLOCKED) {
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).inflate(0.02), 1f, 0.15f, 0.15f, 1f);
        }
        buffers.endBatch(RenderType.lines());

        float scale = 0.6f + 0.06f * Mth.sin((time + partial) * 0.15f);
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        for (Ghost ghost : GHOSTS) {
            pose.pushPose();
            pose.translate(ghost.pos().getX() + 0.5, ghost.pos().getY() + 0.5, ghost.pos().getZ() + 0.5);
            pose.scale(scale, scale, scale);
            pose.translate(-0.5, -0.5, -0.5);
            dispatcher.renderSingleBlock(ghost.state(), pose, buffers, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();

        pose.popPose();
    }

    private static void scan(Minecraft mc, ClientLevel level, Player player, long time) {
        ANCHORS.clear();
        GHOSTS.clear();
        BLOCKED.clear();

        ForgeOfGodsMachine closestForge = null;
        GodforgeModules.Site target = null;
        double best = Double.MAX_VALUE;
        for (ForgeOfGodsMachine forge : ForgeOfGodsMachine.loaded(level)) {
            if (!forge.isFormed() || forge.getPos().distToCenterSqr(player.position()) > RANGE_SQR) continue;
            for (GodforgeModules.Site site : GodforgeModules.sites(forge.moduleFrame())) {
                GodforgeModules.Status status = GodforgeModules.status(level, site);
                if (status == GodforgeModules.Status.UNLOADED) continue;
                boolean complete = status == GodforgeModules.Status.COMPLETE;
                ANCHORS.add(new Anchor(site.anchor(), complete ? 2 : 0));
                if (!complete) {
                    double distance = site.anchor().distToCenterSqr(player.position());
                    if (distance < best) {
                        best = distance;
                        target = site;
                        closestForge = forge;
                    }
                }
            }
        }
        if (target == null) {
            if (!ANCHORS.isEmpty() && time % 20 == 0) {
                mc.gui.setOverlayMessage(Component.translatable("godforge.module.all_complete")
                        .withStyle(ChatFormatting.GREEN), false);
            }
            return;
        }

        for (int i = 0; i < ANCHORS.size(); i++) {
            if (ANCHORS.get(i).pos().equals(target.anchor())) {
                ANCHORS.set(i, new Anchor(target.anchor(), 1));
            }
        }

        int remaining = 0;
        for (GodforgeModules.Cell cell : GodforgeModules.cells()) {
            BlockPos pos = target.cellPos(cell);
            BlockState expected = GodforgeModules.expected(cell.letter()).defaultBlockState();
            BlockState current = level.getBlockState(pos);
            if (current.is(expected.getBlock())) continue;
            remaining++;
            if (current.canBeReplaced()) {
                GHOSTS.add(new Ghost(pos, expected));
            } else {
                BLOCKED.add(pos);
            }
        }

        if (time % 20 == 0) {
            mc.gui.setOverlayMessage(Component.translatable("godforge.module.holding_hint",
                    closestForge.getModuleCount(), GodforgeModules.MAX_MODULES, remaining)
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }
}
