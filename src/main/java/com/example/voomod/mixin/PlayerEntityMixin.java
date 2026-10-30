package com.example.voomod.mixin;

import com.example.voomod.ModeState;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    // PlayerEntity.tick() zera noClip/onGround no começo; religamos logo antes do movimento (super.tick()).
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tick()V"))
    private void voomod$modo(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!ModeState.isActive(self)) return;

        // Atravessar blocos
        self.noClip = true;

        // Com noClip o "onGround" nunca é atualizado e ficava preso em true,
        // o que cancelava o voo e fazia afundar no chão. Igual ao modo espectador:
        self.setOnGround(false);

        // Voo sempre ligado enquanto estiver no modo (espaço sobe, shift desce)
        PlayerAbilities a = self.getAbilities();
        a.allowFlying = true;
        a.flying = true;
    }
}
