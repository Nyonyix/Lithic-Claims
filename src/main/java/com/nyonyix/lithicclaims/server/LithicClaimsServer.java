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
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LithicClaims.MODID)
public class LithicClaimsServer
{
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean deny(Level level, BlockPos pos, @Nullable Entity source)
    {
        Claim claim = ClaimManager.getClaimContains(level, pos);

        if (claim.owner().equals(Team.ZERO_UUID)) return false;
        if ((!ClaimManager.getIsProtected(level, claim))) return false;

        return !(source instanceof Player p && TeamManager.isInTeam(level, p.getUUID(), claim.owner()));
    }

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
    public static void onServerTick(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels())
        {
            if (server.getTickCount() % 20 == 0)
            {
                ClaimManager.onTick(level);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
//        LithicClaimsCommandsOld.register(event.getDispatcher());
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

        Claim claim  = ClaimManager.getClaimContains(level, pos);
        if (!state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS) && ClaimManager.getIsProtected(level, claim)  && !TeamManager.isInTeam(level, player.getUUID(), claim.owner()) && !claim.owner().equals(Team.ZERO_UUID))
        {
            event.setCanceled(true);
            return;
        }

        if (state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            ClaimManager.trigger(level, pos, player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        Level level = event.getPlayer().level();
        BlockPos pos = event.getPos();
        BlockState state =level.getBlockState(pos);
        Claim claim = ClaimManager.getClaimContains(level, pos);

        if (ClaimManager.getIsProtected(level, claim) && !TeamManager.isInTeam(level, event.getPlayer().getUUID(), claim.owner()) && !claim.owner().equals(Team.ZERO_UUID))
        {
            event.setCanceled(true);
            return;
        }

        if (state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            ClaimManager.claimCleanUp(event.getPlayer().level(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onPLayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        Claim claim = ClaimManager.getClaimContains(level, pos);

        if (ClaimManager.getIsProtected(level, claim) && !TeamManager.isInTeam(level, event.getEntity().getUUID(), claim.owner()))
        {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onEntityPlaceEvent(BlockEvent.EntityPlaceEvent event)
    {
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();

        if (deny(level, pos, event.getEntity()))
        {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onEntityMultiPlaceEvent(BlockEvent.EntityMultiPlaceEvent event)
    {
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();

        if (deny(level, pos, event.getEntity()))
        {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onBlockToolModificationEvent(BlockEvent.BlockToolModificationEvent event)
    {
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        Claim claim = ClaimManager.getClaimContains(level, pos);

        if (ClaimManager.getIsProtected(level, claim) && !TeamManager.isInTeam(level, event.getPlayer().getUUID(), claim.owner()))
        {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
        Level level = event.getLevel();

        List<BlockPos> effectedBlockPos = new ArrayList<>(event.getAffectedBlocks());
        for (BlockPos pos : effectedBlockPos)
        {
            Claim claim = ClaimManager.getClaimContains(level, pos);

            if (deny(level, pos, event.getExplosion().getIndirectSourceEntity()))
            {
                event.getAffectedBlocks().remove(pos);
                continue;
            }

            if (event.getLevel().getBlockState(pos).is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
            {
                ClaimManager.claimCleanUp(level, pos);
            }
        }

        List<Entity> effectedEntities = new ArrayList<>(event.getAffectedEntities());
        for (Entity entity : effectedEntities)
        {
            BlockPos entityPos = entity.blockPosition();
            Claim claim = ClaimManager.getClaimContains(level, entityPos);

            if (deny(level, entityPos, event.getExplosion().getIndirectSourceEntity()))
            {
                event.getAffectedEntities().remove(entity);
                continue;
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntityEvent(AttackEntityEvent event)
    {
        if (event.getTarget() instanceof Player)
        {

        }
    }
}
