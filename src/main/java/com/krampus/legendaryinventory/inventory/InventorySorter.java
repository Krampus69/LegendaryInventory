package com.krampus.legendaryinventory.inventory;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.IItemHandlerModifiable;
import com.krampus.legendaryinventory.weight.WeightTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class InventorySorter {

    private InventorySorter() {}

    public static void sort(IItemHandlerModifiable handler, boolean byWeight) {
        sort(handler, 0, handler.getSlots(), byWeight);
    }

    private static void sort(IItemHandlerModifiable handler, int from, int to, boolean byWeight) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = from; i < to; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        mergeStacks(stacks);
        stacks.sort(byWeight ? InventorySorter::compareWeight : InventorySorter::compare);

        for (int i = from; i < to; i++) {
            int index = i - from;
            handler.setStackInSlot(i, index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY);
        }
    }

    private static void mergeStacks(List<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (stack.isEmpty() || stack.getCount() >= stack.getMaxStackSize()) {
                continue;
            }
            for (int j = i + 1; j < list.size(); j++) {
                ItemStack other = list.get(j);
                if (other.isEmpty() || !ItemStack.isSameItemSameTags(stack, other)) {
                    continue;
                }
                int room = stack.getMaxStackSize() - stack.getCount();
                if (room <= 0) {
                    break;
                }
                int moved = Math.min(room, other.getCount());
                stack.grow(moved);
                other.shrink(moved);
            }
        }
        list.removeIf(ItemStack::isEmpty);
    }

    private static int compareWeight(ItemStack a, ItemStack b) {
        int wa = WeightTable.of(a);
        int wb = WeightTable.of(b);
        if (wa != wb) {
            return wb - wa;
        }
        return compare(a, b);
    }

    private static int compare(ItemStack a, ItemStack b) {
        Category ca = Category.of(a);
        Category cb = Category.of(b);
        if (ca != cb) {
            return ca.ordinal() - cb.ordinal();
        }
        int result = ca.comparator.compare(a, b);
        return result != 0 ? result : fallback(a, b);
    }

    private static int fallback(ItemStack a, ItemStack b) {
        int byId = Item.getId(a.getItem()) - Item.getId(b.getItem());
        if (byId != 0) {
            return byId;
        }
        int byDamage = a.getDamageValue() - b.getDamageValue();
        if (byDamage != 0) {
            return byDamage;
        }
        return b.getCount() - a.getCount();
    }

    private static int foodCompare(ItemStack a, ItemStack b) {
        FoodProperties fa = a.getItem().getFoodProperties(a, null);
        FoodProperties fb = b.getItem().getFoodProperties(b, null);
        if (fa == null || fb == null) {
            return 0;
        }
        int byNutrition = fb.getNutrition() - fa.getNutrition();
        if (byNutrition != 0) {
            return byNutrition;
        }
        return Float.compare(fb.getSaturationModifier(), fa.getSaturationModifier());
    }

    private static int tierCompare(ItemStack a, ItemStack b) {
        int ta = a.getItem() instanceof TieredItem t ? t.getTier().getLevel() : 0;
        int tb = b.getItem() instanceof TieredItem t ? t.getTier().getLevel() : 0;
        return tb - ta;
    }

    private static int swordCompare(ItemStack a, ItemStack b) {
        float da = a.getItem() instanceof SwordItem s ? s.getDamage() : 0.0F;
        float db = b.getItem() instanceof SwordItem s ? s.getDamage() : 0.0F;
        return Float.compare(db, da);
    }

    private static int armorCompare(ItemStack a, ItemStack b) {
        if (!(a.getItem() instanceof ArmorItem aa) || !(b.getItem() instanceof ArmorItem ab)) {
            return 0;
        }
        int bySlot = aa.getEquipmentSlot().getIndex() - ab.getEquipmentSlot().getIndex();
        if (bySlot != 0) {
            return bySlot;
        }
        int byDefense = ab.getDefense() - aa.getDefense();
        if (byDefense != 0) {
            return byDefense;
        }
        return Float.compare(ab.getToughness(), aa.getToughness());
    }

    private static int enchantCompare(ItemStack a, ItemStack b) {
        return enchantPower(b) - enchantPower(a);
    }

    private static int enchantPower(ItemStack stack) {
        int total = 0;
        for (int level : EnchantmentHelper.getEnchantments(stack).values()) {
            total += level;
        }
        return total;
    }

    private static int damageCompare(ItemStack a, ItemStack b) {
        return a.getDamageValue() - b.getDamageValue();
    }

    @SafeVarargs
    private static Comparator<ItemStack> chain(Comparator<ItemStack>... parts) {
        return (a, b) -> {
            for (Comparator<ItemStack> part : parts) {
                int result = part.compare(a, b);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

    private static Predicate<ItemStack> is(Class<? extends Item> type) {
        return stack -> type.isInstance(stack.getItem());
    }

    private enum Category {

        FOOD(ItemStack::isEdible, InventorySorter::foodCompare),
        TORCH(stack -> stack.getItem() == Blocks.TORCH.asItem()),
        PICKAXE(is(net.minecraft.world.item.PickaxeItem.class), chain(InventorySorter::tierCompare, InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        SHOVEL(is(net.minecraft.world.item.ShovelItem.class), chain(InventorySorter::tierCompare, InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        AXE(is(net.minecraft.world.item.AxeItem.class), chain(InventorySorter::tierCompare, InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        SWORD(is(SwordItem.class), chain(InventorySorter::swordCompare, InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        TOOL(is(DiggerItem.class), chain(InventorySorter::tierCompare, InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        ARMOR(is(ArmorItem.class), chain(InventorySorter::armorCompare, InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        BOW(is(BowItem.class), chain(InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        CROSSBOW(is(CrossbowItem.class), chain(InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        TRIDENT(is(TridentItem.class), chain(InventorySorter::enchantCompare, InventorySorter::damageCompare)),
        ARROW(is(ArrowItem.class)),
        POTION(is(PotionItem.class)),
        MINECART(is(MinecartItem.class)),
        RAIL(stack -> stack.getItem() == Blocks.RAIL.asItem()
            || stack.getItem() == Blocks.POWERED_RAIL.asItem()
            || stack.getItem() == Blocks.DETECTOR_RAIL.asItem()
            || stack.getItem() == Blocks.ACTIVATOR_RAIL.asItem()),
        DYE(is(DyeItem.class)),
        MISC(stack -> !(stack.getItem() instanceof BlockItem)),
        BLOCK(stack -> true);

        private final Predicate<ItemStack> predicate;
        private final Comparator<ItemStack> comparator;

        Category(Predicate<ItemStack> predicate) {
            this(predicate, (a, b) -> 0);
        }

        Category(Predicate<ItemStack> predicate, Comparator<ItemStack> comparator) {
            this.predicate = predicate;
            this.comparator = comparator;
        }

        static Category of(ItemStack stack) {
            for (Category category : values()) {
                if (category.predicate.test(stack)) {
                    return category;
                }
            }
            return BLOCK;
        }
    }
}
