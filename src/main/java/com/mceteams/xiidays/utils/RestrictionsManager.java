package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.XIIDays;
import com.mceteams.xiidays.utils.data.RestrictionsData;
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

    public static boolean isItemAllowed(Item item) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        return RestrictionsData.isItemAllowed(itemKey.toString());
    }

    public static boolean isBlockAllowed(Block block) {
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        return RestrictionsData.isBlockAllowed(blockKey.toString());
    }

    public static Component getStatusComponent(Item item) {
        boolean allowed = isItemAllowed(item);
        if (item instanceof BlockItem blockItem) {
            if (!isBlockAllowed(blockItem.getBlock())) allowed = false;
        }
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        return Component.literal((allowed ? "§aAutorisé" : "§cInterdit") + " §7→ " + itemKey);
    }

    public static Component getStatusComponent(Block block) {
        boolean allowed = isBlockAllowed(block);
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        return Component.literal((allowed ? "§aAutorisé" : "§cInterdit") + " §7→ " + blockKey);
    }

    public static boolean isAllowedCompletely(Item item) {
        boolean allowed = isItemAllowed(item);
        if (item instanceof BlockItem blockItem) {
            if (!isBlockAllowed(blockItem.getBlock())) allowed = false;
        }
        return allowed;
    }

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
                inv.setItem(i, slot);
            }
        }
        ItemStack main = player.getMainHandItem();
        if (remainingToRemove > 0 && !main.isEmpty() && ItemStack.isSameItemSameComponents(main, toRemove)) {
            int take = Math.min(main.getCount(), remainingToRemove);
            main.shrink(take);
            remainingToRemove -= take;
        }
        return toRemove.getCount() - remainingToRemove;
    }

    private static boolean isForbidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        if (!RestrictionsData.isItemAllowed(itemKey.toString())) return true;
        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            return !RestrictionsData.isBlockAllowed(blockKey.toString());
        }
        return false;
    }

    public static boolean isForbidden(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item).toString();
        if (!RestrictionsData.isItemAllowed(key)) return true;
        if (item instanceof BlockItem bi) {
            var blockKey = BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString();
            return !RestrictionsData.isBlockAllowed(blockKey);
        }
        return false;
    }

    public static boolean isForbidden(Block block) {
        var key = BuiltInRegistries.BLOCK.getKey(block).toString();
        return !RestrictionsData.isBlockAllowed(key);
    }

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

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        Item item = stack.getItem();
        Player player = event.getPlayer();
        if (hasBypass(player)) return;

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        if (!RestrictionsData.isItemAllowed(itemKey.toString())) {
            event.setCanPickup(TriState.FALSE);
            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, "§cVous ne pouvez pas récupérer cet objet", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
            return;
        }

        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (!RestrictionsData.isBlockAllowed(blockKey.toString())) {
                event.setCanPickup(TriState.FALSE);
                if (player instanceof ServerPlayer serverPlayer) {
                    notifyPlayer(serverPlayer, "§cVous ne pouvez pas récupérer ce block", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Block block = event.getState().getBlock();
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        if (!RestrictionsData.isBlockAllowed(blockKey.toString())) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, "§cVous ne pouvez pas casser ce block !", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockNeighborNotifyEvent(BlockEvent.NeighborNotifyEvent event) {
        Block block = event.getState().getBlock();
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        if (!RestrictionsData.isBlockAllowed(blockKey.toString())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack craftedStack = event.getCrafting();
        Item item = craftedStack.getItem();
        Player player = event.getEntity();
        if (hasBypass(player)) return;

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        boolean isForbidden = !RestrictionsData.isItemAllowed(itemKey.toString());
        boolean isBlockForbidden = false;
        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            isBlockForbidden = !RestrictionsData.isBlockAllowed(blockKey.toString());
        }

        if (isForbidden || isBlockForbidden) {
            int craftedAmount = craftedStack.getCount();
            int removed = removeItemFromPlayerInventory(player, craftedStack.copy());
            XIIDays.LOGGER.info("RestrictionsManager: removed {} of crafted {} from player {}", removed, craftedStack, player.getName().getString());

            if (event.getInventory() instanceof CraftingContainer craftingContainer) {
                for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
                    ItemStack ingredientStack = craftingContainer.getItem(i);
                    if (!ingredientStack.isEmpty()) {
                        ItemStack returnStack = ingredientStack.copy();
                        if (!player.getInventory().add(returnStack)) {
                            player.drop(returnStack, false);
                        }
                        craftingContainer.setItem(i, ItemStack.EMPTY);
                    }
                }
            }

            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, isBlockForbidden ?
                        "§cVous ne pouvez pas craft ce block" :
                        "§cVous ne pouvez pas craft cet item", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        ItemStack smeltedStack = event.getSmelting();
        Item item = smeltedStack.getItem();
        Player player = event.getEntity();
        if (hasBypass(player)) return;

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        boolean isForbidden = !RestrictionsData.isItemAllowed(itemKey.toString());
        boolean isBlockForbidden = false;
        if (item instanceof BlockItem blockItem) {
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            isBlockForbidden = !RestrictionsData.isBlockAllowed(blockKey.toString());
        }

        if (isForbidden || isBlockForbidden) {
            int removed = removeItemFromPlayerInventory(player, smeltedStack.copy());
            XIIDays.LOGGER.info("RestrictionsManager: removed {} of smelted {} from player {}", removed, smeltedStack, player.getName().getString());
            if (player instanceof ServerPlayer serverPlayer) {
                notifyPlayer(serverPlayer, isBlockForbidden ?
                        "§cVous ne pouvez pas fabriquer ce block" :
                        "§cVous ne pouvez pas fabriquer cet item", new NotifyOptions().actionBar(true).sound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f));
            }
        }
    }

    public static void setItemsAccess(boolean allow, Item it) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(it);
        String itemName = itemKey.toString();
        if (allow) {
            if (!RestrictionsData.isItemAllowed(itemName)) {
                RestrictionsData.setItemAllowed(itemName, true);
            }
        } else {
            if (RestrictionsData.isItemAllowed(itemName)) {
                removeForbiddenItemFromAll(it);
                RestrictionsData.setItemAllowed(itemName, false);
            }
        }
    }

    public static void setBlockAccess(boolean allow, Block bl) {
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(bl);
        String blockName = blockKey.toString();
        if (allow) {
            if (!RestrictionsData.isBlockAllowed(blockName)) {
                RestrictionsData.setBlockAllowed(blockName, true);
            }
        } else {
            if (RestrictionsData.isBlockAllowed(blockName)) {
                removeForbiddenBlockFromAll(bl);
                RestrictionsData.setBlockAllowed(blockName, false);
            }
        }
    }

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

    public static void removeForbiddenBlockFromAll(Block block) {
        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
        String blockName = blockKey.toString();
        assert ServerLifecycleHooks.getCurrentServer() != null;
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (hasBypass(player)) continue;
            boolean removedAny = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
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
