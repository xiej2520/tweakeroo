package tweakeroo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tweakeroo.config.FeatureToggle;

@Mixin(ItemBlock.class)
public abstract class MixinItemBlockNoClip {
    @WrapOperation(method = "canPlaceBlockOnSide", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/world/World;mayPlace(Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/util/EnumFacing;Lnet/minecraft/entity/Entity;)Z"
    ))
    private boolean canNoClipPlace(World worldIn, Block blockIn, BlockPos pos, boolean skipCollisionCheck, EnumFacing side, Entity placer,
                                   Operation<Boolean> original, @Local(argsOnly = true) EntityPlayer player) {
        if (FeatureToggle.TWEAK_CREATIVE_NO_CLIP.getBooleanValue() && player.isCreative() && player.capabilities.isFlying)
        {
            return worldIn.mayPlace(blockIn, pos, true, side, null);
        }
        else
        {
            return original.call(worldIn, blockIn, pos, skipCollisionCheck, side, placer);
        }
    }
}
