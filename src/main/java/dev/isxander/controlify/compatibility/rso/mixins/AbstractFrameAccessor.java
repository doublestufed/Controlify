//? if reeses-sodium-options {
/*package dev.isxander.controlify.compatibility.rso.mixins;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

import /^$ sodium-package >>^/ me.jellysquid.mods.sodium .client.gui.options.control.ControlElement;

@Mixin(value = AbstractFrame.class, remap = false)
public interface AbstractFrameAccessor {
    @Accessor
    List<ControlElement<?>> getControlElements();
}
*///?}
