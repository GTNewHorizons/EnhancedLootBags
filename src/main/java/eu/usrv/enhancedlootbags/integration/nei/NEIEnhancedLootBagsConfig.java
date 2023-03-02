package eu.usrv.enhancedlootbags.integration.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import eu.usrv.enhancedlootbags.EnhancedLootBags;

@SuppressWarnings("unused")
public class NEIEnhancedLootBagsConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.registerRecipeHandler(new LootBagRecipeHandler());
        API.registerUsageHandler(new LootBagRecipeHandler());
    }

    @Override
    public String getName() {
        return EnhancedLootBags.MODNAME;
    }

    @Override
    public String getVersion() {
        return EnhancedLootBags.VERSION;
    }
}
