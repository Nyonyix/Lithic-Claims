package com.nyonyix.lithicclaims.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.nyonyix.lithicclaims.LithicClaims;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class LithicClaimsArgumentTypes
{
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, LithicClaims.MODID);
    public static final Supplier<SingletonArgumentInfo<LithicClaimsTeamArgument>> TEAM = ARGUMENT_TYPES.register("team", () -> ArgumentTypeInfos.registerByClass(LithicClaimsTeamArgument.class, SingletonArgumentInfo.contextFree(LithicClaimsTeamArgument::team)));
    public static final Supplier<SingletonArgumentInfo<LithicClaimsStanceArgument>> STANCE = ARGUMENT_TYPES.register("stance", () -> ArgumentTypeInfos.registerByClass(LithicClaimsStanceArgument.class, SingletonArgumentInfo.contextFree(LithicClaimsStanceArgument::stance)));
}
