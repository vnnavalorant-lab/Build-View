package com.example.voomod.mixin;

import com.example.voomod.ModeState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    // PlayerEntity.tick() zera noClip no começo; religamos logo antes do movimento (super.tick()).
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tick()V"))
    private void voomod$noClip(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (ModeState.isActive(self)) {
            self.noClip = true;
        }
    }
}
