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

import malilib.render.RenderContext;
import malilib.util.data.Color4f;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RenderWrap;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.item.ItemStack;
import tweakeroo.mixin.IMixinRenderItem;

import java.awt.*;


public class ShulkerBoxItemContentHintRenderer
{
    // the display width of an item slot
    private static final int SLOT_WIDTH = 16;
    static final ThreadLocal<Boolean> isRendering = ThreadLocal.withInitial(() -> false);

    public static void render(RenderItem itemRenderer, ItemStack itemStack, int x, int y)
    {
        ShulkerBoxItemContentHintCommon.Info info = ShulkerBoxItemContentHintCommon.prepareInformation(itemStack);
        if (!info.enabled)
        {
            return;
        }

        RenderContext renderContext = RenderContext.DUMMY;

        //RenderUtils.Scaler scaler = RenderUtils.createScaler(x, y + SLOT_WIDTH, info.scale);
        //scaler.apply(renderContext);
        RenderWrap.pushMatrix(renderContext);
        RenderWrap.translate(-x * info.scale, -(y + SLOT_WIDTH) * info.scale, 0, renderContext);
        RenderWrap.scale(info.scale, info.scale, 1, renderContext);
        RenderWrap.translate(x / info.scale, (y + SLOT_WIDTH) / info.scale, 0, renderContext);

        if (info.allItemSame || info.allItemSameIgnoreNbt)
        {
            renderMiniItem(
                renderContext,
                itemRenderer,
                info, x, y
            );
        }
        if (!info.allItemSame)
        {
            renderText(
                itemRenderer.zLevel + 150,
                info, x, y
            );
        }

        //scaler.restore();
        RenderWrap.popMatrix(renderContext);

        //boolean mixedBox = !info.allItemSameIgnoreNbt && !info.allItemSame;
        if (
            //(!mixedBox || TweakerMoreConfigs.SHULKER_BOX_ITEM_CONTENT_HINT_SHOW_BAR_ON_MIXED.getBooleanValue())
                0 < info.fillRatio && info.fillRatio < 1
        )
        {
            renderBar(
                renderContext,
                itemRenderer,
                info.fillRatio, x, y
            );
        }

    }

    private static void renderMiniItem(
        RenderContext renderContext,
        RenderItem itemRenderer,
        ShulkerBoxItemContentHintCommon.Info info, int x, int y)
    {
        isRendering.set(true);

        float zOffset = itemRenderer.zLevel;

        RenderWrap.pushMatrix(renderContext);
        try
        {
            itemRenderer.zLevel += 10;
            // scale the z axis, so the lighting of the item can render correctly
            // see net.minecraft.client.render.item.ItemRenderer.renderGuiItemModel for z offset applying
            RenderWrap.scale(1, 1, info.scale, renderContext);
            RenderWrap.translate(0F, 0F, (100.0F + itemRenderer.zLevel) * (float) (1 / info.scale - 1), renderContext);

            // we do this manually so no need to care about extra z-offset modification of itemRenderer in its ItemRenderer#renderGuiItem
            //itemRenderer.renderGuiItemIcon(
            //    info.stack, x, y
            //);
            itemRenderer.renderItemAndEffectIntoGUI(info.stack, x, y);
        }
        finally
        {
            isRendering.set(false);
            RenderWrap.popMatrix(renderContext);
            itemRenderer.zLevel = zOffset;
        }
    }

    private static void renderText(
        double zOffset, ShulkerBoxItemContentHintCommon.Info info, int x, int y
    )
    {
        FontRenderer fontRenderer = GameWrap.getClient().fontRenderer;

        String text = info.allItemSameIgnoreNbt ? "*" : "...";
        boolean putTextOnRight = info.allItemSameIgnoreNbt && info.scale <= 0.75;
        float width = fontRenderer.getStringWidth(text);
        float height = fontRenderer.FONT_HEIGHT;
        float textX = putTextOnRight ? x + SLOT_WIDTH + 0.5F : x + (SLOT_WIDTH - width) * 0.5F;
        float textY = putTextOnRight ? y + (SLOT_WIDTH - height) * 0.5F : y + SLOT_WIDTH - height - 3;
        double textScale = SLOT_WIDTH / height * 0.7 * (putTextOnRight ? 0.9 : 1);
        int textColor = 0xDDDDDD;

        //RenderUtils.Scaler textScaler = RenderUtils.createScaler(textX + width * 0.5, textY + height * 0.5, textScale);
        //textScaler.apply(RenderContext.of(
        //));
        RenderContext ctx = RenderContext.DUMMY;
        RenderWrap.pushMatrix(ctx);
        RenderWrap.translate(-(textX + width * 0.5) * textScale, -(textY + height * 0.5) * textScale, 0, ctx);
        RenderWrap.scale(textScale, textScale, 1, ctx);
        RenderWrap.translate((textX + width * 0.5) / textScale, (textY + height * 0.5) / textScale, 0, ctx);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        fontRenderer.drawStringWithShadow(text, textX, textY, textColor);
        GlStateManager.enableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();

        //textScaler.restore();
        RenderWrap.popMatrix(ctx);
    }

    @FunctionalInterface
    private interface GuiQuadDrawer
    {
        void draw(int x, int y, int width, int height, int color);
    }

    private static void renderBar(
        RenderContext renderContext,
        RenderItem itemRenderer,
        double fillRatio, int x, int y)
    {
        final int HEIGHT = SLOT_WIDTH / 2;
        final int WIDTH = 1;

        x = x + SLOT_WIDTH - WIDTH;
        y = y + SLOT_WIDTH - HEIGHT;

        // ====== [begin] ref: net.minecraft.client.render.item.ItemRenderer#renderGuiItemOverlay ======
        // (mc1.20+) net.minecraft.client.gui.DrawContext.drawItemInSlot

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();

        int h = (int)Math.round(fillRatio * HEIGHT);
        int color = Color.HSBtoRGB((float)(fillRatio / 3), 1.0F, 1.0F);
        if (h == 0)
        {
            // make sure h > 0 so it's visible enough
            h = 1;
            // make the color darker
            Color4f holder = Color4f.fromColor(color);
            color = new Color4f(holder.r / 2, holder.g / 2, holder.b / 2).intValue;
        }

        IMixinRenderItem accessor = (IMixinRenderItem) itemRenderer;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        GuiQuadDrawer drawer = (x_, y_, width_, height_, color_) -> {
            accessor.invokeDraw(bufferBuilder, x_, y_, width_ , height_,color_ >> 16 & 0xFF, color_ >> 8 & 0xFF, color_ & 0xFF, 0xFF);
        };

        drawer.draw( x, y, WIDTH, HEIGHT, 0x040404);
        drawer.draw(x, y + HEIGHT - h, WIDTH, h, color);

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();

        // ====== [end] ref: net.minecraft.client.render.item.ItemRenderer#renderGuiItemOverlay ======
    }
}
