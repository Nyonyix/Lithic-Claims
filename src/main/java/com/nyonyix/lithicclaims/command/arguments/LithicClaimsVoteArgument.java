package com.nyonyix.lithicclaims.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nyonyix.lithicclaims.data.Stance;
import com.nyonyix.lithicclaims.data.Vote;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class LithicClaimsVoteArgument implements ArgumentType<Vote>
{
    private static final Collection<String> EXAMPLES = List.of("yay", "Nay");

    public static final DynamicCommandExceptionType ERROR_INVALID_VOTE = new DynamicCommandExceptionType(value -> Component.translatableEscape("lithicclaims.argument.vote.invalid", value));

    public static LithicClaimsVoteArgument vote()
    {
        return new LithicClaimsVoteArgument();
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Vote> voteArgument(String vote)
    {
        return Commands.argument(vote, vote());
    }

    public static Vote getVote(CommandContext<CommandSourceStack> context, String argName)
    {
        return context.getArgument(argName, Vote.class);
    }

    @Override
    public Vote parse(StringReader reader) throws CommandSyntaxException
    {
        int start = reader.getCursor();
        String value = reader.readUnquotedString();

        for (Vote vote : Vote.values())
        {
            if (vote.equals(Vote.INVALID)) continue;
            if (vote.name().equalsIgnoreCase(value)) return vote;
        }

        reader.setCursor(start);
        throw ERROR_INVALID_VOTE.createWithContext(reader, value);
    }

    @Override
    public <S>CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(Arrays.stream(Vote.values()).filter(vote -> !vote.equals(Vote.INVALID)).map(vote -> vote.name().toLowerCase(Locale.ROOT)), builder);
    }

    @Override
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }
}
