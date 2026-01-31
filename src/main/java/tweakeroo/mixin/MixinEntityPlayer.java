package tweakeroo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tweakeroo.config.FeatureToggle;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    @WrapOperation(method = "onUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z")
    )
    private boolean doNoClip(EntityPlayer player, Operation<Boolean> original)
    {
        if (FeatureToggle.TWEAK_CREATIVE_NO_CLIP.getBooleanValue() && player.isCreative() && player.capabilities.isFlying)
        {
            return true;
        }
        else
        {
            return original.call(player);
        }
    }

    @WrapOperation(method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z")
    )
    private boolean collidesWithEntities(EntityPlayer player, Operation<Boolean> original)
    {
        if (FeatureToggle.TWEAK_CREATIVE_NO_CLIP.getBooleanValue() && player.isCreative() && player.capabilities.isFlying)
        {
            return true;
        }
        else
        {
            return original.call(player);
        }
    }
}
