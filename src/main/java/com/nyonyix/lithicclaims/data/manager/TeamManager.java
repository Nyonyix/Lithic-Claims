package com.nyonyix.lithicclaims.data.manager;

import com.mojang.logging.LogUtils;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.TeamAttachment;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;

public class TeamManager
{
    public static final Logger LOGGER = LogUtils.getLogger();

    // Add and Remove

    public static void addMember(Level level, UUID playerUUID, UUID teamUUID)
    {
        Team team = getTeam(level, teamUUID);
        if (team.stance() == Stance.INVALID)
        {
            LOGGER.warn("Unable to find team to add member: ", teamUUID.toString());
            return;
        }

        List<UUID> updatedMembers = new ArrayList<>(team.members());
        updatedMembers.add(playerUUID);
        addToActive(level, new Team(teamUUID, team.ownedClaims(), updatedMembers, team.relations(), team.stance(), team.colour()));
    }

    public static void addClaim(Level level, Claim claim, UUID teamUUID)
    {
        Team team = getTeam(level, teamUUID);
        if (team.stance() == Stance.INVALID)
        {
            LOGGER.warn("Unable to find team to add claim: ", teamUUID.toString());
            return;
        }

        List<Claim> updatedClaims = new ArrayList<>(team.ownedClaims());
        updatedClaims.add(claim);
        addToActive(level, new Team(teamUUID, updatedClaims, team.members(), team.relations(), team.stance(), team.colour()));
    }

    public static void removeClaim(Level level, Claim claim, UUID teamUUID)
    {
        Team team = getTeam(level, teamUUID);
        if (team.stance() == Stance.INVALID)
        {
            LOGGER.warn("Unable to find team to remove claim: ", teamUUID.toString());
            return;
        }

        team.ownedClaims().remove(claim);
        addToActive(level, team);
    }

    public static void addToActive(Level level, Team team)
    {
        Map<UUID, Team> activeTeams = new HashMap<>(getActiveTeams(level));
        activeTeams.put(team.id(), team);
        level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));
    }

    public static Team newTeam(Level level, UUID player, Stance stance, int colour)
    {
        UUID teamUUID = UUID.randomUUID();
        List<UUID> members = new ArrayList<>(List.of(player));

        Team team = new Team(teamUUID, new ArrayList<>(), members, Map.of(), stance, colour);
        addToActive(level, team);
        return team;
    }

    public static Team getTeam(Level level, UUID uuid)
    {
        return level.getData(LithicClaimsAttachments.TEAM_ATTACHMENT).activeTeams().getOrDefault(uuid, Team.createDefault());
    }

    public static Map<UUID, Team> getActiveTeams(Level level)
    {
        Map <UUID, Team> activeTeams = level.getData(LithicClaimsAttachments.TEAM_ATTACHMENT).activeTeams();
        return activeTeams != null ? activeTeams : Map.of();
    }

    public static boolean isInTeam(Level level, UUID playerUUID, UUID teamUUID)
    {
        Team team = getTeam(level, teamUUID);
        if (team.stance() == Stance.INVALID) return false;
        return team.members().contains(playerUUID);
    }

    public static Team getTeamByPlayer(Level level, UUID playerUUID)
    {
        Map<UUID, Team> activeTeams = getActiveTeams(level);
        if (activeTeams.isEmpty()) return Team.createDefault();

        for (Map.Entry<UUID, Team> entry : activeTeams.entrySet())
        {
            if (entry.getValue().members().contains(playerUUID)) return entry.getValue();
        }

        return Team.createDefault();
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
}
