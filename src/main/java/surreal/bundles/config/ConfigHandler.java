package surreal.bundles.config;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import surreal.bundles.Bundles;
import surreal.bundles.utils.HashStrategies;

import java.util.Objects;
import java.util.Set;

// Please fuck off forge
public class ConfigHandler {

    private static Configuration CONFIG;

    public static int bundleLimit, bundleLevels;
    public static boolean allowCustomColors, isBlackList, allowStorageItems, allowTools, respectStackSize;
    public static String[] itemList;

    // Config Cache
    private static Set<ItemStack> itemSet = null;

    public static Configuration configuration() {
        return CONFIG;
    }

    public static void initialize(FMLPreInitializationEvent event) {
        CONFIG = new Configuration(event.getSuggestedConfigurationFile());
        CONFIG.load();
        sync();
    }

    private static void sync() {
        String serverSide = "Server Side";
        String clientSide = "Client Side";

        CONFIG.addCustomCategoryComment(serverSide, "Server side options.");
        CONFIG.setCategoryRequiresMcRestart(serverSide, true);

        bundleLimit = CONFIG.getInt("Bundle Size", serverSide, 64, 0, Integer.MAX_VALUE, "Amounts of items bundle can get.");
        allowStorageItems = CONFIG.getBoolean("Allow Storage Items", serverSide, false, "Allow items to be added to bundles like shulker boxes, forestry backpacks etc.");
        allowTools = CONFIG.getBoolean("Allow Tools", serverSide, true, "Allow Tools to be added to bundles.");
        respectStackSize = CONFIG.getBoolean("Respect Items Max Stack Size", serverSide, true, "Respect maximum stack sizes of items.");
        isBlackList = CONFIG.getBoolean("Is Blacklist", serverSide, true, "Is item list blacklist or not.");
        itemList = CONFIG.getStringList("Item List", serverSide, new String[0], "List of items that should be allowed to be added to bundle or not. If isBlacklist is true this will act as blacklist and whitelist if that's false.\nYou don't need to specify mod id if item is from Minecraft. Metadata and NBT is optional.\n<item_id>@<metadata>#<nbt> e.g. minecraft:dirt, dye@15, thermalfoundation:material@19, mod:examplewithtag@15#{integer:2}");

        CONFIG.addCustomCategoryComment(clientSide, "Client side options. You mostly don't need to restart.");
        bundleLevels = CONFIG.getInt("Bundle Levels", clientSide, 1, 0, Integer.MAX_VALUE, "Amount of texture changes.");
        allowCustomColors = CONFIG.getBoolean("Custom Colors", clientSide, false, "Allows mixing colors like leather armor. You need to restart both server and client for this to work properly.\nI might separate it but i don't want to");

        itemSet = getItemList();

        if (CONFIG.hasChanged()) CONFIG.save();
    }

    @SubscribeEvent
    public static void configChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Bundles.MODID)) {
            sync();
            itemSet = getItemList();
        }
    }

    public static boolean canPutItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean blacklist = isBlackList != itemSet.contains(stack);
        boolean tools = allowTools || (!stack.isItemStackDamageable() && !(stack.getItem() instanceof ItemTool));
        boolean storage = allowStorageItems || (!stack.hasTagCompound() || !hasAnyKeys(Objects.requireNonNull(stack.getTagCompound()), "BlockEntityTag", "Items"));
        return blacklist && tools && storage;
    }

    private static boolean hasAnyKeys(NBTTagCompound tag, String... keys) {
        for (String str : keys) {
            if (tag.hasKey(str)) return true;
        }

        return false;
    }

    public static Set<ItemStack> getItemList() {
        if (itemList.length > 0) {
            Set<ItemStack> ret = new ObjectOpenCustomHashSet<>(HashStrategies.ITEMSTACK_STRATEGY);
            String metaIndex = "@";
            String tagIndex = "#";

            Logger logger = LogManager.getLogger(Bundles.MODID);

            int index = 1;
            for (String str : itemList) {
                str = str.trim();

                Item item;
                int meta = 0;
                NBTTagCompound tag = null;

                if (str.contains(metaIndex)) {
                    String[] itemAndMeta = str.split(metaIndex);

                    if (str.contains(tagIndex)) {
                        String[] metaAndTag = itemAndMeta[1].split(tagIndex);

                        meta = Integer.parseInt(itemAndMeta[0]);

                        try {
                            tag = JsonToNBT.getTagFromJson(metaAndTag[1]);
                        } catch (NBTException e) {
                            logger.error("Tag of item in config list at {} is set wrong, please fix it immediately!", index);
                        }
                    } else {
                        meta = Integer.parseInt(itemAndMeta[1]);
                    }

                    str = itemAndMeta[0];
                }

                item = Item.getByNameOrId(str);

                ItemStack stack;

                if (item == null || item == Items.AIR) {
                    logger.error("ID of item in config item list at {} is set wrong, please fix it immediately!", index);
                    stack = ItemStack.EMPTY;
                } else stack = new ItemStack(item, 1, meta);

                if (!stack.isEmpty()) {
                    stack.setTagCompound(tag);
                    ret.add(stack);
                }

                index++;
            }

            return ret;
        } else
            return new ObjectOpenHashSet<>();
    }
}
