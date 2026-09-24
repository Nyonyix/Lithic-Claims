package com.nyonyix.lithicclaims.data.datagen.lang;

import com.nyonyix.lithicclaims.LithicClaims;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class LithicClaimsLanguageProvider extends LanguageProvider
{
    public LithicClaimsLanguageProvider(PackOutput output)
    {
        super(output, LithicClaims.MODID, "en_us");
    }

    @Override
    protected void addTranslations()
    {
        add("lithicclaims.claim.overlap", "This position overlaps with an existing claim");
        add("lithicclaims.claim.createClaim", "A new claim has been created");

        add("lithicclaims.team.addMember", "You have been added");
        add("lithicclaims.team.createTeam", "Team %s has been created, Use [PLACEHOLDER] to rename");

        add("lithicclaims.configuration.claim", "Claims");
        add("lithicclaims.configuration.claimArea", "Claim Area");
    }
}
