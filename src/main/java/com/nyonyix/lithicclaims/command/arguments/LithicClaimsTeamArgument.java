package com.nyonyix.lithicclaims.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nyonyix.lithicclaims.data.manager.TeamManager;
import com.nyonyix.lithicclaims.data.record.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LithicClaimsTeamArgument implements ArgumentType<String>
{
    private static final Collection<String> EXAMPLES = List.of("Gardeners", "\"Moltin Empire\"", "97c8e614-6624-4d36-92a7-e86725b0df3e");

    public static final DynamicCommandExceptionType ERROR_TEAM_NOT_FOUND = new DynamicCommandExceptionType(name -> Component.translatableEscape("lithicclaims.argument.team.notFound", name));
    public static final SimpleCommandExceptionType ERROR_NOT_IN_TEAM = new SimpleCommandExceptionType(Component.translatableEscape("lithicclaims.argument.team.notInTeam"));

    public static LithicClaimsTeamArgument team()
    {
        return new LithicClaimsTeamArgument();
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> teamArgument(String name)
    {
        return Commands.argument(name, team()).suggests(((context, builder) -> suggest(context.getSource(), builder)));
    }

    public static Team getTeam(CommandContext<CommandSourceStack> context, String argName) throws CommandSyntaxException
    {
        Level level = context.getSource().getLevel();
        String argument = context.getArgument(argName, String.class);

        Team byName = TeamManager.getTeamByName(level, argument);
        if (!byName.id().equals(Team.ZERO_UUID)) return byName;

        try
        {
            Team byUUID = TeamManager.getTeam(level, UUID.fromString(argument));
            if (!byUUID.id().equals(Team.ZERO_UUID)) return byUUID;
        }
        catch (IllegalArgumentException e) {}

        throw ERROR_TEAM_NOT_FOUND.create(argument);
    }

    public static Team getPlayerTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        Team team = TeamManager.getTeamByPlayer(context.getSource().getLevel(), context.getSource().getPlayerOrException().getUUID());
        if (team.id().equals(Team.ZERO_UUID)) throw ERROR_NOT_IN_TEAM.create();

        return team;
    }

    public static CompletableFuture<Suggestions> suggest(CommandSourceStack source, SuggestionsBuilder builder)
    {
        String remaining = builder.getRemainingLowerCase();

        for (Team team : TeamManager.getActiveTeams(source.getLevel()).values())
        {
            String suggestion = StringArgumentType.escapeIfRequired(team.name());

            if (suggestion.toLowerCase(Locale.ROOT).contains(remaining)) builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException
    {
        return reader.readString();
    }

    @Override
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }
}
