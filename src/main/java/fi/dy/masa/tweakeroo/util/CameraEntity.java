package fi.dy.masa.tweakeroo.util;

import javax.annotation.Nullable;

import fi.dy.masa.tweakeroo.mixin.IMixinGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.recipe.book.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;

public class CameraEntity extends ClientPlayerEntity
{
    @Nullable private static Entity originalCameraEntity;
    @Nullable private static CameraEntity camera;
    private static boolean cullChunksOriginal;
    private static Vec3d cameraMotion = Vec3d.ZERO;
    private static boolean sprinting;
    private static boolean originalCameraWasPlayer;

    public CameraEntity(MinecraftClient mc, ClientWorld world,
                        ClientPlayNetworkHandler nethandler, StatHandler stats,
                        ClientRecipeBook recipeBook)
    {
        super(mc, world, nethandler, stats, recipeBook);
    }

    @Override
    public boolean isSpectator()
    {
        return true;
    }

    public static void movementTick(boolean sneak, boolean jump)
    {
        CameraEntity camera = getCamera();

        if (camera != null && Configs.Generic.FREE_CAMERA_PLAYER_MOVEMENT.getBooleanValue() == false)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            GameOptions options = mc.options;

            camera.updateLastTickPosition();

            if (options.keySprint.isPressed())
            {
                sprinting = true;
            }
            else if (options.keyForward.isPressed() == false && options.keyBack.isPressed() == false)
            {
                sprinting = false;
            }

            cameraMotion = MiscUtils.calculatePlayerMotionWithDeceleration(cameraMotion, 0.15, 0.4, sprinting);
            camera.handleMotion(cameraMotion.x, cameraMotion.y, cameraMotion.z);
        }
    }

    private static double getMoveSpeed()
    {
        double base = 0.07;

        if (FeatureToggle.TWEAK_FLY_SPEED.getBooleanValue())
        {
            base = Configs.getActiveFlySpeedConfig().getDoubleValue();
        }

        return base * 10;
    }

    private void handleMotion(double forward, double up, double strafe)
    {
        float yaw = this.yaw;
        double scale = getMoveSpeed();
        double xFactor = Math.sin(yaw * Math.PI / 180.0);
        double zFactor = Math.cos(yaw * Math.PI / 180.0);

        double x = (strafe * zFactor - forward * xFactor) * scale;
        double y = up * scale;
        double z = (forward * zFactor + strafe * xFactor) * scale;

        this.setVelocity(new Vec3d(x, y, z));
        this.move(MovementType.SELF, this.getVelocity());

        this.chunkX = (int) Math.floor(this.getX()) >> 4;
        this.chunkY = (int) Math.floor(this.getY()) >> 4;
        this.chunkZ = (int) Math.floor(this.getZ()) >> 4;
    }

    private void updateLastTickPosition()
    {
        this.lastRenderX = this.getX();
        this.lastRenderY = this.getY();
        this.lastRenderZ = this.getZ();

        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();

        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;

        this.prevHeadYaw = this.headYaw;
    }

    public void setCameraRotations(float yaw, float pitch)
    {
        this.yaw = yaw;
        this.pitch = pitch;

        this.headYaw = this.yaw;

        //this.prevRotationYaw = this.rotationYaw;
        //this.prevRotationPitch = this.rotationPitch;

        //this.prevRotationYawHead = this.rotationYaw;
        //this.setRenderYawOffset(this.rotationYaw);
    }

    public void updateCameraRotations(float yawChange, float pitchChange)
    {
        this.yaw += yawChange * 0.15F;
        this.pitch = MathHelper.clamp(this.pitch + pitchChange * 0.15F, -90F, 90F);

        this.setCameraRotations(this.yaw, this.pitch);
    }

    private static CameraEntity createCameraEntity(MinecraftClient mc)
    {
        ClientPlayerEntity player = mc.player;
        CameraEntity camera = new CameraEntity(mc, mc.world, player.networkHandler, player.getStatHandler(), player.getRecipeBook());
        camera.noClip = true;

        camera.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.yaw, player.pitch);
        camera.setRotation(player.yaw, player.pitch);

        return camera;
    }

    @Nullable
    public static CameraEntity getCamera()
    {
        return camera;
    }

    public static void setCameraState(boolean enabled)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world != null && mc.player != null)
        {
            if (enabled)
            {
                createAndSetCamera(mc);
            }
            else
            {
                removeCamera(mc);
            }

            ((IMixinGameRenderer) mc.gameRenderer).setRenderHand(! enabled);
        }
    }

    public static boolean originalCameraWasPlayer()
    {
        return originalCameraWasPlayer;
    }

    private static void createAndSetCamera(MinecraftClient mc)
    {
        camera = createCameraEntity(mc);
        originalCameraEntity = mc.getCameraEntity();
        originalCameraWasPlayer = originalCameraEntity == mc.player;
        cullChunksOriginal = mc.chunkCullingEnabled;

        mc.setCameraEntity(camera);
        mc.chunkCullingEnabled = false; // Disable chunk culling

        // Disable the motion option when entering camera mode
        Configs.Generic.FREE_CAMERA_PLAYER_MOVEMENT.setBooleanValue(false);
    }

    private static void removeCamera(MinecraftClient mc)
    {
        // Re-fetch the player entity, in case the player died while in Free Camera mode and the instance changed
        mc.setCameraEntity(originalCameraWasPlayer ? mc.player : originalCameraEntity);
        mc.chunkCullingEnabled = cullChunksOriginal;
        originalCameraEntity = null;

        if (mc.world != null && camera != null)
        {
            CameraUtils.markChunksForRebuildOnDeactivation(camera.chunkX, camera.chunkZ);
        }

        camera = null;
    }
}
