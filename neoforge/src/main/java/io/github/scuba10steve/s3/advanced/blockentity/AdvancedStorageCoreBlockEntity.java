package io.github.scuba10steve.s3.advanced.blockentity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.scuba10steve.s3.advanced.block.BlockAdvancedStatistics;
import io.github.scuba10steve.s3.advanced.block.BlockAutoCrafter;
import io.github.scuba10steve.s3.advanced.block.BlockCoalGenerator;
import io.github.scuba10steve.s3.advanced.block.BlockMachineInterface;
import io.github.scuba10steve.s3.advanced.block.BlockRecipeMemoryBox;
import io.github.scuba10steve.s3.advanced.block.BlockSolarGenerator;
import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStatisticsBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.config.S3AdvancedConfig;
import io.github.scuba10steve.s3.advanced.crafting.CrafterSlot;
import io.github.scuba10steve.s3.advanced.crafting.CraftingCoordinator;
import io.github.scuba10steve.s3.advanced.crafting.CraftingEngine;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageCraftingDisplayMenu;
import io.github.scuba10steve.s3.advanced.gui.server.AdvancedStorageDisplayMenu;
import io.github.scuba10steve.s3.advanced.init.ModBlockEntities;
import io.github.scuba10steve.s3.advanced.network.CraftableSyncPacket;
import io.github.scuba10steve.s3.block.BlockCraftingBox;
import io.github.scuba10steve.s3.blockentity.StorageCoreBlockEntity;
import io.github.scuba10steve.s3.util.BlockRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class AdvancedStorageCoreBlockEntity extends StorageCoreBlockEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedStorageCoreBlockEntity.class);

    private final List<RecipeMemoryBoxBlockEntity> recipeMemoryBoxes = new ArrayList<>();
    private final List<AutoCrafterBlockEntity> autoCrafters = new ArrayList<>();
    private final List<MachineInterfaceBlockEntity> machineInterfaces = new ArrayList<>();
    private final List<SolarGeneratorBlockEntity> solarGenerators = new ArrayList<>();
    private final List<CoalGeneratorBlockEntity> coalGenerators = new ArrayList<>();
    private final List<IEnergyStorage> energyProviders = new ArrayList<>();
    private final Map<RecipeMemoryBoxBlockEntity, AutoCrafterBlockEntity> rmbToCrafter = new LinkedHashMap<>();
    private final Map<RecipeMemoryBoxBlockEntity, MachineInterfaceBlockEntity> rmbToMachineInterface = new LinkedHashMap<>();
    private boolean advancedHasCraftingBox = false;
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

    public List<MachineInterfaceBlockEntity> getMachineInterfaces() {
        return Collections.unmodifiableList(machineInterfaces);
    }

    /** Returns the sum of current FE/t output from all solar and coal generators adjacent to the multiblock. */
    public int getTotalGenerationRate() {
        int rate = 0;
        for (SolarGeneratorBlockEntity sg : solarGenerators) {
            rate += sg.getCurrentRate();
        }
        for (CoalGeneratorBlockEntity cg : coalGenerators) {
            if (cg.isLit()) {
                rate += S3AdvancedConfig.COAL_GENERATION_RATE.get();
            }
        }
        return rate;
    }

    /** Returns the total FE/t currently consumed by all active components in the multiblock. */
    public int getTotalPowerDraw() {
        return totalPowerDraw;
    }

    /** Returns the RMB currently paired to the given crafter, or null. */
    public RecipeMemoryBoxBlockEntity getRmbForCrafter(AutoCrafterBlockEntity crafter) {
        return rmbToCrafter.entrySet().stream()
            .filter(e -> e.getValue() == crafter)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    /** Returns the RMB currently paired to the given MI, or null. */
    public RecipeMemoryBoxBlockEntity getRmbForMachineInterface(MachineInterfaceBlockEntity mi) {
        return rmbToMachineInterface.entrySet().stream()
            .filter(e -> e.getValue() == mi)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    /** Read-only view of the RMB→crafter map, for tests. */
    public Map<RecipeMemoryBoxBlockEntity, AutoCrafterBlockEntity> getRmbToCrafter() {
        return Collections.unmodifiableMap(rmbToCrafter);
    }

    /**
     * BFS from start, following multiblock-connected blocks, to find the
     * AdvancedStorageCoreBlockEntity. Used by menus that need core state on GUI open.
     */
    public static AdvancedStorageCoreBlockEntity findCore(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> bfsQueue = new ArrayDeque<>();
        bfsQueue.add(start);
        visited.add(start);
        while (!bfsQueue.isEmpty()) {
            BlockPos pos = bfsQueue.poll();
            net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();
            if (block instanceof io.github.scuba10steve.s3.advanced.block.BlockAdvancedStorageCore
                    && level.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity core) {
                return core;
            }
            if (block instanceof io.github.scuba10steve.s3.block.StorageMultiblock
                    || block instanceof io.github.scuba10steve.s3.advanced.block.BlockAdvancedStorageCore) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        bfsQueue.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void scanMultiblock() {
        recipeMemoryBoxes.clear();
        autoCrafters.clear();
        machineInterfaces.clear();
        solarGenerators.clear();
        coalGenerators.clear();
        energyProviders.clear();
        advancedHasCraftingBox = false;
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
            } else if (state.getBlock() instanceof BlockMachineInterface) {
                if (level.getBlockEntity(pos) instanceof MachineInterfaceBlockEntity mi) {
                    machineInterfaces.add(mi);
                    totalPowerDraw += S3AdvancedConfig.MACHINE_INTERFACE_ENERGY_PER_TICK.get();
                }
            } else if (state.getBlock() instanceof BlockAdvancedStatistics) {
                if (level.getBlockEntity(pos) instanceof AdvancedStatisticsBlockEntity) {
                    totalPowerDraw += S3AdvancedConfig.ADVANCED_STATISTICS_ENERGY_PER_TICK.get();
                }
            } else if (state.getBlock() instanceof BlockCraftingBox) {
                advancedHasCraftingBox = true;
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);  // mark visited regardless of multiblock membership
                    BlockRef ref = new BlockRef(level.getBlockState(neighbor).getBlock(), neighbor);
                    if (isPartOfMultiblock(ref)) {
                        queue.add(neighbor);
                    } else {
                        BlockState neighborState = level.getBlockState(neighbor);
                        // Collect generators by BE type
                        if (neighborState.getBlock() instanceof BlockSolarGenerator) {
                            if (level.getBlockEntity(neighbor) instanceof SolarGeneratorBlockEntity sg) {
                                solarGenerators.add(sg);
                            }
                        } else if (neighborState.getBlock() instanceof BlockCoalGenerator) {
                            if (level.getBlockEntity(neighbor) instanceof CoalGeneratorBlockEntity cg) {
                                coalGenerators.add(cg);
                            }
                        }
                        // Collect as energy provider if it can be extracted from
                        IEnergyStorage provider = level.getCapability(
                            Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite());
                        if (provider != null && provider.canExtract()) {
                            energyProviders.add(provider);
                        }
                    }
                }
            }
        }

        // Resolve RMB → crafter pairs based on RMB facing direction
        rmbToCrafter.clear();
        rmbToMachineInterface.clear();
        Set<Object> claimed = new HashSet<>();
        LOGGER.info("[Core] Pairing scan: {} RMBs, {} crafters, {} MIs",
            recipeMemoryBoxes.size(), autoCrafters.size(), machineInterfaces.size());
        for (RecipeMemoryBoxBlockEntity rmb : recipeMemoryBoxes) {
            Direction facing = level.getBlockState(rmb.getBlockPos()).getValue(
                io.github.scuba10steve.s3.advanced.block.BlockRecipeMemoryBox.FACING);
            BlockPos facingPos = rmb.getBlockPos().relative(facing);
            net.minecraft.world.level.block.entity.BlockEntity facingBe = level.getBlockEntity(facingPos);
            LOGGER.info("[Core] RMB at {} facing {} → facingPos {} → BE={}",
                rmb.getBlockPos(), facing, facingPos,
                facingBe == null ? "null" : facingBe.getClass().getSimpleName());
            if (facingBe instanceof AutoCrafterBlockEntity ac
                    && autoCrafters.contains(ac) && !claimed.contains(ac)) {
                rmbToCrafter.put(rmb, ac);
                claimed.add(ac);
                LOGGER.info("[Core] Paired RMB at {} → AC at {}", rmb.getBlockPos(), ac.getBlockPos());
            } else if (facingBe instanceof MachineInterfaceBlockEntity mi
                    && machineInterfaces.contains(mi) && !claimed.contains(mi)) {
                rmbToMachineInterface.put(rmb, mi);
                claimed.add(mi);
            }
        }

        if (level != null && !level.isClientSide) {
            sendCraftableSyncToViewers();
        }
    }

    @Override
    public void tick() {
        super.tick(); // handles multiblock scan
        if (level == null || level.isClientSide) {
            return;
        }
        for (IEnergyStorage provider : energyProviders) {
            int space = energyStorage.receiveEnergy(Integer.MAX_VALUE, true);
            if (space <= 0) break;
            int extracted = provider.extractEnergy(space, false);
            if (extracted > 0) energyStorage.receiveEnergy(extracted, false);
        }
        if (energyStorage.consume(totalPowerDraw)) {
            setChanged();
            if (craftingCoordinator.tick(getInventory(), rmbToCrafter)) {
                forceSyncToClients();
            }
            for (MachineInterfaceBlockEntity mi : machineInterfaces) {
                RecipeMemoryBoxBlockEntity pairedRmb = getRmbForMachineInterface(mi);
                // MI uses slot 0 of its paired RMB — one pattern per MI pairing by design.
                RecipePattern pattern = pairedRmb != null ? pairedRmb.getPattern(0) : null;
                mi.tryTick(getInventory(), level,
                    (pattern != null && !pattern.isEmpty()) ? pattern : null);
            }
        } else if (craftingCoordinator.getQueueSize() > 0) {
            LOGGER.debug("[Core] Energy check failed: stored={} needed={} queueSize={}",
                energyStorage.getEnergyStored(), totalPowerDraw, craftingCoordinator.getQueueSize());
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

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Call super to trigger StorageSyncPacket delivery (side effect).
        // Return value is discarded.
        super.createMenu(containerId, playerInventory, player);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                new CraftableSyncPacket(worldPosition, collectCraftableEntries()));
        }

        if (advancedHasCraftingBox) {
            return new AdvancedStorageCraftingDisplayMenu(containerId, playerInventory, worldPosition);
        } else {
            return new AdvancedStorageDisplayMenu(containerId, playerInventory, worldPosition);
        }
    }

    private List<CraftableSyncPacket.Entry> collectCraftableEntries() {
        List<CraftableSyncPacket.Entry> entries = new ArrayList<>();
        for (Map.Entry<RecipeMemoryBoxBlockEntity, AutoCrafterBlockEntity> entry : rmbToCrafter.entrySet()) {
            RecipeMemoryBoxBlockEntity rmb = entry.getKey();
            AutoCrafterBlockEntity crafter = entry.getValue();
            List<RecipePattern> patterns = rmb.getPatterns();
            for (int i = 0; i < patterns.size(); i++) {
                RecipePattern pattern = patterns.get(i);
                if (!pattern.isEmpty() && !pattern.getOutput().isEmpty()) {
                    entries.add(new CraftableSyncPacket.Entry(
                        new CrafterSlot(crafter.getBlockPos(), i),
                        pattern.getOutput().copy()
                    ));
                }
            }
        }
        return entries;
    }

    private void sendCraftableSyncToViewers() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CraftableSyncPacket packet = new CraftableSyncPacket(worldPosition, collectCraftableEntries());
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if ((player.containerMenu instanceof AdvancedStorageDisplayMenu m && m.getPos().equals(worldPosition))
                    || (player.containerMenu instanceof AdvancedStorageCraftingDisplayMenu m2 && m2.getPos().equals(worldPosition))) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
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
