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
        add("lithicclaims.team.createTeam", "Team %s has been created");

        add("lithicclaims.configuration.claim", "Claims");
        add("lithicclaims.configuration.claimArea", "Claim Area");

        add("lithicclaims.command.claim.Teleport", "Click To Teleport");
        add("lithicclaims.command.claim.listFailure", "Currently no active claims");
        add("lithicclaims.command.claim.noClaimFound", "Failed to find claim at: %s");
        add("lithicclaims.command.claim.removeSuccess", "Removed claim at: %s");
        add("lithicclaims.comamnd.claim.modifyOwnerFailure", "Team %s already owns this claim");
        add("lithicclaims.command.claim.modifyOwnerSuccess", "Team %s is the new owner of %s");
        add("lithicclaims.command.claim.modifyAreaSuccess", "Claim %s area has been changed by %d");
        add("lithicclaims.command.team.createFailure", "%s is already in a team");
        add("lithicclaims.command.team.noTeamFound", "Failed to find team: %s");
        add("lithicclaims.command.team.removeSuccess", "Team %s was removed");
        add("lithicclaims.command.team.modiyLeaderSuccess", "%s is the new leader of %s");
        add("lithicclaims.command.team.modifyStanceSuccess", "%s is the new stance of %s");
        add("lithicclaims.command.team.modifyColourSuccess", "%s is the new colour for %s");
        add("lithicclaims.command.team.modifyNameSuccess", "%s is the new name of %s");
        add("lithicclaims.command.team.modifyStanceCooldown", "Your stance cooldown has yet to expire");
        add("lithicclaims.command.team.modifyPermission", "You lack the permission to perform this action");
        add("lithicclaims.command.team.resetCooldownSuccess", "The stance cooldown for team %s has been reset: %s");
        add("lithicclaims.command.team.colour", "This");
    }
}
