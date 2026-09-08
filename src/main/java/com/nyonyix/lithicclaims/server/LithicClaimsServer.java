package com.nyonyix.lithicclaims.server;

import com.mojang.logging.LogUtils;
import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.command.LithicClaimsCommands;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.PlayerAttachment;
import com.nyonyix.lithicclaims.data.datagen.lang.LithicClaimsLanguageProvider;
import com.nyonyix.lithicclaims.data.datagen.tag.LithicClaimsBlockTagProvider;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.manager.TeamManager;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LithicClaims.MODID)
public class LithicClaimsServer
{
    public static final Logger LOGGER = LogUtils.getLogger();

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
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        LithicClaimsCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (player.isSpectator()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            ClaimManager.trigger(level, event.getPos(), player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        Level level = event.getPlayer().level();
        BlockPos pos = event.getPos();
        BlockState state =level.getBlockState(pos);

        if (state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            ClaimManager.claimCleanUp(event.getPlayer().level(), event.getPos());
        }
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
        LOGGER.info(event.getExplosion().getIndirectSourceEntity().toString());

        for (BlockPos pos : event.getAffectedBlocks())
        {
            ClaimManager.claimCleanUp(event.getLevel(), pos);
        }

        //Block Protections
    }

    public static void onAttackEntityEvent(AttackEntityEvent event)
    {
        if (event.getTarget() instanceof Player)
        {
            Player attacker = event.getEntity();
            Player victim = (Player) event.getTarget();
            Level level = event.getEntity().level();

            Team attackerTeam = TeamManager.getTeamByPlayer(level, attacker.getUUID());
            Team victimTeam = TeamManager.getTeamByPlayer(level, victim.getUUID());

            if (attackerTeam.stance().equals(Stance.PEACEFUL) || victimTeam.stance().equals(Stance.PEACEFUL))
            {
                event.setCanceled(true);
                return;
            }

            Instant attackTime = Instant.now();
            attacker.setData(LithicClaimsAttachments.PLAYER_ATTACHMENT, new PlayerAttachment(attackTime));
        }
    }
}
