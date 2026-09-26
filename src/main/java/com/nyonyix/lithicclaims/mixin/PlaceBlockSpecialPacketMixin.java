package com.nyonyix.lithicclaims.mixin;

import com.nyonyix.lithicclaims.common.LithicClaimsCommon;
import net.dries007.tfc.network.PlaceBlockSpecialPacket;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlaceBlockSpecialPacket.class)
public class PlaceBlockSpecialPacketMixin
{
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void lithicClaimsDenyInClaim(ServerPlayer player, CallbackInfo ci)
    {
        Level level = player.level();

        if (player == null) return;

        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult bhr) || bhr.getDirection() != Direction.UP) return;
        if (LithicClaimsCommon.isDenied(level, bhr.getBlockPos().above(), player) || LithicClaimsCommon.isDenied(level, bhr.getBlockPos(), player))
        {
            ci.cancel();
        }
    }
}
