package tweakeroo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import malilib.util.MathUtils;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tweakeroo.config.Configs;
import tweakeroo.config.FeatureToggle;
import tweakeroo.config.Hotkeys;
import tweakeroo.util.CameraUtils;
import tweakeroo.util.MiscUtils;
import tweakeroo.util.SnapAimMode;

@Mixin(Entity.class)
public abstract class MixinEntity
{
    @Shadow public World world;

    @Shadow public float rotationPitch;
    @Shadow public float rotationYaw;
    @Shadow public float prevRotationYaw;
    @Shadow public float prevRotationPitch;
    @Shadow public double motionX;
    @Shadow public double motionY;
    @Shadow public double motionZ;

    @Shadow private static double renderDistanceWeight;
    private double forcedPitch;
    private double forcedYaw;

    @Redirect(method = "move",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z", ordinal = 0)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z", ordinal = 0))
    private boolean fakeSneaking(Entity entity)
    {
        if (FeatureToggle.TWEAK_FAKE_SNEAKING.getBooleanValue() && ((Object) this) instanceof EntityPlayerSP)
        {
            return true;
        }

        return ((Entity) (Object) this).isSneaking();
    }

    @Inject(method = "moveRelative",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/math/MathHelper;sin(F)F"), cancellable = true)
    private void moreAccurateMoveRelative(float strafe, float up, float forward, float friction, CallbackInfo ci)
    {
        if ((Object) this instanceof EntityPlayerSP)
        {
            if (CameraUtils.shouldPreventPlayerMovement())
            {
                ci.cancel();
            }
            else if (FeatureToggle.TWEAK_SNAP_AIM.getBooleanValue())
            {
                double xFactor = Math.sin(this.rotationYaw * Math.PI / 180D);
                double zFactor = Math.cos(this.rotationYaw * Math.PI / 180D);

                this.motionX += (double) (strafe * zFactor - forward * xFactor);
                this.motionY += (double) up;
                this.motionZ += (double) (forward * zFactor + strafe * xFactor);

                ci.cancel();
            }
        }
    }

    @Inject(method = "turn",
            at = @At(value = "FIELD",
                     target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F", ordinal = 0))
    private void overrideYaw(float yawChange, float pitchChange, CallbackInfo ci)
    {
        if ((Object) this instanceof EntityPlayerSP)
        {
            if (CameraUtils.shouldPreventPlayerMovement())
            {
                this.rotationYaw = this.prevRotationYaw;
                this.rotationPitch = this.prevRotationPitch;

                CameraUtils.updateCameraRotations(yawChange, pitchChange);

                return;
            }

            if (FeatureToggle.TWEAK_AIM_LOCK.getBooleanValue())
            {
                this.rotationYaw = (float) this.forcedYaw;
                this.rotationPitch = (float) this.forcedPitch;
                return;
            }

            if (FeatureToggle.TWEAK_SNAP_AIM.getBooleanValue())
            {
                int pitchLimit = Configs.Generic.SNAP_AIM_PITCH_OVERSHOOT.getBooleanValue() ? 180 : 90;
                SnapAimMode mode = Configs.Generic.SNAP_AIM_MODE.getValue();
                boolean snapAimLock = FeatureToggle.TWEAK_SNAP_AIM_LOCK.getBooleanValue();

                // Not locked, or not snapping the yaw (ie. not in Yaw or Both modes)
                boolean updateYaw = snapAimLock == false || mode == SnapAimMode.PITCH;
                // Not locked, or not snapping the pitch (ie. not in Pitch or Both modes)
                boolean updatePitch = snapAimLock == false || mode == SnapAimMode.YAW;

                this.updateCustomPlayerRotations(yawChange, pitchChange, updateYaw, updatePitch, pitchLimit);

                this.rotationYaw = MiscUtils.getSnappedYaw(this.forcedYaw);
                this.rotationPitch = MiscUtils.getSnappedPitch(this.forcedPitch);
                return;
            }

            if (FeatureToggle.TWEAK_ELYTRA_CAMERA.getBooleanValue() && Hotkeys.ELYTRA_CAMERA.getKeyBind().isKeyBindHeld())
            {
                int pitchLimit = Configs.Generic.SNAP_AIM_PITCH_OVERSHOOT.getBooleanValue() ? 180 : 90;

                this.updateCustomPlayerRotations(yawChange, pitchChange, true, true, pitchLimit);

                CameraUtils.setCameraYaw((float) this.forcedYaw);
                CameraUtils.setCameraPitch((float) this.forcedPitch);

                this.rotationYaw = this.prevRotationYaw;
                this.rotationPitch = this.prevRotationPitch;

                return;
            }

            // Update the internal rotations while no locking features are enabled
            // They will then be used as the forced rotations when some of the locking features are activated.
            this.forcedYaw = this.rotationYaw;
            this.forcedPitch = this.rotationPitch;
        }
    }

    private void updateCustomPlayerRotations(float yawChange, float pitchChange, boolean updateYaw, boolean updatePitch, float pitchLimit)
    {
        if (updateYaw)
        {
            this.forcedYaw += (double) yawChange * 0.15D;
        }

        if (updatePitch)
        {
            this.forcedPitch = MathUtils.clamp(this.forcedPitch - (double) pitchChange * 0.15D, -pitchLimit, pitchLimit);
        }
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onCheckGlowing(CallbackInfoReturnable<Boolean> cir)
    {
        if (FeatureToggle.TWEAK_OUTLINE_ENTITIES.getBooleanValue())
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isInRangeToRenderDist", at = @At("RETURN"), cancellable = true)
    private void overrideEntityRenderDistance(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) double distance)
    {
        if (FeatureToggle.TWEAK_ENTITY_RENDER_DISTANCE.getBooleanValue())
        {
            double d = ((Entity) (Object) this).getEntityBoundingBox().getAverageEdgeLength();
            if (Double.isNaN(d)) {
                d = 1.0;
            }

            d *= 64.0 * renderDistanceWeight * Configs.Generic.ENTITY_RENDER_DISTANCE.getDoubleValue();
            cir.setReturnValue(distance < d * d);
        }
    }
}
