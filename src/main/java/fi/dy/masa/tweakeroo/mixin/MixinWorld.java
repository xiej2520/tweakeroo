package fi.dy.masa.tweakeroo.mixin;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import fi.dy.masa.tweakeroo.config.Configs;

@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow @Final public List<BlockEntity> blockEntities;

    @Shadow @Final public List<BlockEntity> tickingBlockEntities;

    @Shadow @Final protected List<BlockEntity> unloadedBlockEntities;

    @Inject(method = "tickEntity(Ljava/util/function/Consumer;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void preventEntityTicking(Consumer<Entity> consumer, Entity entityIn, CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_ENTITY_TICKING.getBooleanValue() && (entityIn instanceof PlayerEntity) == false)
        {
            ci.cancel();
        }
    }

    @Inject(method = "tickBlockEntities", at = @At("HEAD"), cancellable = true)
    private void disableBlockEntityTicking(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_TILE_ENTITY_TICKING.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "tickBlockEntities",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;unloadedBlockEntities:Ljava/util/List;", ordinal = 0))
    private void optimizedTileEntityRemoval(CallbackInfo ci)
    {
        if (Configs.Fixes.TILE_UNLOAD_OPTIMIZATION.getBooleanValue())
        {
            if (this.unloadedBlockEntities.isEmpty() == false)
            {
                HashSet<BlockEntity> remove = new HashSet<>();
                remove.addAll(this.unloadedBlockEntities);

                this.tickingBlockEntities.removeAll(remove);
                this.blockEntities.removeAll(remove);
                this.unloadedBlockEntities.clear();
            }
        }
    }
}
