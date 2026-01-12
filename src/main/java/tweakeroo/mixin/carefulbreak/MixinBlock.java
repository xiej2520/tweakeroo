package tweakeroo.mixin.carefulbreak;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tweakeroo.config.FeatureToggle;
import tweakeroo.util.data.CarefulBreakPlayer;

@Mixin(Block.class)
public abstract class MixinBlock {

    @Inject(method = "spawnAsEntity", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void carefulBreakPickup(World worldIn, BlockPos pos, ItemStack stack,
                                           CallbackInfo ci, @Local EntityItem entityItem)
    {
        if (FeatureToggle.TWEAK_CAREFUL_BREAK.getBooleanValue()
                && CarefulBreakPlayer.playerMiningBlock.map(Entity::isSneaking).orElse(false))
        {
            entityItem.setNoPickupDelay();
            entityItem.onCollideWithPlayer(CarefulBreakPlayer.playerMiningBlock.get());
        }
    }

}
