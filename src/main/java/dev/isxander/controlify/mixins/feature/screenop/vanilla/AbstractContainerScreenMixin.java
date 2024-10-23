package dev.isxander.controlify.mixins.feature.screenop.vanilla;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.screenop.compat.vanilla.AbstractContainerScreenProcessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

//? if >=1.21.2 {
import net.minecraft.client.gui.ItemSlotMouseAction;
import dev.isxander.controlify.screenop.compat.vanilla.ItemSlotControllerAction;
//?}

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ScreenProcessorProvider {
    @Shadow @Nullable protected Slot hoveredSlot;

    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ClickType actionType);

    //? if >=1.21.2
    @Shadow @Final private List<ItemSlotMouseAction> itemSlotMouseActions;

    @Unique
    protected AbstractContainerScreenProcessor<?> screenProcessor = new AbstractContainerScreenProcessor<>(
            (AbstractContainerScreen<?>) (Object) this,
            () -> hoveredSlot,
            this::slotClicked,
            this::handleControllerItemSlotActions
    );

    @Override
    public ScreenProcessor<?> screenProcessor() {
        return screenProcessor;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void setPrevSlotShare(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci, @Share("prevSlot") LocalRef<Slot> prevSlot) {
        prevSlot.set(this.hoveredSlot);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void triggerSlotHovered(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci, @Share("prevSlot") LocalRef<Slot> prevSlot) {
        @Nullable Slot oldSlot = prevSlot.get();
        @Nullable Slot newSlot = this.hoveredSlot;

        if (oldSlot != null || newSlot != null) {
            if (oldSlot == null || (newSlot != null && newSlot.index != oldSlot.index)) {
                screenProcessor.onHoveredSlotChanged(newSlot, oldSlot);
            }
        }
    }

    @Unique
    protected boolean handleControllerItemSlotActions(ControllerEntity controller) {
        //? if >=1.21.2 {
        for (ItemSlotMouseAction itemSlotMouseAction : this.itemSlotMouseActions) {
            if (itemSlotMouseAction instanceof ItemSlotControllerAction itemSlotControllerAction
                    && itemSlotMouseAction.matches(this.hoveredSlot)
                    && itemSlotControllerAction.controlify$onControllerInput(this.hoveredSlot.getItem(), this.hoveredSlot.index, controller)) {
                return true;
            }
        }
        //?}
        return false;
    }

    //? if >=1.21.2 {
    @ModifyExpressionValue(
            method = "mouseScrolled",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;hasItem()Z"
            )
    )
    private boolean allowItemSlotScrolling(boolean original) {
        return original && !Controlify.instance().currentInputMode().isController();
    }
    //?}
}
