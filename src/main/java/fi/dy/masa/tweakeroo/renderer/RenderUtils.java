package fi.dy.masa.tweakeroo.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.mixin.IMixinHorseBaseEntity;
import fi.dy.masa.tweakeroo.util.MiscUtils;
import fi.dy.masa.tweakeroo.util.RayTraceUtils;
import fi.dy.masa.tweakeroo.util.SnapAimMode;

public class RenderUtils
{
    private static long lastRotationChangeTime;

    public static void renderHotbarSwapOverlay(MinecraftClient mc)
    {
        PlayerEntity player = mc.player;

        if (player != null && mc.currentScreen == null)
        {
            final int scaledWidth = GuiUtils.getScaledWindowWidth();
            final int scaledHeight = GuiUtils.getScaledWindowHeight();
            final int offX = Configs.Generic.HOTBAR_SWAP_OVERLAY_OFFSET_X.getIntegerValue();
            final int offY = Configs.Generic.HOTBAR_SWAP_OVERLAY_OFFSET_Y.getIntegerValue();
            int startX = offX;
            int startY = offY;

            fi.dy.masa.malilib.config.HudAlignment align = (fi.dy.masa.malilib.config.HudAlignment) Configs.Generic.HOTBAR_SWAP_OVERLAY_ALIGNMENT.getOptionListValue();

            switch (align)
            {
                case TOP_RIGHT:
                    startX = scaledWidth - offX - 9 * 18;
                    break;
                case BOTTOM_LEFT:
                    startY = scaledHeight - offY - 3 * 18;
                    break;
                case BOTTOM_RIGHT:
                    startX = scaledWidth - offX - 9 * 18;
                    startY = scaledHeight - offY - 3 * 18;
                    break;
                case CENTER:
                    startX = scaledWidth / 2 - offX - 9 * 18 / 2;
                    startY = scaledHeight / 2 - offY - 3 * 18 / 2;
                    break;
                default:
            }

            int x = startX;
            int y = startY;
            TextRenderer textRenderer = mc.textRenderer;

            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            fi.dy.masa.malilib.render.RenderUtils.bindTexture(ContainerScreen.BACKGROUND_TEXTURE);
            fi.dy.masa.malilib.render.RenderUtils.drawTexturedRect(x - 1, y - 1, 7, 83, 9 * 18, 3 * 18);

            textRenderer.drawWithShadow("1", x - 10, y +  4, 0xFFFFFF);
            textRenderer.drawWithShadow("2", x - 10, y + 22, 0xFFFFFF);
            textRenderer.drawWithShadow("3", x - 10, y + 40, 0xFFFFFF);

            for (int row = 1; row <= 3; row++)
            {
                for (int column = 0; column < 9; column++)
                {
                    ItemStack stack = player.inventory.getInvStack(row * 9 + column);

                    if (stack.isEmpty() == false)
                    {
                        fi.dy.masa.malilib.render.InventoryOverlay.renderStackAt(stack, x, y, 1, mc);
                    }

                    x += 18;
                }

                y += 18;
                x = startX;
            }
        }
    }

    public static void renderInventoryOverlay(MinecraftClient mc)
    {
        World world = fi.dy.masa.malilib.util.WorldUtils.getBestWorld(mc);
        Entity cameraEntity = EntityUtils.getCameraEntity();

        if (cameraEntity == mc.player && world instanceof ServerWorld)
        {
            // We need to get the player from the server world (if available, ie. in single player),
            // so that the player itself won't be included in the ray trace
            Entity serverPlayer = world.getPlayerByUuid(mc.player.getUuid());

            if (serverPlayer != null)
            {
                cameraEntity = serverPlayer;
            }
        }

        HitResult trace = RayTraceUtils.getRayTraceFromEntity(world, cameraEntity, false);

        if (trace.getType() == HitResult.Type.MISS)
        {
            return;
        }

        Inventory inv = null;
        ShulkerBoxBlock block = null;
        LivingEntity entityLivingBase = null;

        if (trace.getType() == HitResult.Type.BLOCK)
        {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();
            Block blockTmp = world.getBlockState(pos).getBlock();

            if (blockTmp instanceof ShulkerBoxBlock)
            {
                block = (ShulkerBoxBlock) blockTmp;
            }

            inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);
        }
        else if (trace.getType() == HitResult.Type.ENTITY)
        {
            Entity entity = ((EntityHitResult) trace).getEntity();

            if (entity instanceof LivingEntity)
            {
                entityLivingBase = (LivingEntity) entity;
            }

            if (entity instanceof Inventory)
            {
                inv = (Inventory) entity;
            }
            else if (entity instanceof VillagerEntity)
            {
                inv = ((VillagerEntity) entity).getInventory();
            }
            else if (entity instanceof HorseBaseEntity)
            {
                inv = ((IMixinHorseBaseEntity) entity).getHorseInventory();
            }
        }

        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        int x = xCenter - 52 / 2;
        int y = yCenter - 92;

        if (inv != null && inv.getInvSize() > 0)
        {
            final boolean isHorse = (entityLivingBase instanceof HorseBaseEntity);
            final int totalSlots = isHorse ? inv.getInvSize() - 2 : inv.getInvSize();
            final int firstSlot = isHorse ? 2 : 0;

            final fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType type = (entityLivingBase instanceof VillagerEntity) ? fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType.VILLAGER : fi.dy.masa.malilib.render.InventoryOverlay.getInventoryType(inv);
            final fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties props = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryPropsTemp(type, totalSlots);
            final int rows = (int) Math.ceil((double) totalSlots / props.slotsPerRow);
            int xInv = xCenter - (props.width / 2);
            int yInv = yCenter - props.height - 6;

            if (rows > 6)
            {
                yInv -= (rows - 6) * 18;
                y -= (rows - 6) * 18;
            }

            if (entityLivingBase != null)
            {
                x = xCenter - 55;
                xInv = xCenter + 2;
                yInv = Math.min(yInv, yCenter - 92);
            }

            fi.dy.masa.malilib.render.RenderUtils.setShulkerboxBackgroundTintColor(block, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());

            if (isHorse)
            {
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, 1, 2, mc);
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, 2, mc);
                xInv += 32 + 4;
            }

            if (totalSlots > 0)
            {
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, totalSlots, mc);
                fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, firstSlot, totalSlots, mc);
            }
        }

        if (entityLivingBase != null)
        {
            fi.dy.masa.malilib.render.InventoryOverlay.renderEquipmentOverlayBackground(x, y, entityLivingBase);
            fi.dy.masa.malilib.render.InventoryOverlay.renderEquipmentStacks(entityLivingBase, x, y, mc);
        }
    }

    public static void renderPlayerInventoryOverlay(MinecraftClient mc)
    {
        int x = GuiUtils.getScaledWindowWidth() / 2 - 176 / 2;
        int y = GuiUtils.getScaledWindowHeight() / 2 + 10;
        int slotOffsetX = 8;
        int slotOffsetY = 8;
        fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType type = fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType.GENERIC;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, x, y, 9, 27, mc);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, mc.player.inventory, x + slotOffsetX, y + slotOffsetY, 9, 9, 27, mc);
    }

    public static void renderHotbarScrollOverlay(MinecraftClient mc)
    {
        Inventory inv = mc.player.inventory;
        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        final int x = xCenter - 176 / 2;
        final int y = yCenter + 6;
        fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType type = fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType.GENERIC;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, x, y     , 9, 27, mc);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, x, y + 70, 9,  9, mc);

        // Main inventory
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, x + 8, y +  8, 9, 9, 27, mc);
        // Hotbar
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, x + 8, y + 78, 9, 0,  9, mc);

        int currentRow = Configs.Internal.HOTBAR_SCROLL_CURRENT_ROW.getIntegerValue();
        fi.dy.masa.malilib.render.RenderUtils.drawOutline(x + 5, y + currentRow * 18 + 5, 9 * 18 + 4, 22, 2, 0xFFFF2020);
    }

    public static float getLavaFog(Entity entity, float originalFog)
    {
        if (entity instanceof LivingEntity)
        {
            LivingEntity living = (LivingEntity) entity;
            final int resp = EnchantmentHelper.getRespiration(living);
            final int aqua = EnchantmentHelper.getEquipmentLevel(Enchantments.AQUA_AFFINITY, living);
            // The original fog value of 2.0F is way too much to reduce gradually from.
            // You would only be able to see meaningfully with the full reduction.
            final float baseFog = 0.6F;
            final float respDecrement = (baseFog * 0.75F) / 3F - 0.02F;
            float fog = baseFog;

            if (living.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
            {
                fog -= baseFog * 0.4F;
            }

            if (aqua > 0) {
                fog -= respDecrement;
            }

            if (resp > 0)
            {
                fog -= (float) resp * respDecrement;
                fog = Math.max(0.12F,  fog);
            }

            return fog < baseFog ? fog : originalFog;
        }

        return originalFog;
    }

    public static void overrideLavaFog(Entity entity)
    {
        float fog = getLavaFog(entity, 2.0F);

        if (fog < 2.0F)
        {
            RenderSystem.fogDensity(fog);
        }
    }

    public static void renderDirectionsCursor(float zLevel, float partialTicks)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        RenderSystem.pushMatrix();

        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        RenderSystem.translated(width / 2, height / 2, zLevel);
        Entity entity = mc.getCameraEntity();
        RenderSystem.rotatef(entity.prevPitch + (entity.pitch - entity.prevPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
        RenderSystem.rotatef(entity.prevYaw + (entity.yaw - entity.prevYaw) * partialTicks, 0.0F, 1.0F, 0.0F);
        RenderSystem.scalef(-1.0F, -1.0F, -1.0F);
        RenderSystem.renderCrosshair(10);

        RenderSystem.popMatrix();
    }

    public static void notifyRotationChanged()
    {
        lastRotationChangeTime = System.currentTimeMillis();
    }

    public static void renderSnapAimAngleIndicator()
    {
        long current = System.currentTimeMillis();

        if (current - lastRotationChangeTime < 750)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
            final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
            SnapAimMode mode = (SnapAimMode) Configs.Generic.SNAP_AIM_MODE.getOptionListValue();

            if (mode != SnapAimMode.PITCH)
            {
                renderSnapAimAngleIndicatorYaw(xCenter, yCenter, 80, 12, mc);
            }

            if (mode != SnapAimMode.YAW)
            {
                renderSnapAimAngleIndicatorPitch(xCenter, yCenter, 12, 50, mc);
            }
        }
    }

    private static void renderSnapAimAngleIndicatorYaw(int xCenter, int yCenter, int width, int height, MinecraftClient mc)
    {
        double step = Configs.Generic.SNAP_AIM_YAW_STEP.getDoubleValue();
        double realYaw = MathHelper.floorMod(MiscUtils.getLastRealYaw(), 360.0D);
        double snappedYaw = MiscUtils.calculateSnappedAngle(realYaw, step);
        double startYaw = snappedYaw - (step / 2.0);
        final int x = xCenter - width / 2;
        final int y = yCenter + 10;
        int lineX = x + (int) ((MathHelper.wrapDegrees(realYaw - startYaw)) / step * width);
        TextRenderer textRenderer = mc.textRenderer;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        int bgColor = Configs.Generic.SNAP_AIM_INDICATOR_COLOR.getIntegerValue();

        // Draw the main box
        fi.dy.masa.malilib.render.RenderUtils.drawOutlinedBox(x, y, width, height, bgColor, 0xFFFFFFFF);

        String str = MathHelper.wrapDegrees(snappedYaw) + "°";
        textRenderer.draw(str, xCenter - textRenderer.getStringWidth(str) / 2, y + height + 2, 0xFFFFFFFF);

        str = "<  " + MathHelper.wrapDegrees(snappedYaw - step) + "°";
        textRenderer.draw(str, x - textRenderer.getStringWidth(str), y + height + 2, 0xFFFFFFFF);

        str = MathHelper.wrapDegrees(snappedYaw + step) + "°  >";
        textRenderer.draw(str, x + width, y + height + 2, 0xFFFFFFFF);

        if (Configs.Generic.SNAP_AIM_ONLY_CLOSE_TO_ANGLE.getBooleanValue())
        {
            double threshold = Configs.Generic.SNAP_AIM_THRESHOLD_YAW.getDoubleValue();
            int snapThreshOffset = (int) (width * threshold / step);

            // Draw the middle region
            fi.dy.masa.malilib.render.RenderUtils.drawRect(xCenter - snapThreshOffset, y, snapThreshOffset * 2, height, 0x6050FF50);

            if (threshold < (step / 2.0))
            {
                fi.dy.masa.malilib.render.RenderUtils.drawRect(xCenter - snapThreshOffset, y, 2, height, 0xFF20FF20);
                fi.dy.masa.malilib.render.RenderUtils.drawRect(xCenter + snapThreshOffset, y, 2, height, 0xFF20FF20);
            }
        }

        // Draw the current angle indicator
        fi.dy.masa.malilib.render.RenderUtils.drawRect(lineX, y, 2, height, 0xFFFFFFFF);
    }

    private static void renderSnapAimAngleIndicatorPitch(int xCenter, int yCenter, int width, int height, MinecraftClient mc)
    {
        double step = Configs.Generic.SNAP_AIM_PITCH_STEP.getDoubleValue();
        int limit = Configs.Generic.SNAP_AIM_PITCH_OVERSHOOT.getBooleanValue() ? 180 : 90;
        //double realPitch = MathHelper.clamp(MathHelper.wrapDegrees(MiscUtils.getLastRealPitch()), -limit, limit);
        double realPitch = MathHelper.wrapDegrees(MiscUtils.getLastRealPitch());
        double snappedPitch;

        if (realPitch < 0)
        {
            snappedPitch = -MiscUtils.calculateSnappedAngle(-realPitch, step);
        }
        else
        {
            snappedPitch = MiscUtils.calculateSnappedAngle(realPitch, step);
        }

        snappedPitch = MathHelper.clamp(MathHelper.wrapDegrees(snappedPitch), -limit, limit);

        int x = xCenter - width / 2;
        int y = yCenter - height - 10;

        renderPitchIndicator(x, y, width, height, realPitch, snappedPitch, step, true, mc);
    }

    public static void renderPitchLockIndicator(MinecraftClient mc)
    {
        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        int width = 12;
        int height = 50;
        int x = xCenter - width / 2;
        int y = yCenter - height - 10;
        double currentPitch = mc.player.pitch;
        double centerPitch = 0;
        double indicatorRange = 180;

        renderPitchIndicator(x, y, width, height, currentPitch, centerPitch, indicatorRange, false, mc);
    }

    private static void renderPitchIndicator(int x, int y, int width, int height,
            double currentPitch, double centerPitch, double indicatorRange, boolean isSnapRange, MinecraftClient mc)
    {
        double startPitch = centerPitch - (indicatorRange / 2.0);
        double printedRange = isSnapRange ? indicatorRange : indicatorRange / 2;
        int lineY = y + (int) ((MathHelper.wrapDegrees(currentPitch) - startPitch) / indicatorRange * height);
        double angleUp = centerPitch - printedRange;
        double angleDown = centerPitch + printedRange;

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
        TextRenderer textRenderer = mc.textRenderer;

        if (isSnapRange)
        {
            String strUp   = String.format("%6.1f° ^", MathHelper.wrapDegrees(angleUp));
            String strDown = String.format("%6.1f° v", MathHelper.wrapDegrees(angleDown));
            textRenderer.draw(strUp, x + width + 4, y - 4, 0xFFFFFFFF);
            textRenderer.draw(strDown, x + width + 4, y + height - 4, 0xFFFFFFFF);

            String str = String.format("%6.1f°", MathHelper.wrapDegrees(isSnapRange ? centerPitch : currentPitch));
            textRenderer.draw(str, x + width + 4, y + height / 2 - 4, 0xFFFFFFFF);
        }
        else
        {
            String str = String.format("%4.1f°", MathHelper.wrapDegrees(isSnapRange ? centerPitch : currentPitch));
            textRenderer.draw(str, x + width + 4, lineY - 4, 0xFFFFFFFF);
        }

        int bgColor = Configs.Generic.SNAP_AIM_INDICATOR_COLOR.getIntegerValue();
        // Draw the main box
        fi.dy.masa.malilib.render.RenderUtils.drawOutlinedBox(x, y, width, height, bgColor, 0xFFFFFFFF);

        int yCenter = y + height / 2 - 1;

        if (isSnapRange && Configs.Generic.SNAP_AIM_ONLY_CLOSE_TO_ANGLE.getBooleanValue())
        {
            double step = Configs.Generic.SNAP_AIM_YAW_STEP.getDoubleValue();
            double threshold = Configs.Generic.SNAP_AIM_THRESHOLD_PITCH.getDoubleValue();
            int snapThreshOffset = (int) ((double) height * threshold / indicatorRange);

            fi.dy.masa.malilib.render.RenderUtils.drawRect(x, yCenter - snapThreshOffset, width, snapThreshOffset * 2, 0x6050FF50);

            if (threshold < (step / 2.0))
            {
                fi.dy.masa.malilib.render.RenderUtils.drawRect(x, yCenter - snapThreshOffset, width, 2, 0xFF20FF20);
                fi.dy.masa.malilib.render.RenderUtils.drawRect(x, yCenter + snapThreshOffset, width, 2, 0xFF20FF20);
            }
        }
        else if (isSnapRange == false)
        {
            fi.dy.masa.malilib.render.RenderUtils.drawRect(x + 1, yCenter - 1, width - 2, 2, 0xFFC0C0C0);
        }

        // Draw the current angle indicator
        fi.dy.masa.malilib.render.RenderUtils.drawRect(x, lineY - 1, width, 2, 0xFFFFFFFF);
    }
}
