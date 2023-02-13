package net.teamfruit.easyharvest;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiPredicate;

public final class EasyHarvest extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
    }

    private final CanBuildPredicate canBuild = CanBuildPredicate.getPredicate();

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = e.getClickedBlock();
        if (block == null)
            return;

        // クワに応じて半径を変える
        int radius = 0;
        ItemStack itemHand = e.getPlayer().getInventory().getItem(EquipmentSlot.HAND);

        if (itemHand != null) {
            switch (itemHand.getType()) {
                case WOODEN_HOE:
                    radius = 1;
                    break;
                case STONE_HOE:
                    radius = 2;
                    break;
                case IRON_HOE:
                    radius = 3;
                    break;
                case GOLDEN_HOE:
                    radius = 4;
                    break;
                case DIAMOND_HOE:
                    radius = 5;
                    break;
                case NETHERITE_HOE:
                    radius = 6;
                    break;
                default:
                    break;
            }
        }

        Player player = e.getPlayer();
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                Block blockRelative = block.getRelative(i, 0, j);
                if (!canBuild.test(player, blockRelative.getLocation()))
                    continue;

                BlockData blockData = blockRelative.getBlockData();
                if (!(blockData instanceof Ageable))
                    continue;

                Ageable ageable = (Ageable) blockData;
                if (ageable.getAge() < ageable.getMaximumAge())
                    continue;

                Material type = blockRelative.getType();
                blockRelative.breakNaturally();
                blockRelative.setType(type);
                ageable.setAge(0);
            }
        }
    }

    private static class CanBuildPredicate implements BiPredicate<Player, Location> {
        public static CanBuildPredicate getPredicate() {
            return Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard")
                    ? new WorldEditCanBuildPredicate()
                    : new CanBuildPredicate();
        }

        @Override
        public boolean test(Player player, Location location) {
            return true;
        }

        private static class WorldEditCanBuildPredicate extends CanBuildPredicate {
            private final RegionQuery query;


            public WorldEditCanBuildPredicate() {
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                query = container.createQuery();
            }

            @Override
            public boolean test(Player p, Location l) {
                return query.testBuild(BukkitAdapter.adapt(l), WorldGuardPlugin.inst().wrapPlayer(p), Flags.BLOCK_BREAK, Flags.BLOCK_PLACE);
            }
        }
    }

}
