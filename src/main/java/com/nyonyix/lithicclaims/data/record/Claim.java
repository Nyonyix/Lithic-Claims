package com.nyonyix.lithicclaims.data.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record Claim(
        AABB claimArea,
        UUID owner,
        BlockPos location,
        long creationTick
)
{
    private static final Codec<AABB> AABB_CODEC = RecordCodecBuilder.create(i -> i.group(
            Vec3.CODEC.fieldOf("min").forGetter(AABB::getMinPosition),
            Vec3.CODEC.fieldOf("max").forGetter(AABB::getMaxPosition)
    ).apply(i, AABB::new));

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Claim> CODEC = RecordCodecBuilder.create(i -> i.group(
            AABB_CODEC.fieldOf("claim_area").forGetter(Claim::claimArea),
            UUID_CODEC.fieldOf("owner").forGetter(Claim::owner),
            BlockPos.CODEC.fieldOf("location").forGetter(Claim::location),
            Codec.LONG.fieldOf("creation_calendar_tick").forGetter(Claim::creationTick)
    ).apply(i, Claim::new));

    public static Claim createDefault() {return  new Claim(new AABB(0, 0, 0, 0, 0, 0), Team.ZERO_UUID, BlockPos.ZERO, 0);}
    public Claim withClaimArea(AABB claimArea) {return new Claim(claimArea, this.owner, this.location, this.creationTick);}
    public Claim withOwner(UUID owner) {return new Claim(this.claimArea, owner, this.location, this.creationTick);}
    public Claim withLocation(BlockPos pos) {return new Claim(this.claimArea, this.owner, location, this.creationTick);}
    public Claim withCreationTick(long creationTick) {return new Claim(this.claimArea, this.owner, this.location, creationTick);}
}
