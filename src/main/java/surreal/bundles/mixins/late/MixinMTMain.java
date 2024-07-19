package surreal.bundles.mixins.late;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import surreal.bundles.items.ItemBundle;
import yalter.mousetweaks.Main;
import yalter.mousetweaks.impl.IGuiScreenHandler;
import zone.rong.mixinextras.injector.ModifyExpressionValue;

@Mixin(Main.class)
public abstract class MixinMTMain {

    @Shadow private static Slot firstRightClickedSlot;

    @Shadow private static IGuiScreenHandler handler;

    @ModifyExpressionValue(method = "onUpdateInGui", at = @At(value = "FIELD", target = "Lyalter/mousetweaks/MTConfig;rmbTweak:Z", ordinal = 1))
    private static boolean bundles$firstCheck(boolean original) {
        ItemStack targetStack = firstRightClickedSlot.getStack(), stackOnMouse = Minecraft.getMinecraft().player.inventory.getItemStack();
        return original && bundles$isBundle(targetStack, stackOnMouse);
    }

    @ModifyExpressionValue(method = "onUpdateInGui", at = @At(value = "INVOKE", target = "Lyalter/mousetweaks/impl/IMouseState;isButtonPressed(Lyalter/mousetweaks/impl/MouseButton;)Z", ordinal = 1))
    private static boolean bundles$secondCheck(boolean original) {
        ItemStack targetStack = handler.getSlotUnderMouse().getStack(), stackOnMouse = Minecraft.getMinecraft().player.inventory.getItemStack();
        return original && bundles$isBundle(targetStack, stackOnMouse);
    }

    @Unique
    private static boolean bundles$isBundle(ItemStack stack1, ItemStack stack2) {
        return !(stack1.getItem() instanceof ItemBundle) && !(stack2.getItem() instanceof ItemBundle);
    }
}
