package com.example.voomod.client;

import com.example.voomod.ModeState;
import com.example.voomod.VooMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class VooModClient implements ClientModInitializer {
    private static KeyBinding exitKey;

    @Override
    public void onInitializeClient() {
        VooMod.openScreenHook = pos -> MinecraftClient.getInstance().setScreen(new UpgradeScreen(pos));

        exitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voomod.exit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.voomod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (exitKey.wasPressed()) {
                if (ModeState.clientActive && client.player != null) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(false);
                    buf.writeBlockPos(BlockPos.ORIGIN);
                    ClientPlayNetworking.send(VooMod.TOGGLE_PACKET, buf);
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(VooMod.SYNC_PACKET, (client, handler, buf, sender) -> {
            boolean on = buf.readBoolean();
            client.execute(() -> {
                ModeState.clientActive = on;
                client.options.setPerspective(on ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON);
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ModeState.clientActive = false);
    }
}
