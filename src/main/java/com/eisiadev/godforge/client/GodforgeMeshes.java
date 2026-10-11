package com.eisiadev.godforge.client;

import com.eisiadev.godforge.GodforgeMod;
import com.eisiadev.godforge.structure.GodforgeBlocks;
import com.eisiadev.godforge.structure.GodforgeLayout;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class GodforgeMeshes {

    private GodforgeMeshes() {}

    private static final int SPHERE_SLICES = 128;
    private static final int SPHERE_STACKS = 128;

    @Nullable private static VertexBuffer sphere;
    private static final VertexBuffer[] RINGS = new VertexBuffer[4];
    private static boolean ringBuildFailed = false;
    @Nullable private static VertexBuffer beam;

    public static void release() {
        if (sphere != null) { sphere.close(); sphere = null; }
        for (int i = 0; i < RINGS.length; i++) {
            if (RINGS[i] != null) { RINGS[i].close(); RINGS[i] = null; }
        }
        if (beam != null) { beam.close(); beam = null; }
        ringBuildFailed = false;
    }

    public static VertexBuffer sphere() {
        if (sphere == null) {
            BufferBuilder bb = new BufferBuilder(SPHERE_SLICES * SPHERE_STACKS * 6 * 20);
            bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            for (int i = 0; i < SPHERE_STACKS; i++) {
                double v0 = (double) i / SPHERE_STACKS;
                double v1 = (double) (i + 1) / SPHERE_STACKS;
                double phi0 = Math.PI / 2.0 - i * Math.PI / SPHERE_STACKS;
                double phi1 = Math.PI / 2.0 - (i + 1) * Math.PI / SPHERE_STACKS;
                double y0 = Math.sin(phi0), y1 = Math.sin(phi1);
                double r0 = Math.cos(phi0), r1 = Math.cos(phi1);
                for (int j = 0; j < SPHERE_SLICES; j++) {
                    float uu0 = (float) (1.0 - (double) j / SPHERE_SLICES);
                    float uu1 = (float) (1.0 - (double) (j + 1) / SPHERE_SLICES);
                    double th0 = j * 2.0 * Math.PI / SPHERE_SLICES;
                    double th1 = (j + 1) * 2.0 * Math.PI / SPHERE_SLICES;
                    double x00 = r0 * Math.cos(th0), z00 = r0 * Math.sin(th0);
                    double x10 = r1 * Math.cos(th0), z10 = r1 * Math.sin(th0);
                    double x11 = r1 * Math.cos(th1), z11 = r1 * Math.sin(th1);
                    double x01 = r0 * Math.cos(th1), z01 = r0 * Math.sin(th1);

                    tri(bb, x00, y0, z00, uu0, (float) v0);
                    tri(bb, x10, y1, z10, uu0, (float) v1);
                    tri(bb, x11, y1, z11, uu1, (float) v1);

                    tri(bb, x00, y0, z00, uu0, (float) v0);
                    tri(bb, x11, y1, z11, uu1, (float) v1);
                    tri(bb, x01, y0, z01, uu1, (float) v0);
                }
            }
            sphere = upload(bb);
        }
        return sphere;
    }

    private static void tri(BufferBuilder bb, double x, double y, double z, float u, float v) {
        bb.vertex(x, y, z).uv(u, v).endVertex();
    }

    @Nullable
    public static VertexBuffer ring(int ring) {
        if (RINGS[ring] == null && !ringBuildFailed) {
            try {
                RINGS[ring] = buildRing(ring);
            } catch (Exception e) {
                ringBuildFailed = true;
                GodforgeMod.LOGGER.error("[Godforge] Failed to bake ring {} mesh", ring, e);
            }
        }
        return RINGS[ring];
    }

    private static long key(int back, int down, int left) {
        return ((long) (back + 1024) << 40) | ((long) (down + 1024) << 20) | (long) (left + 1024);
    }

    private static VertexBuffer buildRing(int ring) {
        List<GodforgeLayout.Cell> cells = GodforgeLayout.ringCells(ring);

        Map<Character, BlockState> byLetter = new HashMap<>();
        Map<Long, BlockState> grid = new HashMap<>(cells.size() * 2);
        for (GodforgeLayout.Cell c : cells) {
            BlockState state = byLetter.computeIfAbsent(c.letter(), ch -> {
                Block b = GodforgeBlocks.forLetter(ch);
                return b.defaultBlockState();
            });
            grid.put(key(c.back(), c.down(), c.left()), state);
        }

        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create(42L);
        BufferBuilder bb = new BufferBuilder(1 << 20);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        List<BakedQuad> quads = new ArrayList<>();
        for (GodforgeLayout.Cell c : cells) {
            BlockState state = byLetter.get(c.letter());
            BakedModel model = dispatcher.getBlockModel(state);

            float cx = c.back() - GodforgeLayout.STAR_DISTANCE;
            float cy = -c.down();
            float cz = c.left();

            quads.clear();
            for (Direction dir : Direction.values()) {
                if (isHidden(grid, c, dir)) continue;
                random.setSeed(42L);
                quads.addAll(model.getQuads(state, dir, random, ModelData.EMPTY, null));
            }
            random.setSeed(42L);
            quads.addAll(model.getQuads(state, null, random, ModelData.EMPTY, null));

            for (BakedQuad q : quads) {
                int[] v = q.getVertices();
                for (int i = 0; i < 4; i++) {
                    int o = i * 8;
                    float x = Float.intBitsToFloat(v[o]) - 0.5f + cx;
                    float y = Float.intBitsToFloat(v[o + 1]) - 0.5f + cy;
                    float z = Float.intBitsToFloat(v[o + 2]) - 0.5f + cz;
                    float u = Float.intBitsToFloat(v[o + 4]);
                    float w = Float.intBitsToFloat(v[o + 5]);
                    bb.vertex(x, y, z).uv(u, w).endVertex();
                }
            }
        }
        return upload(bb);
    }

    private static boolean isHidden(Map<Long, BlockState> grid, GodforgeLayout.Cell c, Direction dir) {
        int nb = c.back() + dir.getStepX();
        int nd = c.down() - dir.getStepY();
        int nl = c.left() + dir.getStepZ();
        BlockState neighbor = grid.get(key(nb, nd, nl));
        return neighbor != null && neighbor.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static VertexBuffer uploadBeam(BufferBuilder.RenderedBuffer rendered) {
        if (beam == null) beam = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        beam.bind();
        beam.upload(rendered);
        return beam;
    }

    public static BufferBuilder beginBeam() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_TEX);
        return bb;
    }

    private static VertexBuffer upload(BufferBuilder bb) {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(bb.end());
        VertexBuffer.unbind();
        return vb;
    }
}
