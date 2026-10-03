package com.nyonyix.lithicclaims.server;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class ServerConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static
    {
        BUILDER.push("claim");
    }

    public static final ModConfigSpec.DoubleValue CLAIM_AREA = BUILDER.comment("Claim Area, Area of the claim rectangle around the marker").defineInRange("claimArea", 96.0, 64.0, 256.0);
    public static final ModConfigSpec.IntValue ADD_MEMBER_TIMEOUT = BUILDER.comment("Member add timeout, How long in ticks does the marker wait for a member before timeout").defineInRange("addMemberTimeout", 300, 100, 1200);

    static
    {
        BUILDER.pop();
        BUILDER.push("team");
    }

    public static final ModConfigSpec.LongValue TEAM_STANCE_COOLDOWN = BUILDER.comment("Team stance cooldown in minutes, How long should go by real world play time before changing stances").defineInRange("teamStanceCooldown", 360L, 60L, 720L);
    public static final ModConfigSpec.DoubleValue MEMBER_COUNT_PERCENT = BUILDER.comment("Percentage of members, Percentage threshold for offline and raiding protection as well as other internal math").defineInRange("protectionMemberCountPercent", 0.5, 0.25, 1.0);
    public static final ModConfigSpec.DoubleValue MEMBER_VOTE_PERCENT = BUILDER.comment("Percentage for votes, Percentage for when the vote is considered passed").defineInRange("protectionMemberCountPercent", 0.5, 0.25, 1.0);
    public static final ModConfigSpec.IntValue TEAM_VOTE_TICKS = BUILDER.comment("Team vote duration in ticks, How long should the vote window remain open in ticks").defineInRange("teamStanceCooldown", 600, 200, 6000);

    static
    {
        BUILDER.pop();
        BUILDER.push("protection");
    }

    public static final ModConfigSpec.DoubleValue PROTECTION_MEMBER_DIST_WANDER = BUILDER.comment("Distance for wander, How far you can leave your claim before it's protected").defineInRange("protectionMemberDistWander", 512.0, 128.0, 2048.0);

    static
    {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
