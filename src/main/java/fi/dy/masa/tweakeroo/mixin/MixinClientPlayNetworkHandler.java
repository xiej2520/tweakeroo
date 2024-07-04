package fi.dy.masa.tweakeroo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CombatEventS2CPacket;
import net.minecraft.network.packet.s2c.play.ContainerSlotUpdateS2CPacket;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import fi.dy.masa.tweakeroo.util.MiscUtils;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{

    @Shadow private final MinecraftClient client;
    @Unique private static Hand totemOfUndyingRestockHand;

    protected MixinClientPlayNetworkHandler(MinecraftClient client) {
        this.client = client;
    }

    @Inject(method = "onContainerSlotUpdate", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/container/Container;setStackInSlot(ILnet/minecraft/item/ItemStack;)V"),
            cancellable = true)
    private void onHandleSetSlot(ContainerSlotUpdateS2CPacket packet, CallbackInfo ci)
    {
        if (PlacementTweaks.shouldSkipSlotSync(packet.getSlot(), packet.getItemStack()))
        {
            ci.cancel();
        }
    }


    @Inject(method = "onContainerSlotUpdate", at = @At("RETURN"))
    private void afterHandleSetSlot(ContainerSlotUpdateS2CPacket packet, CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_HAND_RESTOCK.getBooleanValue() && totemOfUndyingRestockHand != null)
        {
            if (this.client.player == null)
            {
                totemOfUndyingRestockHand = null;
            }
            else if (this.client.player.getStackInHand(totemOfUndyingRestockHand).isEmpty())
            {
                PlacementTweaks.tryRestockHand(this.client.player, totemOfUndyingRestockHand, Items.TOTEM_OF_UNDYING.getStackForRender());
                totemOfUndyingRestockHand = null;
            }
        }
    }


    @Inject(method = "onCombatEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    private void onPlayerDeath(CombatEventS2CPacket packetIn, CallbackInfo ci)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (FeatureToggle.TWEAK_PRINT_DEATH_COORDINATES.getBooleanValue() && mc.player != null)
        {
            MiscUtils.printDeathCoordinates(mc);
        }
    }


    @Inject(
            method = "getActiveTotemOfUndying",
            at = @At(value = "RETURN", ordinal = 0)
    )
    private static void onPlayerUseTotemOfUndying(PlayerEntity player, CallbackInfoReturnable<ItemStack> cir, @Local Hand hand)
    {
        if (FeatureToggle.TWEAK_HAND_RESTOCK.getBooleanValue())
        {
            totemOfUndyingRestockHand = hand;
            PlacementTweaks.cacheStackInHand(hand);
        }
    }
}
