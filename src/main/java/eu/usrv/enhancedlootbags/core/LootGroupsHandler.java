/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.items.ItemLootBag;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import eu.usrv.enhancedlootbags.core.serializer.LootGroupsFactory;
import eu.usrv.enhancedlootbags.net.msg.LootBagClientSyncMessage;
import eu.usrv.yamcore.auxiliary.ItemDescriptor;
import eu.usrv.yamcore.auxiliary.LogHelper;
import eu.usrv.yamcore.auxiliary.TextFormatHelper;
import eu.usrv.yamcore.persisteddata.PersistedDataBase;

public class LootGroupsHandler {

    private LogHelper _mLogger = EnhancedLootBags.Logger;
    private String _mConfigFileName;
    private LootGroupsFactory _mLGF = new LootGroupsFactory();

    private LootGroups _mLootGroups = null;
    private LootGroups _mClientSideLootGroups = null;

    private eu.usrv.yamcore.persisteddata.PersistedDataBase _mPersistedDB = null;

    private boolean _mInitialized = false;

    public LootGroups getLootGroups() {
        return _mLootGroups;
    }

    public LootGroups getLootGroupsClient() {
        return _mClientSideLootGroups;
    }

    /**
     * Calculate the unique loot-identifier for given loot and player. This is used to keep track of stuff the player
     * got already
     *
     * @param pPlayer
     * @param pDrop
     * @return
     */
    public String getUniqueLootIdentifier(EntityPlayer pPlayer, LootGroup pGroup, Drop pDrop) {
        return String.format("%s_%s_%s", pPlayer.getUniqueID().toString(), pGroup.getGroupID(), pDrop.getIdentifier());
    }

    /**
     * Check if pPlayer is allowed to receive item pDrop in group pGroup
     *
     * @param pPlayer
     * @param pGroup
     * @param pDrop
     * @return
     */
    public boolean isDropAllowedForPlayer(EntityPlayer pPlayer, LootGroup pGroup, Drop pDrop,
            boolean pUpdateDropCount) {
        InitStorage();
        String pDropUID = getUniqueLootIdentifier(pPlayer, pGroup, pDrop);

        if (pDrop.getLimitedDropCount() > 0) {
            int tReceivedAmount = getDropCount(pPlayer, pGroup, pDrop);
            if (tReceivedAmount >= pDrop.getLimitedDropCount()) {
                return false;
            } else {
                if (pUpdateDropCount) {
                    _mPersistedDB.setValue(pDropUID, tReceivedAmount + 1);
                }
                return true;
            }
        }

        return true;
    }

    public int getDropCount(EntityPlayer pPlayer, LootGroup pGroup, Drop pDrop) {
        InitStorage();
        if (pDrop.getLimitedDropCount() <= 0) return 0;
        return _mPersistedDB.getValueAsInt(getUniqueLootIdentifier(pPlayer, pGroup, pDrop), 0);
    }

    private void InitStorage() {
        if (_mPersistedDB == null) {
            _mPersistedDB = new PersistedDataBase(
                    DimensionManager.getCurrentSaveRootDirectory(),
                    "LootBags.dat",
                    "LootBagStorage");
            _mPersistedDB.Load();
        }
    }

    /**
     * Increase the drop counter for given drop by 1
     *
     * @param pPlayer
     * @param pGroup
     * @param pDrop
     */
    public void updateDropCount(EntityPlayer pPlayer, LootGroup pGroup, Drop pDrop) {
        InitStorage();

        boolean tResult = true;
        String pDropUID = getUniqueLootIdentifier(pPlayer, pGroup, pDrop);

        if (pDrop.getLimitedDropCount() > 0)
            _mPersistedDB.setValue(pDropUID, _mPersistedDB.getValueAsInt(pDropUID, 0) + 1);
        else _mLogger.warn("Unable to update DropCount for LootID %s. Limit is 0!", pDrop.getIdentifier());
    }

    public LootGroupsHandler(File pConfigBaseDir) {
        File tConfDir = new File(pConfigBaseDir, EnhancedLootBags.NICEFOLDERNAME);
        if (!tConfDir.exists()) tConfDir.mkdirs();

        _mConfigFileName = new File(tConfDir, "LootBags.xml").toString();
    }

    /**
     * Init sample configuration if none could be found
     */
    public void InitSampleConfig() {
        Drop pigDiamondLimitedDrop = _mLGF.createDrop(
                "minecraft:diamond",
                "sample_Loot_DiamondDrop",
                "{display:{Lore:[\"Oh, shiny\"]}}",
                1,
                false,
                100,
                5,
                "");
        Drop pigCakeUnlimitedDrop = _mLGF.createDrop("minecraft:cake", "sample_Loot_CakeDrop", 1, false, 100, 0);
        Drop pigRandomCharcoalDrop = _mLGF.createDrop("minecraft:coal:1", "sample_Loot_CharcoalDrop", 5, true, 100, 0);

        LootGroup tTrashGroup = _mLGF.createLootGroup(0, "Generic trash group", EnumRarity.common, 1, 1, false);
        LootGroup tSampleGroup = _mLGF.createLootGroup(1, "Sample Item group", EnumRarity.common, 1, 1, true);
        tSampleGroup.getDrops().add(pigDiamondLimitedDrop);
        tSampleGroup.getDrops().add(pigCakeUnlimitedDrop);
        tTrashGroup.getDrops().add(pigRandomCharcoalDrop);

        _mLootGroups = new LootGroups();
        _mLootGroups.getLootTable().add(tSampleGroup);
        _mLootGroups.getLootTable().add(tTrashGroup);
    }

    private HashMap<String, LootGroup> _mBufferedLootGroups = new HashMap<String, LootGroup>();

    public LootGroup getMergedGroupFromID(int pGroupID, int pFortuneLevel) {
        LootGroup tReturnGroup = null;
        LootGroup tTargetGroup = getGroupByID(pGroupID);
        String tMergedGroupID = getFormattedGroupID(pGroupID, pFortuneLevel);

        if (tTargetGroup != null) {
            if (!tTargetGroup.getCombineWithTrash() || (EnhancedLootBags.ELBCfg.AllowFortuneBags && pFortuneLevel == 3))
                tReturnGroup = tTargetGroup;
            else {
                tReturnGroup = _mBufferedLootGroups.get(tMergedGroupID);
                if (tReturnGroup == null) {
                    LootGroup tTrashGroup = getGroupByID(tTargetGroup.getTrashGroup());
                    if (tTrashGroup != null) {
                        // Copy the original group
                        LootGroup tMerged = _mLGF.copyLootGroup(tTargetGroup);
                        // Add a copy for each trash loot to the drop list

                        for (Drop tDr : tTrashGroup.getDrops()) {
                            tMerged.getDrops().add(_mLGF.copyDrop(tDr, pFortuneLevel));
                        }

                        tMerged.updateMaxWeight();

                        // Store the new list in our buffer
                        _mBufferedLootGroups.put(tMergedGroupID, tMerged);

                        // Set as return group
                        tReturnGroup = tMerged;
                    } else {
                        tReturnGroup = tTargetGroup;
                    }
                }
            }
        } else
            _mLogger.error(String.format("TargetGroup for ID returned null, this shouldn't happen. ID: %d", pGroupID));

        // Now shuffle the list a few times, to ensure randomness
        tReturnGroup.shuffleLoot();
        return tReturnGroup;
    }

    private String getFormattedGroupID(int pGroupID, int pFortuneLevel) {
        return String.format("%d-%d", pGroupID, pFortuneLevel);
    }

    public LootGroup getGroupByIDClient(int pGroupID) {
        for (LootGroup tGrp : _mClientSideLootGroups.getLootTable()) if (tGrp.getGroupID() == pGroupID) return tGrp;
        return null;
    }

    public LootGroup getGroupByID(int pGroupID) {
        for (LootGroup tGrp : _mLootGroups.getLootTable()) if (tGrp.getGroupID() == pGroupID) return tGrp;
        return null;
    }

    /**
     * Save the loot configuration to disk
     *
     * @return
     */
    public boolean SaveLootGroups() {
        try {
            JAXBContext tJaxbCtx = JAXBContext.newInstance(LootGroups.class);
            Marshaller jaxMarsh = tJaxbCtx.createMarshaller();
            jaxMarsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxMarsh.marshal(_mLootGroups, new FileOutputStream(_mConfigFileName, false));
            return true;
        } catch (Exception e) {
            _mLogger.error("[LootBags] Unable to create new LootBags.xml. Is the config directory write protected?");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load the loot configuration from disk. Will not overwrite the existing loottable if errors occour Only called
     * ONCE! Upon PostLoad(). Call reload() instead
     */
    public void LoadConfig() {
        if (_mInitialized) {
            _mLogger.error("[LootBags] Something just called LoadConfig AFTER it has been initialized!");
            return;
        }

        File tConfigFile = new File(_mConfigFileName);
        if (!tConfigFile.exists()) {
            InitSampleConfig();
            SaveLootGroups();
        }

        // Fix for broken XML file; If it can't be loaded on reboot, keep it
        // there to be fixed, but load
        // default setting instead, so an Op/Admin can do reload ingame
        if (!ReloadLootGroups("")) {
            _mLogger.warn("[LootBags] Configuration File seems to be damaged, nothing will be loaded!");
            EnhancedLootBags.AdminLogonErrors.AddErrorLogOnAdminJoin("[LootBags] Config file not loaded due errors");
            InitSampleConfig();
        }
        _mInitialized = true;
    }

    /**
     * Initiate reload. Will reload the config from disk and replace the internal list. If the file contains errors,
     * nothing will be replaced, and an errormessage will be sent to the command issuer.
     *
     * This method will just load the config the first time it is called, as this will happen in the servers
     * load/postinit phase. After that, every call is caused by someone who tried to do an ingame reload. If that is
     * successful, the updated config is broadcasted to every connected client
     *
     * @return
     */
    public boolean reload() {
        boolean tState = ReloadLootGroups("");
        if (_mInitialized) {
            if (tState) sendClientUpdate();
            else _mLogger.error("[LootBags] Reload of LootBag file failed. Not sending client update");
        }
        return tState;
    }

    private static Item mLootBagItem = null;

    public static Item getLootBagItem() {
        return mLootBagItem;
    }

    public void registerBagItem() {
        mLootBagItem = new ItemLootBag(this);
        GameRegistry.registerItem(mLootBagItem, "lootbag");
    }

    private String getClientSideXMLStream() {
        try {
            StringWriter tSW = new StringWriter();
            JAXBContext tJaxbCtx = JAXBContext.newInstance(LootGroups.class);
            Marshaller jaxMarsh = tJaxbCtx.createMarshaller();
            jaxMarsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            LootGroups tReducedGroup = _mLGF.copy(_mLootGroups, false);
            jaxMarsh.marshal(tReducedGroup, tSW);

            return tSW.toString();
        } catch (Exception e) {
            _mLogger.error("[LootBags] Unable to serialize object");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Verify the loaded config and report errors if found
     *
     * @param pLootGroupsToCheck
     * @return
     */
    private boolean VerifyConfig(LootGroups pLootGroupsToCheck, boolean pIsLocalConfig) {
        boolean tSuccess = true;
        List<Integer> tIDlist = new ArrayList<Integer>();
        List<String> tNameList = new ArrayList<String>();

        for (LootGroup X : pLootGroupsToCheck.getLootTable()) {
            if (tIDlist.contains(X.getGroupID())) {
                _mLogger.error(String.format("[LootBags] LootGroup ID %d already exists!", X.getGroupID()));
                tSuccess = false;
                break;
            } else tIDlist.add(X.getGroupID());

            if (tNameList.contains(X.getGroupName())) {
                _mLogger.error(
                        String.format("[LootBags] LootGroup with the Name %s already exists!", X.getGroupName()));
                tSuccess = false;
                break;
            } else tNameList.add(X.getGroupName());

            // Skip lootItems check if we received the server config
            if (pIsLocalConfig) { // This is a server-run. Verify we actually do have all items defined
                if (X.getDrops().size() == 0) {
                    _mLogger.error(
                            String.format("[LootBags] LootGroup ID %d is empty. Adding dummy item", X.getGroupID()));
                    String tNothingDrop = ItemDescriptor.fromItem(Items.cookie).toString();
                    X.getDrops().add(_mLGF.createDrop(tNothingDrop, "cookiedropbecauseempty", 1, false, 100, 0));

                    // tSuccess = false; // Don't break on empty groups
                    break;
                }

                for (Drop Y : X.getDrops()) {
                    if (ItemDescriptor.fromString(Y.getItemName()) == null) {
                        _mLogger.error(
                                String.format(
                                        "[LootBags] In ItemDropID: [%s], can't find item [%s]",
                                        Y.getIdentifier(),
                                        Y.getItemName()));
                        tSuccess = false; // Maybe add the nothing-item here? Or the invalid-item item from HQM
                    }

                    if (Y.getNBTTag() != null && !Y.getNBTTag().isEmpty()) {
                        try {
                            NBTTagCompound tNBT = (NBTTagCompound) JsonToNBT.func_150315_a(Y.getNBTTag());
                            if (tNBT == null) tSuccess = false;
                        } catch (Exception e) {
                            _mLogger.error(
                                    String.format(
                                            "[LootBags] In ItemDropID: [%s], NBTTag is invalid",
                                            Y.getIdentifier()));
                            tSuccess = false;
                        }
                    }
                }
            }
        }
        return tSuccess;
    }

    /**
     * (Re)load lootgroups
     *
     * @return
     */
    private boolean ReloadLootGroups(String pXMLContent) {
        boolean tResult = false;

        try {
            JAXBContext tJaxbCtx = JAXBContext.newInstance(LootGroups.class);
            Unmarshaller jaxUnmarsh = tJaxbCtx.createUnmarshaller();

            LootGroups tNewItemCollection = null;
            boolean tLocalConfig = pXMLContent.isEmpty();

            if (tLocalConfig) {
                File tConfigFile = new File(_mConfigFileName);
                tNewItemCollection = (LootGroups) jaxUnmarsh.unmarshal(tConfigFile);
            } else {
                StringReader reader = new StringReader(pXMLContent);
                tNewItemCollection = (LootGroups) jaxUnmarsh.unmarshal(reader);
            }

            if (!VerifyConfig(tNewItemCollection, tLocalConfig)) {
                _mLogger.error(
                        "[LootBags] New config will NOT be activated. Please check your error-log and try again");
                tResult = false;
            } else {
                if (tLocalConfig) {
                    _mLootGroups = tNewItemCollection;
                    _mBufferedLootGroups.clear(); // Also empty the buffered groups; As we might've gotten some
                    // group-relationship changs
                }
                _mClientSideLootGroups = tNewItemCollection;

                tResult = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tResult;
    }

    /**
     * SERVERSIDE
     *
     * @param pEvent
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent pEvent) {
        if (pEvent.player instanceof EntityPlayerMP) sendClientUpdate((EntityPlayerMP) pEvent.player);
    }

    private void sendClientUpdate() {
        sendClientUpdate(null);
    }

    /**
     * SERERSIDE Send client update with changed definition. set pPlayer to null to send to everyone on the server
     *
     * @param pPlayer
     */
    private void sendClientUpdate(EntityPlayerMP pPlayer) {
        String tPayload = getClientSideXMLStream();
        if (!tPayload.isEmpty()) {
            if (pPlayer != null && pPlayer instanceof EntityPlayerMP)
                EnhancedLootBags.NW.sendTo(new LootBagClientSyncMessage(tPayload), pPlayer);
            else if (pPlayer == null) EnhancedLootBags.NW.sendToAll(new LootBagClientSyncMessage(tPayload));
            else _mLogger.error("[LootBags.sendClientUpdate] Target is no EntityPlayer and not null");
        } else _mLogger.error("[LootBags] Unable to send update to clients; Received empty serialized object");
    }

    /**
     * CLIENTSIDE This is called when the server has sent an update
     *
     * @param pPayload
     */
    public void processServerConfig(String pPayload) {
        if (ReloadLootGroups(pPayload)) _mLogger.info("[LootBags] Received and activated configuration from server");
        else _mLogger.warn("[LootBags] Received invalid configuration from server; Not activated!");
    }

    /**
     * Get a list of all configured drops that are defined as a "ItemGroup" within the lootgroup. If no itemGroup is
     * defined, the list will only contain the given item
     *
     * @param pGrp
     * @param pSelectedDrop
     * @return
     */
    public List<Drop> getItemGroupDrops(LootGroup pGrp, Drop pSelectedDrop) {
        List<Drop> tGrp = new ArrayList<Drop>();

        if (pSelectedDrop.getItemDropGroup().isEmpty()) tGrp.add(pSelectedDrop);
        else {
            for (Drop tDr : pGrp.getDrops())
                if (tDr.getItemDropGroup().equalsIgnoreCase(pSelectedDrop.getItemDropGroup())) tGrp.add(tDr);

            if (tGrp.isEmpty()) // Just in case something REALLY derpy happens..
                tGrp.add(pSelectedDrop);
        }
        return tGrp;
    }

    /**
     * Creates a fake Array of ItemStacks for given LootGroupID This should only execute on the SERVER thread
     *
     * @param pLootGroupID
     * @return
     */
    public ItemStack[] createFakeInventoryFromID(int pLootGroupID, int pSlotCount) {
        ItemStack[] tList = new ItemStack[pSlotCount];
        int i = 0;
        try {
            LootGroup lg = getGroupByID(pLootGroupID);
            if (lg != null) {
                for (Drop dr : lg.getDrops()) {
                    if (i < pSlotCount) {
                        ItemStack tPendingStack = dr.getItemStack();
                        if (tPendingStack == null) {
                            _mLogger.error(
                                    String.format(
                                            "Unable to generate ItemStack for drop ID %s (%s)",
                                            dr.getIdentifier(),
                                            dr.getItemName()));
                            continue;
                        }
                        addDropInformationNBT(tPendingStack, dr, pLootGroupID);
                        tList[i] = tPendingStack;
                        i++;
                    } else {
                        _mLogger.warn(
                                String.format(
                                        "Warning: LootBagID %d contains more items than the GUI can currently display! (%d) The result will be truncated",
                                        pLootGroupID,
                                        pSlotCount));
                        break;
                    }
                }
            } else _mLogger.error("LootGroup for ID %d returned null", pLootGroupID);
        } catch (Exception e) {
            _mLogger.error("Unable to build Itemlist for Lootbag GUI");
            e.printStackTrace();
        }
        return tList;
    }

    public static int recalcWeightByFortune(int pOldWeight, int pFortuneLevel) {
        if (pFortuneLevel > 0) {
            if (pFortuneLevel < 3) {
                return pOldWeight - (int) Math.floor((double) pOldWeight * (0.33D * (double) pFortuneLevel));
            } else {
                return 0;
            }
        }
        return pOldWeight;
    }

    private static String NBT_COMPOUND_LOOTBAGINFO = "LootBagDrop";
    private static String NBT_S_DROP_ID = "LBDID";
    private static String NBT_S_DROP_ITGROUP = "LBDGroup";
    private static String NBT_I_DROP_AMOUNT = "LBDAmount";
    private static String NBT_I_DROP_LIMIT = "LBDLimit";
    private static String NBT_I_DROP_WEIGHT = "LBDWeight";
    private static String NBT_B_DROP_ISRND = "LBDRnd";

    private static String NBT_B_MERGETRASH = "LBDTrash";
    private static String NBT_D_DROPCHANCE_F0 = "LBDCF0";
    private static String NBT_D_DROPCHANCE_F1 = "LBDCF1";
    private static String NBT_D_DROPCHANCE_F2 = "LBDCF2";
    private static String NBT_D_DROPCHANCE_F3 = "LBDCF3";

    /**
     * Add NBT Information about this Item to the FakeStack, to disaply detailed information on the client
     *
     * @param pStack
     * @param pDrop
     */
    private void addDropInformationNBT(ItemStack pStack, Drop pDrop, int pBagID) {
        NBTTagCompound tTag = pStack.getTagCompound();
        if (tTag == null) tTag = new NBTTagCompound();

        LootGroup tItemGroup = getGroupByID(pBagID);

        NBTTagCompound tLootTag = tTag.getCompoundTag(NBT_COMPOUND_LOOTBAGINFO);

        tLootTag.setString(NBT_S_DROP_ID, pDrop.getIdentifier());
        tLootTag.setString(
                NBT_S_DROP_ITGROUP,
                (pDrop.getItemDropGroup().isEmpty()) ? "- no group -" : pDrop.getItemDropGroup());
        tLootTag.setInteger(NBT_I_DROP_AMOUNT, pDrop.getAmount());
        tLootTag.setInteger(NBT_I_DROP_LIMIT, pDrop.getLimitedDropCount());
        tLootTag.setInteger(NBT_I_DROP_WEIGHT, pDrop.getChance());
        tLootTag.setBoolean(NBT_B_DROP_ISRND, pDrop.getIsRandomAmount());
        tLootTag.setBoolean(NBT_B_MERGETRASH, tItemGroup.getCombineWithTrash());
        tLootTag.setDouble(NBT_D_DROPCHANCE_F0, calcPercentageFromWeight(pDrop, tItemGroup, FortuneLevel.LV0));
        tLootTag.setDouble(NBT_D_DROPCHANCE_F1, calcPercentageFromWeight(pDrop, tItemGroup, FortuneLevel.LV1));
        tLootTag.setDouble(NBT_D_DROPCHANCE_F2, calcPercentageFromWeight(pDrop, tItemGroup, FortuneLevel.LV2));
        tLootTag.setDouble(NBT_D_DROPCHANCE_F3, calcPercentageFromWeight(pDrop, tItemGroup, FortuneLevel.LV3));

        tTag.setTag(NBT_COMPOUND_LOOTBAGINFO, tLootTag);
        pStack.setTagCompound(tTag);
    }

    public double calcPercentageFromWeight(Drop drop, LootGroup lootGroup, FortuneLevel fortuneLevel) {
        return calcPercentageFromWeight(
                drop.getChance(),
                lootGroup.getMaxWeight() + getTrashWeight(lootGroup, fortuneLevel));
    }

    private int getTrashWeight(LootGroup lootGroup, FortuneLevel fortuneLevel) {
        if (lootGroup.getGroupID() == 0 || !lootGroup.getCombineWithTrash()) return 0;

        LootGroup trashGroup = getGroupByID(lootGroup.getTrashGroup());
        int trashWeight = trashGroup.getMaxWeight();
        return recalcWeightByFortune(trashWeight, fortuneLevel.level);
    }

    private static double calcPercentageFromWeight(double pItemWeight, double pTotalWeight) {
        double tRet = 0.0D;

        tRet = 100.0D / pTotalWeight * pItemWeight;

        tRet = (double) Math.round(tRet * 100) / 100;

        return tRet;
    }

    @SubscribeEvent
    public void onToolTip(ItemTooltipEvent pEvent) {
        ItemStack tStack = pEvent.itemStack;
        if (tStack != null) {
            NBTTagCompound tComp = tStack.getTagCompound();
            if (tComp != null) {
                NBTTagCompound tDropInfo = tComp.getCompoundTag(NBT_COMPOUND_LOOTBAGINFO);
                if (!tDropInfo.hasNoTags()) {
                    List<String> tToolTipInfo = new ArrayList<String>();
                    tToolTipInfo.add(" ");
                    tToolTipInfo.add(getFrmStr("__b__6 == Drop Information == __r"));
                    tToolTipInfo.add(getFrmStr(String.format("__lDropID :__r %s", tDropInfo.getString(NBT_S_DROP_ID))));
                    tToolTipInfo.add(
                            getFrmStr(String.format("__lAmount :__r %d", tDropInfo.getInteger(NBT_I_DROP_AMOUNT))));
                    tToolTipInfo
                            .add(getFrmStr(String.format("__lRandom :__r %b", tDropInfo.getBoolean(NBT_B_DROP_ISRND))));
                    tToolTipInfo
                            .add(getFrmStr(String.format("__lLimit  :__r %d", tDropInfo.getInteger(NBT_I_DROP_LIMIT))));
                    tToolTipInfo.add(
                            getFrmStr(String.format("__lWeight :__r %d", tDropInfo.getInteger(NBT_I_DROP_WEIGHT))));
                    tToolTipInfo.add(
                            getFrmStr(String.format("__lIGroup :__r %s", tDropInfo.getString(NBT_S_DROP_ITGROUP))));
                    tToolTipInfo.add(getFrmStr("__b__6 == Trash/Fortune Behavior == __r"));
                    tToolTipInfo.add(
                            getFrmStr(
                                    String.format(
                                            "__lMerges w Trash   :__r %b",
                                            tDropInfo.getBoolean(NBT_B_MERGETRASH))));
                    tToolTipInfo.add(
                            getFrmStr(
                                    String.format(
                                            "__lDrop %% (F0/1/2/3):__r %.2f | %.2f | %.2f | %.2f",
                                            tDropInfo.getDouble(NBT_D_DROPCHANCE_F0),
                                            tDropInfo.getDouble(NBT_D_DROPCHANCE_F1),
                                            tDropInfo.getDouble(NBT_D_DROPCHANCE_F2),
                                            tDropInfo.getDouble(NBT_D_DROPCHANCE_F3))));

                    pEvent.toolTip.addAll(tToolTipInfo);
                }
            }
        }
    }

    private String getFrmStr(String pSource) {
        return TextFormatHelper.DecodeStringCodes(pSource);
    }

    public enum FortuneLevel {

        LV0(0),
        LV1(1),
        LV2(2),
        LV3(3);

        public final int level;

        FortuneLevel(int level) {
            this.level = level;
        }
    }
}
