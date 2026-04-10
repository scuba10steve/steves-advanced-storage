package io.github.scuba10steve.s3.advanced.gametest;

import com.google.common.hash.Hashing;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class GameTestStructureProvider implements DataProvider {
    private final PackOutput output;
    private final String namespace;
    private final List<StructureDefinition> structures = new ArrayList<>();

    public record BlockPlacement(String block, int x, int y, int z, Map<String, String> properties) {
        /** Convenience constructor for blocks with no state properties. */
        public BlockPlacement(String block, int x, int y, int z) {
            this(block, x, y, z, Map.of());
        }
    }

    public record EntityPlacement(String entity, double x, double y, double z) {}

    public record ItemPlacement(String item, int count, double x, double y, double z) {}

    public record StructureContent(
        List<BlockPlacement> blocks,
        List<EntityPlacement> entities,
        List<ItemPlacement> items
    ) {
        public static StructureContent empty() {
            return new StructureContent(List.of(), List.of(), List.of());
        }
    }

    private record StructureDefinition(String name, int sizeX, int sizeY, int sizeZ, StructureContent content) {}

    public GameTestStructureProvider(PackOutput output, String namespace) {
        this.output = output;
        this.namespace = namespace;
    }

    public GameTestStructureProvider addEmpty(String name, int sizeX, int sizeY, int sizeZ) {
        return add(name, sizeX, sizeY, sizeZ, StructureContent.empty());
    }

    public GameTestStructureProvider add(String name, int sizeX, int sizeY, int sizeZ, StructureContent content) {
        structures.add(new StructureDefinition(name, sizeX, sizeY, sizeZ, content));
        return this;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (StructureDefinition def : structures) {
            futures.add(writeStructure(cachedOutput, def));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> writeStructure(CachedOutput cachedOutput, StructureDefinition def) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, def.name());
        Path outputPath = output.getOutputFolder()
            .resolve("data/" + id.getNamespace() + "/structure/" + id.getPath() + ".nbt");

        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        ListTag size = new ListTag();
        size.add(IntTag.valueOf(def.sizeX()));
        size.add(IntTag.valueOf(def.sizeY()));
        size.add(IntTag.valueOf(def.sizeZ()));
        tag.put("size", size);

        // Build palette and blocks from content.
        // The palette key includes sorted properties so that the same block with different
        // states maps to distinct palette entries (e.g. RMB facing=east vs facing=north).
        Map<String, Integer> paletteMap = new LinkedHashMap<>();
        // Map from palette key -> BlockPlacement (so we can write properties into the palette later)
        Map<String, BlockPlacement> paletteKeyToPlacement = new LinkedHashMap<>();
        String airKey = "minecraft:air";
        paletteMap.put(airKey, 0);
        paletteKeyToPlacement.put(airKey, new BlockPlacement("minecraft:air", 0, 0, 0));

        ListTag blocksList = new ListTag();
        for (BlockPlacement bp : def.content().blocks()) {
            // Build a stable palette key: "block[prop1=val1,prop2=val2]"
            String paletteKey = buildPaletteKey(bp);
            int stateIndex = paletteMap.computeIfAbsent(paletteKey, k -> paletteMap.size());
            paletteKeyToPlacement.putIfAbsent(paletteKey, bp);

            CompoundTag blockEntry = new CompoundTag();
            ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(bp.x()));
            pos.add(IntTag.valueOf(bp.y()));
            pos.add(IntTag.valueOf(bp.z()));
            blockEntry.put("pos", pos);
            blockEntry.putInt("state", stateIndex);
            blocksList.add(blockEntry);
        }
        tag.put("blocks", blocksList);

        ListTag palette = new ListTag();
        for (Map.Entry<String, BlockPlacement> kv : paletteKeyToPlacement.entrySet()) {
            BlockPlacement bp = kv.getValue();
            CompoundTag entry = new CompoundTag();
            entry.putString("Name", bp.block());
            if (!bp.properties().isEmpty()) {
                CompoundTag propsTag = new CompoundTag();
                // Write properties in sorted order for determinism
                new TreeMap<>(bp.properties()).forEach(propsTag::putString);
                entry.put("Properties", propsTag);
            }
            palette.add(entry);
        }
        tag.put("palette", palette);

        // Build entities from content (explicit entities + item placements)
        ListTag entitiesList = new ListTag();
        for (EntityPlacement ep : def.content().entities()) {
            entitiesList.add(createEntityTag(ep.entity(), ep.x(), ep.y(), ep.z(), null));
        }
        for (ItemPlacement ip : def.content().items()) {
            CompoundTag itemNbt = new CompoundTag();
            CompoundTag itemStack = new CompoundTag();
            itemStack.putString("id", ip.item());
            itemStack.putInt("count", ip.count());
            itemNbt.put("Item", itemStack);
            entitiesList.add(createEntityTag("minecraft:item", ip.x(), ip.y(), ip.z(), itemNbt));
        }
        tag.put("entities", entitiesList);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, baos);
            byte[] bytes = baos.toByteArray();
            cachedOutput.writeIfNeeded(outputPath, bytes, Hashing.sha1().hashBytes(bytes));
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Failed to write structure: " + id, e));
        }
    }

    private static String buildPaletteKey(BlockPlacement bp) {
        if (bp.properties().isEmpty()) {
            return bp.block();
        }
        StringBuilder sb = new StringBuilder(bp.block()).append('[');
        new TreeMap<>(bp.properties()).forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
        sb.setCharAt(sb.length() - 1, ']');
        return sb.toString();
    }

    private static CompoundTag createEntityTag(String entityId, double x, double y, double z, CompoundTag extraNbt) {
        CompoundTag entityEntry = new CompoundTag();

        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(x));
        pos.add(DoubleTag.valueOf(y));
        pos.add(DoubleTag.valueOf(z));
        entityEntry.put("pos", pos);

        ListTag blockPos = new ListTag();
        blockPos.add(IntTag.valueOf((int) x));
        blockPos.add(IntTag.valueOf((int) y));
        blockPos.add(IntTag.valueOf((int) z));
        entityEntry.put("blockPos", blockPos);

        CompoundTag nbt = extraNbt != null ? extraNbt.copy() : new CompoundTag();
        nbt.putString("id", entityId);
        entityEntry.put("nbt", nbt);

        return entityEntry;
    }

    @Override
    public String getName() {
        return "S3 Advanced Game Test Structures";
    }
}
