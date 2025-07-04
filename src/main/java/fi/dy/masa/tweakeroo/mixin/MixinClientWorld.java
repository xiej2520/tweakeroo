package fi.dy.masa.tweakeroo.mixin;

import java.util.function.BiFunction;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;

import fi.dy.masa.tweakeroo.config.Configs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World implements IMixinClientWorld
{
    @Shadow public abstract Vec3d method_23777(BlockPos blockPos, float f);

    protected MixinClientWorld(LevelProperties settings, DimensionType dimType, BiFunction<World, Dimension, ChunkManager> func, Profiler profiler)
    {
        super(settings, dimType, func, profiler, true);
    }

    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void disableClientEntityTicking(Entity entity, CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_CLIENT_ENTITY_UPDATES.getBooleanValue() &&
            (entity instanceof PlayerEntity) == false)
        {
            ci.cancel();
        }
    }

    @Inject(method = "addEntitiesToChunk", at = @At("HEAD"), cancellable = true)
    private void fixChunkEntityLeak(WorldChunk chunk, CallbackInfo ci)
    {
        if (Configs.Fixes.CLIENT_CHUNK_ENTITY_DUPE.getBooleanValue())
        {
            for (int y = 0; y < 16; ++y)
            {
                // The chunk already has entities, which means it's a re-used existing chunk,
                // in such a case we don't want to add the from the world entities again, otherwise
                // they are basically duped within the Chunk.
                if (chunk.getEntitySectionArray()[y].size() > 0)
                {
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method = "checkBlockRerender", at = @At("HEAD"), cancellable = true)
    private void disableChunkReRenders(BlockPos pos, BlockState old, BlockState updated, CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_CHUNK_RENDERING.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "scheduleBlockRenders", at = @At("HEAD"), cancellable = true)
    private void disableChunkReRenders(int x, int y, int z, CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_CHUNK_RENDERING.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "updateListeners", at = @At("HEAD"), cancellable = true)
    private void disableChunkReRenders(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_CHUNK_RENDERING.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "getSkyDarknessHeight", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_overrideSkyDarknessHeight(CallbackInfoReturnable<Double> cir)
    {
        // Disable the dark sky effect in normal situations
        // by moving the y threshold below the bottom of the world
        if (Configs.Disable.DISABLE_SKY_DARKNESS.getBooleanValue())
        {
            cir.setReturnValue(-2.0);
        }
    }

    @Inject(method = "getFogColor", at = @At("HEAD"), cancellable = true)
    private void adjustFogColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir)
    {
        if (FeatureToggle.TWEAK_MATCHING_SKY_FOG.getBooleanValue())
        {
            if (this.dimension.hasSkyLight() && this.isRaining() == false)
            {
                cir.setReturnValue(this.method_23777(EntityUtils.getCameraEntity().getBlockPos(), tickDelta));
            }
        }
    }
}
