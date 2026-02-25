package fi.dy.masa.tweakeroo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import fi.dy.masa.tweakeroo.config.Configs;

@Mixin(InGameOverlayRenderer.class)
public abstract class MixinInGameOverlayRenderer
{
    @WrapOperation(method = "renderOverlays",
                   at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnFire()Z"))
    private static boolean tweakeroo_disableFireOverlay(ClientPlayerEntity instance, Operation<Boolean> original)
    {
        if (Configs.Disable.DISABLE_FIRE_OVERLAY.getBooleanValue())
        {
            return false;
        }

        return original.call(instance);
    }
}
