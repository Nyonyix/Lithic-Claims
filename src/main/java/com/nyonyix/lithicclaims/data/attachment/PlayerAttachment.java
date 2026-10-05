package com.nyonyix.lithicclaims.data.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.manager.PlayerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public record PlayerAttachment(
//        Stance stance,
        Instant lastAggressive,
        Instant lastLogin,
        Duration onlineTime
)
{
    public static final Codec<PlayerAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
//            Codec.STRING.xmap(name -> Enum.valueOf(Stance.class, name.toUpperCase(Locale.ROOT)), Enum::name).fieldOf("stance").forGetter(PlayerAttachment::stance),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("last_aggressive").forGetter(PlayerAttachment::lastAggressive),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("last_login").forGetter(PlayerAttachment::lastLogin),
            Codec.LONG.xmap(Duration::ofSeconds, Duration::getSeconds).fieldOf("online_time").forGetter(PlayerAttachment::onlineTime)
    ).apply(i, PlayerAttachment::new));

    public static final StreamCodec<ByteBuf, PlayerAttachment> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static PlayerAttachment createDefault() {return new PlayerAttachment(Instant.EPOCH, Instant.EPOCH, Duration.ZERO);}

//    public PlayerAttachment withStance(Stance stance) {return new PlayerAttachment(stance, this.lastAggressive, this.lastLogin, this.onlineTime);}
    public PlayerAttachment withLastAggressive(Instant lastAggressive) {return new PlayerAttachment(lastAggressive, this.lastLogin, this.onlineTime);}
    public PlayerAttachment withLastLogin(Instant lastLogin) {return new PlayerAttachment(this.lastAggressive, lastLogin, this.onlineTime);}
    public PlayerAttachment withOnlineTime(Duration onlineTime) {return new PlayerAttachment(this.lastAggressive, this.lastLogin, onlineTime);}
}
