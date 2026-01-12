package tweakeroo.mixin.carefulbreak;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tweakeroo.util.data.CarefulBreakPlayer;

import java.util.Optional;

@Mixin(PlayerInteractionManager.class)
public abstract class MixinPlayerInteractionManager {

    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "tryHarvestBlock", at = @At("HEAD"))
    private void setPlayerMiningBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        CarefulBreakPlayer.playerMiningBlock = Optional.of(this.player);
    }

    @Inject(method = "tryHarvestBlock", at = @At("RETURN"))
    private void unsetPlayerMiningBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        CarefulBreakPlayer.playerMiningBlock = Optional.empty();
    }

}
