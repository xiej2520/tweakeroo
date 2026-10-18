package tweakeroo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderFallingBlock;
import net.minecraft.entity.item.EntityFallingBlock;

import tweakeroo.config.FeatureToggle;

@Mixin(RenderFallingBlock.class)
public abstract class MixinRenderFallingBlock
{
    // Scale falling block model to the hitbox when it isn't the original 0.98 x 0.98 size.
    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityFallingBlock;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    private void scaleToTrueSize(EntityFallingBlock entity, double x, double y, double z,
                                 float entityYaw, float partialTicks, CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_FALLING_BLOCK_TRUE_SIZE.getBooleanValue()
            && (entity.width != 0.98F || entity.height != 0.98F))
        {
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(entity.width, entity.height, entity.width);
            GlStateManager.translate(-x, -y, -z);
        }
    }
}
