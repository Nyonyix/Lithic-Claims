package com.nyonyix.lithicclaims.mixin;

import com.nyonyix.lithicclaims.LithicClaims;
import com.nyonyix.lithicclaims.data.manager.ClaimManager;
import net.dries007.tfc.util.PowderKegExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PowderKegExplosion.class)
public abstract class PowderKegExplosionMixin
{
    @Shadow @Final private Level level;
    @Shadow @Final private Entity source;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void finalizeExplosionIntercept(boolean spawnParticles, CallbackInfo ci)
    {
//        getToBlow().forEach(pos -> ClaimManager.claimCleanUp(level, pos));
        PowderKegExplosion explosion = (PowderKegExplosion) (Object) this;

        for (BlockPos pos : explosion.getToBlow())
        {
            ClaimManager.claimCleanUp(level, pos);
            LithicClaims.LOGGER.info(pos.toString());
        }
    }
}
