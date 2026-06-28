package tweakeroo.mixin;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderItem.class)
public interface IMixinRenderItem {

    @Invoker("draw")
    void invokeDraw(BufferBuilder renderer, int x, int y, int width, int height, int red, int green, int blue, int alpha);
}
