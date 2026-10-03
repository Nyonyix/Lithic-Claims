package com.nyonyix.lithicclaims.server;

import com.mojang.logging.LogUtils;
import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.command.LithicClaimsCommands;
import com.nyonyix.lithicclaims.common.LithicClaimsCommon;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.PlayerAttachment;
import com.nyonyix.lithicclaims.data.datagen.lang.LithicClaimsLanguageProvider;
import com.nyonyix.lithicclaims.data.datagen.tag.LithicClaimsBlockTagProvider;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.manager.PlayerManager;
import com.nyonyix.lithicclaims.data.manager.TeamManager;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.dries007.tfc.util.events.StartFireEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
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
    public static void onServerTick(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels())
        {
            if (server.getTickCount() % 20 == 0)
            {
                ClaimManager.onTick(level);
                TeamManager.onTick(level);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        LithicClaimsCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        Level level = event.getPlayer().level();
        BlockPos pos = event.getPos();
        Player player =event.getPlayer();

        if (LithicClaimsCommon.isDenied(level, pos, event.getPlayer()))
        {
            event.setCanceled(true);
            return;
        }
        else if (!ClaimManager.getClaimContains(level, pos).owner().equals(TeamManager.getTeamByPlayer(level, player.getUUID())))
        {
            PlayerAttachment data = PlayerManager.getPlayerData(player);
            PlayerManager.saveAttachment(player, data.withLastAggressive(Instant.now()));
        }

        if (event.getState().is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            ClaimManager.claimCleanUp(event.getPlayer().level(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onEntityPlaceEvent(BlockEvent.EntityPlaceEvent event)
    {
        if (!LithicClaimsCommon.isDenied((Level) event.getLevel(), event.getPos(), event.getEntity())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityMultiPlaceEvent(BlockEvent.EntityMultiPlaceEvent event)
    {
        if (!LithicClaimsCommon.isDenied((Level) event.getLevel(), event.getPos(), event.getEntity())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event)
    {
        if (!LithicClaimsCommon.isDenied((Level) event.getLevel(), event.getPos(), event.getEntity())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockToolModificationEvent(BlockEvent.BlockToolModificationEvent event)
    {
        if (!LithicClaimsCommon.isDenied((Level) event.getLevel(), event.getPos(), event.getPlayer())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
        Level level = event.getLevel();

        if (event.getExplosion().getIndirectSourceEntity() instanceof Player player)
        {
            PlayerAttachment data = PlayerManager.getPlayerData(player);
            PlayerManager.saveAttachment(player, data.withLastAggressive(Instant.now()));
        }

        List<BlockPos> effectedBlockPos = new ArrayList<>(event.getAffectedBlocks());
        for (BlockPos pos : effectedBlockPos)
        {
            if (LithicClaimsCommon.isDenied(level, pos, event.getExplosion().getIndirectSourceEntity()))
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

            if (LithicClaimsCommon.isDenied(level, entityPos, event.getExplosion().getIndirectSourceEntity()))
            {
                event.getAffectedEntities().remove(entity);
                continue;
            }
        }
    }

    @SubscribeEvent
    public static void onStartFire(StartFireEvent event)
    {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Direction face = event.getTargetedFace();

        if (LithicClaimsCommon.isDenied(level, pos, event.getPlayer()))
        {
            event.setCanceled(true);
            return;
        }

        if (face != null && LithicClaimsCommon.isDenied(level, pos.relative(face), event.getPlayer()))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event)
    {
        if (event.getEntity().level().isClientSide()) return;

        Team team = TeamManager.getTeamByPlayer(event.getEntity().level(), event.getEntity().getUUID());
        if (team.id().equals(Team.ZERO_UUID)) return;

        event.setDisplayname(event.getUsername().copy().withColor(team.colour()));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        PlayerAttachment data = PlayerManager.getPlayerData(player);

        List<UUID> debugUUIDs = List.of(UUID.fromString("db52851e-3851-4688-af6b-e71fb1069e43"), UUID.fromString("97bb311e-a082-4cd4-a082-fdcb318d6432"));
        if (debugUUIDs.contains(player.getUUID())) player.connection.disconnect(Component.literal("\"Unhandled exception. System.DllNotFoundException: Unable to load DLL 'null': The specified module could not be found. (0x8007007E)\""));

        if (data.stance().equals(Stance.INVALID))
        {
            PlayerManager.saveAttachment(player, data.withStance(Stance.NEUTRAL).withLastLogin(Instant.now()));
        }
        else
        {
            PlayerManager.saveAttachment(player, data.withLastLogin(Instant.now()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        Player player = event.getEntity();
        Team team = TeamManager.getTeamByPlayer(player.level(), player.getUUID());
        if (team.id().equals(Team.ZERO_UUID)) return;

        PlayerAttachment data = PlayerManager.getPlayerData(player);
        Instant sessionStart = data.lastLogin().isAfter(team.lastStanceChange()) ? data.lastLogin() : team.lastStanceChange();
        Duration spentOnline = Duration.between(sessionStart, Instant.now());
        Map<UUID, Duration> onlineTimes = new HashMap<>(team.onlineTimes());

        onlineTimes.merge(player.getUUID(), spentOnline, Duration::plus);
        PlayerManager.saveAttachment(player, data.withOnlineTime(data.onlineTime().plus(spentOnline)));
        TeamManager.saveAttachment(player.level(), team.withOnlineTimes(onlineTimes));
    }
}