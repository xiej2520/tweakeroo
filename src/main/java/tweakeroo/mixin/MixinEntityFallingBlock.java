package tweakeroo.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tweakeroo.config.FeatureToggle;

@Mixin(EntityFallingBlock.class)
public abstract class MixinEntityFallingBlock extends Entity
{
    public MixinEntityFallingBlock(World world) {
        super(world);
    }

    // Falling blocks are created as 0.98 x 0.98, but the size isn't saved. After its chunk is
    // reloaded, the server entity constructs the default Entity size 0.6 x 1.8 x 0.6. The client
    // always shows them as 0.98. For an integrated server, copy the real size.
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void syncServerSize(CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_FALLING_BLOCK_TRUE_SIZE.getBooleanValue() && this.world.isRemote)
        {
            IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
            WorldServer world = server != null ? server.getWorld(this.dimension) : null;
            Entity entity = world != null ? world.getEntityByID(this.getEntityId()) : null;

            if (entity instanceof EntityFallingBlock && (entity.width != this.width || entity.height != this.height))
            {
                this.setSize(entity.width, entity.height);
                this.setPosition(this.posX, this.posY, this.posZ);
            }
        }
    }

}
