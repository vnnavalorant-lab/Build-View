package com.example.voomod.client;

import com.example.voomod.ModeState;
import com.example.voomod.VooMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class UpgradeScreen extends Screen {
    private final BlockPos pos;

    public UpgradeScreen(BlockPos pos) {
        super(Text.translatable("screen.voomod.title"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        boolean active = ModeState.clientActive;
        Text label = Text.translatable(active ? "screen.voomod.leave" : "screen.voomod.upgrade");
        this.addDrawableChild(ButtonWidget.builder(label, button -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(!ModeState.clientActive);
            buf.writeBlockPos(pos);
            ClientPlayNetworking.send(VooMod.TOGGLE_PACKET, buf);
            this.close();
        }).dimensions(this.width / 2 - 60, this.height / 2 - 10, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
