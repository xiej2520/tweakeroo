package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.renderer.RenderUtils;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud extends DrawableHelper
{
    @Shadow @Final private PlayerListHud playerListHud;
    @Shadow @Final private MinecraftClient client;
    @Shadow private int scaledWidth;

    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void overridePlayerForRendering(CallbackInfoReturnable<PlayerEntity> cir)
    {
        // Fix the hotbar rendering in the Free Camera mode by using the actual player
        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue() && this.client.player != null)
        {
            cir.setReturnValue(this.client.player);
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_overrideHotbarRendering(float tickDelta, CallbackInfo ci)
    {
        // This turns off rendering of the hotbar
        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue() &&
            !Configs.Generic.FREE_CAMERA_SHOW_HOTBAR.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void tweakeroo_overrideHeldItemTooltipRendering(CallbackInfo ci)
    {
        // This turns off rendering of the item "tooltips" when selecting hotbar items
        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue() &&
            !Configs.Generic.FREE_CAMERA_SHOW_HOTBAR.getBooleanValue() &&
            !Configs.Generic.FREE_CAMERA_SHOW_HANDS.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = { "renderStatusBars", "renderMountHealth", "renderMountJumpBar", "renderExperienceBar" }, at = @At("HEAD"), cancellable = true)
    private void tweakeroo_overrideStatusBarRendering(CallbackInfo ci)
    {
        // This turns off all status bars
        if (FeatureToggle.TWEAK_FREE_CAMERA.getBooleanValue() &&
            !Configs.Generic.FREE_CAMERA_SHOW_STATUS_BARS.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At(value = "FIELD",
                target = "Lnet/minecraft/client/options/GameOptions;debugEnabled:Z", ordinal = 0), cancellable = true)
    private void overrideCursorRender(CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_F3_CURSOR.getBooleanValue())
        {
            RenderUtils.renderDirectionsCursor(this.getBlitOffset(), this.client.getTickDelta());
            ci.cancel();
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/hud/PlayerListHud;tick(Z)V",
                     ordinal = 1, shift = At.Shift.AFTER))
    private void alwaysRenderPlayerList(float partialTicks, CallbackInfo ci)
    {
        if (FeatureToggle.TWEAK_PLAYER_LIST_ALWAYS_ON.getBooleanValue())
        {
            Scoreboard scoreboard = this.client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(0);

            this.playerListHud.tick(true);
            this.playerListHud.render(this.scaledWidth, scoreboard, objective);
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void disableScoreboardRendering(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_SCOREBOARD_RENDERING.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void disableStatusEffectHudRendering(CallbackInfo ci)
    {
        if (Configs.Disable.DISABLE_STATUS_EFFECT_HUD.getBooleanValue())
        {
            ci.cancel();
        }
    }
}
