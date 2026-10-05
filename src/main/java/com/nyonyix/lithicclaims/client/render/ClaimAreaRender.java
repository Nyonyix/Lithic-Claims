package com.nyonyix.lithicclaims.client.render;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.manager.TeamManager;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ClaimAreaRender
{
    private static final float GROW_PER_TICK = 1.0f / 15.0f;
    private static final float LINGER_TICKS = 60.0f;
    private final Map<BlockPos, AreaAnimation> animations = new Hashtable<>();
    private boolean showAll = false;

    private static final class AreaAnimation
    {
        private final AABB from;
        private final AABB to;
        private float progress;
        private float linger;

        private AreaAnimation(AABB from, AABB to)
        {
            this.from = from;
            this.to = to;
        }
    }

    public static final ClaimAreaRender INSTANCE =  new ClaimAreaRender();
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("lithicclaims.key.toggle_claim_areas", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_ALT, "lithicclaims.key.categories");

    private ClaimAreaRender() {}

    @Nullable
    private static BlockPos getHoveredMarker(Minecraft mc)
    {
        if (mc.screen != null) return null;
        if (!(mc.hitResult instanceof BlockHitResult hit)) return null;

        BlockPos pos = hit.getBlockPos();
        return mc.level.getBlockState(pos).is(LithicClaimsTags.Blocks.CLAIM_MARKERS) ? pos : null;
    }

    @Nullable
    private static AreaAnimation createAreaAnimation(Minecraft mc, BlockPos pos)
    {
        Claim claim = ClaimManager.getClaim(mc.level, pos);
        if (claim.owner().equals(Team.ZERO_UUID)) return null;

        AABB to = claim.claimArea();
        AABB from = new AABB(pos);
        if (to.getSize() < 1.0E-4) return null;

        if (mc.level.isLoaded(pos))
        {
            Entity cameraEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
            VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos, CollisionContext.of(cameraEntity));

            if (!shape.isEmpty()) from = shape.bounds().move(pos);
        }

        return new AreaAnimation(from, to);
    }

    private Set<BlockPos> activeTargets(Minecraft mc, @Nullable BlockPos hovered)
    {
        if (this.showAll) return ClaimManager.getActiveClaims(mc.level).keySet();

        return hovered != null ? Set.of(hovered) : Set.of();
    }

    public boolean isShowingAll()
    {
        return showAll;
    }

    public void handleInput()
    {
        while (TOGGLE_KEY.consumeClick())
        {
            this.showAll = !this.showAll;

            if (!this.showAll) this.animations.values().forEach(anim -> anim.linger = 0.0f);
        }
    }

    public void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        if (mc.level == null || mc.player == null || poseStack == null) return;

        BlockPos hovered = getHoveredMarker(mc);
        Set<BlockPos> active = activeTargets(mc, hovered);

        for (BlockPos pos : active)
        {
            this.animations.computeIfAbsent(pos, p -> createAreaAnimation(mc, p));
        }

        if (this.animations.isEmpty()) return;

        float delta = event.getPartialTick().getRealtimeDeltaTicks();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

        for (Iterator<Map.Entry<BlockPos, AreaAnimation>> it = this.animations.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry<BlockPos, AreaAnimation> entry = it.next();
            AreaAnimation anim = entry.getValue();
            boolean isActive = active.contains(entry.getKey());

            if (isActive)
            {
                anim.progress = Math.clamp(anim.progress + delta * GROW_PER_TICK, 0.0f, 1.0f);

                anim.linger = anim.progress >= 1.0f ? LINGER_TICKS : 0.0f;
            }
            else
            {
                anim.linger -= delta;

                if (anim.linger <= 0.0f)
                {
                    anim.progress = Math.clamp(anim.progress + -delta * GROW_PER_TICK, 0.0f, 1.0f);

                    if (anim.progress <= 0.0f)
                    {
                        it.remove();
                        continue;
                    }
                }
            }

            if (!event.getFrustum().isVisible(anim.to)) continue;

            float t = anim.progress * anim.progress * (3.0f - 2.0f * anim.progress);

            AABB box = new AABB(
                    Mth.lerp(t, anim.from.minX, anim.to.minX),
                    Mth.lerp(t, anim.from.minY, anim.to.minY),
                    Mth.lerp(t, anim.from.minZ, anim.to.minZ),
                    Mth.lerp(t, anim.from.maxX, anim.to.maxX),
                    Mth.lerp(t, anim.from.maxY, anim.to.maxY),
                    Mth.lerp(t, anim.from.maxZ, anim.to.maxZ)
            ).move(-cam.x, -cam.y, -cam.z);

            float alpha = anim.progress;

            int teamColour = TeamManager.getTeam(mc.level, ClaimManager.getClaim(mc.level, entry.getKey()).owner()).colour();
            float r = FastColor.ARGB32.red(teamColour) / 255.0f;
            float g = FastColor.ARGB32.green(teamColour) / 255.0f;
            float b = FastColor.ARGB32.blue(teamColour) / 255.0f;

            LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, alpha);
        }

        buffer.endBatch(RenderType.lines());
    }
}
