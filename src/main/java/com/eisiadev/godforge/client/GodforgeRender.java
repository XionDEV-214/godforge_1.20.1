package com.eisiadev.godforge.client;

import com.eisiadev.godforge.GodforgeMod;
import com.eisiadev.godforge.machine.ForgeOfGodsMachine;
import com.eisiadev.godforge.structure.GodforgeLayout;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.serialization.Codec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GodforgeRender extends DynamicRender<ForgeOfGodsMachine, GodforgeRender> {

    public static final Codec<GodforgeRender> CODEC = Codec.unit(GodforgeRender::new);
    public static final DynamicRenderType<ForgeOfGodsMachine, GodforgeRender> TYPE = new DynamicRenderType<>(CODEC);

    private static final ResourceLocation[] STAR_LAYERS = {
            ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "textures/model/star_layer_0.png"),
            ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "textures/model/star_layer_1.png"),
            ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "textures/model/star_layer_2.png")
    };
    private static final ResourceLocation SPACE_LAYER =
            ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "textures/model/space_layer.png");

    private static final float STAR_RADIUS = 20f;
    private static final float ROTATION_SPEED = 5f;
    private static final float COLOR_R = 179f / 255f, COLOR_G = 204f / 255f, COLOR_B = 1f;
    private static final float GAMMA = 3.0f;

    private static final float STAR_DEGREES_PER_TICK_PER_SPEED = 0.2f;

    private static final float BACK_PLATE_DISTANCE = -121.5f, BACK_PLATE_RADIUS = 13f;
    private static final int BEAM_QUADS = 16;

    public GodforgeRender() {}

    @Override
    public DynamicRenderType<ForgeOfGodsMachine, GodforgeRender> getType() {
        return TYPE;
    }

    private static boolean active(ForgeOfGodsMachine machine) {
        return machine.isFormed() && machine.isAnimating();
    }

    @Override
    public boolean shouldRender(ForgeOfGodsMachine machine, Vec3 cameraPos) {
        if (!active(machine)) return false;
        BlockPos star = machine.getStarCenterPos();
        return Vec3.atCenterOf(star).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(ForgeOfGodsMachine machine) {
        return active(machine);
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public AABB getRenderBoundingBox(ForgeOfGodsMachine machine) {
        AABB star = new AABB(machine.getStarCenterPos()).inflate(70);
        return star.minmax(new AABB(machine.getPos()));
    }

    @Override
    public void render(ForgeOfGodsMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!active(machine) || !GodforgeShaders.ready()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float timer = mc.level.getGameTime() + partialTick;
        int rings = Math.max(1, Math.min(3, machine.getRingCount()));

        Direction front = machine.getFrontFacing();
        Direction upwards = machine.getUpwardsFacing();
        boolean flipped = machine.isFlipped();
        Direction back = RelativeDirection.BACK.getRelative(front, upwards, flipped);
        Direction up = RelativeDirection.UP.getRelative(front, upwards, flipped);
        Direction left = RelativeDirection.LEFT.getRelative(front, upwards, flipped);

        poseStack.pushPose();
        poseStack.translate(
                0.5 + back.getStepX() * GodforgeLayout.STAR_DISTANCE,
                0.5 + back.getStepY() * GodforgeLayout.STAR_DISTANCE,
                0.5 + back.getStepZ() * GodforgeLayout.STAR_DISTANCE);
        poseStack.mulPoseMatrix(new Matrix4f(
                back.getStepX(), back.getStepY(), back.getStepZ(), 0,
                up.getStepX(), up.getStepY(), up.getStepZ(), 0,
                left.getStepX(), left.getStepY(), left.getStepZ(), 0,
                0, 0, 0, 1));

        Matrix4f frame = new Matrix4f(poseStack.last().pose());

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        try {
            RenderSystem.depthMask(true);
            drawStarLayer(frame, 0, 1.0f, STAR_RADIUS, new Vector3f(0, 1, 1), 130f, timer);

            drawRings(frame, rings, timer);

            RenderSystem.depthMask(false);
            drawStarLayer(frame, 1, 0.4f, STAR_RADIUS * 1.02f, new Vector3f(1, 1, 0), -49f, timer);
            drawStarLayer(frame, 2, 0.2f, STAR_RADIUS * 1.04f, new Vector3f(1, 0, 1), 67f, timer);

            drawBeams(frame, rings, timer);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    }

    private void drawStarLayer(Matrix4f frame, int layer, float alpha, float radius, Vector3f axis,
                               float baseDegrees, float timer) {
        ShaderInstance shader = GodforgeShaders.star();
        shader.getUniform("StarColor").set(COLOR_R, COLOR_G, COLOR_B, alpha);
        shader.getUniform("Gamma").set(GAMMA);

        float degrees = baseDegrees + (timer * ROTATION_SPEED * STAR_DEGREES_PER_TICK_PER_SPEED) % 360000f;
        Matrix4f mv = new Matrix4f(frame)
                .rotate((float) Math.toRadians(degrees), axis.normalize())
                .scale(radius);

        RenderSystem.setShaderTexture(0, STAR_LAYERS[layer]);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        draw(GodforgeMeshes.sphere(), shader, mv);
    }

    private void drawRings(Matrix4f frame, int rings, float timer) {
        ShaderInstance shader = GodforgeShaders.ring();
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawRing(frame, shader, 1, timer / 6f * 7f);
        if (rings > 1) drawRing(frame, shader, 2, -timer / 4f * 5f);
        if (rings > 2) drawRing(frame, shader, 3, timer * 3f);
    }

    private void drawRing(Matrix4f frame, ShaderInstance shader, int ring, float spinDegrees) {
        VertexBuffer mesh = GodforgeMeshes.ring(ring);
        if (mesh == null) return;
        Matrix4f mv = new Matrix4f(frame).rotateX((float) Math.toRadians(spinDegrees % 360f));
        draw(mesh, shader, mv);
    }

    private static float lensDistance(int id) {
        return switch (id) {
            case 0 -> -61.5f;
            case 1 -> -54.5f;
            default -> -44.5f;
        };
    }

    private static float lensRadius(int id) {
        return switch (id) {
            case 0 -> 1.1f;
            case 1 -> 3.5f;
            default -> 5f;
        };
    }

    private static float interpolate(float x0, float x1, float y0, float y1, float x) {
        return y0 + ((x - x0) * (y1 - y0)) / (x1 - x0);
    }

    private static float startAngle(int rings) {
        float x = -lensDistance(rings - 1);
        float y = lensRadius(rings - 1);
        float alpha = (float) Math.atan2(y, x);
        float beta = (float) Math.asin(STAR_RADIUS / Math.sqrt(x * x + y * y));
        return alpha + ((float) Math.PI / 2f - beta);
    }

    private static float[][] softBeam(int rings) {
        float angle = startAngle(rings);
        float radius = STAR_RADIUS * 1.1f;
        float startx = -radius * (float) Math.cos(angle);
        float starty = radius * (float) Math.sin(angle);

        float[][] pts = new float[rings + 2][];
        int n = 0;
        pts[n++] = new float[] { starty, startx, 0f };
        for (int i = rings - 1; i >= 0; i--) {
            pts[n++] = new float[] { lensRadius(i), lensDistance(i), 1f };
        }
        pts[n++] = new float[] { BACK_PLATE_RADIUS, BACK_PLATE_DISTANCE, -0.05f };
        if (n != pts.length) throw new IllegalStateException("softBeam points: " + n + "/" + pts.length);
        return pts;
    }

    private static float[][] intenseBeam(int rings) {
        float angle = startAngle(rings);
        float radius = STAR_RADIUS * 1.05f;
        float startx = -radius * (float) Math.cos(angle);
        float starty = radius * (float) Math.sin(angle);

        int firstLens = rings - 1;
        float nextx = lensDistance(firstLens);
        float nexty = lensRadius(firstLens) * 0.75f;

        float backx = Math.max(-radius, (nextx + radius) / 2f);
        float backy = interpolate(startx, nextx, starty, nexty, backx);

        float[][] pts = new float[rings + 3][];
        int n = 0;
        pts[n++] = new float[] { backy, backx, 0f };

        float transparency = 0.2f;
        for (int i = rings - 1; i >= 0; i--) {
            pts[n++] = new float[] { lensRadius(i) / 2f, lensDistance(i), transparency };
            transparency += 0.3f;
        }

        float currx = lensDistance(0);
        float curry = lensRadius(0) / 2f;
        float lastx = BACK_PLATE_DISTANCE;
        float lasty = Math.min(lensRadius(firstLens), BACK_PLATE_RADIUS);
        float midx = lastx + 8f;
        float midy = interpolate(currx, lastx, curry, lasty, midx);

        pts[n++] = new float[] { midy, midx, transparency };
        pts[n++] = new float[] { lasty, lastx, 0f };
        if (n != pts.length) throw new IllegalStateException("intenseBeam points: " + n + "/" + pts.length);
        return pts;
    }

    private void drawBeams(Matrix4f frame, int rings, float timer) {
        ShaderInstance shader = GodforgeShaders.beam();

        Vector3f cam = new Matrix4f(frame).invert().transformPosition(new Vector3f());
        float camLocalX = -cam.z, camLocalY = cam.y, camLocalZ = cam.x;
        float cameraAngle = (float) Math.atan2(camLocalY, camLocalX);

        RenderSystem.setShaderTexture(0, SPACE_LAYER);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        shader.getUniform("CameraPos").set(camLocalX, camLocalY, camLocalZ);

        shader.getUniform("BeamColor").set(COLOR_R, COLOR_G, COLOR_B);
        shader.getUniform("Intensity").set(2f);
        drawBeam(frame, shader, softBeam(rings), cameraAngle, timer);

        shader.getUniform("BeamColor").set(1f, 1f, 1f);
        shader.getUniform("Intensity").set(4f);
        drawBeam(frame, shader, intenseBeam(rings), cameraAngle, timer);
    }

    private void drawBeam(Matrix4f frame, ShaderInstance shader, float[][] pts, float cameraAngle, float timer) {
        BufferBuilder bb = GodforgeMeshes.beginBeam();
        final float pi = (float) Math.PI;
        float shift = cameraAngle - pi / 2f;
        float tcl = timer / 240f;

        for (int seg = 0; seg < pts.length - 1; seg++) {
            float[] p0 = pts[seg], p1 = pts[seg + 1];
            for (int quad = 0; quad < BEAM_QUADS; quad++) {
                for (int local = 0; local < 6; local++) {
                    int idOffset = (local > 1 && local < 5) ? 0 : 1;
                    float angle = (pi * (quad + idOffset)) / BEAM_QUADS + shift;

                    float[] p = (local > 0 && local < 4) ? p0 : p1;
                    float radius = p[0], offset = p[1], trans = p[2];

                    float lx = (float) Math.cos(angle) * radius;
                    float ly = (float) Math.sin(angle) * radius;

                    float heightOffset = offset / 256f + tcl;
                    float u = heightOffset;
                    float v = angle / (2f * pi) + heightOffset / 3f + tcl;

                    float encoded = Math.max(0f, Math.min(1f, (trans + 0.25f) / 1.5f));
                    bb.vertex(offset, ly, -lx).color(encoded, 0f, 0f, 1f).uv(u, v).endVertex();
                }
            }
        }
        VertexBuffer mesh = GodforgeMeshes.uploadBeam(bb.end());
        mesh.drawWithShader(frame, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
    }

    private static void draw(VertexBuffer mesh, ShaderInstance shader, Matrix4f modelView) {
        mesh.bind();
        mesh.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
    }
}
