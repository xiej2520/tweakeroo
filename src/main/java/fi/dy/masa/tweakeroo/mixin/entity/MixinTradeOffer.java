package fi.dy.masa.tweakeroo.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.village.TradeOffer;
import fi.dy.masa.tweakeroo.config.Configs;

@Mixin(TradeOffer.class)
public abstract class MixinTradeOffer
{
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventTradeLocking(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_VILLAGER_TRADE_LOCKING.getBooleanValue())
        {
            // Prevents the trade from getting locked, by not incrementing uses
            ci.cancel();
        }
    }
}
