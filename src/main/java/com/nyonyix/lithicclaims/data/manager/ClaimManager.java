package com.nyonyix.lithicclaims.data.manager;

import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.ServerConfig;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;;

public class ClaimManager
{
    private static Map<BlockPos, Claim> addToActive() {}

    private static boolean isProtected(Level level, Claim claim)
    {
        Team team = claim.owner();
        float wanderDist = (float) ServerConfig.PROTECTION_MEMBER_DIST_WANDER.getAsDouble();
        float hostileDist = (float) ServerConfig.PROTECTION_MEMBER_DIST_HOSTILE.getAsDouble();
        float hostileClaimDist = (float) ServerConfig.PROTECTION_CLAIM_DIST_HOSTILE.getAsDouble();
        float memberCountPercent = (float) ServerConfig.PROTECTION_MEMBER_COUNT_PERCENT.getAsDouble();

        if (team.stance() == 0x1) return true;

        if (TeamManager.percentMembersOnline(level, team.id()) >= memberCountPercent) return false;
        if (TeamManager.percentMembersNearVec(level, Vec3.atCenterOf(claim.location()), team, wanderDist) >= memberCountPercent) return false;

        List<Team> hostileTeams = new ArrayList<>();
        team.relations().forEach((u, b) -> {if (b == -0x1) hostileTeams.add(TeamManager.getTeam(level, u));});

        for (Team hostileTeam : hostileTeams)
        {
            for (Claim hostileClaim : hostileTeam.ownedClaims())
            {
                if (TeamManager.percentMembersNearVec(level, Vec3.atCenterOf(hostileClaim.location()), team, hostileDist) >= memberCountPercent) return false;
                if (Vec3.atCenterOf(claim.location()).distanceToSqr(Vec3.atCenterOf(hostileClaim.location())) >= hostileClaimDist * hostileClaimDist && TeamManager.percentMembersOnline(level, team.id()) >= memberCountPercent) return false;
            }
        }

        return true;
    }

    public static Map<BlockPos, Claim> getActiveClaims(Level level)
    {
        Map<BlockPos, Claim> active = level.getData(LithicClaimsAttachments.CLAIM_ATTACHMENT).activeClaims();
        return active != null ? active : Map.of();
    }

    public static Claim getClaim(Level level, BlockPos pos)
    {
        return level.getData(LithicClaimsAttachments.CLAIM_ATTACHMENT).activeClaims().getOrDefault(pos, Claim.createDefault());
    }

    public static void init(Level level, BlockPos pos, Player player)
    {
        Map<BlockPos, Claim> activeClaims = getActiveClaims(level);
        float claimArea = (float) ServerConfig.CLAIM_AREA.getAsDouble();

        if (!activeClaims.isEmpty())
        {
            for (Claim claim : activeClaims.values())
            {
                float overlapDist = claimArea * 2;

                if (claim.location().equals(pos))
                {
                    if (isProtected(level, claim)) return;

                    //addMember
                }
                else if (Vec3.atCenterOf(claim.location()).distanceToSqr(Vec3.atCenterOf(pos)) <= overlapDist * overlapDist)
                {
                    player.displayClientMessage(Component.translatable("lithicclaims.claim.overlap").withStyle(ChatFormatting.DARK_RED), false);
                    return;
                }
            }
        }

        double claimRadius = (double) claimArea / 2;
        Vec3 min = new Vec3(pos.getX() - claimRadius, pos.getY() - claimRadius,pos.getZ() - claimRadius);
        Vec3 max = new Vec3(pos.getX() + claimRadius, pos.getY() + claimRadius, pos.getZ() + claimRadius)

        Claim newClaim = new Claim(new AABB(min, max), );
    }
}
