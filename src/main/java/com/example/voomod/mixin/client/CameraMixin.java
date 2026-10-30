package com.example.voomod.mixin.client;

import com.example.voomod.ModeState;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    // ===== AJUSTE AQUI (enquadramento da imagem de referência) =====
    private static final double DISTANCIA = 1.4;   // distância atrás do jogador (vanilla = 4.0)
    private static final double OMBRO = -1.0;      // negativo = jogador na ESQUERDA da tela; positivo = DIREITA
    private static final double ALTURA = -0.2;     // negativo = câmera mais baixa (jogador mais alto na tela)
    // ===============================================================

    @Shadow private boolean thirdPerson;

    @Shadow protected abstract void moveBy(double x, double y, double z);

    // A câmera NÃO colide com blocos no modo: a distância fica sempre fixa,
    // então o enquadramento é o mesmo dentro de paredes, no chão, em cavernas, etc.
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void voomod$semColisao(double desired, CallbackInfoReturnable<Double> cir) {
        if (ModeState.clientActive) {
            cir.setReturnValue(DISTANCIA);
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void voomod$ombro(CallbackInfo ci) {
        if (ModeState.clientActive && this.thirdPerson) {
            this.moveBy(0.0, ALTURA, OMBRO);
        }
    }
}
