package fi.dy.masa.tweakeroo.mixin;

import net.minecraft.entity.data.TrackedData;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.util.CameraEntity;
import fi.dy.masa.tweakeroo.util.CameraUtils;
import fi.dy.masa.tweakeroo.util.DummyMovementInput;
import fi.dy.masa.tweakeroo.util.InventoryUtils;


@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity
{
    @Shadow public Input input;
    @Shadow protected int field_3935; // sprintToggleTimer

    @Shadow public float lastNauseaStrength;
    @Shadow public float nextNauseaStrength;
    @Shadow private boolean field_3939; // falling
    private final DummyMovementInput dummyMovementInput = new DummyMovementInput(null);
    @Unique private Input realInput;
    @Unique private float realNextNauseaStrength;
    @Unique private ItemStack autoSwitchElytraChestplate = ItemStack.EMPTY;

    public MixinClientPlayerEntity(ClientWorld worldIn, GameProfile playerProfile)
    {
        super(worldIn, playerProfile);
    }

    @Redirect(method = "updateNausea()V",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/screen/Screen;isPauseScreen()Z"))
    private boolean onDoesGuiPauseGame(Screen gui)
    {
        // Spoof the return value to prevent entering the if block
        if (Configs.Disable.DISABLE_PORTAL_GUI_CLOSING.getBooleanValue())
        {
            return true;
        }

        return gui.isPauseScreen();
    }

    @Inject(method = "updateNausea", at = @At("HEAD"))
    private void disableNauseaEffectPre(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_NAUSEA_EFFECT.getBooleanValue())
        {
            this.nextNauseaStrength = this.realNextNauseaStrength;
        }
    }

    @Inject(method = "updateNausea", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;tickNetherPortalCooldown()V"))
    private void disableNauseaEffectPost(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_NAUSEA_EFFECT.getBooleanValue())
        {
            // This is used to set the value to the correct value for the duration of the
            // updateNausea() method, so that the portal sound plays correctly only once.
            this.realNextNauseaStrength = this.nextNauseaStrength;
            this.lastNauseaStrength = 0.0f;
            this.nextNauseaStrength = 0.0f;
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void fixElytraDeployment(CallbackInfo ci)
    {
        if (Configs.Fixes.ELYTRA_FIX.getBooleanValue() && this.isSubmergedInWater() == false)
        {
            this.setFlag(7, true);
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "FIELD",
                target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z", ordinal = 1))
    private void overrideSprint(CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_PERMANENT_SPRINT.getBooleanValue() &&
            ! this.isSprinting() && ! this.isUsingItem() && this.input.movementForward >= 0.8F &&
            (this.getHungerManager().getFoodLevel() > 6.0F || this.abilities.allowFlying) &&
            ! this.hasStatusEffect(StatusEffects.BLINDNESS) && this.isTouchingWater() == false)
        {
            this.setSprinting(true);
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "FIELD",
                target = "Lnet/minecraft/client/network/ClientPlayerEntity;horizontalCollision:Z"))
    private boolean overrideCollidedHorizontally(ClientPlayerEntity player)
    {
        if (Configs.Disable.DISABLE_WALL_UNSPRINT.getBooleanValue())
        {
            return false;
        }

        return player.horizontalCollision;
    }

    @Inject(method = "tickMovement",
            slice = @Slice(from = @At(value = "INVOKE",
                                      target = "Lnet/minecraft/client/network/ClientPlayerEntity;getHungerManager()" +
                                               "Lnet/minecraft/entity/player/HungerManager;")),
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER,
                     target = "Lnet/minecraft/client/network/ClientPlayerEntity;field_3935:I")) // spintToggleTimer
    private void disableDoubleTapSprint(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_DOUBLE_TAP_SPRINT.getBooleanValue())
        {
            this.field_3935 = 0;
        }
    }

    @Inject(method = "tickMovement",
            at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"))
    private void onFallFlyingCheckChestSlot(CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_AUTO_SWITCH_ELYTRA.getBooleanValue())
        {
            // Sakura's version calculating fall distance...
            //if ((!this.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && this.fallDistance > 20.0f)

            // Auto switch if it is not elytra after falling, or is totally broken.
            // This also shouldn't activate on the Ground if the Chest Equipment is EMPTY,
            // or not an Elytra to be swapped back.
            //
            // !isOnGround(): Minecraft also check elytra even if the player is on the ground, skip it.
            // !isSwimming(): Check if the Player is swimming so, it doesn't perform the Elytra Swap while underwater.
            if (!this.onGround && !this.isSwimming()
                && (this.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA
                || (this.getEquippedStack(EquipmentSlot.CHEST).getDamage() > this.getEquippedStack(EquipmentSlot.CHEST).getMaxDamage() - 10)
                && (!this.getEquippedStack(EquipmentSlot.CHEST).isEmpty() || this.autoSwitchElytraChestplate.getItem() == Items.ELYTRA)))
            {
                this.autoSwitchElytraChestplate = this.getEquippedStack(EquipmentSlot.CHEST).copy();
                InventoryUtils.swapElytraWithChestPlate(this);
            }
        }
        else
        {
            // reset auto switch item if the feature is disabled.
            this.autoSwitchElytraChestplate = ItemStack.EMPTY;
        }
    }


    @Inject(method = "onTrackedDataSet", at = @At("RETURN"))
    private void onStopFlying(TrackedData<?> data, CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_AUTO_SWITCH_ELYTRA.getBooleanValue())
        {
            if (FLAGS.equals(data) && this.field_3939)
            {
                if (!this.isFallFlying() && this.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)
                {
                    if (!this.autoSwitchElytraChestplate.isEmpty() && this.autoSwitchElytraChestplate.getItem() != Items.ELYTRA)
                    {
                        if (this.inventory.getCursorStack().isEmpty())
                        {
                            int targetSlot = InventoryUtils.findSlotWithItem(this.playerContainer, this.autoSwitchElytraChestplate, true, false);

                            if (targetSlot >= 0)
                            {
                                InventoryUtils.swapItemToEquipmentSlot(this, EquipmentSlot.CHEST, targetSlot);
                                this.autoSwitchElytraChestplate = ItemStack.EMPTY;
                            }
                        }
                    }
                    else
                    {
                        // if cached previous item is empty, try to swap back to the default chest plate.
                        InventoryUtils.swapElytraWithChestPlate(this);
                    }
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void disableMovementInputsPre(CallbackInfo ci)
    {
        if (CameraUtils.shouldPreventPlayerMovement())
        {
            this.realInput = this.input;
            this.input = this.dummyMovementInput;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void disableMovementInputsPost(CallbackInfo ci)
    {
        if (this.realInput != null)
        {
            this.input = this.realInput;
            this.realInput = null;
        }
    }

    @Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
    private void allowPlayerMovementInFreeCameraMode(CallbackInfoReturnable<Boolean> cir)
    {
        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue() && CameraEntity.originalCameraWasPlayer())
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void preventHandSwing(Hand hand, CallbackInfo ci)
    {
        if (CameraUtils.shouldPreventPlayerInputs())
        {
            ci.cancel();
        }
    }
}
