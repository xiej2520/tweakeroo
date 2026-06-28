package tweakeroo.mixin;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tweakeroo.tweaks.ShulkerBoxItemContentHintRenderer;

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    @Inject(
        method = "renderItemAndEffectIntoGUI(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderItem;renderItemModelIntoGUI(Lnet/minecraft/item/ItemStack;IILnet/minecraft/client/renderer/block/model/IBakedModel;)V",
            shift = At.Shift.AFTER
        )
    )
    private void shulkerItemContentHint_impl(EntityLivingBase entityLivingBase, ItemStack itemStack, int i, int j, CallbackInfo ci)
    {
        ShulkerBoxItemContentHintRenderer.render(
            (RenderItem) (Object)this, itemStack, i, j
        );
    }
}
