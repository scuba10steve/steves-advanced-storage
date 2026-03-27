package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.RecipePattern;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

public class RecipePatternMenu extends AbstractContainerMenu {

    /** ContainerData indices */
    private static final int DATA_MATCH_COUNT = 0;
    private static final int DATA_SELECTED_INDEX = 1;
    private static final int DATA_COUNT = 2;

    /** Menu slot layout: 0–8 ingredient (ghost), 9 output (read-only), 10–36 player inv, 37–45 hotbar */
    private static final int INGREDIENT_SLOTS = 9;
    private static final int OUTPUT_SLOT = 9;

    private final BlockPos pos;
    private final int patternIndex;
    /** Non-null on server only. */
    private final RecipeMemoryBoxBlockEntity blockEntity;
    private final SimpleContainer ingredientContainer;
    private final SimpleContainer outputContainer;
    private final ContainerData data;
    /** Server-only: recipes that match the current ingredient grid. */
    private final List<RecipeHolder<CraftingRecipe>> matchedRecipes = new ArrayList<>();

    // Client constructor
    public RecipePatternMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), buf.readInt(), null,
             new SimpleContainer(INGREDIENT_SLOTS), new SimpleContainer(1),
             new SimpleContainerData(DATA_COUNT));
    }

    // Server constructor — opened from RecipeMemoryBoxMenu.clicked()
    public RecipePatternMenu(int containerId, Inventory playerInventory,
                              RecipeMemoryBoxBlockEntity be, int patternIndex) {
        this(containerId, playerInventory, be.getBlockPos(), patternIndex, be,
             buildIngredientContainer(be.getPattern(patternIndex)), new SimpleContainer(1),
             new SimpleContainerData(DATA_COUNT));
        // Resolve recipes from the pre-loaded ingredient grid
        resolveRecipes();
    }

    private RecipePatternMenu(int containerId, Inventory playerInventory, BlockPos pos, int patternIndex,
                               RecipeMemoryBoxBlockEntity be, SimpleContainer ingredients,
                               SimpleContainer output, ContainerData data) {
        super(ModMenuTypes.RECIPE_PATTERN.get(), containerId);
        this.pos = pos;
        this.patternIndex = patternIndex;
        this.blockEntity = be;
        this.ingredientContainer = ingredients;
        this.outputContainer = output;
        this.data = data;
        addDataSlots(data);

        // Ingredient ghost slots (3×3 grid). Interaction is overridden in clicked().
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            int row = i / 3, col = i % 3;
            addSlot(new Slot(ingredientContainer, i, 26 + col * 18, 17 + row * 18) {
                // Slot contents are managed exclusively via clicked(); block default behavior.
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
                @Override
                public boolean mayPickup(Player player) { return false; }
            });
        }

        // Output display slot (read-only)
        addSlot(new Slot(outputContainer, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
            @Override
            public boolean mayPickup(Player player) { return false; }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static SimpleContainer buildIngredientContainer(RecipePattern pattern) {
        SimpleContainer container = new SimpleContainer(9);
        for (int i = 0; i < 9; i++) {
            container.setItem(i, pattern.getIngredient(i).copy());
        }
        return container;
    }

    /**
     * Ghost slot interaction: left-click sets the slot to a copy of the cursor item (count 1).
     * Right-click clears the slot. Player's held item is never consumed.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < INGREDIENT_SLOTS) {
            if (clickType == ClickType.PICKUP) {
                ItemStack cursor = getCarried();
                if (button == 0 && !cursor.isEmpty()) {
                    ingredientContainer.setItem(slotId, cursor.copyWithCount(1));
                } else {
                    ingredientContainer.setItem(slotId, ItemStack.EMPTY);
                }
                resolveRecipes();
                return;
            }
            // Ignore all other click types on ingredient slots
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Resolves crafting recipes from the current ingredient grid (server only). */
    private void resolveRecipes() {
        if (blockEntity == null || !(blockEntity.getLevel() instanceof ServerLevel serverLevel)) return;
        matchedRecipes.clear();

        List<ItemStack> items = new ArrayList<>(INGREDIENT_SLOTS);
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            items.add(ingredientContainer.getItem(i));
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        matchedRecipes.addAll(
            serverLevel.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, input, serverLevel));

        data.set(DATA_MATCH_COUNT, matchedRecipes.size());
        int idx = Math.min(data.get(DATA_SELECTED_INDEX), Math.max(0, matchedRecipes.size() - 1));
        data.set(DATA_SELECTED_INDEX, idx);

        if (!matchedRecipes.isEmpty()) {
            outputContainer.setItem(0,
                matchedRecipes.get(idx).value().getResultItem(serverLevel.registryAccess()).copy());
        } else {
            outputContainer.setItem(0, ItemStack.EMPTY);
        }
    }

    /**
     * Button 0 = Save pattern.
     * Button 1 = Previous matching recipe.
     * Button 2 = Next matching recipe.
     * Button 3 = Back (reopen RecipeMemoryBoxMenu).
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            savePattern(player);
            return true;
        }
        if (id == 3) {
            if (blockEntity != null && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(blockEntity,
                    buf -> buf.writeBlockPos(blockEntity.getBlockPos()));
            }
            return true;
        }
        if (id == 4) {
            for (int i = 0; i < INGREDIENT_SLOTS; i++) {
                ingredientContainer.setItem(i, ItemStack.EMPTY);
            }
            resolveRecipes();
            return true;
        }
        int count = data.get(DATA_MATCH_COUNT);
        if (count == 0) return false;
        int idx = data.get(DATA_SELECTED_INDEX);
        if (id == 1) { // Prev
            data.set(DATA_SELECTED_INDEX, (idx - 1 + count) % count);
            resolveRecipes();
            return true;
        }
        if (id == 2) { // Next
            data.set(DATA_SELECTED_INDEX, (idx + 1) % count);
            resolveRecipes();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private void savePattern(Player player) {
        if (blockEntity == null || !(blockEntity.getLevel() instanceof ServerLevel serverLevel)) return;
        RecipePattern pattern = new RecipePattern();
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            pattern.setIngredient(i, ingredientContainer.getItem(i));
        }
        int idx = data.get(DATA_SELECTED_INDEX);
        if (!matchedRecipes.isEmpty() && idx < matchedRecipes.size()) {
            RecipeHolder<CraftingRecipe> holder = matchedRecipes.get(idx);
            pattern.setPinnedRecipeId(holder.id());
            pattern.setOutput(holder.value().getResultItem(serverLevel.registryAccess()));
        }
        blockEntity.setPattern(patternIndex, pattern);
    }

    public int getMatchCount() { return data.get(DATA_MATCH_COUNT); }
    public int getSelectedIndex() { return data.get(DATA_SELECTED_INDEX); }

    /** Called from the JEI ghost-slot fill packet handler to bulk-set ingredient slots. */
    public void setIngredients(List<ItemStack> items) {
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            ItemStack stack = i < items.size() ? items.get(i) : ItemStack.EMPTY;
            ingredientContainer.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        resolveRecipes();
    }

    /** Factory method used by RecipeMemoryBoxMenu to open this menu for a specific pattern slot. */
    public static MenuProvider createMenuProvider(RecipeMemoryBoxBlockEntity be, int patternIndex) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.s3_advanced.recipe_pattern");
            }
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
                return new RecipePatternMenu(containerId, inv, be, patternIndex);
            }
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
