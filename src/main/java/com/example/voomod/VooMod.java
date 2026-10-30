package com.example.voomod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.function.Consumer;

public class VooMod implements ModInitializer {
    public static final String MOD_ID = "voomod";
    public static final Identifier TOGGLE_PACKET = new Identifier(MOD_ID, "toggle");
    public static final Identifier SYNC_PACKET = new Identifier(MOD_ID, "sync");

    /** Distância máxima (em blocos) do bloco de origem. Passou disso, sai do modo e volta. */
    public static final double MAX_DISTANCIA = 40.0;

    public static Block UPGRADE_BLOCK;
    public static Item UPGRADE_BLOCK_ITEM;

    /** O cliente troca isso para abrir a tela. */
    public static Consumer<BlockPos> openScreenHook = pos -> {};

    @Override
    public void onInitialize() {
        UPGRADE_BLOCK = Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "upgrade_block"),
                new UpgradeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f, 6.0f)));
        UPGRADE_BLOCK_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "upgrade_block"),
                new BlockItem(UPGRADE_BLOCK, new Item.Settings()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> entries.add(UPGRADE_BLOCK_ITEM));

        // Cliente pediu para ligar/desligar o modo
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PACKET, (server, player, handler, buf, sender) -> {
            boolean enable = buf.readBoolean();
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> {
                if (enable) {
                    boolean nearBlock = player.getWorld().getBlockState(pos).isOf(UPGRADE_BLOCK)
                            && player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 64.0;
                    if (nearBlock && !ModeState.isActive(player)) {
                        enter(player, pos);
                    }
                } else if (ModeState.isActive(player)) {
                    leave(player, null);
                }
            });
        });

        // Reaplicar ao entrar no mundo
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (ModeState.isActive(p)) {
                if (OriginData.of(server).get(p.getUuid()) == null) {
                    leave(p, null); // sem origem salva: sai do modo
                } else {
                    applyOn(p);
                }
            } else {
                sendSync(p, false);
            }
        });

        // Respawn (ex.: /kill) tira o modo
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            newP.removeScoreboardTag(ModeState.TAG);
            OriginData.of(newP.server).remove(newP.getUuid());
            sendSync(newP, false);
        });

        // Sem dano enquanto estiver no modo (só /kill passa)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity p && ModeState.isActive(p)) {
                return source.isOf(DamageTypes.GENERIC_KILL);
            }
            return true;
        });

        // Limite de área, sem queda, resgate do void
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            OriginData data = OriginData.of(server);
            for (ServerPlayerEntity p : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
                if (!ModeState.isActive(p)) continue;

                OriginData.Origin o = data.get(p.getUuid());
                if (o == null) {
                    leave(p, null);
                    continue;
                }

                boolean outraDimensao = !p.getWorld().getRegistryKey().getValue().toString().equals(o.dim);
                double dx = p.getX() - (o.block.getX() + 0.5);
                double dy = p.getY() - (o.block.getY() + 0.5);
                double dz = p.getZ() - (o.block.getZ() + 0.5);
                boolean longe = dx * dx + dy * dy + dz * dz > MAX_DISTANCIA * MAX_DISTANCIA;

                if (outraDimensao || longe) {
                    leave(p, "Você saiu da área permitida (" + (int) MAX_DISTANCIA + " blocos). Voltando ao ponto de origem.");
                    continue;
                }

                p.fallDistance = 0;
                ServerWorld w = p.getServerWorld();
                if (p.getY() < w.getBottomY() - 16) {
                    p.requestTeleport(p.getX(), w.getBottomY() + 2, p.getZ());
                    p.getAbilities().flying = true;
                    p.sendAbilitiesUpdate();
                }
            }
        });
    }

    private static void applyOn(ServerPlayerEntity p) {
        PlayerAbilities a = p.getAbilities();
        a.allowFlying = true;
        a.flying = true;
        p.sendAbilitiesUpdate();
        sendSync(p, true);
    }

    /** Liga o modo e guarda de onde o jogador saiu. */
    public static void enter(ServerPlayerEntity p, BlockPos block) {
        String dim = p.getWorld().getRegistryKey().getValue().toString();
        OriginData.of(p.server).put(p.getUuid(),
                new OriginData.Origin(dim, p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch(), block));
        p.addCommandTag(ModeState.TAG);
        applyOn(p);
    }

    /** Desliga o modo e leva o jogador de volta ao ponto onde foi ativado. */
    public static void leave(ServerPlayerEntity p, String message) {
        OriginData data = OriginData.of(p.server);
        OriginData.Origin o = data.get(p.getUuid());
        data.remove(p.getUuid());

        p.removeScoreboardTag(ModeState.TAG);
        p.interactionManager.getGameMode().setAbilities(p.getAbilities());
        p.sendAbilitiesUpdate();

        if (o != null) {
            ServerWorld w = p.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(o.dim)));
            if (w != null) {
                p.teleport(w, o.x, o.y, o.z, o.yaw, o.pitch);
            }
        }

        p.setVelocity(Vec3d.ZERO);
        p.velocityModified = true;
        p.fallDistance = 0;
        moveToSafeSpot(p);
        sendSync(p, false);

        if (message != null) {
            p.sendMessage(Text.literal(message), true);
        }
    }

    /** Se o ponto de origem estiver bloqueado, sobe até achar espaço livre. */
    private static void moveToSafeSpot(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Box box = p.getBoundingBox();
        int max = Math.max(0, w.getTopY() - (int) p.getY());
        for (int dy = 0; dy <= max; dy++) {
            if (w.isSpaceEmpty(p, box.offset(0, dy, 0))) {
                if (dy > 0) p.requestTeleport(p.getX(), p.getY() + dy, p.getZ());
                return;
            }
        }
    }

    private static void sendSync(ServerPlayerEntity p, boolean on) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(on);
        ServerPlayNetworking.send(p, SYNC_PACKET, buf);
    }
}
