package com.nyonyix.lithicclaims.data.manager;

import com.nyonyix.lithicclaims.ServerConfig;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.ClaimAttachment;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.*;;

public class ClaimManager
{
    private static boolean isProtected(Level level, Claim claim)
    {
        Team team = claim.owner();
        float wanderDist = (float) ServerConfig.PROTECTION_MEMBER_DIST_WANDER.getAsDouble();
        float hostileDist = (float) ServerConfig.PROTECTION_MEMBER_DIST_HOSTILE.getAsDouble();
        float hostileClaimDist = (float) ServerConfig.PROTECTION_CLAIM_DIST_HOSTILE.getAsDouble();
        float memberCountPercent = (float) ServerConfig.PROTECTION_MEMBER_COUNT_PERCENT.getAsDouble();

        if (team.stance() == Stance.PEACEFUL) return true;

        if (TeamManager.percentMembersOnline(level, team.id()) >= memberCountPercent) return false;
        if (TeamManager.percentMembersNearVec(level, Vec3.atCenterOf(claim.location()), team, wanderDist) >= memberCountPercent) return false;

        List<Team> hostileTeams = new ArrayList<>();
        team.relations().forEach((u, b) -> {if (b == Stance.HOSTILE) hostileTeams.add(TeamManager.getTeam(level, u));});

        for (Team hostileTeam : hostileTeams)
        {
            for (Claim hostileClaim : hostileTeam.ownedClaims())
            {
                if (TeamManager.percentMembersNearVec(level, Vec3.atCenterOf(hostileClaim.location()), team, hostileDist) >= memberCountPercent) return false;
                if (Vec3.atCenterOf(claim.location()).distanceToSqr(Vec3.atCenterOf(hostileClaim.location())) <= hostileClaimDist * hostileClaimDist && TeamManager.percentMembersOnline(level, team.id()) >= memberCountPercent) return false;
            }
        }

        return true;
    }

    private static void addToActive(Level level, Claim claim)
    {
        List<Claim> activeClaims = new ArrayList<>(getActiveClaims(level));
        activeClaims.add(claim);
        level.setData(LithicClaimsAttachments.CLAIM_ATTACHMENT, new ClaimAttachment(activeClaims));
    }

    public static void removeFromActive(Level level, Claim claim)
    {
        List<Claim> activeClaims = new ArrayList<>(getActiveClaims(level));
        activeClaims.remove(claim);
        level.setData(LithicClaimsAttachments.CLAIM_ATTACHMENT, new ClaimAttachment(activeClaims));
    }

    public static List<Claim> getActiveClaims(Level level)
    {
        List<Claim> active = level.getData(LithicClaimsAttachments.CLAIM_ATTACHMENT).activeClaims();
        return active != null ? active : List.of();
    }

    public static Claim getClaim(Level level, BlockPos pos)
    {
        for (Claim claim : level.getData(LithicClaimsAttachments.CLAIM_ATTACHMENT).activeClaims())
        {
            if (claim.location().equals(pos)) return claim;
        }

        return Claim.createDefault();
    }

    public static void trigger(Level level, BlockPos pos, Player player)
    {
        List<Claim> activeClaims = getActiveClaims(level);
        float claimArea = (float) ServerConfig.CLAIM_AREA.getAsDouble();

        if (!activeClaims.isEmpty())
        {
            for (Claim claim : activeClaims)
            {
                if (claim.location().equals(pos))
                {
                    if (isProtected(level, claim)) return;
                    if (TeamManager.isInTeam(level, player.getUUID(), claim.owner().id())) return;

                    TeamManager.addMember(level, player.getUUID(), claim.owner().id());
                    player.displayClientMessage(Component.translatable("lithicclaims.claim.addMember").withStyle(ChatFormatting.DARK_GREEN), true);
                    return;
                }
                else if (Vec3.atCenterOf(claim.location()).distanceToSqr(Vec3.atCenterOf(pos)) <= claimArea * claimArea)
                {
                    player.displayClientMessage(Component.translatable("lithicclaims.claim.overlap").withStyle(ChatFormatting.DARK_RED), true);
                    return;
                }
            }
        }

        Team team = TeamManager.getTeamByPlayer(level, player.getUUID());
        if (team.stance() == Stance.INVALID) team = TeamManager.newTeam(level, player.getUUID(), Stance.NEUTRAL, 0xFFFFFF);

        long calendarTicks = 0L;
        if (ModList.get().isLoaded("tfc"))
        {
            calendarTicks = Calendars.get(level).getCalendarTicks();
        }

        double claimRadius = (double) claimArea / 2;
        Vec3 min = new Vec3(pos.getX() - claimRadius, pos.getY() - claimRadius,pos.getZ() - claimRadius);
        Vec3 max = new Vec3(pos.getX() + claimRadius, pos.getY() + claimRadius, pos.getZ() + claimRadius);

        Claim newClaim = new Claim(new AABB(min, max), team, pos, calendarTicks);
        addToActive(level, newClaim);

        TeamManager.addClaim(level, newClaim, team.id());
        TeamManager.getTeam(level, team.id());
    }
}
