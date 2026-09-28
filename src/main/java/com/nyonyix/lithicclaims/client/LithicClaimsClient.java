package com.nyonyix.lithicclaims.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.client.render.ClaimAreaRender;
import com.nyonyix.lithicclaims.common.LithicClaimsCommon;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import javax.annotation.Nullable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

@Mod(value = LithicClaims.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = LithicClaims.MODID, value = Dist.CLIENT)
public class LithicClaimsClient
{
    public LithicClaimsClient(ModContainer container)
    {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        ClaimAreaRender.INSTANCE.handleInput();
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if(!(mc.hitResult instanceof BlockHitResult hit)) return;
        if (!event.isAttack()) return;
        if (!LithicClaimsCommon.isDenied(mc.level, hit.getBlockPos(), mc.player)) return;

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.level.getBlockState(event.getTarget().getBlockPos()).is(LithicClaimsTags.Blocks.CLAIM_USE_EXCEPTION) || mc.level.getBlockState(event.getTarget().getBlockPos()).is(LithicClaimsTags.Blocks.CLAIM_MARKERS)) return;
        if (!LithicClaimsCommon.isDenied(mc.level, event.getTarget().getBlockPos(), mc.player)) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        ClaimAreaRender.INSTANCE.render(event);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(ClaimAreaRender.TOGGLE_KEY);
    }
}
