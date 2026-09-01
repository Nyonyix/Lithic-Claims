package com.nyonyix.lithicclaims.data.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nyonyix.lithicclaims.data.record.Team;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.UUID;

public record TeamAttachment(Map<UUID, Team> activeTeams)
{
    public static final Codec<TeamAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Team.UUID_CODEC, Team.CODEC).fieldOf("active_teams").forGetter(TeamAttachment::activeTeams)
    ).apply(i, TeamAttachment::new));

    public static final StreamCodec<ByteBuf, TeamAttachment> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static TeamAttachment createDefault() {return new TeamAttachment(Map.of());}
}
