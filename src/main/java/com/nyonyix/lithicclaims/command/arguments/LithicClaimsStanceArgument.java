package com.nyonyix.lithicclaims.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.nyonyix.lithicclaims.data.Stance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class LithicClaimsStanceArgument implements ArgumentType<Stance>
{
    private static final Collection<String> EXAMPLES = List.of("hostile", "Neutral", "PEACEFUL");

    public static final DynamicCommandExceptionType ERROR_INVALID_STANCE = new DynamicCommandExceptionType(value -> Component.translatableEscape("lithicclaims.argument.stance.invalid", value));

    public static LithicClaimsStanceArgument stance()
    {
        return new LithicClaimsStanceArgument();
    }

    public static Stance getStance(CommandContext<CommandSourceStack> context, String argName)
    {
        return context.getArgument(argName, Stance.class);
    }

    @Override
    public Stance parse(StringReader reader) throws CommandSyntaxException
    {
        int start = reader.getCursor();
        String value = reader.readUnquotedString();

        for (Stance stance : Stance.values())
        {
            if (stance.equals(Stance.INVALID)) continue;
            if (stance.name().equalsIgnoreCase(value)) return stance;
        }

        reader.setCursor(start);
        throw ERROR_INVALID_STANCE.createWithContext(reader, value);
    }

    @Override
    public <S>CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(Arrays.stream(Stance.values()).filter(stance -> !stance.equals(Stance.INVALID)).map(stance -> stance.name().toLowerCase(Locale.ROOT)), builder);
    }

    @Override
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }
}
