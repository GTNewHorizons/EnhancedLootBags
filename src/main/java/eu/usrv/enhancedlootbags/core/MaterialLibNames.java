package eu.usrv.enhancedlootbags.core;

import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.api.StackResolver;

import cpw.mods.fml.common.registry.GameRegistry;
import eu.usrv.enhancedlootbags.EnhancedLootBags;

/// Rewrites the `ml:<Material>:<shapeToken>` form a LootBags.xml drop accepts into the `modid:item[:meta]` form
/// [eu.usrv.yamcore.auxiliary.ItemDescriptor] parses. A drop names the material and shape because MaterialLib item
/// metadata is a material index that shifts whenever the material set changes.
///
/// This is the only class touching MaterialLib, so it stays unloaded while MaterialLib is absent. Its lookups read
/// MaterialLib's resolved registries and run no earlier than init.
public final class MaterialLibNames {

    private MaterialLibNames() {}

    /// The `modid:item[:meta]` name of the stack `pItemName` names, or null when it is malformed or names nothing
    /// MaterialLib serves.
    public static String canonicalize(String pItemName) {
        String[] tParts = pItemName.split(":");
        if (tParts.length != 3) {
            EnhancedLootBags.Logger.error(
                    String.format(
                            "[LootBags] MaterialLib entry [%s] is not of the form ml:<Material>:<Shape>",
                            pItemName));
            return null;
        }

        ItemStack tStack = StackResolver.getStack(tParts[1], tParts[2], 1);
        if (tStack == null) return null;

        GameRegistry.UniqueIdentifier tUID = GameRegistry.findUniqueIdentifierFor(tStack.getItem());
        if (tUID == null) {
            EnhancedLootBags.Logger.error(
                    String.format("[LootBags] MaterialLib entry [%s] resolved to an unregistered item", pItemName));
            return null;
        }

        if (tStack.getItemDamage() > 0) return String.format("%s:%d", tUID.toString(), tStack.getItemDamage());
        return tUID.toString();
    }
}
