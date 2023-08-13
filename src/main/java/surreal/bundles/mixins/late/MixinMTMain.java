package surreal.bundles.mixins.late;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import surreal.bundles.items.ItemBundle;
import yalter.mousetweaks.MTConfig;
import yalter.mousetweaks.Main;
import yalter.mousetweaks.impl.IGuiScreenHandler;
import yalter.mousetweaks.impl.IMouseState;
import yalter.mousetweaks.impl.MouseButton;
import yalter.mousetweaks.util.MTLog;

@Mixin(Main.class)
public abstract class MixinMTMain {

    @Shadow(remap = false) private static GuiScreen oldGuiScreen;

    @Shadow(remap = false) private static IGuiScreenHandler handler;

    @Shadow(remap = false)
    private static IGuiScreenHandler findHandler(GuiScreen currentScreen) {
        return null;
    }

    @Shadow(remap = false) private static IMouseState mouseState;

    @Shadow(remap = false) private static boolean disableForThisContainer;

    @Shadow(remap = false) private static boolean disableWheelForThisContainer;

    @Shadow(remap = false) private static boolean oldRMBDown;

    @Shadow(remap = false) private static Slot firstRightClickedSlot;

    @Shadow(remap = false) private static Slot oldSelectedSlot;

    @Shadow(remap = false)
    private static boolean areStacksCompatible(ItemStack a, ItemStack b) {
        return false;
    }

    @Shadow(remap = false)
    private static void handleWheel(Slot selectedSlot) {
    }

    private static void onUpdateInGui(GuiScreen currentScreen) {
        if (oldGuiScreen != currentScreen) {
            oldGuiScreen = currentScreen;
            MTLog.logger.debug("You have just opened " + currentScreen.getClass().getSimpleName() + ".");
            handler = findHandler(currentScreen);
            mouseState.clear();
            if (handler == null) {
                disableForThisContainer = true;
                MTLog.logger.debug("No valid handler found; MT is disabled.");
                return;
            }

            disableForThisContainer = handler.isMouseTweaksDisabled();
            disableWheelForThisContainer = handler.isWheelTweakDisabled();
            MTLog.logger.debug("Handler: " + handler.getClass().getSimpleName() + "; MT is " + (disableForThisContainer ? "disabled" : "enabled") + "; wheel tweak is " + (disableWheelForThisContainer ? "disabled" : "enabled") + ".");
        }

        if (MTConfig.rmbTweak || MTConfig.lmbTweakWithItem || MTConfig.lmbTweakWithoutItem || MTConfig.wheelTweak) {
            if (!disableForThisContainer) {
                Slot selectedSlot = handler.getSlotUnderMouse();
                ItemStack targetStack;
                ItemStack stackOnMouse;
                if (mouseState.isButtonPressed(MouseButton.RIGHT)) {
                    if (!oldRMBDown) {
                        firstRightClickedSlot = selectedSlot;
                    }

                    if (firstRightClickedSlot != null) {
                        targetStack = firstRightClickedSlot.getStack();
                        stackOnMouse = Minecraft.getMinecraft().player.inventory.getItemStack();

                        if (bundles$isBundle(targetStack, stackOnMouse) && MTConfig.rmbTweak && handler.disableRMBDraggingFunctionality() && (firstRightClickedSlot != selectedSlot || oldSelectedSlot == selectedSlot) && !handler.isIgnored(firstRightClickedSlot) && !handler.isCraftingOutput(firstRightClickedSlot)) {
                            if (!stackOnMouse.isEmpty() && areStacksCompatible(stackOnMouse, targetStack) && firstRightClickedSlot.isItemValid(stackOnMouse)) {
                                handler.clickSlot(firstRightClickedSlot, MouseButton.RIGHT, false);
                            }
                        }
                    }

                } else {
                    firstRightClickedSlot = null;
                }

                if (oldSelectedSlot != selectedSlot) {
                    oldSelectedSlot = selectedSlot;
                    if (selectedSlot == null) {
                        return;
                    }

                    if (firstRightClickedSlot == selectedSlot) {
                        firstRightClickedSlot = null;
                    }

                    MTLog.logger.debug("You have selected a new slot, it's slot number is " + selectedSlot.slotNumber);
                    targetStack = selectedSlot.getStack().copy();
                    stackOnMouse = Minecraft.getMinecraft().player.inventory.getItemStack().copy();
                    boolean shiftIsDown = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
                    if (bundles$isBundle(targetStack, stackOnMouse) && mouseState.isButtonPressed(MouseButton.RIGHT)) {
                        if (MTConfig.rmbTweak && !handler.isIgnored(selectedSlot) && !handler.isCraftingOutput(selectedSlot) && !stackOnMouse.isEmpty() && areStacksCompatible(stackOnMouse, targetStack) && selectedSlot.isItemValid(stackOnMouse)) {
                            handler.clickSlot(selectedSlot, MouseButton.RIGHT, false);
                        }
                    } else if (mouseState.isButtonPressed(MouseButton.LEFT)) {
                        if (!stackOnMouse.isEmpty()) {
                            if (MTConfig.lmbTweakWithItem && !handler.isIgnored(selectedSlot) && !targetStack.isEmpty() && areStacksCompatible(stackOnMouse, targetStack)) {
                                if (shiftIsDown) {
                                    handler.clickSlot(selectedSlot, MouseButton.LEFT, true);
                                } else if (stackOnMouse.getCount() + targetStack.getCount() <= stackOnMouse.getMaxStackSize()) {
                                    handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
                                    if (!handler.isCraftingOutput(selectedSlot)) {
                                        handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
                                    }
                                }
                            }
                        } else if (MTConfig.lmbTweakWithoutItem && !targetStack.isEmpty() && shiftIsDown && !handler.isIgnored(selectedSlot)) {
                            handler.clickSlot(selectedSlot, MouseButton.LEFT, true);
                        }
                    }
                }

                handleWheel(selectedSlot);
            }
        }
    }

    @Unique
    private static boolean bundles$isBundle(ItemStack stack1, ItemStack stack2) {
        return !(stack1.getItem() instanceof ItemBundle) && !(stack2.getItem() instanceof ItemBundle);
    }
}
