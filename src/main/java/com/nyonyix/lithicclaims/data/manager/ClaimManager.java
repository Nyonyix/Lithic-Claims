package com.nyonyix.lithicclaims.data.manager;

import com.nyonyix.lithicclaims.server.ServerConfig;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.ClaimAttachment;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.TeamAttachment;
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

import java.time.Instant;
import java.util.*;

public class ClaimManager
{
    private static boolean isProtected(Level level, Claim claim)
    {
        Team team = TeamManager.getTeam(level, claim.owner());
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

    public static Map<BlockPos, Claim> getActiveClaims(Level level)
    {
        Map<BlockPos, Claim> active = level.getData(LithicClaimsAttachments.CLAIM_ATTACHMENT).activeClaims();
        return active != null ? active : Map.of();
    }

    public static Claim getClaim(Level level, BlockPos pos)
    {
        return getActiveClaims(level).getOrDefault(pos, Claim.createDefault());
    }

    public static Claim getClaimContains(Level level, BlockPos pos)
    {
        for (Claim claim : getActiveClaims(level).values())
        {
            if (claim.claimArea().contains(Vec3.atCenterOf(pos))) return claim;
        }

        return Claim.createDefault();
    }

    public static void saveAttachment(Level level, Claim claim)
    {
        Map<BlockPos, Claim> activeClaims = new HashMap<>(getActiveClaims(level));
        activeClaims.put(claim.location(), claim);
        level.setData(LithicClaimsAttachments.CLAIM_ATTACHMENT, new ClaimAttachment(activeClaims));
    }

    public static void saveAttachment(Level level, Map<BlockPos, Claim> activeClaims)
    {
        level.setData(LithicClaimsAttachments.CLAIM_ATTACHMENT, new ClaimAttachment(activeClaims));
    }

    public static void removeClaim(Level level, BlockPos pos)
    {
        removeClaim(level, getClaim(level, pos));
    }

    public static void removeClaim(Level level, Claim claim)
    {
        Map<BlockPos, Claim> activeClaims = new HashMap<>(getActiveClaims(level));

        activeClaims.remove(claim.location());
        saveAttachment(level, activeClaims);
    }

    public static void trigger(Level level, BlockPos pos, Player player)
    {
        Map<BlockPos, Claim> activeClaims = getActiveClaims(level);
        float claimArea = (float) ServerConfig.CLAIM_AREA.getAsDouble();

        if (!activeClaims.isEmpty())
        {
            for (Claim claim : activeClaims.values())
            {
                if (claim.location().equals(pos))
                {
                    if (isProtected(level, claim)) return;
                    if (TeamManager.isInTeam(level, player.getUUID(), claim.owner())) return;

                    Map<UUID, Team> activeTeams = new HashMap<>(TeamManager.getActiveTeams(level));
                    Team team = activeTeams.get(claim.owner());
                    List<UUID> members = new ArrayList<>(team.members());
                    members.add(player.getUUID());
                    activeTeams.put(team.id(), team.withMembers(members));
                    level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));

                    player.displayClientMessage(Component.translatable("lithicclaims.claim.addMember").withStyle(ChatFormatting.DARK_GREEN), true);
                    return;
                }
                else if (Vec3.atCenterOf(claim.location()).distanceToSqr(Vec3.atCenterOf(pos)) <= claimArea * claimArea)
                {
                    player.displayClientMessage(Component.translatable("lithicclaims.claim.overlap").withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }

        Team team = TeamManager.getTeamByPlayer(level, player.getUUID());
        if (team.stance() == Stance.INVALID)
        {
            team = TeamManager.createTeam(level, "New Auto Team", player, Stance.NEUTRAL, 0xFFFFFF);
        }

        long calendarTicks = 0L;
        if (ModList.get().isLoaded("tfc"))
        {
            calendarTicks = Calendars.get(level).getCalendarTicks();
        }

        double claimRadius = (double) claimArea / 2;
        Vec3 min = new Vec3(pos.getX() - claimRadius, pos.getY() - claimRadius,pos.getZ() - claimRadius);
        Vec3 max = new Vec3(pos.getX() + claimRadius, pos.getY() + claimRadius, pos.getZ() + claimRadius);

        Claim newClaim = new Claim(new AABB(min, max), team.id(), pos, calendarTicks);
        saveAttachment(level, newClaim);
        player.displayClientMessage(Component.translatable("lithicclaims.claim.createClaim").withStyle(ChatFormatting.DARK_GREEN), true);

        Map<UUID, Team> activeTeams = new HashMap<>(TeamManager.getActiveTeams(level));
        List<Claim> ownedClaims = new ArrayList<>(team.ownedClaims());
        ownedClaims.add(newClaim);
        team = team.withOwnedClaims(ownedClaims);
        activeTeams.put(team.id(), team);
        level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));
    }

    public static void claimCleanUp(Level level, BlockPos pos)
    {
        claimCleanUp(level, getClaim(level, pos));
    }

    public static void claimCleanUp(Level level, Claim claim)
    {
        Map<UUID, Team> activeTeams = new HashMap<>(TeamManager.getActiveTeams(level));
        Team team = TeamManager.getTeam(level, claim.owner());
        List<Claim> ownedClaims = new ArrayList<>(team.ownedClaims());

        removeClaim(level, claim);
        ownedClaims.remove(claim);
        team = team.withOwnedClaims(ownedClaims);
        activeTeams.put(team.id(), team);

       TeamManager.saveAttachment(level, activeTeams);
    }
}
