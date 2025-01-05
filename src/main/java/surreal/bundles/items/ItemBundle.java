package surreal.bundles.items;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import surreal.bundles.Bundles;
import surreal.bundles.ModSounds;
import surreal.bundles.config.ConfigHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

import static surreal.bundles.utils.NBTConstants.*;

public class ItemBundle extends Item {

    private static final IItemPropertyGetter BUNDLE_PROPERTY = (stack, worldIn, entityIn) -> {
        int amount = getItemAmount(stack);

        if (amount == 0) return 0F;

        float a = (float) amount / ConfigHandler.bundleLimit;
        float b = 1F / ConfigHandler.bundleLevels;

        int level = (int) (a / b) + 1;
        return b * level;
    };

    public ItemBundle() {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
        addPropertyOverride(new ResourceLocation(Bundles.MODID, "filled"), BUNDLE_PROPERTY);
    }

    // Tooltip
    @SideOnly(Side.CLIENT)
    @Override
    @ParametersAreNonnullByDefault
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTagCompound()) {
            if (ConfigHandler.allowCustomColors && getColor(stack) != 13464390) tooltip.add(I18n.format("item.bundles.bundle.dyed"));
            int size = getItemAmount(stack);
            String str = I18n.format("item.bundles.bundle.fullness", size, ConfigHandler.bundleLimit);

            if (size == ConfigHandler.bundleLimit) tooltip.add(TextFormatting.DARK_RED + str);
            else tooltip.add(str);
        }
    }

    // Sub Items
    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        super.getSubItems(tab, items);

        if (!ConfigHandler.allowCustomColors && this.isInCreativeTab(tab)) {
            for (int i = 0; i < 16; i++) {
                ItemStack stack = new ItemStack(this);
                ItemBundle.setColor(stack, EnumDyeColor.byDyeDamage(i));
                items.add(stack);
            }
        }
    }

    // Bar
    @Override
    public double getDurabilityForDisplay(@Nonnull ItemStack stack) {
        return 1D - ((double) getItemAmount(stack) / ConfigHandler.bundleLimit);
    }

    @Override
    public int getRGBDurabilityForDisplay(@Nonnull ItemStack stack) {
        return 0x6C69E2;
    }

    @Override
    public boolean showDurabilityBar(@Nonnull ItemStack stack) {
        return getItemAmount(stack) > 0;
    }

    // Events
    @ParametersAreNonnullByDefault
    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {

        ItemStack bundle = playerIn.getHeldItem(handIn);
        if (!bundle.hasTagCompound() || !playerIn.isSneaking()) return super.onItemRightClick(worldIn, playerIn, handIn);

        if (getItemAmount(bundle) > 0) {
            ItemStack[] items = getItems(bundle);

            if (items != null && items.length > 0) {
                if (!worldIn.isRemote) {
                    for (ItemStack stack : items) {
                        EntityItem item = new EntityItem(worldIn, playerIn.posX, playerIn.posY + 0.3D, playerIn.posZ, stack);
                        item.setNoPickupDelay();
                        worldIn.spawnEntity(item);
                    }

                    NBTTagCompound tag = Objects.requireNonNull(bundle.getTagCompound());
                    tag.removeTag(ITEMS);
                    tag.setTag(ITEMS, new NBTTagList());

                    setAmount(bundle, 0);

                    return new ActionResult<>(EnumActionResult.SUCCESS, bundle);

                } else
                    playerIn.playSound(ModSounds.DROP_CONTENTS, 1F, 1F);
            }
        }


        return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    // NBT Utilities
    public static void addItem(ItemStack bundle, ItemStack stack) {
        NBTTagList list = getItemList(bundle);

        if (list == null) {
            NBTTagCompound compound = getOrCreateTag(bundle);
            list = new NBTTagList();
            compound.setTag(ITEMS, list);
        }

        int empty = getEmptyAmount(bundle);

        int itemSize = getItemstackAmount(stack);
        int itemCount = stack.getCount();

        int countToAdd = Math.min(itemCount, empty / itemSize);
        if (countToAdd == 0) return;

        int amountToAdd = Math.min(itemSize * itemCount, itemSize * countToAdd);

        addAmount(bundle, amountToAdd);

        NBTTagCompound compound = stack.serializeNBT();

        if (compound.hasKey("Count", Constants.NBT.TAG_BYTE)) {
            compound.setByte("Count", (byte) countToAdd);
        }
        else compound.setInteger("Count", countToAdd);

        list.appendTag(compound);

        stack.setCount(stack.getCount() - countToAdd);
    }

    public static ItemStack removeItem(ItemStack bundle, int slot, int amount) {
        NBTTagList list = getItemList(bundle);
        if (list == null) {
            NBTTagCompound compound = getOrCreateTag(bundle);
            list = new NBTTagList();
            compound.setTag(ITEMS, list);
            return ItemStack.EMPTY;
        }

        slot = checkSlot(bundle, slot);

        NBTTagCompound stackTag = list.getCompoundTagAt(slot);
        ItemStack stack = new ItemStack(stackTag);
        if (stack.isEmpty()) return stack;

        amount = Math.min(stack.getCount(), amount);
        if (amount == stack.getCount()) {
            list.removeTag(slot);
        } else {
            int count = stack.getCount() - amount;

            if (stackTag.hasKey("Count", Constants.NBT.TAG_BYTE)) {
                stackTag.setByte("Count", (byte) count);
            }
            else stackTag.setInteger("Count", count);

            stack.setCount(amount);
        }

        addAmount(bundle, -getItemstackAmount(stack) * stack.getCount());

        return stack;
    }

    public static int getSlotCount(ItemStack bundle) {
        NBTTagList list = getItemList(bundle);
        if (list == null) return 0;
        return list.tagCount();
    }

    public static int getEmptyAmount(ItemStack bundle) {
        return ConfigHandler.bundleLimit - getItemAmount(bundle);
    }

    public static int getItemAmount(ItemStack bundle) {
        NBTTagCompound tag = getOrCreateTag(bundle);
        if (!tag.isEmpty() && tag.hasKey(ITEM_AMOUNT)) return tag.getInteger(ITEM_AMOUNT);
        return 0;
    }

    public static boolean isBundleFull(ItemStack bundle) {
        return getItemAmount(bundle) == ConfigHandler.bundleLimit;
    }

    public static ItemStack[] getItems(ItemStack bundle) {
        NBTTagList list = getItemList(bundle);
        if (list == null) return null;

        int count = list.tagCount();

        if (count <= 0) return null;

        ItemStack[] array = new ItemStack[count];

        for (int i = 0; i < count; i++) {
            ItemStack stack = new ItemStack(list.getCompoundTagAt(i));
            array[i] = stack;
        }

        return array;
    }

    public static ItemStack getItem(ItemStack bundle, int slot) {
        NBTTagList list = getItemList(bundle);
        if (list == null) return ItemStack.EMPTY;
        int count = list.tagCount();
        if (count <= 0) return ItemStack.EMPTY;

        slot = checkSlot(bundle, slot);
        return new ItemStack(list.getCompoundTagAt(slot));
    }

    public static int getColor(ItemStack bundle) {
        int def = 13464390;
        if (!bundle.hasTagCompound()) return def;
        NBTTagCompound tag = Objects.requireNonNull(bundle.getTagCompound());
        if (!tag.hasKey(COLOR)) return def;
        return tag.getInteger(COLOR);
    }

    public static void setColor(ItemStack bundle, int color) {
        NBTTagCompound tag = getOrCreateTag(bundle);
        tag.setInteger(COLOR, color);
    }

    public static void setColor(ItemStack bundle, EnumDyeColor color) {
        setColor(bundle, -color.getDyeDamage());
    }

    private static void setAmount(ItemStack bundle, int amount) {
        NBTTagCompound tag = getOrCreateTag(bundle);
        tag.setInteger(ITEM_AMOUNT, amount);
    }

    private static void addAmount(ItemStack bundle, int amount) {
        NBTTagCompound tag = getOrCreateTag(bundle);
        if (tag.hasKey(ITEM_AMOUNT)) amount += tag.getInteger(ITEM_AMOUNT);
        tag.setInteger(ITEM_AMOUNT, amount);
    }

    private static int getItemstackAmount(ItemStack stack) {
        int size = 1;
        if (ConfigHandler.respectStackSize && stack.getMaxStackSize() < 64) size = 64 / stack.getMaxStackSize();
        return size;
    }

    private static NBTTagList getItemList(ItemStack bundle) {
        NBTTagCompound tag = getOrCreateTag(bundle);
        if (tag.isEmpty() || !tag.hasKey(ITEMS) || getItemAmount(bundle) <= 0) return null;
        return tag.getTagList(ITEMS, Constants.NBT.TAG_COMPOUND);
    }

    private static int checkSlot(ItemStack bundle, int slot) {
        NBTTagList list = getItemList(bundle);
        if (list == null || list.tagCount() == 0) return -1;
        if (slot >= list.tagCount()) slot = list.tagCount() - 1;
        else if (slot < 0) slot = 0;
        return slot;
    }

    @Nonnull
    private static NBTTagCompound getOrCreateTag(ItemStack bundle) {
        if (!bundle.hasTagCompound()) bundle.setTagCompound(new NBTTagCompound());
        return Objects.requireNonNull(bundle.getTagCompound());
    }
}