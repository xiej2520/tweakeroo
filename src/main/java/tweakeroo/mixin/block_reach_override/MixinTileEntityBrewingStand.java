package tweakeroo.mixin.block_reach_override;

import net.minecraft.tileentity.TileEntityBrewingStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import tweakeroo.config.Configs;
import tweakeroo.config.FeatureToggle;

@Mixin(TileEntityBrewingStand.class)
public class MixinTileEntityBrewingStand {

    @ModifyConstant(method = "isUsableByPlayer",
        constant = @Constant(doubleValue = 64.0)
    )
    private double overrideIsUsableByPlayerDistance(double original)
    {
        if (FeatureToggle.TWEAK_BLOCK_REACH_OVERRIDE.getBooleanValue())
        {
            return (Configs.Generic.BLOCK_REACH_DISTANCE.getDoubleValue() + 3) * (Configs.Generic.BLOCK_REACH_DISTANCE.getDoubleValue() + 3);
        }

        return original;
    }
}
