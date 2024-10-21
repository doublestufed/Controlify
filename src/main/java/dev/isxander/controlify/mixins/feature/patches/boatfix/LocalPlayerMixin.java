package dev.isxander.controlify.mixins.feature.patches.boatfix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.fixes.boatfix.AnalogBoatInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    //? if >=1.21.2 {
    @Shadow public net.minecraft.client.player.ClientInput input;
    //?} else {
    /*@Shadow public net.minecraft.client.player.Input input;
    *///?}

    @WrapOperation(
            method = "rideTick",
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21.2 {
                    target = "Lnet/minecraft/world/entity/vehicle/AbstractBoat;setInput(ZZZZ)V"
                    //?} else {
                    /*target = "Lnet/minecraft/world/entity/vehicle/Boat;setInput(ZZZZ)V"
                    *///?}
            )
    )
    private void useAnalogInput(
            //? if >=1.21.2 {
            net.minecraft.world.entity.vehicle.AbstractBoat boat,
            //?} else {
            /*Boat boat,
            *///?}
            boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack,
            Operation<Void> original
    ) {
        if (ControlifyApi.get().currentInputMode().isController() && !Controlify.instance().config().globalSettings().shouldUseKeyboardMovement()) {
            ((AnalogBoatInput) boat).controlify$setAnalogInput(
                    input.forwardImpulse,
                    -input.leftImpulse
            );

            return;
        }

        original.call(boat, pressingLeft, pressingRight, pressingForward, pressingBack);
    }
}
