package com.nyonyix.lithicclaims.data.manager;

import com.nyonyix.lithicclaims.server.ServerConfig;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.ClaimAttachment;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.TeamAttachment;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import it.unimi.dsi.fastutil.Pair;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.*;

public class ClaimManager
{
    private static Map<ResourceKey<Level>, Map<BlockPos, Integer>> canAcceptMember = new HashMap<>();
    private static Map<ResourceKey<Level>, Map<BlockPos, Boolean>> isProtected = new HashMap<>();

    public static boolean getIsProtected(Level level, Claim claim)
    {
        return isProtected.getOrDefault(level.dimension(), Map.of(claim.location(), false)).getOrDefault(claim.location(), false);
    }

    public static boolean isProtected(Level level, Claim claim)
    {
        Team ownerTeam = TeamManager.getTeam(level, claim.owner());
        float wanderDist = (float) ServerConfig.PROTECTION_MEMBER_DIST_WANDER.getAsDouble();
        float hostileDist = (float) ServerConfig.PROTECTION_MEMBER_DIST_HOSTILE.getAsDouble();
        float hostileClaimDist = (float) ServerConfig.PROTECTION_CLAIM_DIST_HOSTILE.getAsDouble();
        float memberCountPercent = (float) ServerConfig.PROTECTION_MEMBER_COUNT_PERCENT.getAsDouble();

        if (TeamManager.percentMembersOnline(level, ownerTeam.id()) < memberCountPercent) return true; // If x% is online
        if (ownerTeam.stance().equals(Stance.HOSTILE)) return false; // If owner is hostile
        if (ownerTeam.stance().equals(Stance.PEACEFUL)) return true; // if owner is peaceful
        if (TeamManager.percentMembersNearVec(level, Vec3.atCenterOf(claim.location()), ownerTeam, wanderDist) >= memberCountPercent) return false; // if members are x distance from the claim.

        Map<BlockPos, Claim> activeClaims = getActiveClaims(level);
        for (Claim listClaim : activeClaims.values())
        {
            if (!TeamManager.getTeam(level, listClaim.owner()).stance().equals(Stance.HOSTILE)) continue;
            if (TeamManager.percentMembersNearVec(level, Vec3.atCenterOf(listClaim.location()), TeamManager.getTeam(level, ownerTeam.id()), hostileDist) >= memberCountPercent) return false; // if members are x distance to a hostile claim

            float claimDist = (float) Vec3.atCenterOf(claim.location()).distanceToSqr(Vec3.atCenterOf(listClaim.location()));
            if (claimDist <= hostileClaimDist * hostileClaimDist) return false; // if a hostile claim is x distance near claim
        }

        return true;
    }

    public static boolean isInClaim(Claim claim, BlockPos pos)
    {
        return claim.claimArea().contains(Vec3.atCenterOf(pos));
    }

    public static boolean isInClaim(Level level, BlockPos pos)
    {
        for (Claim claim : getActiveClaims(level).values())
        {
            if (claim.claimArea().contains(Vec3.atCenterOf(pos)))
            {
                return true;
            }
        }

        return false;
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
        AABB newClaimArea = AABB.ofSize(Vec3.atCenterOf(pos), claimArea, claimArea, claimArea);

        if (!activeClaims.isEmpty())
        {
            for (Claim claim : activeClaims.values())
            {
                if (claim.location().equals(pos)) // if claim exists
                {
                    if (player.getUUID().equals(TeamManager.getTeam(level, claim.owner()).leader())) // if clicker is leader
                    {
                        canAcceptMember.put(level.dimension(), canAcceptMember.getOrDefault(level.dimension(), new HashMap<>())).put(pos, level.getServer().getTickCount());

                        player.displayClientMessage(Component.translatable("lithicclaims.claim.leaderClick").withStyle(ChatFormatting.DARK_GREEN), true);
                        return;
                    }
                    else if (!TeamManager.isInTeam(level, player.getUUID(), claim.owner())) // if clicker is not owner and not in claim team
                    {
                        if (!canAcceptMember.containsKey(level.dimension()) || !canAcceptMember.get(level.dimension()).containsKey(pos)) // if canAcceptMembers has the marker in its memory
                        {
                            player.displayClientMessage(Component.translatable("lithicclaims.claim.addMemberNeedLeaderClick").withStyle(ChatFormatting.RED), true);
                            return;
                        }

                        int addMemberTimeout = ServerConfig.ADD_MEMBER_TIMEOUT.getAsInt();
                        if (canAcceptMember.get(level.dimension()).get(pos) >= level.getServer().getTickCount() - addMemberTimeout) // if the timeout has yet to expire
                        {
                            TeamManager.removeMember(level, player.getUUID());
                            TeamManager.addMember(level, player.getUUID(), claim.owner());

                            player.displayClientMessage(Component.translatable("lithicclaims.claim.addMember").withStyle(ChatFormatting.DARK_GREEN), true);
                            return;
                        }
                        else
                        {
                            player.displayClientMessage(Component.translatable("lithicclaims.claim.addMemberNeedLeaderClick").withStyle(ChatFormatting.RED), true);
                            return;
                        }
                    }
                    else
                    {
                        player.displayClientMessage(Component.translatable("lithicclaims.claim.inTeam").withStyle(ChatFormatting.RED), true);
                        return;
                    }
                }
                else if (newClaimArea.intersects(claim.claimArea()) && claim.owner().equals(Team.ZERO_UUID)) // if claims are placed too close together
                {
                    player.displayClientMessage(Component.translatable("lithicclaims.claim.overlap").withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }

        Team team = TeamManager.getTeamByPlayer(level, player.getUUID());
        String defaultTeamName = "New Team";
        if (team.id().equals(Team.ZERO_UUID))
        {
            team = TeamManager.createTeam(level, defaultTeamName, player, Stance.NEUTRAL, 0xFFFFFF);
        }

        long calendarTicks = 0L;
        if (ModList.get().isLoaded("tfc"))
        {
            calendarTicks = Calendars.get(level).getCalendarTicks();
        }

        Claim claim = new Claim(newClaimArea, team.id(), pos, calendarTicks);
        saveAttachment(level, claim);
        TeamManager.addClaim(level, team, claim);
        player.displayClientMessage(Component.translatable("lithicclaims.claim.createClaim").withStyle(ChatFormatting.DARK_GREEN), true);

    }

    public static void onTick(Level level)
    {
        Map<BlockPos, Claim> activeClaims = getActiveClaims(level);

        for (Claim claim : activeClaims.values())
        {
            if (claim.owner().equals(Team.ZERO_UUID))
            {
                removeClaim(level, claim);
                canAcceptMember.get(level.dimension()).remove(claim.location());
                isProtected.get(level.dimension()).remove(claim.location());
                continue;
            }

            if (!isProtected.containsKey(level.dimension()))
            {
                Map<BlockPos, Boolean> claimProtection = new HashMap<>();
                claimProtection.put(claim.location(), isProtected(level, claim));
                isProtected.put(level.dimension(), claimProtection);
            }
            else
            {
                Map<BlockPos, Boolean> claimProtection = new HashMap<>();
                claimProtection.put(claim.location(), isProtected(level, claim));
                isProtected.put(level.dimension(), claimProtection);
            }

            // Do other stuff
        }
    }

    public static void claimCleanUp(Level level, BlockPos pos)
    {
        Claim claim = getClaim(level, pos);
        if (!claim.owner().equals(Team.ZERO_UUID)) claimCleanUp(level, claim);
    }

    public static void claimCleanUp(Level level, Claim claim)
    {
        Map<UUID, Team> activeTeams = new HashMap<>(TeamManager.getActiveTeams(level));
        Team team = TeamManager.getTeam(level, claim.owner());

        if (!team.id().equals(Team.ZERO_UUID))
        {
            List<BlockPos> ownedClaims = new ArrayList<>(team.ownedClaims());
            ownedClaims.remove(claim.location());
            team = team.withOwnedClaims(ownedClaims);
            activeTeams.put(team.id(), team);
            TeamManager.saveAttachment(level, activeTeams);
        }

        removeClaim(level, claim);
    }
}
