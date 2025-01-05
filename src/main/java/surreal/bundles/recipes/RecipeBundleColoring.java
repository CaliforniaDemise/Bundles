package surreal.bundles.recipes;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.registries.IForgeRegistryEntry;
import surreal.bundles.items.ItemBundle;

import javax.annotation.Nonnull;
import java.util.Optional;

public class RecipeBundleColoring extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(@Nonnull InventoryCrafting inv, @Nonnull World worldIn) {

        boolean hasBundle = false;
        boolean hasColor = false;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!DyeUtils.isDye(stack)) {
                if (stack.getItem() instanceof ItemBundle) {
                    if (!hasBundle) hasBundle = true;
                    else return false;
                }
            } else hasColor = true;
        }

        return hasBundle && hasColor;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {

        ItemStack itemstack = ItemStack.EMPTY;
        int[] aint = new int[3];
        int i = 0;
        int j = 0;

        for (int k = 0; k < inv.getSizeInventory(); ++k)
        {
            ItemStack itemstack1 = inv.getStackInSlot(k);

            if (!itemstack1.isEmpty()) {
                if (itemstack1.getItem() instanceof ItemBundle) {

                    itemstack = itemstack1.copy();

                    int l = ItemBundle.getColor(itemstack);
                    if (l < 0) {
                        EnumDyeColor col = EnumDyeColor.byDyeDamage(-l);
                        switch (col) {
                            case WHITE: l = 16383998; break;
                            case ORANGE: l = 16351261; break;
                            case MAGENTA: l = 13061821; break;
                            case LIGHT_BLUE: l = 3847130; break;
                            case YELLOW: l = 16701501; break;
                            case LIME: l = 8439583; break;
                            case PINK: l = 15961002; break;
                            case GRAY: l = 4673362; break;
                            case SILVER: l = 10329495; break;
                            case CYAN: l = 1481884; break;
                            case PURPLE: l = 8991416; break;
                            case BLUE: l = 3949738; break;
                            case BROWN: l = 8606770; break;
                            case GREEN: l = 6192150; break;
                            case RED: l = 11546150; break;
                            case BLACK: l = 1908001; break;
                        }
                    }
                    float f = (float)(l >> 16 & 255) / 255.0F;
                    float f1 = (float)(l >> 8 & 255) / 255.0F;
                    float f2 = (float)(l & 255) / 255.0F;
                    i = (int)((float)i + Math.max(f, Math.max(f1, f2)) * 255.0F);
                    aint[0] = (int)((float)aint[0] + f * 255.0F);
                    aint[1] = (int)((float)aint[1] + f1 * 255.0F);
                    aint[2] = (int)((float)aint[2] + f2 * 255.0F);
                }
                else {

                    Optional<EnumDyeColor> color = DyeUtils.colorFromStack(itemstack1);
                    if (!color.isPresent()) return itemstack;

                    float[] afloat = color.get().getColorComponentValues();
                    int l1 = (int)(afloat[0] * 255.0F);
                    int i2 = (int)(afloat[1] * 255.0F);
                    int j2 = (int)(afloat[2] * 255.0F);
                    i += Math.max(l1, Math.max(i2, j2));
                    aint[0] += l1;
                    aint[1] += i2;
                    aint[2] += j2;
                }
                ++j;
            }
        }

        if (!itemstack.isEmpty()) {
            int i1 = aint[0] / j;
            int j1 = aint[1] / j;
            int k1 = aint[2] / j;
            float f3 = (float)i / (float)j;
            float f4 = (float)Math.max(i1, Math.max(j1, k1));
            i1 = (int)((float)i1 * f3 / f4);
            j1 = (int)((float)j1 * f3 / f4);
            k1 = (int)((float)k1 * f3 / f4);
            int k2 = (i1 << 8) + j1;
            k2 = (k2 << 8) + k1;
            ItemBundle.setColor(itemstack, k2);
        }

        return itemstack;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width > 1 || height > 1;
    }

    @Nonnull
    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }
}