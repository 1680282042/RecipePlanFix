package crashdive;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager.StackFrame;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import com.google.inject.Inject;

@Plugin(id = "crashdive_recipe_plan_fix", name = "Recipe Plan Fix", version = "0.1.0", description = "Bypasses the need for a project bench to program a recipe plan")
public class RecipePlanFixPlugin {

    /*
    * Full double chest
    * # = blocker (black stained glass pane)
    * - = empty
    * P = page
    * G = green stained glass
    * B = book
    *    0 1 2 3 4 5 6 7 8
    * 0  # # # # # # # # #
    * 9  # P # # # # # P #
    * 18 # # # - - - # # #
    * 27 # - # - - - # - #
    * 36 # # # - - - # # #
    * 45 G G G G G G G G G 
    */
    private final int INPUT_SLOT_INDEX = 28;
    private final int OUTPUT_SLOT_INDEX = 34;
    private final int HELP_PAGE_INPUT_INDEX = 10;
    private final int HELP_PAGE_OUTPUT_INDEX = 16;
    private Set<Integer> CRAFTING_SLOT_INDICES = new HashSet<Integer>(); // See onServerStart(...) for values
    private Set<Integer> PROGRAM_BUTTON_INDICES = new HashSet<Integer>(); // See onServerStart(...) for values
    private final String RECIPE_PLAN_NAME = "projectred-expansion:plan";
    private final String AIR_NAME = "minecraft:air";

    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("[RecipePlanFix] Started!");

        // Populate CRAFTING_SLOT_INDICES
        for (int slotIndex : new int[] {21, 22, 23, 30, 31, 32, 39, 40, 41}) {
            CRAFTING_SLOT_INDICES.add(slotIndex);
        }
        // Populate PROGRAM_BUTTON_INDICES
        for (int slotIndex : new int[] {45, 46, 47, 48, 49, 50, 51, 52, 53}) {
            PROGRAM_BUTTON_INDICES.add(slotIndex);
        }
    }


    /**
     * Returns items to a player. Either placing it directly in their inventory
     * or, failing that, at the player's feet.
     * 
     * @param slot The slot that the items should be removed from
     * @param player The player to return items to
     */
    public void returnItemsFromSlotToPlayer(Inventory slot, Player player) {
        // Remove items from inventory slot
        Optional<ItemStack> items = slot.poll();
        if (!items.isPresent()) {
            return;
        }

        // Give items to the player directly
        MainPlayerInventory playerInv = ((PlayerInventory) player.getInventory()).getMain();
        InventoryTransactionResult transaction = playerInv.offer(items.get());
        if (transaction.getType().equals(InventoryTransactionResult.Type.SUCCESS)) {
            return;
        }

        // Drop items that did not fit in inventory
        Location<World> location = player.getLocation();
        Extent extent = location.getExtent();
        for (ItemStackSnapshot stackSnapshot : transaction.getRejectedItems()) {
            Entity item = extent.createEntity(EntityTypes.ITEM, location.getPosition());
            item.offer(Keys.REPRESENTED_ITEM, stackSnapshot);

            // Magic juice from the documentation. Who knows why dropping an item is so complicated???
            try (StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLACEMENT);
                extent.spawnEntity(item);
            }
        }
    }

    /**
     * 
     * Programs the recipe plan 
     * 
     * @param inv The inventory 'menu' for the programmer
     */
    public void programRecipePlan(Inventory inv, Player player) {   
        // Get item stacks
        Inventory recipePlanSlot = null;
        ItemStack recipePlanStack = null;
        ItemStack outputStack = null;
        ItemStack craftingStacks[] = new ItemStack[9];
        int slotIndex = 0;
        int craftingGridIndex = 0;
        for (Inventory slot: inv.slots()) {
            ItemStack stack;
            if (slot.peek().isPresent()) {
                stack = slot.peek().get();
            } else {
                stack = ItemStack.builder().itemType(ItemTypes.NONE).build();
            }
            if (slotIndex == INPUT_SLOT_INDEX) {
                recipePlanSlot = slot;
                recipePlanStack = stack;
            } else if (slotIndex == OUTPUT_SLOT_INDEX) {
                outputStack = stack;
            } else if (CRAFTING_SLOT_INDICES.contains(slotIndex)) {
                craftingStacks[craftingGridIndex] = stack;
                craftingGridIndex++;
            }
            slotIndex++;
        }

        if ((recipePlanSlot == null) || (outputStack == null) || (recipePlanStack == null)) {
            // How did we get here?
            return;
        }

        // Check that a recipe plan is in the input spot
        if (!recipePlanStack.getType().getName().equals(RECIPE_PLAN_NAME)) {
            return;
        }


        // Remove recipe plan
        recipePlanSlot.set(ItemStack.builder().itemType(ItemTypes.NONE).build());

       // Build command
        StringBuilder giveCommand = new StringBuilder();
        giveCommand.append(String.format("give %s %s 1 0 ", player.getName(), RECIPE_PLAN_NAME));
        giveCommand.append("{recipe:[");
        for (ItemStack stack: craftingStacks) {
            // Obtain damage (aka sub-id) of item
            Optional<Object> damage = stack.toContainer().get(DataQuery.of("UnsafeDamage"));
            if (!damage.isPresent()) {
                return;
            }
            giveCommand.append(String.format("{id:\"%s\",", stack.getType().getName()));
            giveCommand.append(String.format("Count:%db,", stack.getQuantity()));
            giveCommand.append(String.format("Damage:%ss},", damage.get().toString()));
        }
        
        Optional<Object> damage = outputStack.toContainer().get(DataQuery.of("UnsafeDamage"));
        if (!damage.isPresent()) {
            logger.warn("Unable to obtain the damage (sub-id) of an item. Aborting programming.");
            return;
        }
        giveCommand.append(String.format("{id:\"%s\",", outputStack.getType().getName()));
        giveCommand.append(String.format("Count:%db,", outputStack.getQuantity()));
        giveCommand.append(String.format("Damage:%ss}", damage.get().toString()));

        giveCommand.append("]}");

        // Execute command
        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), giveCommand.toString());

        // Play anvil sound
        player.playSound(SoundTypes.BLOCK_ANVIL_USE, player.getPosition(), 1);
    }

    /**
     * Ensures that the only thing in the input slot is exactly 0 or 1 recipe plans.
     * Anything that isn't that is removed from the slot and returned to the player
     * 
     * @param inv The inventory 'menu' for the programmer
     * @param player The player that illegal items will be returned to
     * @return true items were removed (the event should be cancelled), false otherwise.
     */
    public boolean removeIllegalItemsFromInputSlot(Inventory inv, Player player) {
        // Obtain the input slot using the iterator
        Iterator<Inventory> slots = inv.slots().iterator();
        for (int i = 0; i < INPUT_SLOT_INDEX; i++) {
            slots.next();
        }
        Inventory inputSlot = slots.next();

        // Obtain the item type of the input slot
        Optional<ItemStack> inputSlotStack = inputSlot.peek();
        if (!inputSlotStack.isPresent()) {
            return false;
        }
        ItemType itemType = inputSlotStack.get().getType();

        // If the item type is air there are no illegal items
        if (itemType.getName().equals(AIR_NAME)) {
            return false;
        }

        // Check that there is only one item. Any more would be illegal.
        if (inputSlotStack.get().getQuantity() > 1) {
            returnItemsFromSlotToPlayer(inputSlot, player);
            return true;
        }

        // Check if the stack contains something other than a recipe plan. Anything else (but air) is illegal.
        if (!itemType.getName().equals(RECIPE_PLAN_NAME)) {
            returnItemsFromSlotToPlayer(inputSlot, player);
            return true;
        }

        return false;
    }

    public void onCloseInventory(InteractInventoryEvent.Close event) {
        // Obtain player
        Optional<Player> player = event.getCause().first(Player.class);
        if (!player.isPresent()) {
            return;
        }

        // Remove items from inventory
        Inventory inv = event.getTargetInventory();
        int slotIndex = -1;
        for (Inventory slot: inv.slots()) {
            slotIndex++;
            if (!slot.peek().isPresent()) {
                continue;
            } 
            if ((slotIndex == OUTPUT_SLOT_INDEX) || CRAFTING_SLOT_INDICES.contains(slotIndex)) {
                returnItemsFromSlotToPlayer(slot, player.get());
            }
        }
    }

    public void onItemInteraction(ClickInventoryEvent event) {
        // Return early if there is no player
        Optional<Player> player = event.getCause().first(Player.class);
        if (!player.isPresent()) {
            return;
        }

        // Return early if the player clicked outside of an inventory
        Optional<Slot> slot = event.getSlot();
        if (!slot.isPresent()) {
            return;
        }
        
        // Return early if the slot clicked on is not in the inventory
        Inventory inv = event.getTargetInventory();
        if (!inv.containsInventory(slot.get())) {
            return;
        }
        
        // Remove anything that doesn't belong in the input slot.
        if (removeIllegalItemsFromInputSlot(inv, player.get())) {
            return;
        }

        // Obtain slot coordinates
        boolean found = false;
        int slotIndex = 0;
        Iterable<Slot> slots = inv.slots();
        for (Slot s : slots) {
            if (s.equals(slot.get())) {
                found = true;
                break;
            }
            slotIndex++;
        }
        if (!found) {
            return;
        }
        int row = slotIndex / 9;

        // Do nothing if the slot is outside of the menu
        if (row > 5) {
            return;
        }

        // Cancel the event if the player interacts with a blocker or help item. (glass panes / paper)   
        if ((slotIndex != OUTPUT_SLOT_INDEX) && 
            (slotIndex != INPUT_SLOT_INDEX) &&
            (!CRAFTING_SLOT_INDICES.contains(slotIndex)) && 
            (!PROGRAM_BUTTON_INDICES.contains(slotIndex))) {
            event.setCancelled(true);
            return;
        }

        // Program recipe plan
        if (PROGRAM_BUTTON_INDICES.contains(slotIndex)) {
            programRecipePlan(inv, player.get());
            event.setCancelled(true);
            return;
        }
    }

    @Listener
    public void onRecipePlanHandInteraction(InteractItemEvent event, @First Player player) {        
        // Obtain item
        Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
        if (!item.isPresent()) {
            return;
        }
        String id = item.get().getType().getId();
        if (!id.equals(RECIPE_PLAN_NAME)) {
            return;
        }

        // Create the programming inventory and show it to the player
        Inventory programmingMenu = Inventory.builder()
                .of(InventoryArchetypes.MENU_GRID)
                .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of("Program Recipe Plan")))
                .property(InventoryDimension.PROPERTY_NAME, new InventoryDimension(9, 6))
                .withCarrier(player)
                .listener(ClickInventoryEvent.class, this::onItemInteraction)
                .listener(InteractInventoryEvent.Close.class, this::onCloseInventory)
                .build(this);
        player.openInventory(programmingMenu);


        // Fill chest
        int slotIndex = -1;
        Iterable<Slot> slots = programmingMenu.slots();
        for (Slot slot : slots) {
            // Increment
            slotIndex++;
            // Select item
            ItemStack stackToInsert;
            if (slotIndex == HELP_PAGE_INPUT_INDEX) {
                stackToInsert = ItemStack.of(ItemTypes.PAPER, 1);
                stackToInsert.offer(Keys.DISPLAY_NAME, Text.of("Place ONE recipe plan below."));
            } else if (slotIndex == HELP_PAGE_OUTPUT_INDEX) {
                stackToInsert = ItemStack.of(ItemTypes.PAPER, 1);
                stackToInsert.offer(Keys.DISPLAY_NAME, Text.of("Place the crafting output (optional)"));
            } else if (slotIndex == INPUT_SLOT_INDEX) {
                continue;
            } else if (CRAFTING_SLOT_INDICES.contains(slotIndex)) {
                continue;
            } else if (slotIndex == OUTPUT_SLOT_INDEX) {
                continue;
            } else if (PROGRAM_BUTTON_INDICES.contains(slotIndex)) {
                stackToInsert = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
                stackToInsert.offer(Keys.DYE_COLOR, DyeColors.GREEN);
                stackToInsert.offer(Keys.DISPLAY_NAME, Text.of("Program recipe plan."));
            } else {
                // Blockers
                stackToInsert = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
                stackToInsert.offer(Keys.DYE_COLOR, DyeColors.BLACK);
                stackToInsert.offer(Keys.DISPLAY_NAME, Text.of(String.valueOf(slotIndex)));
            }
            
            // Insert item
            slot.set(stackToInsert);

        }
    }
}