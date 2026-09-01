package com.nyonyix.lithicclaims;

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

    public static final ModConfigSpec.DoubleValue CLAIM_AREA = BUILDER.comment("Claim Area, Area of the claim rectangle around the marker").defineInRange("claimArea", 32.0, 8.0, 64.0);

    static
    {
        BUILDER.pop();
        BUILDER.push("protection");
    }

    public static final ModConfigSpec.DoubleValue PROTECTION_MEMBER_DIST_HOSTILE = BUILDER.comment("Distance for members to hostile claim, Distance where x% is near hostile claim your own becomes unprotected").defineInRange("protectionMemberDistHostile", 128.0, 32.0, 512.0);
    public static final ModConfigSpec.DoubleValue PROTECTION_CLAIM_DIST_HOSTILE = BUILDER.comment("Distance for claims to hostile claims, How close should hostile claims be to counter each other's protection").defineInRange("protectionClaimDistHostile", 128.0, 32.0, 512.0);
    public static final ModConfigSpec.DoubleValue PROTECTION_MEMBER_DIST_WANDER = BUILDER.comment("Distance for wander, How far you can leave your claim before it's protected").defineInRange("protectionMemberDistWander", 1024.0, 128.0, 5120.0);
    public static final ModConfigSpec.DoubleValue PROTECTION_MEMBER_COUNT_PERCENT = BUILDER.comment("Percentage of members, Percentage threshold for offline and raiding protection").defineInRange("protectionMemberCountPercent", 0.5, 0.25, 1.0);

    static
    {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
