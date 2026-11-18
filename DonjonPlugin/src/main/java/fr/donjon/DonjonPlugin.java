package fr.donjon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class DonjonPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final int PLATFORM_Y = 100;
    private static final int PLATFORM_RADIUS = 6; // plateforme 13x13

    /** Les 3 donjons : nom du monde, affichage, icône du menu, bloc de la plateforme, slot. */
    private enum Donjon {
        CASTEL("donjon_castel", "Donjon Castel", Material.STONE_BRICKS, Material.STONE, NamedTextColor.GRAY, 11),
        CIEUX("donjon_cieux", "Donjon Cieux", Material.GLOWSTONE, Material.GLOWSTONE, NamedTextColor.YELLOW, 13),
        ABYSSE("donjon_abysse", "Donjon Abysse", Material.NETHERRACK, Material.NETHERRACK, NamedTextColor.DARK_RED, 15);

        final String worldName;
        final String display;
        final Material icon;
        final Material platform;
        final NamedTextColor color;
        final int slot;

        Donjon(String worldName, String display, Material icon, Material platform, NamedTextColor color, int slot) {
            this.worldName = worldName;
            this.display = display;
            this.icon = icon;
            this.platform = platform;
            this.color = color;
            this.slot = slot;
        }

        static @Nullable Donjon bySlot(int slot) {
            for (Donjon d : values()) if (d.slot == slot) return d;
            return null;
        }
    }

    /** Holder pour reconnaître notre menu. */
    private static class DonjonMenu implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    /** Générateur de monde vide (void) : rien ne génère, on construit juste la plateforme. */
    private static class VoidGenerator extends ChunkGenerator {
        @Override
        public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
            return new Location(world, 0.5, PLATFORM_Y + 1, 0.5);
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        var cmd = getCommand("donjon");
        if (cmd != null) cmd.setExecutor(this);

        // Création / chargement des 3 mondes + plateformes
        for (Donjon d : Donjon.values()) {
            loadDonjonWorld(d);
        }
        getLogger().info("DonjonPlugin activé : 3 mondes de donjon prêts.");
    }

    private World loadDonjonWorld(Donjon d) {
        World world = Bukkit.getWorld(d.worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(d.worldName);
            creator.environment(World.Environment.NORMAL);
            creator.generator(new VoidGenerator());
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            getLogger().severe("Impossible de créer le monde " + d.worldName);
            return null;
        }
        world.setSpawnLocation(0, PLATFORM_Y + 1, 0);
        buildPlatform(world, d.platform);
        return world;
    }

    private void buildPlatform(World world, Material material) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                world.getBlockAt(x, PLATFORM_Y, z).setType(material, false);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        // /donjon retour : sortir du donjon
        if (args.length > 0 && args[0].equalsIgnoreCase("retour")) {
            World main = Bukkit.getWorlds().get(0);
            player.teleport(main.getSpawnLocation());
            player.sendMessage(Component.text("Tu es revenu au monde principal.", NamedTextColor.GREEN));
            return true;
        }

        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        DonjonMenu holder = new DonjonMenu();
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Choisis ton donjon", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.inventory = inv;

        // Décor
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.empty());
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        for (Donjon d : Donjon.values()) {
            ItemStack item = new ItemStack(d.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(d.display, d.color, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Clique pour y être téléporté", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            inv.setItem(d.slot, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DonjonMenu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof DonjonMenu)) return;

        Donjon d = Donjon.bySlot(event.getSlot());
        if (d == null) return;

        World world = Bukkit.getWorld(d.worldName);
        if (world == null) world = loadDonjonWorld(d);
        if (world == null) {
            player.sendMessage(Component.text("Le monde du donjon est introuvable.", NamedTextColor.RED));
            return;
        }

        player.closeInventory();
        player.teleport(new Location(world, 0.5, PLATFORM_Y + 1, 0.5));
        player.sendMessage(Component.text("Bienvenue dans le ", NamedTextColor.GRAY)
                .append(Component.text(d.display, d.color, TextDecoration.BOLD)));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DonjonMenu) {
            event.setCancelled(true);
        }
    }
}
