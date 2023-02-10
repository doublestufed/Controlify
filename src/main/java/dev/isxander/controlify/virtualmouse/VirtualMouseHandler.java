package dev.isxander.controlify.virtualmouse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.event.ControlifyEvents;
import dev.isxander.controlify.mixins.feature.virtualmouse.KeyboardHandlerAccessor;
import dev.isxander.controlify.mixins.feature.virtualmouse.MouseHandlerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.RoundingMode;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Set;

public class VirtualMouseHandler {
    private static final boolean DEBUG_SNAPPING = FabricLoader.getInstance().isDevelopmentEnvironment();
    private static final ResourceLocation CURSOR_TEXTURE = new ResourceLocation("controlify", "textures/gui/virtual_mouse.png");

    private double targetX, targetY;
    private double currentX, currentY;

    private double scrollX, scrollY;

    private final Minecraft minecraft;
    private boolean virtualMouseEnabled;

    private Set<SnapPoint> snapPoints;
    private SnapPoint snappedPoint, lastSnappedPoint;

    public VirtualMouseHandler() {
        this.minecraft = Minecraft.getInstance();

        if (minecraft.screen != null && minecraft.screen instanceof ISnapBehaviour snapBehaviour)
            snapPoints = snapBehaviour.getSnapPoints();
        else
            snapPoints = Set.of();

        ControlifyEvents.INPUT_MODE_CHANGED.register(this::onInputModeChanged);
    }

    public void handleControllerInput(Controller controller) {
        if (controller.bindings().VMOUSE_TOGGLE.justPressed()) {
            toggleVirtualMouse();
        }

        if (!virtualMouseEnabled) {
            return;
        }

        var leftStickX = controller.state().axes().leftStickX();
        var leftStickY = controller.state().axes().leftStickY();
        var prevLeftStickX = controller.prevState().axes().leftStickX();
        var prevLeftStickY = controller.prevState().axes().leftStickY();

        if (minecraft.screen != null && minecraft.screen instanceof ISnapBehaviour snapBehaviour) {
            snapPoints = snapBehaviour.getSnapPoints();
        } else {
            snapPoints = Set.of();
        }
        if (!snapPoints.contains(snappedPoint))
            snappedPoint = null;

        // if just released stick, snap to nearest snap point
        if (leftStickX == 0 && leftStickY == 0) {
            if ((prevLeftStickX != 0 || prevLeftStickY != 0))
                snapToClosestPoint();
        } else {
            snappedPoint = null;
        }

        // quadratic function to make small movements smaller
        // abs to keep sign
        targetX += leftStickX * Mth.abs(leftStickX) * 20f * controller.config().virtualMouseSensitivity;
        targetY += leftStickY * Mth.abs(leftStickY) * 20f * controller.config().virtualMouseSensitivity;

        targetX = Mth.clamp(targetX, 0, minecraft.getWindow().getWidth());
        targetY = Mth.clamp(targetY, 0, minecraft.getWindow().getHeight());

        scrollY += controller.bindings().VMOUSE_SCROLL_UP.state() - controller.bindings().VMOUSE_SCROLL_DOWN.state();

        var mouseHandler = (MouseHandlerAccessor) minecraft.mouseHandler;
        var keyboardHandler = (KeyboardHandlerAccessor) minecraft.keyboardHandler;

        if (controller.bindings().VMOUSE_LCLICK.justPressed()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_LCLICK.justReleased()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
        }

        if (controller.bindings().VMOUSE_RCLICK.justPressed()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_RCLICK.justReleased()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE, 0);
        }

        if (controller.bindings().VMOUSE_MCLICK.justPressed()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_MCLICK.justReleased()) {
            mouseHandler.invokeOnPress(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GLFW.GLFW_RELEASE, 0);
        }

        if (controller.bindings().VMOUSE_ESCAPE.justPressed()) {
            keyboardHandler.invokeKeyPress(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE, 0, GLFW.GLFW_PRESS, 0);
        } else if (controller.bindings().VMOUSE_ESCAPE.justReleased()) {
            keyboardHandler.invokeKeyPress(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE, 0, GLFW.GLFW_RELEASE, 0);
        }
    }

    public void updateMouse() {
        if (!virtualMouseEnabled) return;

        if (Math.round(targetX * 100) / 100.0 != Math.round(currentX * 100) / 100.0 || Math.round(targetY * 100) / 100.0 != Math.round(currentY * 100) / 100.0) {
            currentX = Mth.lerp(minecraft.getDeltaFrameTime(), currentX, targetX);
            currentY = Mth.lerp(minecraft.getDeltaFrameTime(), currentY, targetY);

            ((MouseHandlerAccessor) minecraft.mouseHandler).invokeOnMove(minecraft.getWindow().getWindow(), currentX, currentY);
        } else {
            currentX = targetX;
            currentY = targetY;
        }

        if (Math.abs(scrollX) >= 0.01 || Math.abs(scrollY) >= 0.01) {
            var currentScrollY = scrollY * Minecraft.getInstance().getDeltaFrameTime();
            scrollY -= currentScrollY;
            var currentScrollX = scrollX * Minecraft.getInstance().getDeltaFrameTime();
            scrollX -= currentScrollX;

            ((MouseHandlerAccessor) minecraft.mouseHandler).invokeOnScroll(minecraft.getWindow().getWindow(), currentScrollX, currentScrollY);
        } else {
            scrollX = scrollY = 0;
        }
    }

    private void snapToClosestPoint() {
        var window = minecraft.getWindow();
        var scaleFactor = new Vector2d((double)window.getGuiScaledWidth() / (double)window.getScreenWidth(), (double)window.getGuiScaledHeight() / (double)window.getScreenHeight());
        var target = new Vector2d(targetX, targetY).mul(scaleFactor);

        if (lastSnappedPoint != null) {
            if (lastSnappedPoint.position().distanceSquared(new Vector2i(target, RoundingMode.FLOOR)) > lastSnappedPoint.range() * lastSnappedPoint.range() * scaleFactor.x()) {
                lastSnappedPoint = null;
            }
        }

        var closestSnapPoint = snapPoints.stream()
                .filter(snapPoint -> snapPoint != lastSnappedPoint) // don't snap to the point currently over snapped point
                .map(snapPoint -> new Pair<>(snapPoint, snapPoint.position().distanceSquared(new Vector2i(target, RoundingMode.FLOOR)))) // map with distance to current pos
                .filter(point -> point.getSecond() <= point.getFirst().range() * point.getFirst().range() * scaleFactor.x()) // filter out of range options
                .min(Comparator.comparingLong(Pair::getSecond)) // find the closest point
                .orElse(new Pair<>(null, Long.MAX_VALUE)).getFirst(); // retrieve point

        if (closestSnapPoint != null) {
            snappedPoint = lastSnappedPoint = closestSnapPoint;

            targetX = snappedPoint.position().x() / scaleFactor.x();
            targetY = snappedPoint.position().y() / scaleFactor.y();
        }
    }

    public void onScreenChanged() {
        if (minecraft.screen != null) {
            if (requiresVirtualMouse()) {
                enableVirtualMouse();
            } else {
                disableVirtualMouse();
            }
            if (Controlify.instance().currentInputMode() == InputMode.CONTROLLER)
                GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        } else if (virtualMouseEnabled) {
            disableVirtualMouse();

            minecraft.mouseHandler.grabMouse(); // re-grab mouse after vmouse disable
        }
    }

    public void onInputModeChanged(InputMode mode) {
        if (mode == InputMode.CONTROLLER) {
            if (requiresVirtualMouse()) {
                enableVirtualMouse();
            }
        } else if (virtualMouseEnabled) {
            disableVirtualMouse();
        }
    }

    public void renderVirtualMouse(PoseStack matrices) {
        if (!virtualMouseEnabled) return;

        if (DEBUG_SNAPPING) {
            for (var snapPoint : snapPoints) {
                GuiComponent.fill(matrices, snapPoint.position().x() - 1, snapPoint.position().y() - 1, snapPoint.position().x() + 1, snapPoint.position().y() + 1, snapPoint == snappedPoint ? 0xFF00FF00 : snapPoint == lastSnappedPoint ? 0xFFFFFF00 : 0xFFFF0000);
            }
        }

        RenderSystem.setShaderTexture(0, CURSOR_TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();

        var scaledX = currentX * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
        var scaledY = currentY * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();

        matrices.pushPose();
        matrices.translate(scaledX, scaledY, 1000f);
        matrices.scale(0.5f, 0.5f, 0.5f);

        GuiComponent.blit(matrices, -16, -16, 0, 0, 32, 32, 32, 32);

        matrices.popPose();

        RenderSystem.disableBlend();
    }

    public void enableVirtualMouse() {
        if (virtualMouseEnabled) return;

        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        virtualMouseEnabled = true;

        if (minecraft.mouseHandler.xpos() == 0 && minecraft.mouseHandler.ypos() == 0) {
            targetX = currentX = minecraft.getWindow().getScreenWidth() / 2f;
            targetY = currentY = minecraft.getWindow().getScreenHeight() / 2f;
        } else {
            targetX = currentX = minecraft.mouseHandler.xpos();
            targetY = currentY = minecraft.mouseHandler.ypos();
        }
        setMousePosition();

        ControlifyEvents.VIRTUAL_MOUSE_TOGGLED.invoker().onVirtualMouseToggled(true);
    }

    public void disableVirtualMouse() {
        if (!virtualMouseEnabled) return;

        // make sure minecraft doesn't think the mouse is grabbed when it isn't
        ((MouseHandlerAccessor) minecraft.mouseHandler).setMouseGrabbed(false);

        Controlify.instance().hideMouse(true, true);
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        setMousePosition();
        virtualMouseEnabled = false;
        targetX = currentX = minecraft.mouseHandler.xpos();
        targetY = currentY = minecraft.mouseHandler.ypos();

        ControlifyEvents.VIRTUAL_MOUSE_TOGGLED.invoker().onVirtualMouseToggled(false);
    }

    private void setMousePosition() {
        GLFW.glfwSetCursorPos(
                minecraft.getWindow().getWindow(),
                targetX,
                targetY
        );
    }

    public boolean requiresVirtualMouse() {
        var isController = Controlify.instance().currentInputMode() == InputMode.CONTROLLER;
        var hasScreen = minecraft.screen != null;
        var forceVirtualMouse = hasScreen && ScreenProcessorProvider.provide(minecraft.screen).forceVirtualMouse();
        var screenIsVMouseScreen = hasScreen && Controlify.instance().config().globalSettings().virtualMouseScreens.stream().anyMatch(s -> s.isAssignableFrom(minecraft.screen.getClass()));

        return isController && hasScreen && (forceVirtualMouse || screenIsVMouseScreen);
    }

    public void toggleVirtualMouse() {
        if (minecraft.screen == null) return;

        var screens = Controlify.instance().config().globalSettings().virtualMouseScreens;
        var screenClass = minecraft.screen.getClass();
        if (screens.contains(screenClass)) {
            screens.remove(screenClass);
            disableVirtualMouse();
            Controlify.instance().hideMouse(true, false);

            minecraft.getToasts().addToast(SystemToast.multiline(
                    minecraft,
                    SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    Component.translatable("controlify.toast.vmouse_disabled.title"),
                    Component.translatable("controlify.toast.vmouse_disabled.description")
            ));
        } else {
            screens.add(screenClass);
            enableVirtualMouse();

            minecraft.getToasts().addToast(SystemToast.multiline(
                    minecraft,
                    SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    Component.translatable("controlify.toast.vmouse_enabled.title"),
                    Component.translatable("controlify.toast.vmouse_enabled.description")
            ));
        }

        Controlify.instance().config().save();
    }

    public boolean isVirtualMouseEnabled() {
        return virtualMouseEnabled;
    }
}