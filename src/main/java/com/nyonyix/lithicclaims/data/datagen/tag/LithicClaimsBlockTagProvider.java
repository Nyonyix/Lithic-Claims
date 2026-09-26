package com.nyonyix.lithicclaims.data.datagen.tag;

import com.eerussianguy.firmalife.FirmaLife;
import com.eerussianguy.firmalife.common.FLTags;
import com.mojang.datafixers.types.templates.Tag;
import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagEntry;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class LithicClaimsBlockTagProvider extends BlockTagsProvider
{
    public LithicClaimsBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper existingFileHelper)
    {
        super(output, provider, LithicClaims.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        tag(LithicClaimsTags.Blocks.CLAIM_MARKERS)
                .add(Blocks.BELL)
                .addOptional(ResourceLocation.parse("tfc:bronze_bell"))
                .addOptional(ResourceLocation.parse("tfc:brass_bell"));

        tag(LithicClaimsTags.Blocks.CLAIM_USE_EXCEPTION)
                .addTag(BlockTags.TRAPDOORS)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.FENCE_GATES)
                .addOptionalTag(FLTags.Blocks.OVEN_BLOCKS);
    }
}
