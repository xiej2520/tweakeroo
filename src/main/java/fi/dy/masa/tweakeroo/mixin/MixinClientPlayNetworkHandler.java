package fi.dy.masa.tweakeroo.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{

    @Shadow private final MinecraftClient client;

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
        method = "onEntityStatus",
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getActiveTotemOfUndying(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;")
    )
    private void onPlayerUseTotemOfUndying(EntityStatusS2CPacket packet, CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_HAND_RESTOCK.getBooleanValue())
        {
            for (Hand hand : Hand.values())
            {
                if (this.client.player.getStackInHand(hand).getItem() == Items.TOTEM_OF_UNDYING)
                {
                    PlacementTweaks.cacheStackInHand(hand);
                    // the slot update packet goes after this packet, let's set it to empty and restock
                    this.client.player.setStackInHand(hand, ItemStack.EMPTY);
                    PlacementTweaks.onProcessRightClickPost(this.client.player, hand);
                }
            }
        }
    }
}
