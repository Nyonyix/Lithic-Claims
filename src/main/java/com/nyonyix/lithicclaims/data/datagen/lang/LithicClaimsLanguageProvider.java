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
        add("lithicclaims.claim.leaderClick", "Any player can join for the next %s seconds");
        add("lithicclaims.claim.addMemberNeedLeaderClick", "The leader needs to right click the marker");
        add("lithicclaims.claim.addMemberAggressive", "You have committed acts of aggression recently");
        add("lithicclaims.claim.addMember", "You have joined %s");

        add("lithicclaims.team.addMember", "You have been added");
        add("lithicclaims.team.createTeam", "Team %s has been created, Use /lithic_claims team name to rename");
        add("lithicclaims.team.tooManyClaims", "Your team has too many claims");

        add("lithicclaims.configuration.claim", "Claim");
        add("lithicclaims.configuration.claimArea", "Claim Area");
        add("lithicclaims.configuration.addMemberTimeout", "Add Member Timeout");
        add("lithicclaims.configuration.maxNumberOfClaims", "Max Number Of Claims`");
        add("lithicclaims.configuration.team", "Team");
        add("lithicclaims.configuration.teamStanceCooldown", "Team Stance Cooldown");
        add("lithicclaims.configuration.memberCountPercent", "Member General Percentage");
        add("lithicclaims.configuration.memberVotePercent", "Member Vote Percentage");
        add("lithicclaims.configuration.teamVoteCooldown", "Team Vote Cooldown");
        add("lithicclaims.configuration.protection", "Protection");
        add("lithicclaims.configuration.protectionMemberDistWander", "Protection Wander Distance");

        add("lithicclaims.argument.claim.notFound", "No claim found at %s");
        add("lithicclaims.argument.claim.notInClaim", "You are not in a claim");
        add("lithicclaims.argument.team.notFound", "No team named %s exists");
        add("lithicclaims.argument.team.notInTeam", "You are not in a team");
        add("lithicclaims.argument.stance.invalid", "Invalid Stance: %s");
        add("lithicclaims.argument.vote.invalid", "Invalid Vote: %s");

        add("lithicclaims.command.claim.teleport", "Click to teleport");
        add("lithicclaims.command.claim.getOwner", "Claim Owner: %s");
        add("lithicclaims.command.claim.getArea", "Claim Area: %s");
        add("lithicclaims.command.claim.setOwner", "Claim ownership transferred to %s");
        add("lithicclaims.command.claim.setOwnerFail", "This claim already belongs to %s");
        add("lithicclaims.command.claim.setArea", "Claim radius set to %s blocks");
        add("lithicclaims.command.claim.remove", "Claim at %s removed");
        add("lithicclaims.command.claim.listFail", "There are no claims to list");
        add("lithicclaims.command.team.newLeader", "Vote Passed, Your team's new leader: %s");
        add("lithicclaims.command.team.newLeaderFail", "Vote Failed");
        add("lithicclaims.command.team.getLeader", "Team leader: %s");
        add("lithicclaims.command.team.getName", "Team name: %s");
        add("lithicclaims.command.team.getStance", "Team stance: %1$s, Cooldown: %2$s");
        add("lithicclaims.command.team.getColour", "Team Colour: %s");
        add("lithicclaims.command.team.setLeader", "%1$s is now leader of %2$s");
        add("lithicclaims.command.team.setName", "Team %2$s renamed to %1$s");
        add("lithicclaims.command.team.setStance", "Team %2$s stance is now %1$s");
        add("lithicclaims.command.team.stancelastChange", "You cannot change stance while on cooldown: %s");
        add("lithicclaims.command.team.stanceNotAllowed", "You must become neutral before changing to peaceful or hostile");
        add("lithicclaims.command.team.setColour", "Team colour set to %s");
        add("lithicclaims.command.team.kick", "%1$s has been kicked from %2$s");
        add("lithicclaims.command.team.kicked", "You have been kicked from %s");
        add("lithicclaims.command.team.disband", "%s has been disbanded");
        add("lithicclaims.command.team.listFail", "There are no teams to list");
        add("lithicclaims.command.team.leave", "You have left %s");
        add("lithicclaims.command.team.createFail", "You are already in the team %s");
        add("lithicclaims.command.team.createTeam", "%s has been created");
        add("lithicclaims.command.team.resetCooldown", "Stance cooldown has been reset for %s");
        add("lithicclaims.command.team.join", "You have joined %s");
        add("lithicclaims.command.team.vote", "Vote submitted to replace %1$s with %2$s");
        add("lithicclaims.command.team.voteCreate", "A vote has been started to replace %1$s with %2$s");
        add("lithicclaims.command.team.voteNone", "There is no active vote");
        add("lithicclaims.command.team.voteExisting", "There is already an active vote");
        add("lithicclaims.command.team.notLeader", "You must be leader to use this command");
        add("lithicclaims.command.team.playerNotInTeam", "%s is not in this team");
        add("lithicclaims.command.player.lastAggressive", "%1$s has last act of aggression was: %2$s");
        add("lithicclaims.command.player.resetAggression", "Aggression reset for %s");
        add("lithicclaims.command.player.team", "%2$s is in %1$s");

        add("lithicclaims.key.toggle_claim_areas", "Toggle Claim Area Display");
        add("lithicclaims.key.categories", "Lithic Claims");
    }
}
