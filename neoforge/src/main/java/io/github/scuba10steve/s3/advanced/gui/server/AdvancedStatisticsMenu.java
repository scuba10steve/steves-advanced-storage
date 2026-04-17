package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStatisticsBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdvancedStatisticsMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    // Storage tab
    public final long totalItems;
    public final long capacity;
    public final int uniqueTypes;
    public final int blockCount;
    public final Map<String, Integer> tierBreakdown;
    public final List<String> presentComponents;

    // Power tab
    public final int generationRate;
    public final int consumptionRate;
    public final int energyStored;
    public final int energyCapacity;

    // Crafting tab
    public final int rmbCount;
    public final int totalPatternSlots;
    public final int usedPatternSlots;
    public final int autoCrafterCount;
    public final int pairedCrafterCount;
    public final int miCount;
    public final int activeMiCount;

    // Client constructor
    public AdvancedStatisticsMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.ADVANCED_STATISTICS.get(), containerId);
        this.pos = buf.readBlockPos();
        boolean hasCore = buf.readBoolean();
        if (hasCore) {
            this.totalItems = buf.readLong();
            this.capacity = buf.readLong();
            this.uniqueTypes = buf.readVarInt();
            this.blockCount = buf.readVarInt();
            int tierSize = buf.readVarInt();
            this.tierBreakdown = new LinkedHashMap<>();
            for (int i = 0; i < tierSize; i++) {
                this.tierBreakdown.put(buf.readUtf(), buf.readVarInt());
            }
            int compSize = buf.readVarInt();
            this.presentComponents = new ArrayList<>();
            for (int i = 0; i < compSize; i++) {
                this.presentComponents.add(buf.readUtf());
            }
            this.generationRate    = buf.readVarInt();
            this.consumptionRate   = buf.readVarInt();
            this.energyStored      = buf.readVarInt();
            this.energyCapacity    = buf.readVarInt();
            this.rmbCount          = buf.readVarInt();
            this.totalPatternSlots = buf.readVarInt();
            this.usedPatternSlots  = buf.readVarInt();
            this.autoCrafterCount  = buf.readVarInt();
            this.pairedCrafterCount = buf.readVarInt();
            this.miCount           = buf.readVarInt();
            this.activeMiCount     = buf.readVarInt();
        } else {
            this.totalItems = 0; this.capacity = 0; this.uniqueTypes = 0; this.blockCount = 0;
            this.tierBreakdown = new LinkedHashMap<>(); this.presentComponents = new ArrayList<>();
            this.generationRate = 0; this.consumptionRate = 0;
            this.energyStored = 0; this.energyCapacity = 0;
            this.rmbCount = 0; this.totalPatternSlots = 0; this.usedPatternSlots = 0;
            this.autoCrafterCount = 0; this.pairedCrafterCount = 0;
            this.miCount = 0; this.activeMiCount = 0;
        }
    }

    // Server constructor (called from AdvancedStatisticsBlockEntity.createMenu)
    public AdvancedStatisticsMenu(int containerId, Inventory playerInventory, AdvancedStatisticsBlockEntity be) {
        super(ModMenuTypes.ADVANCED_STATISTICS.get(), containerId);
        this.pos = be.getBlockPos();
        // Statistics fields are only used client-side; the client receives them via
        // writeSnapshot() in BlockAdvancedStatistics.useWithoutItem(). Default to 0/empty here.
        this.totalItems = 0; this.capacity = 0; this.uniqueTypes = 0; this.blockCount = 0;
        this.tierBreakdown = new LinkedHashMap<>(); this.presentComponents = new ArrayList<>();
        this.generationRate = 0; this.consumptionRate = 0;
        this.energyStored = 0; this.energyCapacity = 0;
        this.rmbCount = 0; this.totalPatternSlots = 0; this.usedPatternSlots = 0;
        this.autoCrafterCount = 0; this.pairedCrafterCount = 0;
        this.miCount = 0; this.activeMiCount = 0;
    }

    /**
     * Writes the full snapshot into buf. Called from BlockAdvancedStatistics.useWithoutItem().
     * Write order MUST match the client constructor read order exactly.
     */
    public static void writeSnapshot(RegistryFriendlyByteBuf buf, AdvancedStorageCoreBlockEntity core) {
        buf.writeLong(core.getInventory().getTotalItemCount());
        buf.writeLong(core.getInventory().getMaxItems());
        buf.writeVarInt(core.getInventory().getStoredItems().size());
        buf.writeVarInt(core.getTotalBlockCount());
        Map<String, Integer> tier = core.getTierBreakdown();
        buf.writeVarInt(tier.size());
        for (Map.Entry<String, Integer> e : tier.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
        List<String> comps = core.getInventory().getPresentComponents();
        buf.writeVarInt(comps.size());
        for (String c : comps) buf.writeUtf(c);
        buf.writeVarInt(core.getTotalGenerationRate());
        buf.writeVarInt(core.getTotalPowerDraw());
        buf.writeVarInt(core.energyStorage.getEnergyStored());
        buf.writeVarInt(core.energyStorage.getMaxEnergyStored());
        List<RecipeMemoryBoxBlockEntity> rmbs = core.getRecipeMemoryBoxes();
        int totalSlots = rmbs.size() * RecipeMemoryBoxBlockEntity.MAX_PATTERNS;
        int usedSlots = 0;
        for (RecipeMemoryBoxBlockEntity rmb : rmbs) {
            for (RecipePattern p : rmb.getPatterns()) {
                if (!p.isEmpty()) usedSlots++;
            }
        }
        buf.writeVarInt(rmbs.size());
        buf.writeVarInt(totalSlots);
        buf.writeVarInt(usedSlots);
        buf.writeVarInt(core.getAutoCrafters().size());
        buf.writeVarInt(core.getRmbToCrafter().size());
        List<MachineInterfaceBlockEntity> mis = core.getMachineInterfaces();
        buf.writeVarInt(mis.size());
        int activeMi = (int) mis.stream()
            .filter(m -> m.getStatus() != MachineInterfaceBlockEntity.Status.IDLE)
            .count();
        buf.writeVarInt(activeMi);
    }

    public BlockPos getPos() { return pos; }
    public long getFreeSpace() { return capacity - totalItems; }
    public int getNetBalance() { return generationRate - consumptionRate; }
    public int getIdleMiCount() { return miCount - activeMiCount; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
