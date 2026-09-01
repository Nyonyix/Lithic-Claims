package com.nyonyix.lithicclaims.data.manager;

import com.nyonyix.lithicclaims.data.attachment.LithicClaimsAttachments;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TeamManager
{
    public static Team getTeam(Level level, UUID uuid)
    {
        return level.getData(LithicClaimsAttachments.TEAM_ATTACHMENT).activeTeams().getOrDefault(uuid, Team.createDefault());
    }

    public static float percentMembersOnline(Level level, UUID uuid)
    {
        float playersNotNull = 0f;
        Team team = getTeam(level, uuid);
        if (team.id().equals(UUID.fromString(""))) return 0.0f;

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
