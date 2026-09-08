package com.nyonyix.lithicclaims.data.manager;

import com.mojang.logging.LogUtils;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.attachment.TeamAttachment;
import com.nyonyix.lithicclaims.data.record.Team;
import com.nyonyix.lithicclaims.server.ServerConfig;
import com.sun.jna.platform.win32.WinDef;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BrewedPotionTrigger;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.*;

public class TeamManager
{
    public static final Logger LOGGER = LogUtils.getLogger();

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
            if (entry.getValue().members().contains(playerUUID)) return entry.getValue();
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

        Team team = new Team(teamUUID, playerUUID, name, List.of(), members, Map.of(), stance, colour, Instant.now());
        player.displayClientMessage(Component.translatable("lithicclaims.team.createTeam", name).withStyle(ChatFormatting.GREEN), true);

        return team;
    }

    public static void saveAttachment(Level level, Team team)
    {
        Map<UUID, Team> activeTeams = new HashMap<>(getActiveTeams(level));
        activeTeams.put(team.id(), team);

        level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));
    }

    public static void saveAttachment(Level level, Map<UUID, Team> activeTeams)
    {
        level.setData(LithicClaimsAttachments.TEAM_ATTACHMENT, new TeamAttachment(activeTeams));
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

        for (Team forTeam : activeTeams.values())
        {
            if (forTeam.relations().containsKey(team.id()))
            {
                Map<UUID, Stance> newRelations = new HashMap<>(forTeam.relations());
                newRelations.remove(team.id());
                activeTeams.put(forTeam.id(), forTeam.withRelations(newRelations));
            }
        }

        activeTeams.remove(team.id());
        saveAttachment(level, activeTeams);
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
       }

       List<UUID> members = new ArrayList<>(team.members());
       members.remove(playerUUID);
       team = team.withMembers(members);

       saveAttachment(level, team);
    }

    public static void changeTeamStance(Level level, Team team, Stance stance)
    {
        float stanceCooldown = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();
        Instant cooldownEnd = team.stanceCooldown().plusSeconds(Math.round(stanceCooldown * 60 * 60));

        if (Instant.now().isBefore(cooldownEnd)) return;

        switch (team.stance())
        {
            case NEUTRAL ->
            {
                team = team.withStance(stance).withStanceCooldown(Instant.now());
                saveAttachment(level, team);
            }
            case HOSTILE, PEACEFUL ->
            {
                if (stance.equals(Stance.NEUTRAL))
                {
                    team = team.withStance(stance).withStanceCooldown(Instant.now());
                    saveAttachment(level, team);
                }
            }
        }
    }

    public static void changeTeamRelations(Level level, Team sourceTeam, Team targetTeam, Stance stance)
    {
        if (targetTeam.stance().equals(Stance.PEACEFUL) || sourceTeam.stance().equals(Stance.PEACEFUL)) return;
        if (targetTeam.stance().equals(Stance.HOSTILE)) return;

        Map<UUID, Stance> sourceRelations = new HashMap<>(sourceTeam.relations());
        Map<UUID, Stance> targetRelations = new HashMap<>(targetTeam.relations());

        float stanceCooldown = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();
        Instant cooldownEnd = sourceTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldown * 60 * 60));
        if (Instant.now().isBefore(cooldownEnd)) return;

        Stance stanceToTarget = sourceRelations.getOrDefault(targetTeam.id(), Stance.NEUTRAL);
        switch (stanceToTarget)
        {
            case NEUTRAL ->
            {
                sourceRelations.put(targetTeam.id(), stance);
                sourceTeam = sourceTeam.withRelations(sourceRelations).withStanceCooldown(Instant.now());
                saveAttachment(level, sourceTeam);

                if (stance.equals(Stance.HOSTILE))
                {
                    targetRelations.put(sourceTeam.id(), stance);
                    targetTeam = targetTeam.withRelations(targetRelations).withStanceCooldown(Instant.now());
                    saveAttachment(level, targetTeam);
                }
            }
            case HOSTILE, PEACEFUL ->
            {
                if (stance.equals(Stance.NEUTRAL))
                {
                    sourceRelations.put(targetTeam.id(), stance);
                    sourceTeam = sourceTeam.withRelations(sourceRelations).withStanceCooldown(Instant.now());
                    saveAttachment(level, sourceTeam);
                }
            }
        }
    }
}
