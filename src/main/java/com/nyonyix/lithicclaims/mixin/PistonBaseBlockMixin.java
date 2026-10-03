package com.nyonyix.lithicclaims.mixin;

import com.nyonyix.lithicclaims.common.LithicClaimsCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin
{
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void lithicClaimsDontMoveInClaim(BlockState state, Level level, BlockPos pos, Direction movementDirection, boolean allowDestroy, Direction pistonFacing, CallbackInfoReturnable<Boolean> cir)
    {
        if (!LithicClaimsCommon.isDenied(level, pos, null) && LithicClaimsCommon.isDenied(level, pos.relative(movementDirection), null))
        {
            cir.setReturnValue(false);
        }
    }
}
