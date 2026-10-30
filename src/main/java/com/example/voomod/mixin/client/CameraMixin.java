package com.example.voomod.mixin.client;

import com.example.voomod.ModeState;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    // ===== AJUSTE AQUI =====
    private static final double DISTANCIA = 2.4;        // vanilla = 4.0 (menor = mais perto)
    private static final double OMBRO = -0.6;           // negativo = jogador aparece à esquerda; positivo = à direita
    private static final double ALTURA = 0.1;           // sobe/desce a câmera
    // =======================

    @Shadow private boolean thirdPerson;

    @Shadow protected abstract void moveBy(double x, double y, double z);

    @ModifyArg(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(D)D"),
            index = 0)
    private double voomod$closer(double original) {
        return ModeState.clientActive ? DISTANCIA : original;
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void voomod$shoulder(CallbackInfo ci) {
        if (ModeState.clientActive && this.thirdPerson) {
            this.moveBy(0.0, ALTURA, OMBRO);
        }
    }
}
