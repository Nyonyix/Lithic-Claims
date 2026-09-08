package com.nyonyix.lithicclaims.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.manager.TeamManager;
import com.nyonyix.lithicclaims.data.record.Claim;
import com.nyonyix.lithicclaims.data.record.Team;
import com.nyonyix.lithicclaims.server.ServerConfig;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.UsernameCache;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class LithicClaimsCommands
{
    private static Team getTeamByNameOrUUID(CommandContext<CommandSourceStack> context, String argString)
    {
        Level level = context.getSource().getLevel();
        String nameOrUUID = StringArgumentType.getString(context, argString);
        String name = null;
        UUID teamUUID = null;

        try
        {
            teamUUID = UUID.fromString(nameOrUUID);
            return TeamManager.getTeam(level, teamUUID);
        }
        catch (IllegalArgumentException e)
        {
            name = nameOrUUID;
            return TeamManager.getTeamByName(level, name);
        }
    }

    private static int executeListClaims(CommandContext<CommandSourceStack> context)
    {
        try
        {
            Level level = context.getSource().getLevel();
            Map<BlockPos, Claim> activeClaims = ClaimManager.getActiveClaims(level);

            if (activeClaims.isEmpty())
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.listFailure"));
                return 0;
            }

            for (Claim claim : activeClaims.values())
            {
                Team team = TeamManager.getTeam(level, claim.owner());
                Component calendarDate = Component.literal("");
                if (ModList.get().isLoaded("tfc"))
                {
                    calendarDate = ICalendar.getTimeAndDate(claim.creationTick(), Calendars.SERVER.getCalendarDaysInMonth());
                }

                Component posComponent = Component.literal(String.format("[%d, %d, %d], \n", claim.location().getX(), claim.location().getY(), claim.location().getZ()))
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tp @s %d %d %d", claim.location().getX(), claim.location().getY(), claim.location().getZ())))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lithicclaims.command.claim.Teleport"))));

                Component claimComponent = Component.literal("Claim: \n").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("Location: ").withStyle(ChatFormatting.AQUA)
                        .append(posComponent)
                        .append(Component.literal("Claim Area: ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("%s\n", claim.claimArea().toString())).withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("Owner UUID: ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("%s\n", claim.owner().toString())).withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("Owner Team Name: ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("%s\n", team.name())).withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("Creation Tick: ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("Raw: %d, Date: ", claim.creationTick())).withStyle(ChatFormatting.GREEN)
                        .append(calendarDate).withStyle(ChatFormatting.GREEN))))))))));

                context.getSource().sendSuccess(() -> claimComponent, true);
            }

            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeClaimRemove(CommandContext<CommandSourceStack> context)
    {
        try
        {
            BlockPos pos = BlockPosArgument.getBlockPos(context, "location");
            Level level = context.getSource().getLevel();
            Claim claim = ClaimManager.getClaim(level, pos);

            if (claim.owner().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.noClaimFound", pos.toShortString()));
                return 0;
            }

            ClaimManager.claimCleanUp(level, pos);

            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.removeSuccess", pos.toShortString()), true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private  static int executeClaimInfo(CommandContext<CommandSourceStack> context)
    {
        try
        {
            Level level = context.getSource().getLevel();
            BlockPos pos = BlockPosArgument.getBlockPos(context, "location");

            Claim claim = ClaimManager.getClaimContains(level, pos);
            if (claim.owner().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.noClaimFound", pos.toShortString()));
                return 0;
            }

            Component calendarDate = Component.literal("");
            if (ModList.get().isLoaded("tfc"))
            {
                calendarDate = ICalendar.getTimeAndDate(claim.creationTick(), Calendars.SERVER.getCalendarDaysInMonth());
            }

            Team team = TeamManager.getTeam(level, claim.owner());
            Component posComponent = Component.literal(String.format("[%d, %d, %d], \n", claim.location().getX(), claim.location().getY(), claim.location().getZ()))
                    .withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tp @s %d %d %d", claim.location().getX(), claim.location().getY(), claim.location().getZ())))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lithicclaims.command.claim.Teleport"))));

            Component claimComponent = Component.literal("Claim: \n").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("Location: ").withStyle(ChatFormatting.AQUA)
                    .append(posComponent)
                    .append(Component.literal("Claim Area: ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format("%s\n", claim.claimArea().toString())).withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("Owner UUID: ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format("%s\n", claim.owner().toString())).withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("Owner Team Name: ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format("%s\n", team.name())).withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("Creation Tick: ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format("Raw: %d, Date: ", claim.creationTick())).withStyle(ChatFormatting.GREEN)
                    .append(calendarDate).withStyle(ChatFormatting.GREEN))))))))));

            context.getSource().sendSuccess(() -> claimComponent, true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeClaimModifyOwner(CommandContext<CommandSourceStack> context)
    {
        try
        {
            BlockPos pos = BlockPosArgument.getBlockPos(context, "location");
            Team team = getTeamByNameOrUUID(context, "name | uuid");
            Level level = context.getSource().getLevel();
            Claim claim = ClaimManager.getClaimContains(level, pos);

            if (claim.owner().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.noClaimFound", pos.toShortString()));
                return 0;
            }
            else if (team.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }
            else if (claim.owner().equals(team.id()))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.modifyOwnerFailure", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            claim = claim.withOwner(team.id());
            ClaimManager.saveAttachment(level, claim);

            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.modifyOwnerSuccess", team.name(), pos.toShortString()), true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeClaimModifyArea(CommandContext<CommandSourceStack> context)
    {
        try
        {
            BlockPos pos = BlockPosArgument.getBlockPos(context, "location");
            int radius = IntegerArgumentType.getInteger(context, "radius");
            Level level = context.getSource().getLevel();


            final Claim oldClaim = ClaimManager.getClaimContains(level, pos);
            if (oldClaim.owner().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.noClaimFound", pos.toShortString()));
            }

            pos = oldClaim.location();
            Vec3 min = new Vec3(pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius);
            Vec3 max = new Vec3(pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius);
            AABB claimArea = new AABB(min, max);

            Claim newClaim = oldClaim.withClaimArea(claimArea);
            ClaimManager.saveAttachment(level, newClaim);

            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.modifyAreaSuccess", oldClaim.location().toShortString(), radius), true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamCreate(CommandContext<CommandSourceStack> context)
    {
        try
        {
            String name = StringArgumentType.getString(context, "name");
            Player player = context.getSource().getPlayer();
            Stance stance = Stance.valueOf(StringArgumentType.getString(context, "stance").toUpperCase(Locale.ROOT));
            int colour = IntegerArgumentType.getInteger(context, "colour");

            Level level = context.getSource().getLevel();

            Team playerTeam = TeamManager.getTeamByPlayer(level, player.getUUID());
            if (!playerTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.createFailure", player.getDisplayName().getString()));
                return 0;
            }

            Team team = TeamManager.createTeam(level, name, player, stance, colour);
            TeamManager.saveAttachment(context.getSource().getLevel(), team);

            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.team.createTeam", team.name()), true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamLeave(CommandContext<CommandSourceStack> context)
    {
        try
        {
            Level level = context.getSource().getLevel();
            Player player = context.getSource().getPlayerOrException();
            Team team = TeamManager.getTeamByPlayer(level, player.getUUID());

            if (team.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.playerNotInTeam", UsernameCache.containsUUID(player.getUUID()) ? UsernameCache.getLastKnownUsername(player.getUUID()) : "Unknown"));
                return 0;
            }

            TeamManager.removeMember(level, player.getUUID());
            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.LeaveSuccess", team.name()), true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamModifyLeader(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team oldTeam = getTeamByNameOrUUID(context, "name | uuid");
            if (oldTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Player player = EntityArgument.getPlayer(context, "player");
            Level level = context.getSource().getLevel();

            if (!TeamManager.isInTeam(level, player.getUUID(), oldTeam.id()))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.playerNotInTeam", UsernameCache.containsUUID(player.getUUID()) ? UsernameCache.getLastKnownUsername(player.getUUID()) : "Unknown"));
                return 0;
            }

            if (TeamManager.isInTeam(level, player.getUUID(), oldTeam.id()) && context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    Team newTeam = oldTeam.withLeader(player.getUUID());
                    TeamManager.saveAttachment(level, newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.modifyLeaderSuccess", player.getDisplayName(), oldTeam.name()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && !TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    Team newTeam = oldTeam.withLeader(player.getUUID());
                    TeamManager.saveAttachment(level, newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.modifyLeaderSuccess", player.getDisplayName(), oldTeam.name()), true);
                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamModifyName(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team oldTeam = getTeamByNameOrUUID(context, "name | uuid");
            if (oldTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Level level = context.getSource().getLevel();

            if (context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    Team newTeam = oldTeam.withName(StringArgumentType.getString(context, "name"));
                    TeamManager.saveAttachment(context.getSource().getLevel(), newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.modifyNameSuccess", newTeam.name(), oldTeam.name()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && !TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    Team newTeam = oldTeam.withName(StringArgumentType.getString(context, "name"));
                    TeamManager.saveAttachment(context.getSource().getLevel(), newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.commands.team.modifyNameSuccess", newTeam.name(), oldTeam.name()), true);
                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamModifyStance(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team oldTeam = getTeamByNameOrUUID(context, "name | uuid");
            if (oldTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Stance stance = Stance.valueOf(StringArgumentType.getString(context, "stance").toUpperCase(Locale.ROOT));
            Level level = context.getSource().getLevel();

            float stanceCooldownConfig = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();
            Instant cooldownEnd = oldTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60));

            Duration cooldownDiff = Duration.between(Instant.now(), oldTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60)));
            boolean ago = cooldownDiff.isNegative();
            long total = Math.abs(cooldownDiff.toSeconds());
            long h = total / 3600;
            long m = (total % 3600) / 60;
            long s = total % 60;
            String time = String.format("%dh, %dm, %ds", h, m, s);
            String result = ago ? time + " ago" : time + " until";

            if (Instant.now().isBefore(cooldownEnd))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyStanceCooldown", result));
                return 0;
            }

            if (context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    TeamManager.changeTeamStance(level, oldTeam, stance);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.modifyStanceSuccess", stance.toString(), oldTeam.name()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && !TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    TeamManager.changeTeamStance(level, oldTeam, stance);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.commands.team.modifyStanceSuccess", stance.toString(), oldTeam.name()), true);
                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamModifyColour(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team oldTeam = getTeamByNameOrUUID(context, "name | uuid");
            if (oldTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            int colour = IntegerArgumentType.getInteger(context, "colour");
            Level level = context.getSource().getLevel();

            if (context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    Team newTeam = oldTeam.withColour(colour);
                    TeamManager.saveAttachment(level, newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.modifyColourSuccess", Component.translatable("lithicclaims.command.team.colour").withColor(newTeam.colour()), oldTeam.name()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && !TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    Team newTeam = oldTeam.withColour(colour);
                    TeamManager.saveAttachment(level, newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.commands.team.modifyColourSuccess", Component.translatable("lithicclaims.command.team.colour").withColor(newTeam.colour()), oldTeam.name()), true);                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeListTeams(CommandContext<CommandSourceStack> context)
    {
        try
        {
            Level level = context.getSource().getLevel();
            Map<UUID, Team> activeTeams = TeamManager.getActiveTeams(level);
            float stanceCooldownConfig = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();

            if (activeTeams.isEmpty())
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.listFailure"));
                return 0;
            }

            for (Team team : activeTeams.values())
            {
                String leaderName = UsernameCache.containsUUID(team.leader()) ? UsernameCache.getLastKnownUsername(team.leader()) : "Unknown";

                Duration cooldownDiff = Duration.between(Instant.now(), team.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60)));
                boolean ago = cooldownDiff.isNegative();
                long total = Math.abs(cooldownDiff.toSeconds());
                long h = total / 3600;
                long m = (total % 3600) / 60;
                long s = total % 60;
                String time = String.format("%dh, %dm, %ds", h, m, s);
                String result = ago ? time + " ago" : time + " until";

                Component teamComponent = Component.literal("Team: \n").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("ID: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%s\n", team.id().toString())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Leader: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%s\n", leaderName)).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Name: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%s\n", team.name())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Number Of claims: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%d\n", team.ownedClaims().size())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Number Of Members: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%d\n", team.members().size())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Stance: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%s\n", Character.toUpperCase(team.stance().toString().charAt(0)) + team.stance().toString().substring(1))).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Colour: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("0x%06X\n", team.colour())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Stance Cooldown: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(result).withStyle(ChatFormatting.GREEN)))))))))))))))));

                context.getSource().sendSuccess(() -> teamComponent, true);
            }

            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

//    private static int executeTeamRelations(CommandContext<CommandSourceStack> context)
//    {
//        try
//        {
//            Team targetTeam = getTeamByNameOrUUID(context, "name | uuid");
//            Stance stance = Stance.valueOf(StringArgumentType.getString(context, "stance").toUpperCase(Locale.ROOT));
//            Player sourcePlayer = context.getSource().getPlayerOrException();
//            Level level = context.getSource().getLevel();
//            Team sourceTeam = TeamManager.getTeamByPlayer(level, sourcePlayer.getUUID());
//
//            float stanceCooldownConfig = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();
//            Instant cooldownEnd = sourceTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60));
//
//            Duration cooldownDiff = Duration.between(Instant.now(), sourceTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60)));
//            boolean ago = cooldownDiff.isNegative();
//            long total = Math.abs(cooldownDiff.toSeconds());
//            long h = total / 3600;
//            long m = (total % 3600) / 60;
//            long s = total % 60;
//            String time = String.format("%dh, %dm, %ds", h, m, s);
//            String result = ago ? time + " ago" : time + " until";
//
//            if (Instant.now().isBefore(cooldownEnd))
//            {
//                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyStanceCooldown", result));
//                return 0;
//            }
//
//            if (targetTeam.stance().equals(Stance.PEACEFUL) || sourceTeam.stance().equals(Stance.PEACEFUL))
//            {
//                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.relationsPeaceful"));
//                return 0;
//            }
//
//            if (targetTeam.stance().equals(Stance.HOSTILE))
//            {
//                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.relationsHostile"));
//                return 0;
//            }
//
//            if (targetTeam.id().equals(Team.ZERO_UUID))
//            {
//                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
//                return 0;
//            }
//
//            if (sourceTeam.id().equals(Team.ZERO_UUID))
//            {
//                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.notInTeam"));
//                return 0;
//            }
//
//            TeamManager.changeTeamRelations(level, sourceTeam, targetTeam, stance);
//
//            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.relationsSuccess",targetTeam.name(), stance.toString()), true);
//            return 1;
//        }
//        catch (Exception e)
//        {
//            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
//            return 0;
//        }
//    }

    private static int executeTeamRelations(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team targetTeam = getTeamByNameOrUUID(context, "name | uuid");
            final Team sourceTeam = TeamManager.getTeamByPlayer(context.getSource().getLevel(), context.getSource().getPlayer().getUUID());
            if (targetTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Level level = context.getSource().getLevel();
            Stance stance = Stance.valueOf(StringArgumentType.getString(context, "stance").toUpperCase(Locale.ROOT));

            float stanceCooldownConfig = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();
            Instant cooldownEnd = sourceTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60));

            Duration cooldownDiff = Duration.between(Instant.now(), sourceTeam.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60)));
            boolean ago = cooldownDiff.isNegative();
            long total = Math.abs(cooldownDiff.toSeconds());
            long h = total / 3600;
            long m = (total % 3600) / 60;
            long s = total % 60;
            String time = String.format("%dh, %dm, %ds", h, m, s);
            String result = ago ? time + " ago" : time + " until";

            if (Instant.now().isBefore(cooldownEnd))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyStanceCooldown", result));
                return 0;
            }

            if (context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), targetTeam.id()) && TeamManager.getLeaderUUID(level, targetTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    TeamManager.changeTeamRelations(level, sourceTeam, targetTeam, stance);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.relationsSuccess",targetTeam.name(), stance.toString()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), targetTeam.id()) && !TeamManager.getLeaderUUID(level, targetTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), targetTeam.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    TeamManager.changeTeamRelations(level, sourceTeam, targetTeam, stance);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.relationsSuccess",targetTeam.name(), stance.toString()), true);
                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamResetCooldown(CommandContext<CommandSourceStack> context)
    {
        try
        {
            Team oldTeam = getTeamByNameOrUUID(context, "name | uuid");

            if (oldTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Team newTeam = oldTeam.withStanceCooldown(Instant.now());
            TeamManager.saveAttachment(context.getSource().getLevel(), newTeam);

            context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.ResetSuccess", newTeam.name()), true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamKick(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team oldTeam = getTeamByNameOrUUID(context, "name | uuid");
            if (oldTeam.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Player player = EntityArgument.getPlayer(context, "player");
            Level level = context.getSource().getLevel();

            if (!oldTeam.members().contains(player.getUUID()))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.playerNotInTeam", StringArgumentType.getString(context, UsernameCache.containsUUID(player.getUUID()) ? UsernameCache.getLastKnownUsername(player.getUUID()) : "Unknown")));
                return 0;
            }

            if (context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    List<UUID> members = new ArrayList<>(oldTeam.members());
                    members.remove(player.getUUID());

                    Team newTeam = oldTeam.withMembers(members);
                    TeamManager.saveAttachment(level, newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.modifyKickSuccess", oldTeam.name()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && !TeamManager.getLeaderUUID(level, oldTeam.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), oldTeam.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    List<UUID> members = new ArrayList<>(oldTeam.members());
                    members.remove(player.getUUID());

                    Team newTeam = oldTeam.withMembers(members);
                    TeamManager.saveAttachment(level, newTeam);

                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.commands.team.modifyKickSuccess", oldTeam.name()), true);
                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamInfo(CommandContext<CommandSourceStack> context)
    {
        try
        {
            Level level = context.getSource().getLevel();
            Team team = getTeamByNameOrUUID(context, "name | uuid");
            float stanceCooldownConfig = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsDouble();

            String leaderName = UsernameCache.containsUUID(team.leader()) ? UsernameCache.getLastKnownUsername(team.leader()) : "Unknown";

            Duration cooldownDiff = Duration.between(Instant.now(), team.stanceCooldown().plusSeconds(Math.round(stanceCooldownConfig * 60 * 60)));
            boolean ago = cooldownDiff.isNegative();
            long total = Math.abs(cooldownDiff.toSeconds());
            long h = total / 3600;
            long m = (total % 3600) / 60;
            long s = total % 60;
            String time = String.format("%dh, %dm, %ds", h, m, s);
            String result = ago ? time + " ago" : time + " until";

            Component teamComponent = Component.literal("Team: \n").withStyle(ChatFormatting.AQUA)
            .append(Component.literal("ID: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%s\n", team.id().toString())).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Leader: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%s\n", leaderName)).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Name: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%s\n", team.name())).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Number Of claims: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%d\n", team.ownedClaims().size())).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Number Of Members: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%d\n", team.members().size())).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Stance: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("%s\n", Character.toUpperCase(team.stance().toString().charAt(0)) + team.stance().toString().substring(1))).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Colour: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(String.format("0x%06X\n", team.colour())).withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Stance Cooldown: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(result).withStyle(ChatFormatting.GREEN)))))))))))))))));

            context.getSource().sendSuccess(() -> teamComponent, true);
            return 1;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    private static int executeTeamDisband(CommandContext<CommandSourceStack> context)
    {
        try
        {
            final Team team = getTeamByNameOrUUID(context, "name | uuid");
            if (team.id().equals(Team.ZERO_UUID))
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.noTeamFound", StringArgumentType.getString(context, "name | uuid")));
                return 0;
            }

            Level level = context.getSource().getLevel();

            if (context.getSource().isPlayer())
            {
                Player sourcePlayer = context.getSource().getPlayerOrException();
                if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), team.id()) && TeamManager.getLeaderUUID(level, team.id()).equals(sourcePlayer.getUUID()))
                {
                    TeamManager.teamCleanUp(level, team);
                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.removeSuccess", team.name()), true);
                    return 1;
                }
                else if (TeamManager.isInTeam(level, sourcePlayer.getUUID(), team.id()) && !TeamManager.getLeaderUUID(level, team.id()).equals(sourcePlayer.getUUID()))
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
                else if (!TeamManager.isInTeam(level, sourcePlayer.getUUID(), team.id()) && context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS))
                {
                    TeamManager.teamCleanUp(level, team);
                    context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.commands.team.removeSuccess", team.name()), true);
                    return 1;
                }
                else
                {
                    context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.modifyPermission"));
                    return 0;
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Error: " + e.getLocalizedMessage()));
            return 0;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> baseCommand = Commands.literal("lithic_claims");

        // Claim Commands
        LiteralArgumentBuilder<CommandSourceStack> claimCommand = Commands.literal("claim").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> claimCommandRemove = Commands.literal("remove");
        LiteralArgumentBuilder<CommandSourceStack> claimCommandInfo = Commands.literal("info");
        LiteralArgumentBuilder<CommandSourceStack> claimCommandList = Commands.literal("list");

        claimCommand.then(claimCommandRemove.then(Commands.argument("location", BlockPosArgument.blockPos()).executes(LithicClaimsCommands::executeClaimRemove)));
        claimCommand.then(claimCommandInfo.then(Commands.argument("location", BlockPosArgument.blockPos()).executes(LithicClaimsCommands::executeClaimInfo)));
        claimCommand.then(claimCommandList.executes(LithicClaimsCommands::executeListClaims));

        // Claim Modify
        LiteralArgumentBuilder<CommandSourceStack> claimCommandModify = Commands.literal("modify");
        LiteralArgumentBuilder<CommandSourceStack> claimCommandModifyOwner = Commands.literal("owner");
        LiteralArgumentBuilder<CommandSourceStack> claimCommandModifyArea = Commands.literal("area");

        claimCommand.then(claimCommandModify.then(Commands.argument("location", BlockPosArgument.blockPos()).then(claimCommandModifyOwner.then(Commands.argument("name | uuid", StringArgumentType.string()).executes(LithicClaimsCommands::executeClaimModifyOwner)))));
        claimCommand.then(claimCommandModify.then(Commands.argument("location", BlockPosArgument.blockPos()).then(claimCommandModifyArea.then(Commands.argument("radius", IntegerArgumentType.integer(0, 128)).executes(LithicClaimsCommands::executeClaimModifyArea)))));

        // Team Commands
        LiteralArgumentBuilder<CommandSourceStack> teamCommand = Commands.literal("team");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandRemove = Commands.literal("remove").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> teamCommandResetCooldown = Commands.literal("reset_cooldown").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> teamCommandCreate = Commands.literal("create");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandLeave = Commands.literal("leave");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandList = Commands.literal("list");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandRelations = Commands.literal("relations");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandKick = Commands.literal("kick");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandInfo = Commands.literal("info");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandDisband = Commands.literal("disband");

        teamCommand.then(teamCommandCreate.then(Commands.argument("name", StringArgumentType.string()).then(Commands.argument("stance", StringArgumentType.string()).then(Commands.argument("colour", IntegerArgumentType.integer(0, 16777215)).executes(LithicClaimsCommands::executeTeamCreate)))));
        teamCommand.then(teamCommandRelations.then(Commands.argument("name | uuid", StringArgumentType.string()).then(Commands.argument("stance", StringArgumentType.string()).executes(LithicClaimsCommands::executeTeamRelations))));
        teamCommand.then(teamCommandKick.then(Commands.argument("name | uuid", StringArgumentType.string()).then(Commands.argument("player", EntityArgument.player())).executes(LithicClaimsCommands::executeTeamKick)));
        teamCommand.then(teamCommandResetCooldown.then(Commands.argument("name | uuid", StringArgumentType.string()).executes(LithicClaimsCommands::executeTeamResetCooldown)));
        teamCommand.then(teamCommandDisband.then(Commands.argument("name | uuid", StringArgumentType.string())).executes(LithicClaimsCommands::executeTeamDisband));
        teamCommand.then(teamCommandInfo.executes(LithicClaimsCommands::executeTeamInfo));
        teamCommand.then(teamCommandList.executes(LithicClaimsCommands::executeListTeams));
        teamCommand.then(teamCommandLeave.executes(LithicClaimsCommands::executeTeamLeave));

        // Team Modify
        LiteralArgumentBuilder<CommandSourceStack> teamCommandModify = Commands.literal("modify");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandModifyLeader = Commands.literal("leader");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandModifyName = Commands.literal("name");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandModifyStance = Commands.literal("stance");
        LiteralArgumentBuilder<CommandSourceStack> teamCommandModifyColour = Commands.literal("colour");

        teamCommandModify.then(Commands.argument("name | uuid", StringArgumentType.string()).then(teamCommandModifyLeader.then(Commands.argument("player", EntityArgument.player()).executes(LithicClaimsCommands::executeTeamModifyLeader))));
        teamCommandModify.then(Commands.argument("name | uuid", StringArgumentType.string()).then(teamCommandModifyName.then(Commands.argument("name", StringArgumentType.string()).executes(LithicClaimsCommands::executeTeamModifyName))));
        teamCommandModify.then(Commands.argument("name | uuid", StringArgumentType.string()).then(teamCommandModifyStance.then(Commands.argument("stance", StringArgumentType.string()).executes(LithicClaimsCommands::executeTeamModifyStance))));
        teamCommandModify.then(Commands.argument("name | uuid", StringArgumentType.string()).then(teamCommandModifyColour.then(Commands.argument("colour", IntegerArgumentType.integer(0, 16777215)).executes(LithicClaimsCommands::executeTeamModifyColour))));
        teamCommand.then(teamCommandModify);

        baseCommand.then(claimCommand);
        baseCommand.then(teamCommand);

        dispatcher.register(baseCommand);
    }
}
