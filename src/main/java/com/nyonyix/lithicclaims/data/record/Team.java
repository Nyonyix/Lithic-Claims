package com.nyonyix.lithicclaims.data.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nyonyix.lithicclaims.data.Stance;
import net.minecraft.util.ExtraCodecs;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record Team(
        UUID id,
        UUID leader,
        String name,
        List<Claim> ownedClaims,
        List<UUID> members,
        Map<UUID, Stance> relations,
        Stance stance,
        int colour,
        Instant stanceCooldown
)
{
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final UUID ZERO_UUID = new UUID(0L, 0L);

    public static final Codec<Team> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("id").forGetter(Team::id),
            UUID_CODEC.fieldOf("leader").forGetter(Team::leader),
            Codec.STRING.fieldOf("name").forGetter(Team::name),
            Codec.list(Claim.CODEC).fieldOf("owned_claims").forGetter(Team::ownedClaims),
            Codec.list(UUID_CODEC).fieldOf("members").forGetter(Team::members),
            Codec.unboundedMap(UUID_CODEC, Codec.STRING.xmap(name -> Enum.valueOf(Stance.class, name), Enum::name)).fieldOf("relations").forGetter(Team::relations),
            Codec.STRING.xmap(name -> Enum.valueOf(Stance.class, name.toUpperCase(Locale.ROOT)), Enum::name).fieldOf("stance").forGetter(Team::stance),
            Codec.INT.fieldOf("colour").forGetter(Team::colour),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("stance_cooldown").forGetter(Team::stanceCooldown)
    ).apply(i, Team::new));

    public static Team createDefault() {return new Team(ZERO_UUID, ZERO_UUID, "invalid", List.of(), List.of(), Map.of(), Stance.INVALID, 0, Instant.EPOCH);}
    public Team withId(UUID id) {return new Team(id, this.leader, this.name, this.ownedClaims, this.members, this.relations, this.stance, this.colour, this.stanceCooldown);}
    public Team withLeader(UUID leader) {return new Team(this.id, leader, this.name, this.ownedClaims, this.members, this.relations, this.stance, this.colour, this.stanceCooldown);}
    public Team withName(String name) {return new Team(this.id, this.leader, name, this.ownedClaims, this.members, this.relations, this.stance, this.colour, this.stanceCooldown);}
    public Team withOwnedClaims(List<Claim> ownedClaims) {return new Team(this.id, this.leader, this.name, ownedClaims, this.members, this.relations, this.stance, this.colour, this.stanceCooldown);}
    public Team withMembers(List<UUID> members) {return new Team(this.id, this.leader, this.name, this.ownedClaims, members, this.relations, this.stance, this.colour, this.stanceCooldown);}
    public Team withRelations(Map<UUID, Stance> relations) {return new Team(this.id, this.leader, this.name, this.ownedClaims, this.members, relations, this.stance, this.colour, this.stanceCooldown);}
    public Team withStance(Stance stance) {return new Team(this.id, this.leader, this.name, this.ownedClaims, this.members, this.relations, stance, this.colour, this.stanceCooldown);}
    public Team withColour(int colour) {return new Team(this.id, this.leader, this.name, this.ownedClaims, this.members, this.relations, this.stance, colour, this.stanceCooldown);}
    public Team withStanceCooldown(Instant stanceCooldown) {return new Team(this.id, this.leader, this.name, this.ownedClaims, this.members, this.relations, this.stance, this.colour, stanceCooldown);}
}