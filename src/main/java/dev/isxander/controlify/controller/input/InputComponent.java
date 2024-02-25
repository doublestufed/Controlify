package dev.isxander.controlify.controller.input;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.*;
import dev.isxander.controlify.controller.input.mapping.UserGamepadMapping;
import dev.isxander.controlify.controller.impl.ConfigImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InputComponent implements ECSComponent, ConfigHolder<InputComponent.Config> {
    public static final ResourceLocation ID = Controlify.id("input");

    private ControllerState
            stateNow = ControllerState.EMPTY,
            stateThen = ControllerState.EMPTY;
    private DeadzoneControllerStateView deadzoneStateNow, deadzoneStateThen;

    private final int buttonCount, axisCount, hatCount;
    private final Set<ResourceLocation> deadzoneAxes;
    private final boolean definitelyGamepad;

    private final IConfig<Config> config;

    public InputComponent(int buttonCount, int axisCount, int hatCount, boolean definitelyGamepad, Set<ResourceLocation> deadzoneAxes) {
        this.buttonCount = buttonCount;
        this.axisCount = axisCount;
        this.hatCount = hatCount;
        this.config = new ConfigImpl<>(Config::new, Config.class);
        this.definitelyGamepad = definitelyGamepad;
        this.deadzoneAxes = deadzoneAxes;
        this.updateDeadzoneView();
    }

    public ControllerStateView stateNow() {
        return this.deadzoneStateNow;
    }
    public ControllerStateView stateThen() {
        return this.deadzoneStateThen;
    }
    
    public ControllerState rawStateNow() {
        return this.stateNow;
    }

    public ControllerState rawStateThen() {
        return this.stateThen;
    }

    public void pushState(ControllerState state) {
        if (confObj().mapping != null) {
            state = confObj().mapping.mapJoystick(state);
        }

        ControllerState then = this.stateNow;
        this.stateNow = state;
        this.stateThen = then;
        this.updateDeadzoneView();
    }

    public int buttonCount() {
        return this.buttonCount;
    }

    public int axisCount() {
        return this.axisCount;
    }
    public int hatCount() {
        return this.hatCount;
    }

    public boolean isDefinitelyGamepad() {
        return this.definitelyGamepad;
    }

    public Set<ResourceLocation> getDeadzoneAxes() {
        if (!confObj().deadzoneOverrides.isEmpty()) {
            return confObj().deadzoneOverrides;
        } else {
            return this.deadzoneAxes;
        }
    }

    @Override
    public IConfig<Config> config() {
        return this.config;
    }

    private void updateDeadzoneView() {
        this.deadzoneStateNow = new DeadzoneControllerStateView(this.stateNow, this.config);
        this.deadzoneStateThen = new DeadzoneControllerStateView(this.stateThen, this.config);
    }

    public static class Config implements ConfigClass {
        public float hLookSensitivity = 1f;
        public float vLookSensitivity = 0.9f;
        public float virtualMouseSensitivity = 1f;
        public boolean reduceAimingSensitivity = true;

        public float buttonActivationThreshold = 0.5f;

        public Map<ResourceLocation, Float> deadzones = new Object2ObjectOpenHashMap<>();
        public boolean deadzonesCalibrated = false;
        public boolean delayedCalibration = false;

        public boolean mixedInput = false;

        @Nullable
        public UserGamepadMapping mapping = null;

        public Set<ResourceLocation> deadzoneOverrides = new HashSet<>();
    }
}