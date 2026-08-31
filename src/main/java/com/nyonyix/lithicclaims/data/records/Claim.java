package com.nyonyix.lithicclaims.data.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record Claim(
        AABB claimArea,
        Team owner,
        BlockPos location,
        long creationCalendarTick
)
{
    private static final Codec<AABB> AABB_CODEC = RecordCodecBuilder.create(i -> i.group(
            Vec3.CODEC.fieldOf("min").forGetter(AABB::getMinPosition),
            Vec3.CODEC.fieldOf("max").forGetter(AABB::getMaxPosition)
    ).apply(i, AABB::new));

    public static final Codec<Claim> CODEC = RecordCodecBuilder.create(i -> i.group(
            AABB_CODEC.fieldOf("claim_area").forGetter(Claim::claimArea),
            Team.CODEC.fieldOf("owner").forGetter(Claim::owner),
            BlockPos.CODEC.fieldOf("location").forGetter(Claim::location),
            Codec.LONG.fieldOf("creation_calendar_tick").forGetter(Claim::creationCalendarTick)
    ).apply(i, Claim::new));
}
