package com.nyonyix.lithicclaims.data.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nyonyix.lithicclaims.data.record.Claim;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;

public record ClaimAttachment(Map<BlockPos, Claim> activeClaims)
{
    public static final Codec<ClaimAttachment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(BlockPos.CODEC, Claim.CODEC).fieldOf("active_claims").forGetter(ClaimAttachment::activeClaims)
    ).apply(i, ClaimAttachment::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimAttachment> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static ClaimAttachment createDefault() {return new ClaimAttachment(Map.of());}
}
