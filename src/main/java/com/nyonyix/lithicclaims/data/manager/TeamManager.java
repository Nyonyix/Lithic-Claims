package com.nyonyix.lithicclaims.data.manager;

import com.mojang.logging.LogUtils;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.StanceChange;
import com.nyonyix.lithicclaims.data.TeamVote;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.PlayerAttachment;
import com.nyonyix.lithicclaims.data.attachment.TeamAttachment;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import com.nyonyix.lithicclaims.server.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TeamManager
{
    private static final Map<ResourceKey<Level>, Map<UUID, TeamVote>> teamVotes = new HashMap<>();

    public static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    public static TeamVote getTeamVotes(Level level, UUID teamID)
    {
        return teamVotes.getOrDefault(level.dimension(), Map.of()).get(teamID);
    }

    public static void putTeamVote(Level level, UUID teamID, TeamVote votes)
    {
        teamVotes.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(teamID, votes);
    }

    public static Duration teamStancePlayTime(Team team)
    {
        return team.onlineTimes().values().stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
    }

    public static Team getTeam(Level level, UUID uuid)
    {
        return level.getData(LithicClaimsAttachments.TEAM_ATTACHMENT).activeTeams().getOrDefault(uuid, Team.createDefault());
    }

    public static UUID getLeaderUUID(Level level, UUID teamUUID)
    {
        return getTeam(level, teamUUID).leader();
    }

    public static Team getTeamByName(Level level, String name)
    {
        Map<UUID, Team> activeTeams = getActiveTeams(level);
        if (activeTeams.isEmpty()) return Team.createDefault();

        for (Team team : activeTeams.values())
        {
            if (team.name().equalsIgnoreCase(name))
            {
                return team;
            }
        }

        return Team.createDefault();
    }

    public static Map<UUID, Team> getActiveTeams(Level level)
    {
        Map <UUID, Team> activeTeams = level.getData(LithicClaimsAttachments.TEAM_ATTACHMENT).activeTeams();
        return activeTeams != null ? activeTeams : Map.of();
    }

    public static Team getTeamByPlayer(Level level, UUID playerUUID)
    {
        Map<UUID, Team> activeTeams = getActiveTeams(level);
        if (activeTeams.isEmpty()) return Team.createDefault();

        for (Map.Entry<UUID, Team> entry : activeTeams.entrySet())
        {
            if (entry.getValue().members().contains(playerUUID))
            {
                return entry.getValue();
            }
        }

        return Team.createDefault();
    }

    public static Team createTeam(Level level, String name, Player player, Stance stance, int colour)
    {
        UUID teamUUID = UUID.randomUUID();
        UUID playerUUID = player.getUUID();
        List<UUID> members = List.of(playerUUID);

        Map<UUID, Team> activeTeams = getActiveTeams(level);
        while (true)
        {
            boolean match = false;
            for (Team iterTeam : activeTeams.values())
            {
                if (iterTeam.name().equalsIgnoreCase(name))
                {
                    match = true;
                    break;
                }
            }

            if (!match) break;

            int i = name.length();
            while (i > 0 && Character.isDigit(name.charAt(i - 1))) i--;
            String prefix = name.substring(0, i);
            String suffix = name.substring(i);
            name = suffix.isEmpty() ? prefix + "1" : prefix + (Long.parseLong(suffix) + 1);
        }

        Team team = new Team(teamUUID, playerUUID, name, new ArrayList<>(), members, new HashMap<>(), stance, colour, Instant.EPOCH);
        return team;
    }

    public static void saveAttachment(Level level, Team team)
    {
        Map<UUID, Team> activeTeams = new HashMap<>(getActiveTeams(level));
        activeTeams.put(team.id(), team);

        level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));
        refreshMemberNames(level, team);
    }

    public static void saveAttachment(Level level, Map<UUID, Team> activeTeams)
    {
        level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));
    }

    public static void refreshMemberNames(Level level, Team team)
    {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        if (team.id().equals(Team.ZERO_UUID)) return;

        for (UUID playerUUID : team.members())
        {
            ServerPlayer member = server.getPlayerList().getPlayer(playerUUID);
            if (member == null) continue;

            member.refreshDisplayName();
            member.refreshTabListName();
        }
    }

    public static void refreshName(Level level, UUID playerUUID)
    {
        MinecraftServer server = level.getServer();
        if (server == null) return;

        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
        if (player == null) return;

        player.refreshTabListName();
        player.refreshDisplayName();
    }

    public static boolean isInTeam(Level level, UUID playerUUID, UUID teamUUID)
    {
        Team team = getTeam(level, teamUUID);
        if (team.stance() == Stance.INVALID) return false;
        return team.members().contains(playerUUID);
    }

    public static float percentMembersOnline(Level level, UUID uuid)
    {
        float playersNotNull = 0f;
        Team team = getTeam(level, uuid);
        if (team.stance() == Stance.INVALID) return 0.0f;

        for (UUID memberUUID : team.members())
        {
            Player member = level.getPlayerByUUID(memberUUID);
            if (member != null) playersNotNull++;
        }

        return playersNotNull / team.members().size();
    }

    public static float percentMembersNearVec(Level level, Vec3 point, Team team, float dist)
    {
        if (team.members().isEmpty()) return 0.0f;

        float numOfPlayerNear = 0;
        for (UUID uuid : team.members())
        {
            Player player = level.getPlayerByUUID(uuid);
            if (player != null && point.distanceToSqr(player.position()) < dist * dist)
            {
                numOfPlayerNear++;
            }
        }

        return numOfPlayerNear / team.members().size();
    }

    public static void teamCleanUp(Level level, UUID teamUUID)
    {
        teamCleanUp(level, getTeam(level, teamUUID));
    }

    public static void teamCleanUp(Level level, Team team)
    {
        Map<UUID, Team> activeTeams = new HashMap<>(getActiveTeams(level));
        if (activeTeams.isEmpty()) return;
        if (team.id().equals(Team.ZERO_UUID)) return;

        for (BlockPos claimLocation : team.ownedClaims())
        {
            ClaimManager.claimCleanUp(level, claimLocation);
        }

        activeTeams.remove(team.id());
        saveAttachment(level, activeTeams);

        for (UUID memberUUID : team.members())
        {
            refreshName(level, memberUUID);
        }
    }

    public static void addClaim(Level level, Team team, Claim claim)
    {
        List<BlockPos> claims = new ArrayList<>(team.ownedClaims());
        claims.add(claim.location());
        saveAttachment(level, team.withOwnedClaims(claims));
    }

    public static void addMember(Level level, Player player, Team team)
    {
        List<UUID> members = new ArrayList<>(team.members());

        Team playerTeam = getTeamByPlayer(level, player.getUUID());
        if (!playerTeam.id().equals(Team.ZERO_UUID))
        {
            removeMember(level, player.getUUID());
        }

        members.add(player.getUUID());
        saveAttachment(level, team.withMembers(members));
    }

    public static void addMember(Level level, UUID playerUUID, UUID teamUUID)
    {
        Player player = level.getPlayerByUUID(playerUUID);
        if (player == null) return;

        addMember(level, player, getTeam(level, teamUUID));
    }

    public static void removeMember(Level level, Player player)
    {
        removeMember(level, player.getUUID());
    }

    public static void removeMember(Level level, UUID playerUUID)
    {
       Team team = getTeamByPlayer(level, playerUUID);
       if (team.id().equals(Team.ZERO_UUID)) return;

       if (team.members().size() <= 1)
       {
           teamCleanUp(level, team);
           return;
       }

       List<UUID> members = new ArrayList<>(team.members());
       Map<UUID, Duration> onlineTimes = new HashMap<>(team.onlineTimes());

       members.remove(playerUUID);
       onlineTimes.remove(playerUUID);

       if (team.leader().equals(playerUUID))
       {
           team = team.withLeader(members.getFirst());
       }

       saveAttachment(level, team.withOnlineTimes(onlineTimes).withMembers(members));
       refreshName(level, playerUUID);
    }

    public static StanceChange changeTeamStance(Level level, Team team, Stance stance)
    {
        long coolDownMinutes = ServerConfig.TEAM_STANCE_COOLDOWN.getAsLong();
        Instant lastAggressive = mostRecentAggression(level, team);
        boolean neverChanged = team.lastStanceChange().equals(Instant.EPOCH);
        boolean neverAggressive = lastAggressive.equals(Instant.EPOCH);
        Duration durationSinceLastAggression = Duration.between(lastAggressive, Instant.now());
        if (team.stance().escalatesFrom(stance) && !neverChanged && teamStancePlayTime(team).compareTo(Duration.ofMinutes(coolDownMinutes)) < 0 && !neverAggressive && durationSinceLastAggression.compareTo(Duration.ofMinutes(coolDownMinutes)) < 0) return StanceChange.ON_COOLDOWN;

        if (team.stance() == Stance.NEUTRAL)
        {
            saveAttachment(level, team.withStance(stance).withStanceCooldown(Instant.now()).withOnlineTimes(new HashMap<>()));
            return StanceChange.SUCCESS;
        }

        if (stance.equals(Stance.NEUTRAL) && (team.stance().equals(Stance.HOSTILE) || team.stance().equals(Stance.PEACEFUL)))
        {
            saveAttachment(level, team.withStance(stance).withStanceCooldown(Instant.now()).withOnlineTimes(new HashMap<>()));
            return StanceChange.SUCCESS;
        }

        return StanceChange.NOT_ALLOWED;
    }

    public static Instant mostRecentAggression(Level level, Team team)
    {
        Instant mostRecent = Instant.EPOCH;

        for (UUID member : team.members())
        {
            Player player = level.getPlayerByUUID(member);
            if (player == null) continue;

            PlayerAttachment data = PlayerManager.getPlayerData(player);

            mostRecent = data.lastAggressive().isAfter(mostRecent) ?  data.lastAggressive() : mostRecent;
        }

        return mostRecent;
    }

    public static void onTick(Level level)
    {
        float memberVotePercent = (float) ServerConfig.MEMBER_VOTE_PERCENT.getAsDouble();
        int voteTicks = ServerConfig.TEAM_VOTE_TICKS.getAsInt();
        Set<UUID> toRemove = new HashSet<>();

        for (Map.Entry<UUID, TeamVote>  entry : teamVotes.getOrDefault(level.dimension(), Map.of()).entrySet())
        {
            if (entry.getValue().getStartTick() <= level.getServer().getTickCount() - voteTicks|| entry.getValue().getPercentageYay() >= memberVotePercent)
            {
                Team team = getTeam(level, entry.getKey());
                toRemove.add(team.id());

                if (entry.getValue().getPercentageYay() >= memberVotePercent)
                {
                    saveAttachment(level, team.withLeader(entry.getValue().getNewLeader()));

                    for (UUID member : team.members())
                    {
                        Player player = level.getPlayerByUUID(member);
                        if (player == null) continue;

                        player.displayClientMessage(Component.translatable("lithicclaims.command.team.newLeader").withStyle(ChatFormatting.GREEN), false);
                    }
                }
                else
                {
                    for (UUID member : team.members())
                    {
                        Player player = level.getPlayerByUUID(member);
                        if (player == null) continue;

                        player.displayClientMessage(Component.translatable("lithicclaims.command.team.newLeaderFail").withStyle(ChatFormatting.RED), false);
                    }
                }
            }
        }

        toRemove.forEach(uuid -> teamVotes.getOrDefault(level.dimension(), Map.of()).remove(uuid));
    }
}
