package com.nyonyix.lithicclaims.command;

import com.ibm.icu.impl.duration.DurationFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.nyonyix.lithicclaims.command.arguments.LithicClaimsStanceArgument;
import com.nyonyix.lithicclaims.command.arguments.LithicClaimsTeamArgument;
import com.nyonyix.lithicclaims.command.arguments.LithicClaimsVoteArgument;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.TeamVote;
import com.nyonyix.lithicclaims.data.attachment.PlayerAttachment;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import com.nyonyix.lithicclaims.data.manager.PlayerManager;
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
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.UsernameCache;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.core.jmx.Server;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class LithicClaimsCommands
{
    @FunctionalInterface
    private interface ClaimRun {int run(CommandContext<CommandSourceStack> context, Claim claim) throws CommandSyntaxException;}
    @FunctionalInterface
    private interface PlayerRun {int run(CommandContext<CommandSourceStack> context, Player claim) throws CommandSyntaxException;}
    @FunctionalInterface
    private interface TeamRun {int run(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException;}

    // value == null takes no value arg
    // bare == null must have args
    private record ClaimCommand(Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> value, ClaimRun bare, ClaimRun action){}

    // value == null takes no value arg
    // bare == null must have args
    private record PlayerCommand(Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> value, PlayerRun bare, PlayerRun action){}

    // value == null takes no value arg
    // bare == null must have args
    // explicitOp has trailing team argument and requires op or P2
    private record TeamCommand(Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> value, TeamRun bare, TeamRun action, boolean explicitOp) {}

    private static final Map<String, ClaimCommand> CLAIM_COMMANDS = new HashMap<>();
    private static final Map<String, PlayerCommand> PLAYER_COMMANDS = new HashMap<>();
    private static final Map<String, TeamCommand> TEAM_COMMANDS = new HashMap<>();

    private static final DynamicCommandExceptionType ERROR_CLAIM_NOT_FOUND = new DynamicCommandExceptionType(name -> Component.translatableEscape("lithicclaims.argument.claim.notFound", name));
    private static final DynamicCommandExceptionType ERROR_PLAYER_NOT_IN_TEAM = new DynamicCommandExceptionType(player -> Component.translatable("lithicclaims.command.team.playerNotInTeam", player));
    private static final SimpleCommandExceptionType ERROR_NOT_IN_CLAIM = new SimpleCommandExceptionType(Component.translatableEscape("lithicclaims.argument.claim.notInClaim"));
    private static final SimpleCommandExceptionType ERROR_NOT_LEADER = new SimpleCommandExceptionType(Component.translatableEscape("lithicclaims.command.team.notLeader"));
    private static final SimpleCommandExceptionType ERROR_NO_ACTIVE_VOTE = new SimpleCommandExceptionType(Component.translatableEscape("lithicclaims.command.team.voteNone"));
    private static final SimpleCommandExceptionType ERROR_EXISTING_ACTIVE_VOTE = new SimpleCommandExceptionType(Component.translatableEscape("lithicclaims.command.team.voteExisting"));

    static
    {
        CLAIM_COMMANDS.put("owner", new ClaimCommand(() -> LithicClaimsTeamArgument.teamArgument("team"), LithicClaimsCommands::getOwner, LithicClaimsCommands::setOwner));
        CLAIM_COMMANDS.put("area", new ClaimCommand(() -> Commands.argument("radius", IntegerArgumentType.integer(32, 128)), LithicClaimsCommands::getArea, LithicClaimsCommands::setArea));
        CLAIM_COMMANDS.put("remove", new ClaimCommand(null, LithicClaimsCommands::remove, LithicClaimsCommands::remove));
        CLAIM_COMMANDS.put("info", new ClaimCommand(null, LithicClaimsCommands::info, LithicClaimsCommands::info));
    }

    static
    {
        PLAYER_COMMANDS.put("last_aggressive", new PlayerCommand(null, LithicClaimsCommands::playerLastAggressive, LithicClaimsCommands::playerLastAggressive));
        PLAYER_COMMANDS.put("team", new PlayerCommand(null, LithicClaimsCommands::playerTeam, LithicClaimsCommands::playerTeam));
    }

    static
    {
        TEAM_COMMANDS.put("leader", new TeamCommand(() -> Commands.argument("player", EntityArgument.player()), LithicClaimsCommands::getLeader, LithicClaimsCommands::setLeader, true));
        TEAM_COMMANDS.put("name", new TeamCommand(() -> Commands.argument("name", StringArgumentType.string()), LithicClaimsCommands::getName, LithicClaimsCommands::setName, true));
        TEAM_COMMANDS.put("stance", new TeamCommand(() -> Commands.argument("stance", LithicClaimsStanceArgument.stance()), LithicClaimsCommands::getStance, LithicClaimsCommands::setStance, true));
        TEAM_COMMANDS.put("colour", new TeamCommand(() -> Commands.argument("colour", IntegerArgumentType.integer(0, 16777215)), LithicClaimsCommands::getColour, LithicClaimsCommands::setColour, true));
        TEAM_COMMANDS.put("kick", new TeamCommand(() -> Commands.argument("player", EntityArgument.player()), null, LithicClaimsCommands::kick, true));
        TEAM_COMMANDS.put("disband", new TeamCommand(null, LithicClaimsCommands::disband, LithicClaimsCommands::disband, true));
        TEAM_COMMANDS.put("info", new TeamCommand(null, LithicClaimsCommands::info, LithicClaimsCommands::info, false));
    }

    //
    // CLAIM STUFFS
    //

    private static Claim implicitClaim(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Claim claim = ClaimManager.getClaimContains(context.getSource().getLevel(), context.getSource().getPlayerOrException().blockPosition());

        if (claim.owner().equals(Team.ZERO_UUID)) throw ERROR_NOT_IN_CLAIM.create();
        return claim;
    }

    private static Claim claimAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        BlockPos location = BlockPosArgument.getBlockPos(context, "location");
        Claim claim = ClaimManager.getClaimContains(context.getSource().getLevel(), location);

        if (claim.owner().equals(Team.ZERO_UUID)) throw ERROR_CLAIM_NOT_FOUND.create(location.toShortString());
        return claim;
    }

    private static void attachClaim(LiteralArgumentBuilder<CommandSourceStack> parent, String name, ClaimCommand def)
    {
        RequiredArgumentBuilder<CommandSourceStack, Coordinates> location = Commands.argument("location", BlockPosArgument.blockPos()).executes(context -> def.action().run(context, claimAt(context)));
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(name);

        if (def.bare() != null) literal.executes(context -> def.bare().run(context, implicitClaim(context)));

        if (def.value() == null)
        {
            literal.then(location);
        }
        else
        {
            RequiredArgumentBuilder<CommandSourceStack, ?> value = def.value().get();
            value.executes(context -> def.action().run(context, implicitClaim(context)));
            value.then(location);
            literal.then(value);
        }

        parent.then(literal);
    }

    private static Component claimInfo(Level level, Claim claim)
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
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lithicclaims.command.claim.teleport"))));

        return Component.literal("Claim: \n").withStyle(ChatFormatting.AQUA)
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
    }

    // Getters

    private static int getOwner(CommandContext<CommandSourceStack> context, Claim claim)
    {
        Team owner = TeamManager.getTeam(context.getSource().getLevel(), claim.owner());

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.getOwner", owner.name()), false);
        return 1;
    }

    private static int getArea(CommandContext<CommandSourceStack> context, Claim claim)
    {
        int radius = (int) (claim.claimArea().getXsize() / 2.0);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.getArea", radius), false);
        return 1;
    }

    // Setters

    private static int setOwner(CommandContext<CommandSourceStack> context, Claim claim) throws CommandSyntaxException
    {
        Level level = context.getSource().getLevel();
        Team newOwner = LithicClaimsTeamArgument.getTeam(context, "team");
        Team oldOwner = TeamManager.getTeam(level, claim.owner());

        if (claim.owner().equals(newOwner.id()))
        {
            context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.setOwnerFail", newOwner.name()));
            return 0;
        }

        List<BlockPos> newOwned = new ArrayList<>(newOwner.ownedClaims());
        List<BlockPos> oldOwned = new ArrayList<>(oldOwner.ownedClaims());
        newOwned.add(claim.location());
        oldOwned.remove(claim.location());

        ClaimManager.saveAttachment(level, claim.withOwner(newOwner.id()));
        TeamManager.saveAttachment(level, newOwner.withOwnedClaims(newOwned));
        TeamManager.saveAttachment(level, oldOwner.withOwnedClaims(oldOwned));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.setOwner", newOwner.name()), false);
        return 1;
    }

    private static int setArea(CommandContext<CommandSourceStack> context, Claim claim)
    {
        int radius = IntegerArgumentType.getInteger(context, "radius") * 2;
        BlockPos pos = claim.location();

        AABB newArea = AABB.ofSize(Vec3.atCenterOf(pos), radius, radius, radius);
        ClaimManager.saveAttachment(context.getSource().getLevel(), claim.withClaimArea(newArea));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.setArea", radius / 2), false);
        return 1;
    }

    // Non-Get/Set

    private static int remove(CommandContext<CommandSourceStack> context, Claim claim)
    {
        ClaimManager.claimCleanUp(context.getSource().getLevel(), claim);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.claim.remove", claim.location().toShortString()), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context, Claim claim)
    {
        context.getSource().sendSuccess(() -> claimInfo(context.getSource().getLevel(), claim), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context)
    {
        Level level = context.getSource().getLevel();
        Map<BlockPos, Claim> activeClaims = ClaimManager.getActiveClaims(level);

        if (activeClaims.isEmpty())
        {
            context.getSource().sendFailure(Component.translatable("lithicclaims.command.claim.listFail"));
            return 0;
        }

        for (Claim claim : activeClaims.values())
        {
            context.getSource().sendSuccess(() -> claimInfo(level, claim), false);
        }

        return 1;
    }

    //
    // Player Stuffs
    //

    private static void attachPlayer(LiteralArgumentBuilder<CommandSourceStack> parent, String name, PlayerCommand def)
    {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> player = Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> def.action().run(context, EntityArgument.getPlayer(context, "player")));
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(name);

        if (def.bare() != null) literal.executes(context -> def.bare().run(context, context.getSource().getPlayerOrException()));

        if (def.value() == null)
        {
            literal.then(player);
        }
        else
        {
            RequiredArgumentBuilder<CommandSourceStack, ?> value = def.value().get();
            value.executes(context -> def.action().run(context, context.getSource().getPlayerOrException()));
            value.then(player);
            literal.then(value);
        }

        parent.then(literal);
    }

    private static String getUsername(UUID playerUUID)
    {
        return UsernameCache.containsUUID(playerUUID) ? UsernameCache.getLastKnownUsername(playerUUID) : "Unknown";
    }

    private static int playerLastAggressive(CommandContext<CommandSourceStack> context, Player player) throws CommandSyntaxException
    {
        PlayerAttachment data = PlayerManager.getPlayerData(player);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.player.lastAggressive", getUsername(player.getUUID()), Duration.between(data.lastAggressive(), Instant.now()).toString()), false);
        return 1;
    }

    private static int playerResetAggression(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Player player = EntityArgument.getPlayer(context, "player");
        PlayerAttachment data = PlayerManager.getPlayerData(player);

        PlayerManager.saveAttachment(player, data.withLastAggressive(Instant.now().minus(Duration.ofMinutes(ServerConfig.TEAM_STANCE_COOLDOWN.getAsLong()))));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.player.resetAggression", getUsername(player.getUUID())), false);
        return 1;
    }

    private static int playerTeam(CommandContext<CommandSourceStack> context, Player player) throws CommandSyntaxException
    {
        Team team = TeamManager.getTeamByPlayer(context.getSource().getLevel(), player.getUUID());

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.player.team", team.name(), getUsername(player.getUUID())), false);
        return 1;
    }

    //
    // Team Stuffs
    //

    private static void requireLeader(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        if (context.getSource().hasPermission(Commands.LEVEL_GAMEMASTERS)) return;

        if (context.getSource().isPlayer())
        {
            Player player = context.getSource().getPlayerOrException();
            Level level = context.getSource().getLevel();

            if (TeamManager.isInTeam(level, player.getUUID(), team.id()) && TeamManager.getLeaderUUID(level, team.id()).equals(player.getUUID())) return;
        }

        throw ERROR_NOT_LEADER.create();
    }

    private static void attachTeam(LiteralArgumentBuilder<CommandSourceStack> parent, String name, TeamCommand def)
    {
        RequiredArgumentBuilder<CommandSourceStack, String> target = LithicClaimsTeamArgument.teamArgument("team").executes(context -> def.action().run(context, LithicClaimsTeamArgument.getTeam(context, "team")));

        if (def.explicitOp()) target.requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS));

        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(name);

        if (def.bare() != null)
        {
            literal.executes(context -> def.bare().run(context, LithicClaimsTeamArgument.getPlayerTeam(context)));
        }

        if (def.value() == null)
        {
            literal.then(target);
        }
        else
        {
            RequiredArgumentBuilder<CommandSourceStack, ?> value = def.value().get();
            value.executes(context -> def.action().run(context, LithicClaimsTeamArgument.getPlayerTeam(context)));
            value.then(target);
            literal.then(value);
        }

        parent.then(literal);
    }

    private static String displayName(UUID player)
    {
        return UsernameCache.containsUUID(player) ? UsernameCache.getLastKnownUsername(player) : "Unknown";
    }

    private static String titleCase(Stance stance)
    {
        String name = stance.name().toUpperCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ROOT);
    }

//    private static String parseInstant(Instant cooldown)
//    {
//        float stanceCooldownConfig = (float) ServerConfig.TEAM_STANCE_COOLDOWN.getAsLong();
//
//        Duration cooldownDiff = Duration.between(Instant.now(), cooldown.plusSeconds(Math.round(stanceCooldownConfig * 60 * 60)));
//        boolean ago = cooldownDiff.isNegative();
//        long total = Math.abs(cooldownDiff.toSeconds());
//        long h = total / 3600;
//        long m = (total % 3600) / 60;
//        long s = total % 60;
//        String time = String.format("%dh, %dm, %ds", h, m, s);
//        return ago ? time + " ago" : "until: " + time;
//    }

    private static String formatDuationString(Duration dur)
    {
        long s = dur.getSeconds();
        return DurationFormatUtils.formatDuration(dur.toMillis(), "HH:mm:ss");
//        return DurationFormatUtils.formatDurationHMS(dur.toMillis());
//        return String.format("%d:%02:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    private static Component teamInfo(Team team)
    {
        return Component.literal("Team: \n").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Leader: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%s\n", displayName(team.leader()))).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Name: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%s\n", team.name())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Number Of Members: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.format("%d\n", team.members().size())).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Stance: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(titleCase(team.stance())).withStyle(ChatFormatting.GREEN)))))))));
    }

    // Getters

    private static int getLeader(CommandContext<CommandSourceStack> context, Team team)
    {
        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.getLeader", displayName(team.leader())), false);
        return 1;
    }

    private static int getName(CommandContext<CommandSourceStack> context, Team team)
    {
        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.getName", team.name()), false);
        return 1;
    }

    private static int getStance(CommandContext<CommandSourceStack> context, Team team)
    {
        Duration remaining = Duration.ofMinutes(ServerConfig.TEAM_STANCE_COOLDOWN.getAsLong()).minusMillis(TeamManager.teamStancePlayTime(team).toMillis());
        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.getStance", titleCase(team.stance()), formatDuationString(remaining.isNegative() ? Duration.ZERO : remaining)), false);
        return 1;
    }

    private static int getColour(CommandContext<CommandSourceStack> context, Team team)
    {
        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.getColour", String.format("0x%06X", team.colour())).withColor(team.colour()), false);
        return 1;
    }

    // Setters

    private static int setLeader(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        requireLeader(context, team);

        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Level level = context.getSource().getLevel();

        if (!TeamManager.isInTeam(level, player.getUUID(), team.id())) throw ERROR_PLAYER_NOT_IN_TEAM.create(displayName(player.getUUID()));

        TeamManager.saveAttachment(level, team.withLeader(player.getUUID()));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.setLeader", displayName(player.getUUID()), team.name()), false);
        return 1;
    }

    private static int setName(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        requireLeader(context, team);

        String name = StringArgumentType.getString(context, "name");
        TeamManager.saveAttachment(context.getSource().getLevel(), team.withName(name));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.setName", name, team.name()), false);
        return 1;
    }

    private static int setStance(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        requireLeader(context, team);

        Stance stance = LithicClaimsStanceArgument.getStance(context, "stance");
        Level level = context.getSource().getLevel();

        switch (TeamManager.changeTeamStance(level, team, stance))
        {
            case ON_COOLDOWN ->
            {
                Duration remaining = Duration.ofMinutes(ServerConfig.TEAM_STANCE_COOLDOWN.getAsLong()).minusMillis(TeamManager.teamStancePlayTime(team).toMillis());
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.stancelastChange", formatDuationString(remaining.isNegative() ? Duration.ZERO : remaining)));
                return 0;
            }
            case NOT_ALLOWED ->
            {
                context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.stanceNotAllowed"));
                return 0;
            }
            default ->
            {
                context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.setStance",titleCase(stance), team.name()), false);
                return 1;
            }
        }
    }

    private static int setColour(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        requireLeader(context, team);

        int colour = IntegerArgumentType.getInteger(context, "colour");
        TeamManager.saveAttachment(context.getSource().getLevel(), team.withColour(colour));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.setColour", String.format("0x%06X", colour)).withColor(colour), false);
        return 1;
    }

    // Non-Get/Set

    private static int kick(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        requireLeader(context, team);

        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Level level = context.getSource().getLevel();

        if (!team.members().contains(player.getUUID())) throw ERROR_PLAYER_NOT_IN_TEAM.create(displayName(player.getUUID()));

        TeamManager.removeMember(level, player);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.kick", displayName(player.getUUID()), team.name()), false);
        player.displayClientMessage(Component.translatable("lithicclaims.command.team.kicked", team.name()).withStyle(ChatFormatting.RED), false);
        return 1;
    }

    private static int disband(CommandContext<CommandSourceStack> context, Team team) throws CommandSyntaxException
    {
        requireLeader(context, team);

        TeamManager.teamCleanUp(context.getSource().getLevel(), team);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.disband", team.name()), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context, Team team)
    {
        context.getSource().sendSuccess(() -> teamInfo(team), false);
        return 1;
    }

    private static int listTeams(CommandContext<CommandSourceStack> context)
    {
        Map<UUID, Team> activeTeams = TeamManager.getActiveTeams(context.getSource().getLevel());

        if (activeTeams.isEmpty())
        {
            context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.listFail"));
            return 0;
        }

        for (Team team : activeTeams.values())
        {
            context.getSource().sendSuccess(() -> teamInfo(team), false);
        }

        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Level level = context.getSource().getLevel();
        Team team = LithicClaimsTeamArgument.getPlayerTeam(context);

        TeamManager.removeMember(level, player);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.leave", team.name()), false);
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Level level = context.getSource().getLevel();

        String name = StringArgumentType.getString(context, "name");
        Stance stance = LithicClaimsStanceArgument.getStance(context, "stance");
        int colour = IntegerArgumentType.getInteger(context, "colour");

        Team existing = TeamManager.getTeamByPlayer(level, player.getUUID());
        if (!existing.id().equals(Team.ZERO_UUID))
        {
            context.getSource().sendFailure(Component.translatable("lithicclaims.command.team.createFail", existing.name()));
            return 0;
        }

        Team team = TeamManager.createTeam(level, name, player, stance, colour);
        TeamManager.saveAttachment(level, team);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.createTeam", team.name()), false);
        return 1;
    }

    private static int resetCooldown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Team team = LithicClaimsTeamArgument.getTeam(context, "team");

        TeamManager.saveAttachment(context.getSource().getLevel(), team.withStanceCooldown(Instant.EPOCH));

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.resetCooldown", team.name()), false);
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Team team = LithicClaimsTeamArgument.getTeam(context, "team");

        TeamManager.addMember(context.getSource().getLevel(), context.getSource().getPlayerOrException(), team);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.join", team.name()), false);
        return 1;
    }

    // "pluto" is a nickname for vote. It's an inside joke.
    private static int pluto(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Team team = LithicClaimsTeamArgument.getPlayerTeam(context);
        UUID oldLeader = team.leader();
        TeamVote votes = TeamManager.getTeamVotes(context.getSource().getLevel(), team.id());

        if (votes == null)
        {
            throw ERROR_NO_ACTIVE_VOTE.create();
        }

        votes.castVote(context.getSource().getPlayerOrException().getUUID(), LithicClaimsVoteArgument.getVote(context, "vote"));
        TeamManager.putTeamVote(context.getSource().getLevel(), team.id(), votes);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.vote", UsernameCache.getLastKnownUsername(oldLeader) != null ? UsernameCache.getLastKnownUsername(oldLeader) : "Unknown", UsernameCache.getLastKnownUsername(votes.getNewLeader()) != null ? UsernameCache.getLastKnownUsername(votes.getNewLeader()) : "Unknown"), false);
        return 1;
    }

    private static int plutoCreate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Team team = LithicClaimsTeamArgument.getPlayerTeam(context);
        UUID oldLeader = team.leader();
        UUID newLeader = EntityArgument.getPlayer(context, "new_leader").getUUID();
        TeamVote votes = new TeamVote(team.members(), context.getSource().getServer().getTickCount(), oldLeader, newLeader);

        if (TeamManager.getTeamVotes(context.getSource().getLevel(), team.id()) != null)
        {
            throw ERROR_EXISTING_ACTIVE_VOTE.create();
        }

        votes.castVote(context.getSource().getPlayerOrException().getUUID(), LithicClaimsVoteArgument.getVote(context, "vote"));
        TeamManager.putTeamVote(context.getSource().getLevel(), team.id(), votes);

        context.getSource().sendSuccess(() -> Component.translatable("lithicclaims.command.team.voteCreate", UsernameCache.getLastKnownUsername(oldLeader) != null ? UsernameCache.getLastKnownUsername(oldLeader) : "Unknown", UsernameCache.getLastKnownUsername(newLeader) != null ? UsernameCache.getLastKnownUsername(newLeader) : "Unknown"), false);
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> baseCommand = Commands.literal("lithic_claims");
        LiteralArgumentBuilder<CommandSourceStack> claimCommand = Commands.literal("claim").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> playerCommand = Commands.literal("player");
        LiteralArgumentBuilder<CommandSourceStack> teamCommand = Commands.literal("team");

        for (Map.Entry<String, ClaimCommand> entry : CLAIM_COMMANDS.entrySet())
        {
            attachClaim(claimCommand, entry.getKey(), entry.getValue());
        }

        claimCommand.then(Commands.literal("list").executes(LithicClaimsCommands::list));

        for (Map.Entry<String, PlayerCommand> entry : PLAYER_COMMANDS.entrySet())
        {
            attachPlayer(playerCommand, entry.getKey(), entry.getValue());
        }

        playerCommand.then(Commands.literal("reset_aggression").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("player", EntityArgument.player()).executes(LithicClaimsCommands::playerResetAggression)));

        for (Map.Entry<String, TeamCommand> entry : TEAM_COMMANDS.entrySet())
        {
            attachTeam(teamCommand, entry.getKey(), entry.getValue());
        }

        teamCommand.then(Commands.literal("list").executes(LithicClaimsCommands::listTeams));
        teamCommand.then(Commands.literal("leave").executes(LithicClaimsCommands::leave));
        teamCommand.then(Commands.literal("reset_cooldown").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(LithicClaimsTeamArgument.teamArgument("team").executes(LithicClaimsCommands::resetCooldown)));
        teamCommand.then(Commands.literal("join").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(LithicClaimsTeamArgument.teamArgument("team").executes(LithicClaimsCommands::join)));
        teamCommand.then(Commands.literal("vote").then(LithicClaimsVoteArgument.voteArgument("vote").executes(LithicClaimsCommands::pluto).then(Commands.argument("new_leader", EntityArgument.player()).executes(LithicClaimsCommands::plutoCreate))));

        LiteralArgumentBuilder<CommandSourceStack> createCommand = Commands.literal("create");
        RequiredArgumentBuilder<CommandSourceStack, String> createName = Commands.argument("name", StringArgumentType.string());
        RequiredArgumentBuilder<CommandSourceStack, Stance> createStance = Commands.argument("stance", LithicClaimsStanceArgument.stance());
        RequiredArgumentBuilder<CommandSourceStack, Integer> createColour = Commands.argument("colour", IntegerArgumentType.integer(0, 16777215));

        createColour.executes(LithicClaimsCommands::create);
        createStance.then(createColour);
        createName.then(createStance);
        createCommand.then(createName);
        teamCommand.then(createCommand);

        baseCommand.then(claimCommand);
        baseCommand.then(playerCommand);
        baseCommand.then(teamCommand);
        dispatcher.register(baseCommand);
    }
}
