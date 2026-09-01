package com.nyonyix.lithicclaims.data.attachment;

import com.nyonyix.lithicclaims.LithicClaims;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class LithicClaimsAttachments
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LithicClaims.MODID);
    public static final Supplier<AttachmentType<PlayerAttachment>> PLAYER_ATTACHMENT = ATTACHMENTS.register("player_attachment", () -> AttachmentType.builder(PlayerAttachment::createDefault).serialize(PlayerAttachment.CODEC).sync(PlayerAttachment.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<ClaimAttachment>> CLAIM_ATTACHMENT = ATTACHMENTS.register("claim_attachment", () -> AttachmentType.builder(ClaimAttachment::createDefault).serialize(ClaimAttachment.CODEC).sync(ClaimAttachment.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<TeamAttachment>> TEAM_ATTACHMENT = ATTACHMENTS.register("team_attachment", () -> AttachmentType.builder(TeamAttachment::createDefault).serialize(TeamAttachment.CODEC).sync(TeamAttachment.STREAM_CODEC).build());
}
