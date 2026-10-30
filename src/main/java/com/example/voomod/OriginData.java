package com.example.voomod;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Guarda, por jogador, o lugar onde o modo foi ativado (sobrevive a reinício do servidor). */
public class OriginData extends PersistentState {
    public static class Origin {
        public final String dim;
        public final double x, y, z;
        public final float yaw, pitch;
        public final BlockPos block;

        public Origin(String dim, double x, double y, double z, float yaw, float pitch, BlockPos block) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.block = block;
        }
    }

    private final Map<UUID, Origin> map = new HashMap<>();

    public static OriginData of(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(OriginData::fromNbt, OriginData::new, "voomod_origins");
    }

    public Origin get(UUID id) {
        return map.get(id);
    }

    public void put(UUID id, Origin origin) {
        map.put(id, origin);
        markDirty();
    }

    public void remove(UUID id) {
        if (map.remove(id) != null) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        for (Map.Entry<UUID, Origin> e : map.entrySet()) {
            Origin o = e.getValue();
            NbtCompound c = new NbtCompound();
            c.putString("dim", o.dim);
            c.putDouble("x", o.x);
            c.putDouble("y", o.y);
            c.putDouble("z", o.z);
            c.putFloat("yaw", o.yaw);
            c.putFloat("pitch", o.pitch);
            c.putLong("block", o.block.asLong());
            all.put(e.getKey().toString(), c);
        }
        nbt.put("origins", all);
        return nbt;
    }

    public static OriginData fromNbt(NbtCompound nbt) {
        OriginData data = new OriginData();
        NbtCompound all = nbt.getCompound("origins");
        for (String key : all.getKeys()) {
            NbtCompound c = all.getCompound(key);
            data.map.put(UUID.fromString(key), new Origin(
                    c.getString("dim"),
                    c.getDouble("x"), c.getDouble("y"), c.getDouble("z"),
                    c.getFloat("yaw"), c.getFloat("pitch"),
                    BlockPos.fromLong(c.getLong("block"))));
        }
        return data;
    }
}
