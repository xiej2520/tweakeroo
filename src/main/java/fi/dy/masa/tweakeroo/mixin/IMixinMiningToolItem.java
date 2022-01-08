package fi.dy.masa.tweakeroo.mixin;

import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MiningToolItem.class)
public interface IMixinMiningToolItem {
    @Accessor("attackDamage")
    @Final
    float tweakeroo_getAttackDamage();
}
