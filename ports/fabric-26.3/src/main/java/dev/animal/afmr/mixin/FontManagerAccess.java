package dev.animal.afmr.mixin;

import java.util.Map;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontManager.class)
public interface FontManagerAccess {
    @Accessor("fontSets") Map<Identifier,FontSet> afmr$fontSets();
}
