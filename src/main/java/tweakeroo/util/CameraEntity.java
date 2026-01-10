package tweakeroo.util;

import javax.annotation.Nullable;

import malilib.util.position.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.World;

import malilib.util.MathUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import tweakeroo.config.Configs;
import tweakeroo.config.FeatureToggle;

public class CameraEntity extends EntityPlayerSP
{
    public CameraEntity(Minecraft mc, World world, NetHandlerPlayClient nethandler,
            StatisticsManager stats, RecipeBook recipeBook)
    {
        super(mc, world, nethandler, stats, recipeBook);
    }

    @Nullable private static Entity originalRenderViewEntity;
    @Nullable private static CameraEntity camera;
    private static boolean cullChunksOriginal;
    private static boolean sprinting;
    private static net.minecraft.util.math.Vec3d cameraMotion = Vec3d.ZERO.toVanilla();


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
            camera.updateLastTickPosition();
            GameSettings options = GameWrap.getClient().gameSettings;
            if (options.keyBindSprint.isKeyDown())
            {
                sprinting = true;
            }
            else if (options.keyBindForward.isKeyDown() == false && options.keyBindBack.isKeyDown() == false)
            {
                sprinting = false;
            }

            cameraMotion = MiscUtils.calculatePlayerMotionWithDeceleration(cameraMotion, 0.15, 0.4, sprinting);
            camera.handleMotion((float) cameraMotion.x, cameraMotion.y, (float) cameraMotion.z);
        }
    }

    private static double getMoveSpeed()
    {
        double base = 0.07;

        if (FeatureToggle.TWEAK_FLY_SPEED.getBooleanValue())
        {
            base = Configs.Internal.ACTIVE_FLY_SPEED_OVERRIDE_VALUE.getDoubleValue();
        }

        return base * 10;
    }

    private void handleMotion(float forward, double up, float strafe)
    {
        float yaw = this.rotationYaw;
        double scale = getMoveSpeed();

        double xFactor = Math.sin(yaw * Math.PI / 180.0);
        double zFactor = Math.cos(yaw * Math.PI / 180.0);

        double x = (strafe * zFactor - forward * xFactor) * scale;
        double y = up * scale;
        double z = (forward * zFactor + strafe * xFactor) * scale;

        this.setVelocity(x, y, z);
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

        this.chunkCoordX = EntityWrap.getChunkX(this);
        this.chunkCoordY = EntityWrap.getChunkY(this);
        this.chunkCoordZ = EntityWrap.getChunkZ(this);
    }

    private void updateLastTickPosition()
    {
        this.prevPosX = this.lastTickPosX = EntityWrap.getX(this);
        this.prevPosY = this.lastTickPosY = EntityWrap.getY(this);
        this.prevPosZ = this.lastTickPosZ = EntityWrap.getZ(this);
    }

    public void setCameraRotations(float yaw, float pitch)
    {
        EntityWrap.setYaw(this, yaw);
        EntityWrap.setPitch(this, pitch);
        this.prevRotationYaw = yaw;
        this.prevRotationPitch = pitch;
        this.setRotationYawHead(yaw);
        this.setRenderYawOffset(yaw);
    }

    public void updateCameraRotations(float yawChange, float pitchChange)
    {
        float yaw = EntityWrap.getYaw(this) + yawChange * 0.15F;
        float pitch = MathUtils.clamp(EntityWrap.getPitch(this) - pitchChange * 0.15F, -90F, 90F);

        this.setCameraRotations(yaw, pitch);
    }

    private static CameraEntity createCameraEntity(Minecraft mc)
    {
        EntityPlayerSP player = GameWrap.getClientPlayer();
        CameraEntity camera = new CameraEntity(mc, mc.world, player.connection, player.getStatFileWriter(), player.getRecipeBook());

        camera.noClip = true;
        camera.setLocationAndAngles(EntityWrap.getX(player),
                                    EntityWrap.getY(player),
                                    EntityWrap.getZ(player),
                                    EntityWrap.getYaw(player), EntityWrap.getPitch(player));

        float yaw = EntityWrap.getYaw(camera);
        float pitch = EntityWrap.getPitch(camera);
        camera.prevRotationYaw = yaw;
        camera.prevRotationPitch = pitch;
        camera.setRotationYawHead(yaw);
        camera.setRenderYawOffset(yaw);

        return camera;
    }

    @Nullable
    public static CameraEntity getCamera()
    {
        return camera;
    }

    public static void setCameraState(boolean enabled)
    {
        Minecraft mc = GameWrap.getClient();

        if (GameWrap.getClientWorld() != null && GameWrap.getClientPlayer() != null)
        {
            if (enabled)
            {
                createAndSetCamera(mc);
            }
            else
            {
                removeCamera(mc);
            }
        }
    }

    private static void createAndSetCamera(Minecraft mc)
    {
        camera = createCameraEntity(mc);
        originalRenderViewEntity = mc.getRenderViewEntity();
        cullChunksOriginal = mc.renderChunksMany;

        mc.setRenderViewEntity(camera);
        mc.renderChunksMany = false; // Disable chunk culling

        // Disable the motion option when entering camera mode
        Configs.Generic.FREE_CAMERA_PLAYER_MOVEMENT.setValue(false);
    }

    private static void removeCamera(Minecraft mc)
    {
        if (GameWrap.getClientWorld() != null && camera != null)
        {
            mc.setRenderViewEntity(originalRenderViewEntity);
            mc.renderChunksMany = cullChunksOriginal;
            CameraUtils.markChunksForRebuildOnDeactivation(camera.chunkCoordX, camera.chunkCoordZ);
        }

        originalRenderViewEntity = null;
        camera = null;
    }
}
