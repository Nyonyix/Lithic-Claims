package com.nyonyix.lithicclaims.data.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.time.Instant;

public record PlayerAttachment(Instant lastAggressive)
{
    public static final Codec<PlayerAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.xmap(Instant::ofEpochMilli, Instant::toEpochMilli).fieldOf("last_aggressive").forGetter(PlayerAttachment::lastAggressive)
    ).apply(i, PlayerAttachment::new));

    public static final StreamCodec<ByteBuf, PlayerAttachment> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static PlayerAttachment createDefault() {return new PlayerAttachment(Instant.EPOCH);}
}
