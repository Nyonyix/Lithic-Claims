package com.nyonyix.lithicclaims.data;

import com.nyonyix.lithicclaims.data.record.Team;

import java.util.*;

public class TeamVote
{
    private final Map<UUID, Vote> votes;
    private final Set<UUID> eligible;
    private final long startTick;
    private final UUID oldLeader;
    private final UUID newLeader;
    private float percentageYay;

    public static TeamVote BLANK = new TeamVote(Set.of(), 0, Team.ZERO_UUID, Team.ZERO_UUID);

    public TeamVote(Collection<UUID> members, long startTick, UUID oldLeader, UUID newLeader)
    {
        this.startTick = startTick;
        this.oldLeader = oldLeader;
        this.newLeader = newLeader;
        this.votes = new HashMap<>();
        this.eligible = Set.copyOf(members);
    }

    public Map<UUID, Vote> getVotes()
    {
        return votes;
    }

    public Vote getVote(UUID memeberUUID)
    {
        return votes.getOrDefault(memeberUUID, Vote.INVALID);
    }

    public long getStartTick()
    {
        return startTick;
    }

    public UUID getOldLeader()
    {
        return oldLeader;
    }

    public UUID getNewLeader()
    {
        return newLeader;
    }

    public float getPercentageYay()
    {
        if (eligible.isEmpty()) return 0.0f;
        return (float) Collections.frequency(votes.values(), Vote.YAY) / eligible.size();
    }

    public boolean castVote(UUID memeberUUID, Vote vote)
    {
        if (!eligible.contains(memeberUUID)) return false;
        votes.put(memeberUUID, vote);
        return true;
    }
}
