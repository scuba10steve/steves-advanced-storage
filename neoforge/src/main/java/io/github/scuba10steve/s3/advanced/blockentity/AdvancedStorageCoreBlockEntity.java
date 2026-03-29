package io.github.scuba10steve.s3.advanced.blockentity;

import io.github.scuba10steve.s3.advanced.block.BlockAutoCrafter;
import io.github.scuba10steve.s3.advanced.block.BlockRecipeMemoryBox;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.crafting.CraftingCoordinator;
import io.github.scuba10steve.s3.advanced.crafting.CraftingEngine;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.blockentity.StorageCoreBlockEntity;
import io.github.scuba10steve.s3.util.BlockRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import java.util.*;

public class AdvancedStorageCoreBlockEntity extends StorageCoreBlockEntity {

    private final List<RecipeMemoryBoxBlockEntity> recipeMemoryBoxes = new ArrayList<>();
    private final List<AutoCrafterBlockEntity> autoCrafters = new ArrayList<>();
    // Field initializer avoids a zero-value window before the constructor body runs.
    private int totalPowerDraw = S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();

    public final InternalEnergyStorage energyStorage;
    public final CraftingEngine craftingEngine = new CraftingEngine();
    public final CraftingCoordinator craftingCoordinator = new CraftingCoordinator(craftingEngine);

    // ContainerData slots: [0-1] energy, [2-3] capacity, [4] energyPerTick, [5] isPowered
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> totalPowerDraw;
                case 5 -> isPowered() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { /* read-only from server */ }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public AdvancedStorageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_STORAGE_CORE.get(), pos, state);
        energyStorage = new InternalEnergyStorage(
            S3AdvancedConfig.CORE_CAPACITY.get(),
            1_000);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.s3_advanced.advanced_storage_core");
    }

    public boolean isPowered() {
        return energyStorage.getEnergyStored() >= totalPowerDraw;
    }

    public List<RecipeMemoryBoxBlockEntity> getRecipeMemoryBoxes() {
        return Collections.unmodifiableList(recipeMemoryBoxes);
    }

    public List<AutoCrafterBlockEntity> getAutoCrafters() {
        return Collections.unmodifiableList(autoCrafters);
    }

    @Override
    public void scanMultiblock() {
        recipeMemoryBoxes.clear();
        autoCrafters.clear();
        super.scanMultiblock();
        totalPowerDraw = S3AdvancedConfig.CORE_ENERGY_PER_TICK.get();

        if (level == null) {
            return;
        }

        // BFS from the core position over all blocks confirmed as multiblock members,
        // collecting RecipeMemoryBox block entities and accumulating their FE/t.
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof BlockRecipeMemoryBox) {
                if (level.getBlockEntity(pos) instanceof RecipeMemoryBoxBlockEntity rmb) {
                    recipeMemoryBoxes.add(rmb);
                    totalPowerDraw += S3AdvancedConfig.RECIPE_MEMORY_BOX_ENERGY_PER_TICK.get();
                }
            } else if (state.getBlock() instanceof BlockAutoCrafter) {
                if (level.getBlockEntity(pos) instanceof AutoCrafterBlockEntity ac) {
                    autoCrafters.add(ac);
                    totalPowerDraw += S3AdvancedConfig.AUTO_CRAFTER_ENERGY_PER_TICK.get();
                }
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!visited.contains(neighbor)) {
                    BlockRef ref = new BlockRef(level.getBlockState(neighbor).getBlock(), neighbor);
                    if (isPartOfMultiblock(ref)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick(); // handles multiblock scan
        if (level == null || level.isClientSide) {
            return;
        }
        if (energyStorage.consume(totalPowerDraw)) {
            setChanged();
            List<CraftingCoordinator.BoxData> boxSnapshots = recipeMemoryBoxes.stream()
                    .map(be -> new CraftingCoordinator.BoxData(be.getBlockPos(), be.getPatterns()))
                    .toList();
            List<CraftingCoordinator.CrafterData> crafterSnapshots = autoCrafters.stream()
                    .map(be -> new CraftingCoordinator.CrafterData(be.getAssignments()))
                    .toList();
            craftingCoordinator.tick(getInventory(), boxSnapshots, crafterSnapshots);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
    }

    public static class InternalEnergyStorage extends EnergyStorage {

        InternalEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        public boolean consume(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        public void setEnergy(int amount) {
            this.energy = Math.min(amount, capacity);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }
    }
}
