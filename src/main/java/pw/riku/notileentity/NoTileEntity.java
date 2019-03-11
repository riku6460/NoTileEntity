package pw.riku.notileentity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.io.*;

public final class NoTileEntity extends JavaPlugin {

    private Set<UUID> players = new HashSet<>();
    private Configuration configuration;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            configuration = new Configuration(getDataFolder().toPath().resolve("config.yml"));
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Failed to load configuration file! disabling NoTileEntity");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.TILE_ENTITY_DATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (players.contains(event.getPlayer().getUniqueId())) {
                    BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);
                    Material material = position.toLocation(event.getPlayer().getWorld()).getBlock().getType();
                    if (configuration.getTargetBlocks().contains(material)) {
                        event.setCancelled(true);
                    }
                }
            }
        });
        manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (players.contains(event.getPlayer().getUniqueId())) {
                    List<NbtBase<?>> list = event.getPacket().getListNbtModifier().read(0);
                    Iterator<NbtBase<?>> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        NbtBase<?> base = iterator.next();
                        if (base instanceof NbtCompound) {
                            NbtCompound compound = (NbtCompound) base;
                            int x = compound.getInteger("x");
                            int y = compound.getInteger("y");
                            int z = compound.getInteger("z");
                            Material material = new Location(event.getPlayer().getWorld(), x, y, z).getBlock().getType();
                            if (configuration.getTargetBlocks().contains(material)) {
                                iterator.remove();
                            }
                        }
                    }
                    event.getPacket().getListNbtModifier().write(0, list);
                }
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            reloadAsync(sender);
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            sender.sendMessage(toggle(uuid) ? "ON" : "OFF");
        } else {
            switch (args[0].toLowerCase()) {
                case "on": {
                    players.add(uuid);
                    sender.sendMessage("ON");
                    break;
                }
                case "off": {
                    players.remove(uuid);
                    sender.sendMessage("OFF");
                    break;
                }
                case "reload": {
                    if (player.hasPermission("notileentity.admin")) {
                        reloadAsync(sender);
                        break;
                    }
                }
                default: {
                    sender.sendMessage(toggle(uuid) ? "ON" : "OFF");
                }
            }
        }
        return true;
    }

    private boolean toggle(UUID uuid) {
        if (players.contains(uuid)) {
            players.remove(uuid);
            return false;
        } else {
            players.add(uuid);
            return true;
        }
    }

    private void reloadAsync(CommandSender receiver) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                configuration.load();
            } catch (IOException | InvalidConfigurationException e) {
                getLogger().warning("Failed to load configuration file!");
                e.printStackTrace();
                receiver.sendMessage("reload failed!");
                return;
            }
            receiver.sendMessage("reload complete!");
        });
    }
}
