package com.nyonyix.lithicclaims.data.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;

public record Team(
        UUID id,
        Map<UUID, Byte> members,
        Map<UUID, Byte> relations,
        byte stance,
        int colour
)
{
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<Team> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("id").forGetter(Team::id),
            Codec.unboundedMap(UUID_CODEC, Codec.BYTE).fieldOf("members").forGetter(Team::members),
            Codec.unboundedMap(UUID_CODEC, Codec.BYTE).fieldOf("relations").forGetter(Team::relations),
            Codec.BYTE.fieldOf("stance").forGetter(Team::stance),
            Codec.INT.fieldOf("colour").forGetter(Team::colour)
    ).apply(i, Team::new));
}