package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.util.CreativeExtraItems;

@Mixin(net.minecraft.item.Item.class)
public abstract class MixinItem
{
    @Inject(method = "getGroup", at = @At("HEAD"), cancellable = true)
    private void overrideItemGroup(CallbackInfoReturnable<net.minecraft.item.ItemGroup> cir)
    {
        if (FeatureToggle.TWEAK_CREATIVE_EXTRA_ITEMS.getBooleanValue())
        {
            net.minecraft.item.ItemGroup group = CreativeExtraItems.getGroupFor((net.minecraft.item.Item) (Object) this);

            if (group != null)
            {
                cir.setReturnValue(group);
            }
        }
    }
}
