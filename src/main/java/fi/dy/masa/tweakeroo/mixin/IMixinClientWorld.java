package fi.dy.masa.tweakeroo.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface IMixinClientWorld
{
     @Accessor("mapStates")
     Map<String, MapState> tweakeroo_getMapStates();
}
