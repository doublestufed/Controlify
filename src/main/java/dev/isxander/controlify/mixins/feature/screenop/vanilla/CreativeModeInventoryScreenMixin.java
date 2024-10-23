package dev.isxander.controlify.mixins.feature.screenop.vanilla;

import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.screenop.compat.vanilla.CreativeModeInventoryScreenProcessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreenMixin implements ScreenProcessorProvider {
    @Unique protected CreativeModeInventoryScreenProcessor screenProcessor = new CreativeModeInventoryScreenProcessor(
            (CreativeModeInventoryScreen) (Object) this,
            () -> hoveredSlot,
            this::slotClicked,
            this::handleControllerItemSlotActions
    );

    @Override
    public ScreenProcessor<?> screenProcessor() {
        return screenProcessor;
    }
}
