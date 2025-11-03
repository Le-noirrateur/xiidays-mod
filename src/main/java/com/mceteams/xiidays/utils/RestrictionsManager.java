package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.XIIDaysManagerMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.mceteams.xiidays.utils.NotifyOptions.notifyPlayer;

public class RestrictionsManager {

    private static final Set<UUID> bypassPlayers = new HashSet<>();

    public static void addBypassPlayer(ServerPlayer player) {
        bypassPlayers.add(player.getUUID());
    }

    public static void addBypassPlayer(Player player) {
        bypassPlayers.add(player.getUUID());
    }

    public static void removeBypassPlayer(ServerPlayer player) {
        bypassPlayers.remove(player.getUUID());
    }

    public static void removeBypassPlayer(Player player) {
        bypassPlayers.remove(player.getUUID());
    }

    public static boolean hasBypass(ServerPlayer player) {
        return bypassPlayers.contains(player.getUUID());
    }

    public static boolean hasBypass(Player player) {
        return bypassPlayers.contains(player.getUUID());
    }

    // #################################################################################################################
    // Statut - Vérification autorisation
    // #################################################################################################################

    public static boolean isItemAllowed(Item item) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String itemName = itemKey.toString();

        
        return DataManager.dataReadBoolean("ItemsAccess", itemName, true);
    }

    public static boolean isBlockAllowed(Block block) {
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        String blockName = blockKey.toString();

        
        return DataManager.dataReadBoolean("BlocksAccess", blockName, true);
    }

    /**
     * Renvoie un composant textuel décrivant le statut d'un item (ou block item)
     */
    public static Component getStatusComponent(Item item) {
        boolean allowed = isItemAllowed(item);

        if (item instanceof BlockItem blockItem) {
            boolean blockAllowed = isBlockAllowed(blockItem.getBlock());
            if (!blockAllowed) allowed = false;
        }

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        return Component.literal((allowed ? "§aAutorisé" : "§cInterdit") + " §7→ " + itemKey);
    }

    /**
     * Renvoie un composant textuel décrivant le statut d'un block
     */
    public static Component getStatusComponent(Block block) {
        boolean allowed = isBlockAllowed(block);

        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        return Component.literal((allowed ? "§aAutorisé" : "§cInterdit") + " §7→ " + blockKey);
    }

    /**
     * Indique si un item (et éventuellement son block) est autorisé
     */
    public static boolean isAllowedCompletely(Item item) {
        boolean allowed = isItemAllowed(item);
        if (item instanceof BlockItem blockItem) {
            if (!isBlockAllowed(blockItem.getBlock())) allowed = false;
        }
        return allowed;
    }

    // #################################################################################################################
    // Utils
    // #################################################################################################################

    // Retire un item spécifique de l'inventaire d'un joueur, retourne le nombre d'items retirés
    private static int removeItemFromPlayerInventory(Player player, ItemStack toRemove) {
        if (toRemove == null || toRemove.isEmpty()) return 0;
        int remainingToRemove = toRemove.getCount();
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize() && remainingToRemove > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, toRemove)) {
                int take = Math.min(slot.getCount(), remainingToRemove);
                slot.shrink(take);
                remainingToRemove -= take;
                inv.setItem(i, slot); // update slot
            }
        }

        // S'il reste à retirer (par ex. dans la main), tenter la main du joueur
        ItemStack main = player.getMainHandItem();
        if (remainingToRemove > 0 && !main.isEmpty() && ItemStack.isSameItemSameComponents(main, toRemove)) {
            int take = Math.min(main.getCount(), remainingToRemove);
            main.shrink(take);
            remainingToRemove -= take;
            // la main est mise à jour automatiquement via l'ItemStack muté
        }

        return toRemove.getCount() - remainingToRemove; // nombre retiré
    }

    // Vérifie si un item (et éventuellement son block) est interdit
    private static boolean isForbidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        Item item = stack.getItem();
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String itemName = itemKey.toString();
        if (!DataManager.dataReadBoolean("ItemsAccess", itemName, true)) return true;

        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            String blockName = blockKey.toString();
            return !DataManager.dataReadBoolean("BlocksAccess", blockName, true);
        }

        return false;
    }

    // Vérifie si un item (et éventuellement son block) est interdit
    public static boolean isForbidden(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item).toString();
        boolean allowedItem = DataManager.dataReadBoolean("ItemsAccess", key, true);

        if (!allowedItem) return true;

        if (item instanceof BlockItem bi) {
            var blockKey = BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString();
            boolean allowedBlock = DataManager.dataReadBoolean("BlocksAccess", blockKey, true);
            return !allowedBlock;
        }

        return false;
    }

    // Vérifie si un block est interdit
    public static boolean isForbidden(Block block) {
        var key = BuiltInRegistries.BLOCK.getKey(block).toString();
        return !DataManager.dataReadBoolean("BlocksAccess", key, true);
    }
    
    // #################################################################################################################
    // Events
    // #################################################################################################################

    // Lors de la connexion d'un joueur, on vérifie son inventaire
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RestrictionsManager.hasBypass(player)) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && RestrictionsManager.isForbidden(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                notifyPlayer(player, "§cUn item interdit a été supprimé de votre inventaire !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Lors du placement d'un block
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (RestrictionsManager.hasBypass(player)) return;

        Block block = event.getPlacedBlock().getBlock();
        if (RestrictionsManager.isForbidden(block)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer sp) {
                notifyPlayer(sp, "§cVous ne pouvez pas placer ce block !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Lors de l'utilisation d'un item (clic droit)
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (RestrictionsManager.hasBypass(player)) return;

        ItemStack stack = event.getItemStack();
        if (RestrictionsManager.isForbidden(stack)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer sp) {
                notifyPlayer(sp, "§cCet objet est interdit !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Lors de l'attaque avec un item (clic gauche)
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (RestrictionsManager.hasBypass(player)) return;

        ItemStack stack = player.getMainHandItem();
        if (RestrictionsManager.isForbidden(stack)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer sp) {
                notifyPlayer(sp, "§cVous ne pouvez pas utiliser cet item pour attaquer !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Lors du tick d'un joueur (vérification inventaire)
    @SubscribeEvent
    public static void onPlayerTick(PlayerEvent.StartTracking event) {
        Entity entity = event.getEntity();

        if (entity instanceof ServerPlayer player) {
            if (RestrictionsManager.hasBypass(player)) return;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (RestrictionsManager.isForbidden(stack)) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    notifyPlayer(player, "§cUn item interdit a été supprimé de votre inventaire !", new NotifyOptions().actionBar(true).sound(SoundEvents.LAVA_EXTINGUISH, SoundSource.MASTER, 1f, 1f));
                }
            }
        }
    }

    // Lors de la tentative de ramassage d'un item
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        Item item = stack.getItem();
        Player player = event.getPlayer();

        if (hasBypass(player)) {
            return;
        }

        

        // Obtenir le nom de l'item
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String itemName = itemKey.toString();

        // Vérifier si l'item est interdit
        if (!DataManager.dataReadBoolean("ItemsAccess", itemName, true)) {
            event.setCanPickup(TriState.FALSE);

            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, "§cVous ne pouvez pas récupérer cet objet", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
            return;
        }

        // Si c'est un BlockItem, vérifier aussi le block
        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            String blockName = blockKey.toString();

            if (!DataManager.dataReadBoolean("BlocksAccess", blockName, true)) {
                event.setCanPickup(TriState.FALSE);

                if (player instanceof ServerPlayer serverPlayer) {
                    notifyPlayer(serverPlayer, "§cVous ne pouvez pas récupérer ce block", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
                }
            }
        }
    }

    // Lors de la casse d'un block
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Block block = event.getState().getBlock();
        Player player = event.getPlayer();

        if (hasBypass(player)) {
            return;
        }

        

        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        String blockName = blockKey.toString();

        if (!DataManager.dataReadBoolean("BlocksAccess", blockName, true)) {
            event.setCanceled(true);

            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, "§cVous ne pouvez pas casser ce block !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Lors de la notification des voisins d'un block (redstone, etc.)
    @SubscribeEvent
    public static void onBlockNeighborNotifyEvent(BlockEvent.NeighborNotifyEvent event) {
        Block block = event.getState().getBlock();

        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        String blockName = blockKey.toString();

        if (!DataManager.dataReadBoolean("BlocksAccess", blockName, true)) {
            event.setCanceled(true);
        }
    }

    // Lors du craft d'un item
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack craftedStack = event.getCrafting();
        Item item = craftedStack.getItem();
        Player player = event.getEntity();

        if (hasBypass(player)) {
            return;
        }

        

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String itemName = itemKey.toString();

        boolean isForbidden = !DataManager.dataReadBoolean("ItemsAccess", itemName, true);
        boolean isBlockForbidden = false;

        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            String blockName = blockKey.toString();
            isBlockForbidden = !DataManager.dataReadBoolean("BlocksAccess", blockName, true);
        }

        if (isForbidden || isBlockForbidden) {
            int craftedAmount = craftedStack.getCount();

            // 1) Retirer le produit déjà donné au joueur (évite la duplication)
            int removed = removeItemFromPlayerInventory(player, craftedStack.copy());
            XIIDaysManagerMod.LOGGER.info("RestrictionsManager: removed {} of crafted {} from player {}", removed, craftedStack, player.getName().getString());

            // 2) Essayer de restituer les ingrédients présents dans la grille de craft
            if (event.getInventory() instanceof CraftingContainer craftingContainer) {
                for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
                    ItemStack ingredientStack = craftingContainer.getItem(i);
                    if (!ingredientStack.isEmpty()) {
                        // on copie et on rend les ingrédients au joueur (si possible).
                        ItemStack returnStack = ingredientStack.copy();
                        if (!player.getInventory().add(returnStack)) {
                            player.drop(returnStack, false);
                        }
                        // vider la case de la grille pour éviter double restitution
                        craftingContainer.setItem(i, ItemStack.EMPTY);
                    }
                }
            }

            // 3) Message/son
            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, isBlockForbidden ?
                        "§cVous ne pouvez pas craft ce block" :
                        "§cVous ne pouvez pas craft cet item", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Lors du smelt d'un item
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        ItemStack smeltedStack = event.getSmelting();
        Item item = smeltedStack.getItem();
        Player player = event.getEntity();

        if (hasBypass(player)) {
            return;
        }

        

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String itemName = itemKey.toString();

        boolean isForbidden = !DataManager.dataReadBoolean("ItemsAccess", itemName, true);
        boolean isBlockForbidden = false;

        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            String blockName = blockKey.toString();
            isBlockForbidden = !DataManager.dataReadBoolean("BlocksAccess", blockName, true);
        }

        if (isForbidden || isBlockForbidden) {
            // Retirer le smelted item du joueur / conteneur
            int removed = removeItemFromPlayerInventory(player, smeltedStack.copy());
            XIIDaysManagerMod.LOGGER.info("RestrictionsManager: removed {} of smelted {} from player {}", removed, smeltedStack, player.getName().getString());

            // Optionnel : si le smelt provient d'un four automatisé et qu'il existe un tile entity,
            // il faudrait vider la slot de sortie du four. Ici, on couvre le cas "joueur qui récupère".
            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, isBlockForbidden ?
                        "§cVous ne pouvez pas fabriquer ce block" :
                        "§cVous ne pouvez pas fabriquer cet item", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

//    @SubscribeEvent
//    public static void onCommand(CommandEvent event) {
//        if (event.getParseResults().getReader().getString().startsWith("/spectate")) {
//            event.setCanceled(true);
//        }
//    }

    // #################################################################################################################
    // Fonctions
    // #################################################################################################################

    // Modifie l'accès d'un item (allow = true => autorisé, false => interdit)
    public static void setItemsAccess(boolean allow, Item it) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(it);
        String itemName = itemKey.toString(); // Exemple: "minecraft:diamond"

        

        if (allow) { // if Allow
            if (!DataManager.dataReadBoolean("ItemsAccess", itemName, true)) {
                DataManager.dataModify("ItemsAccess", itemName, true);
            }
        } else { // if Deny
            if (DataManager.dataReadBoolean("ItemsAccess", itemName, true)) {
                removeForbiddenItemFromAll(it);
                DataManager.dataModify("ItemsAccess", itemName, false);
            }
        }
    }

    // Modifie l'accès d'un block (allow = true => autorisé, false => interdit)
    public static void setBlockAccess(boolean allow, Block bl) {
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(bl);
        String blockName = blockKey.toString(); // Exemple: "minecraft:diamond_block"

        

        if (allow) { // if Allow
            if (!DataManager.dataReadBoolean("BlocksAccess", blockName, true)) {
                DataManager.dataModify("BlocksAccess", blockName, true);
            }
        } else { // if Deny
            if (DataManager.dataReadBoolean("BlocksAccess", blockName, true)) {
                removeForbiddenBlockFromAll(bl);
                DataManager.dataModify("BlocksAccess", blockName, false);
            }
        }
    }

    // Retire un item interdit de l'inventaire de tous les joueurs
    public static void removeForbiddenItemFromAll(Item item) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String itemName = itemKey.toString();

        assert ServerLifecycleHooks.getCurrentServer() != null;
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (hasBypass(player)) continue;

            boolean removedAny = false;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemName)) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    removedAny = true;
                }
            }

            if (removedAny) {
                notifyPlayer(player, "§cUn item interdit a été supprimé de votre inventaire !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    // Retire un block interdit de l'inventaire de tous les joueurs
    public static void removeForbiddenBlockFromAll(Block block) {
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        String blockName = blockKey.toString();

        assert ServerLifecycleHooks.getCurrentServer() != null;
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (hasBypass(player)) continue;

            boolean removedAny = false;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);

                // Vérifie si l'item est un BlockItem et correspond au block interdit
                if (stack.getItem() instanceof BlockItem blockItem) {
                    if (BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString().equals(blockName)) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        removedAny = true;
                    }
                }
            }

            if (removedAny) {
                notifyPlayer(player, "§cUn block interdit a été supprimé de votre inventaire !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }
}
