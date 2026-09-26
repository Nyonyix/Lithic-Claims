package com.nyonyix.lithicclaims.client;

import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.common.LithicClaimsCommon;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.checkerframework.checker.signature.qual.SignatureBottom;

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
}
