package cc.reconnected.chatbox.data;

import cc.reconnected.chatbox.Chatbox;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class StateSaverAndLoader extends PersistentState {
    public HashMap<UUID, ChatboxPlayerData> players = new HashMap<>();

    /**
     * LicenseUUID -> PlayerUUID
     * <p>
     * Contains mappings of license tokens UUID to players UUID
     */
    public HashMap<UUID, UUID> licenses = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        var playersNbt = new NbtCompound();
        players.forEach((uuid, data) -> {
            var playerNbt = new NbtCompound();
            playerNbt.putBoolean("spy", data.enableSpy);
            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("players", playersNbt);

        var licensesNbt = new NbtCompound();
        licenses.forEach((license, playerId) -> {
            licensesNbt.putUuid(license.toString(), playerId);
        });
        nbt.put("licenses", licensesNbt);
        return nbt;
    }

    public static StateSaverAndLoader createFromNbt(NbtCompound nbt) {
        var state = new StateSaverAndLoader();

        var playersNbt = nbt.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            var playerData = new ChatboxPlayerData();

            playerData.enableSpy = playersNbt.getCompound(key).getBoolean("spy");
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        var licensesNbt = nbt.getCompound("licenses");
        licensesNbt.getKeys().forEach(license -> {
            var playerUuid = licensesNbt.getUuid(license);
            state.licenses.put(UUID.fromString(license), playerUuid);
        });
        return state;
    }

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        var persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        var state = persistentStateManager.getOrCreate(
                StateSaverAndLoader::createFromNbt,
                StateSaverAndLoader::new,
                Chatbox.MOD_ID
        );
        state.markDirty();
        return state;
    }

    public static ChatboxPlayerData getPlayerState(LivingEntity player) {
        var serverState = getServerState(player.getWorld().getServer());
        var playerState = serverState.players.computeIfAbsent(player.getUuid(), uuid -> new ChatboxPlayerData());
        return playerState;
    }
}
