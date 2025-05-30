package fi.dy.masa.tweakeroo.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import net.minecraft.block.InfestedBlock;
import net.minecraft.item.*;
import net.minecraft.util.DefaultedList;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.tweakeroo.Tweakeroo;

public class CreativeExtraItems
{
    private static final ArrayListMultimap<ItemGroup, ItemStack> ADDED_ITEMS = ArrayListMultimap.create();
    private static final HashMap<Item, ItemGroup> OVERRIDDEN_GROUPS = new HashMap<>();

    @Nullable
    public static ItemGroup getGroupFor(Item item)
    {
        return OVERRIDDEN_GROUPS.get(item);
    }

    public static List<ItemStack> getExtraStacksForGroup(ItemGroup group)
    {
        return ADDED_ITEMS.get(group);
    }

    public static void setCreativeExtraItems(List<String> items)
    {
        setCreativeExtraItems(ItemGroup.TRANSPORTATION, items);
    }

    private static void setCreativeExtraItems(ItemGroup group, List<String> items)
    {
        ADDED_ITEMS.clear();
        OVERRIDDEN_GROUPS.clear();

        if (items.isEmpty())
        {
            return;
        }

        Tweakeroo.logger.info("Adding extra items to creative inventory group '{}'", group.getName());

        for (String str : items)
        {
            ItemStack stack = InventoryUtils.getItemStackFromString(str);

            if (stack != null && stack.isEmpty() == false)
            {
                if (stack.hasTag())
                {
                    ADDED_ITEMS.put(group, stack);
                }
                else
                {
                    OVERRIDDEN_GROUPS.put(stack.getItem(), group);
                }
            }
        }
    }

    public static void removeInfestedBlocks(DefaultedList<ItemStack> stacks)
    {
        stacks.removeIf((stack) -> stack.getItem() instanceof BlockItem &&
                                   ((BlockItem) stack.getItem()).getBlock() instanceof InfestedBlock);
    }
}
