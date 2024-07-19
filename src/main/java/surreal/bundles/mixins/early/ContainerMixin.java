package surreal.bundles.mixins.early;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import surreal.bundles.ModSounds;
import surreal.bundles.config.ConfigHandler;
import surreal.bundles.items.ItemBundle;

import java.util.List;

@Mixin(Container.class)
public abstract class ContainerMixin {

    @Shadow
    public List<Slot> inventorySlots;

    @Shadow private int dragEvent;

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    public void slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        System.out.println("type: " + dragType + "      event: " + dragEvent);

        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            Slot slot = this.inventorySlots.get(slotId);
            ItemStack stack = player.inventory.getItemStack();

            if (slot != null && (((dragType == 1 || dragType == 4) && dragEvent == 0) || ((dragType == 5 || dragType == 6) && dragEvent == 1))) {
                if (stack.getItem() instanceof ItemBundle) {
                    int itemAmount = ItemBundle.getItemAmount(stack);

                    if (slot.getHasStack() && slot.canTakeStack(player) && itemAmount < ConfigHandler.bundleLimit && ConfigHandler.canPutItem(slot.getStack())) {
                        bundles$insertItem(player, stack, slot.getStack());
                        cir.setReturnValue(ItemStack.EMPTY);
                    }
                    else if (itemAmount > 0) {
                        bundles$removeItem(player, stack, slot);
                        cir.setReturnValue(ItemStack.EMPTY);
                    }
                }
                else if (slot.getStack().getItem() instanceof ItemBundle) {
                    int itemAmount = ItemBundle.getItemAmount(slot.getStack());

                    if (!stack.isEmpty() && itemAmount < ConfigHandler.bundleLimit && ConfigHandler.canPutItem(stack)) {
                        bundles$insertItem(player, slot.getStack(), stack);
                        cir.setReturnValue(ItemStack.EMPTY);
                    }
                    else if (itemAmount > 0) {
                        bundles$removeHeldItem(player, slot.getStack());
                        cir.setReturnValue(ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    @Unique
    private void bundles$insertItem(EntityPlayer player, ItemStack bundle, ItemStack stackToAdd) {
        if (player.world.isRemote) player.playSound(ModSounds.INSERT, 1, 1);
        ItemBundle.addItem(bundle, stackToAdd);
    }

    @Unique
    private void bundles$removeItem(EntityPlayer player, ItemStack bundle, Slot slotInventory) {
        if (slotInventory.getHasStack()) {
            for (int i = 0; i < ItemBundle.getSlotCount(bundle); i++) {
                ItemStack stack = ItemBundle.getItem(bundle, i);
                if (Container.canAddItemToSlot(slotInventory, stack, true) && slotInventory.isItemValid(stack)) {
                    if (player.world.isRemote) player.playSound(ModSounds.REMOVE, 1, 1);
                    slotInventory.putStack(ItemBundle.removeItem(bundle, i, Math.min(slotInventory.getItemStackLimit(stack), stack.getMaxStackSize())));
                    return;
                }
            }
        }

        int slot = GuiScreen.isCtrlKeyDown() ? Integer.MAX_VALUE : 0;
        ItemStack st = ItemBundle.getItem(bundle, slot);

        if (Container.canAddItemToSlot(slotInventory, st, true) && slotInventory.isItemValid(st)) {
            if (player.world.isRemote) player.playSound(ModSounds.REMOVE, 1, 1);
            slotInventory.putStack(ItemBundle.removeItem(bundle, slot, Math.min(slotInventory.getItemStackLimit(st), st.getMaxStackSize())));
        }
    }

    @Unique
    private void bundles$removeHeldItem(EntityPlayer player, ItemStack bundle) {
        if (!player.inventory.getItemStack().isEmpty()) {
            for (int i = 0; i < ItemBundle.getSlotCount(bundle); i++) {
                ItemStack stack = ItemBundle.getItem(bundle, i);
                if (ItemStack.areItemStacksEqual(stack, player.inventory.getItemStack())) {
                    if (player.world.isRemote) player.playSound(ModSounds.REMOVE, 1, 1);
                    player.inventory.setItemStack(ItemBundle.removeItem(bundle, i, stack.getMaxStackSize()));
                    return;
                }
            }
        }

        int slot = GuiScreen.isCtrlKeyDown() ? Integer.MAX_VALUE : 0;
        ItemStack st = ItemBundle.getItem(bundle, slot);

        if (player.world.isRemote) player.playSound(ModSounds.REMOVE, 1, 1);
        player.inventory.setItemStack(ItemBundle.removeItem(bundle, slot, st.getMaxStackSize()));
    }
}