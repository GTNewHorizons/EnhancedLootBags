package eu.usrv.enhancedlootbags.core;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.api.StackResolver;

import cpw.mods.fml.common.registry.GameRegistry;
import eu.usrv.enhancedlootbags.EnhancedLootBags;

/// Rewrites the `ml:<Material>:<shapeToken>` form a LootBags.xml drop accepts into the `modid:item[:meta]` form
/// [eu.usrv.yamcore.auxiliary.ItemDescriptor] parses. A drop names the material and shape: MaterialLib item metadata
/// is a material index that shifts whenever the material set changes.
///
/// MaterialLib is touched only here, so the class stays unloaded while MaterialLib is absent. Lookups read
/// MaterialLib's resolved registries and are valid no earlier than init.
public final class MaterialLibNames {

    private MaterialLibNames() {}

    /// The `modid:item[:meta]` name for `pItemName`, or null when it is malformed or names nothing MaterialLib
    /// serves.
    @Nullable
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
