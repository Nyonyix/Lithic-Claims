package com.nyonyix.lithicclaims.server;

import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import com.nyonyix.lithicclaims.data.datagen.lang.LithicClaimsLanguageProvider;
import com.nyonyix.lithicclaims.data.datagen.tag.LithicClaimsBlockTagProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LithicClaims.MODID)
public class LithicClaimsServer
{
    @SubscribeEvent
    static void gatherData(GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> provider = event.getLookupProvider();

        gen.addProvider(event.includeServer(), new LithicClaimsBlockTagProvider(packOutput, provider, event.getExistingFileHelper()));

        gen.addProvider(event.includeClient(), new LithicClaimsLanguageProvider(packOutput));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!event.getEntity().isShiftKeyDown()) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (!player.getMainHandItem().isEmpty()) return;
        if (player.isSpectator()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            player.sendSystemMessage(Component.literal("Hello").withStyle(ChatFormatting.AQUA));
            event.setCanceled(true);
        }
    }
}
