Details for modders and modpack authors :

## Commands

/inventory reload                      reload weights.json
/inventory set <item> <weight>         set a weight, saved to weights.json
/inventory capacity <player>           show capacity
/inventory capacity <player> <total>   set capacity
/inventory capacity <player> reset     remove tablet bonus
/inventory clear <player>              wipe every inventory, including Curios and Cosmetic Armor

Capacity is a normal attribute, `legendaryinventory:carry_capacity`, so `/attribute` works as well.

## Compatibility

The mod does not replace the vanilla inventory screen. It swaps the 27 main slots of any open menu for scrolling ones at the same positions, so screens draw as usual and buttons from other mods stay where they are.
Curios, Cosmetic Armor Reworked, JEI, etc...
Ask the author (Krampus) for more compat if necessary.

A menu is skipped if its player inventory slots are a Slot subclass. Set debug.logMenuAugment to see why a screen was not picked up. Mod authors can opt in through the API.

## API for modders

com.krampus.legendaryinventory.api.LegendaryInventoryApi

java
setDefaultWeight(new ResourceLocation("mymod", "heavy_thing"), 100);
registerWeightProvider((stack, current)...);
allowAugmentedSlotClass(MySlot.class);
excludeMenuFromAugment(MyMenu.class);
getExtendedInventory(player);
getCapacity(player); addCapacityBonus(player, 50);
Use the addCapacityBonus to add your own weight value increasing item.

Defaults set through the API are overridden by `weights.json`. Providers run for every stack that gets weighed, keep them cheap.
