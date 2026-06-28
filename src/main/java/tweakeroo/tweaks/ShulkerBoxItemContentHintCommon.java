/*
 * This file is part of the TweakerMore project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2023  Fallen_Breath and contributors
 *
 * TweakerMore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TweakerMore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TweakerMore.  If not, see <https://www.gnu.org/licenses/>.
 */

package tweakeroo.tweaks;

import malilib.util.data.Identifier;
import malilib.util.game.wrap.DefaultedList;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import tweakeroo.config.FeatureToggle;

import java.util.Optional;
import java.util.function.Predicate;

import static malilib.util.inventory.StorageItemInventoryUtils.readStoredItems;

public class ShulkerBoxItemContentHintCommon
{
    public static class Info
    {
        public boolean enabled;
        public boolean allItemSame;
        public boolean allItemSameIgnoreNbt;
        public double scale;
        public ItemStack stack;
        public double fillRatio;
    }

    public static Info prepareInformation(ItemStack itemStack)
    {
        Info info = new Info();
        info.enabled = false;
        if (!FeatureToggle.TWEAK_SHULKER_BOX_ITEM_CONTENT_HINT.getBooleanValue())
        {
            return info;
        }
        if (ShulkerBoxItemContentHintRenderer.isRendering.get())  // no hint if it's rendering our hint item
        {
            return info;
        }
        if (!(itemStack.getItem() instanceof ItemShulkerBox))
        {
            return info;
        }

        DefaultedList<ItemStack> list = DefaultedList.ofSize(27, ItemStack.EMPTY);
        readStoredItems(itemStack, (pair) -> list.set(pair.getKey(), pair.getValue()));
        Optional<DefaultedList<ItemStack>> stackList = Optional.of(list);
        if (!stackList.isPresent())
        {
            return info;
        }

        Predicate<ItemStack> stackFilter = stack -> true;
        //if (TweakerMoreConfigs.SHULKER_BOX_ITEM_CONTENT_HINT_CUSTOM_NAMES_OVERRIDE_ITEM.getBooleanValue())
        {
            Optional<ItemStack> override = computeCustomNameOverride(itemStack);
            if (override.isPresent())
            {
                stackFilter = stack -> ItemStack.areItemsEqual(stack, override.get());
            }
        }

        ItemStack std = null;
        info.allItemSame = true;
        info.allItemSameIgnoreNbt = true;
        for (ItemStack stack : stackList.get())
        {
            if (stack.isEmpty() || !stackFilter.test(stack))
            {
                continue;
            }

            if (std == null)
            {
                std = stack;
                continue;
            }

            boolean itemEqual = ItemStack.areItemsEqual(stack, std);
            boolean itemAndNbtEqual =
                //#if MC >= 12000
                //$$ ItemStack.canCombine(stack, std);
                //#else
                itemEqual && ItemStack.areItemStackTagsEqual(stack, std);
            //#endif

            if (!itemAndNbtEqual)
            {
                info.allItemSame = false;
            }
            if (!itemEqual)
            {
                info.allItemSameIgnoreNbt = false;
            }
        }

        if (std == null)
        {
            return info;
        }

        info.stack = std;
        //info.scale = TweakerMoreConfigs.SHULKER_BOX_ITEM_CONTENT_HINT_SCALE.getDoubleValue();
        info.scale = 0.8;
        if (info.scale <= 0)
        {
            return info;
        }

        info.enabled = true;
        info.fillRatio = -1;

        int slotAmount = 27;
        if (slotAmount != -1)
        {
            double sum = 0;
            for (ItemStack stack : stackList.get())
            {
                sum += 1.0D * stack.getCount() / stack.getMaxStackSize();
            }

            double ratio = sum / slotAmount;
            if (ratio >= 0 && ratio <= 1)
            {
                info.fillRatio = ratio;
            }
        }

        return info;
    }

    private static Optional<ItemStack> computeCustomNameOverride(ItemStack shulkerBoxItemStack)
    {
        if (!shulkerBoxItemStack.hasDisplayName())
        {
            return Optional.empty();
        }

        Optional<Identifier> itemIdOpt = Optional.of(new Identifier(shulkerBoxItemStack.getDisplayName()));
        if (!itemIdOpt.isPresent())
        {
            return Optional.empty();
        }

        if (!Item.REGISTRY.containsKey(itemIdOpt.get()))
        {
            return Optional.empty();
        }

        Item item = Item.REGISTRY.
            // remapping from low version to mc1.21.3 will rename the `get` to `getEntry`, which is bad
            //#disable-remap
                getObject(itemIdOpt.get());
        //#enable-remap
        return Optional.of(new ItemStack(item));
    }
}
