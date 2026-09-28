package com.nyonyix.lithicclaims.common;

import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.manager.TeamManager;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = LithicClaims.MODID)
public class LithicClaimsCommon
{
    public static boolean isDenied(Level level, BlockPos pos, @Nullable Entity entity)
    {
        Claim claim  = ClaimManager.getClaimContains(level, pos);

        if (claim.owner().equals(Team.ZERO_UUID)) return false;
        if (!claim.isProtected()) return false;

        return !(entity instanceof Player player && TeamManager.isInTeam(level, player.getUUID(), claim.owner()));
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (!isDenied(event.getLevel(), event.getPos(), event.getEntity())) return;

        event.setCanceled(true);

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD)
        {
            event.setUseItem(TriState.FALSE);
        }
    }
     @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
     {
         event.getPosition().ifPresent(pos ->
         {
             if (isDenied(event.getEntity().level(), pos, event.getEntity()))
             {
                 event.setCanceled(true);
             }
         });
     }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        Player player = event.getEntity();
        if (player.isSpectator()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(LithicClaimsTags.Blocks.CLAIM_MARKERS))
        {
            if (!level.isClientSide() && event.getHand() == InteractionHand.MAIN_HAND)
            {
                event.setUseItem(TriState.FALSE);
                ClaimManager.trigger(level, pos, player);
            }
            return;
        }

        if (!isDenied(level, pos, player)) return;

        if (state.is(LithicClaimsTags.Blocks.CLAIM_USE_EXCEPTION) && !player.isShiftKeyDown())
        {
            event.setUseItem(TriState.FALSE);
            return;
        }


        event.setCanceled(true);
        if (level.isClientSide())
        {
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
    {
        if (!isDenied(event.getLevel(), event.getPos(), event.getEntity())) return;

        event.setCanceled(true);
        if (event.getLevel().isClientSide())
        {
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (!isDenied(event.getLevel(), event.getPos(), event.getEntity())) return;

        event.setCanceled(true);
        if (event.getLevel().isClientSide())
        {
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event)
    {
        if (!isDenied(event.getLevel(), event.getPos(), event.getEntity())) return;

        event.setCanceled(true);
        if (event.getLevel().isClientSide())
        {
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event)
    {
        if (!isDenied(event.getEntity().level(), event.getTarget().blockPosition(), event.getEntity())) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event)
    {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;

        Claim claim = ClaimManager.getClaim(level, event.getPos());
        if (claim.owner().equals(Team.ZERO_UUID)) return;
        if (level.getBlockState(claim.location()).is(LithicClaimsTags.Blocks.CLAIM_MARKERS)) return;

        ClaimManager.claimCleanUp(level, claim);
    }
}
