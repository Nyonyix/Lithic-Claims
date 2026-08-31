package com.nyonyix.lithicclaims.data;

import com.nyonyix.lithicclaims.LithicClaims;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class LithicClaimsTags
{
    public static class Blocks
    {
        public static final TagKey<Block> CLAIM_MARKERS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(LithicClaims.MODID, "claim_markers"));
    }
}
