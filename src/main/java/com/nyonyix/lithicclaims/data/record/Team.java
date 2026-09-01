package com.nyonyix.lithicclaims.data.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nyonyix.lithicclaims.data.Stance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Team(
        UUID id,
        List<Claim> ownedClaims,
        List<UUID> members,
        Map<UUID, Stance> relations,
        Stance stance,
        int colour
)
{
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final UUID ZERO_UUID = new UUID(0L, 0L);

    public static final Codec<Team> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("id").forGetter(Team::id),
            Codec.list(Claim.CODEC).fieldOf("owned_claims").forGetter(Team::ownedClaims),
            Codec.list(UUID_CODEC).fieldOf("members").forGetter(Team::members),
            Codec.unboundedMap(UUID_CODEC, Codec.STRING.xmap(name -> Enum.valueOf(Stance.class, name), Enum::name)).fieldOf("relations").forGetter(Team::relations),
            Codec.STRING.xmap(name -> Enum.valueOf(Stance.class, name), Enum::name).fieldOf("stance").forGetter(Team::stance),
            Codec.INT.fieldOf("colour").forGetter(Team::colour)
    ).apply(i, Team::new));

    public static Team createDefault() {return new Team(ZERO_UUID, List.of(), List.of(), Map.of(), Stance.INVALID, 0);}
}