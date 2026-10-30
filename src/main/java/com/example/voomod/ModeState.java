package com.example.voomod;

import net.minecraft.entity.player.PlayerEntity;

public final class ModeState {
    public static final String TAG = "voomod_active";

    /** Só usado no cliente (jogador local). */
    public static boolean clientActive = false;

    private ModeState() {}

    public static boolean isActive(PlayerEntity p) {
        if (p.getWorld().isClient) {
            return p.isMainPlayer() && clientActive;
        }
        return p.getCommandTags().contains(TAG);
    }
}
