package com.nyonyix.lithicclaims.data.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Team(
        UUID id,
        List<Claim> ownedClaims,
        List<UUID> members,
        Map<UUID, Byte> relations,
        byte stance,
        int colour
)
{
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Team> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("id").forGetter(Team::id),
            Codec.list(Claim.CODEC).fieldOf("owned_claims").forGetter(Team::ownedClaims),
            Codec.list(UUID_CODEC).fieldOf("members").forGetter(Team::members),
            Codec.unboundedMap(UUID_CODEC, Codec.BYTE).fieldOf("relations").forGetter(Team::relations),
            Codec.BYTE.fieldOf("stance").forGetter(Team::stance),
            Codec.INT.fieldOf("colour").forGetter(Team::colour)
    ).apply(i, Team::new));

    public static Team createDefault() {return new Team(UUID.fromString(""), List.of(), List.of(), Map.of(), (byte) 0, 0);}
}