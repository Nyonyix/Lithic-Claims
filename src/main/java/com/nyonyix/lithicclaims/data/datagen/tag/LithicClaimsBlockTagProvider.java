package com.nyonyix.lithicclaims.data.datagen.tag;

import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.data.LithicClaimsTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
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
    }
}
