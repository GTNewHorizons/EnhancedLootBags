# EnhancedLootBags
LootBags redefined. 

# Copyright / License / Modpack usage
You may use this mod in any modpack you want to. You may also fork this repository and continue to develop this mod to cover your own ideas. Pullrequests are welcome.

# How to use Lootbag Commands

I copy past this from The old Forum.

Orginal Post by Namikon.


You have 4 options to manipulate lootgroups and items ingame, as described below.
You can also edit the file with any editor; But make sure you check the syntax by loading the finished file in your client.
Due to the nature of how the lootbags are loaded, it is currently not possible to reload those configuration files; A restart is required.
Also, even if the command to add groups will work on a server, the client's won't get notified about that change, and thus the new lootbags won't be available.
You can however add loot to an existing list; But it's not recommended to do so, as client and server will be out of sync

You can edit the file on the server, and just reload it. It will be pushed to all clients. This also includes new and removed groups.
So yes, you can actually add new items to the game, while it is running

To reload, type
/lbag reload

Loot Items:
There is no limit in how many items you can add to a group. The more items are added, the more the weight-values will affect the dropchance
A warning: The itemDrop identifier that is generated must be unique. And once you've defined it, (and used in a real environment) you should never change
this ID, as all player progress will be lost if you do.

/lbag addloot <LootGroup ID>
- Adds the current item you hold in your hand to the lootgroup with given ID

/lbag addloot <LootGroup ID> <ItemAmount> <Chance> <Limited Drop Count> <RandomAmount>
- Same as the first command, but instead lets you define more parameters than the first command

Args:
<LootGroup ID> - Can be any number between 1 and 32767
The unique ID of the lootgroup you wish to add the item to

<ItemAmount> - Can be any number between 1 and 64
The amount of items that the player should receive if this item is selected

<Chance> - Can be any number between 1 and 255
The chance that the player will receive this loot. It's a weight-value. A higher number is more likely to drop than a lower one

<Limited Drop Count> - Can be any number between 0 and 255
This lets you limit the number of times a player can receive this loot.
If you set this to 5, the player may receive this item up to 5 times. After that, the item will never drop again.
Set this value to 0 to disable this function

<RandomAmount> - Can be either 0 or 1 (Boolean value)
If enabled, the player won't receive the full amount of items specified in <ItemAmount>; But instead a random number bewteen 1 and <ItemAmount>
Set to 0 to disable, 1 to enable.

LootGroups:
Lootgroups define how many LootBags there will be available ingame. The ID you define here must be unique, or the module won't load up.
The group ID is basically the metaID the lootbags will have; So if the lootbag has item ID 123, and the group ID is set to 5, the final ingame-item
that will spawn items for that group will have the full ID 123:5. That's why you can't set a groupID below 1, or above 32767.
A warning: Once you've defined a group ID, never change it. While lootbags won't cause corrupted worlds or crashes, recorded itemdrops are lost, as the GroupID is
used to keep track of a players progress.
As of FILL_IN_VERSION, you can localize group names of lootbags. For example, fill in `GroupName`s with full localization keys, and provide your own lang file and load it with mods like TXLoader. If it's not found, the raw GroupName is displayed.

/lbag addgroup <Group ID>
- Adds a new group with given ID as unique Identifier

/lbag addgroup <Group ID> <Rarity> <MinItems> <MaxItems>
- Same as the first command, but with more control about parameters

Args:
<Group ID> - Can be any number between 1 and 32767
The unique groupID. Make sure you read the description!

<Rarity> - A number between 0 and 3, where 0 is common and 3 is epic
Defines the ingame rarity. That is used to display the itemname. Defines the textcolor of the item

<MinItems> - Any number between 1 and 16
Must be lower or equal than MaxItems. Defines the minimum amount of items a lootbag will generate per opening.
The number is choosen randomly between min and max

<MaxItems> - Any number between 1 and 16
Must be higher or equal than MinItems. Defines the maximum amount of items a lootbag will generate per opening.
The number is choosen randomly between min and max



A few small changes happend in v1.1.22:

New command "/lbag addinventory <groupID>"
Pretty obvious what it does; It dumps your entire inventory into loot-group <groupID>. No excuses anymore for "This takes so much time" Dodgy

GroupID 0 is now the generic lootgroup. The lootbag with metaID 0 is hidden from NEI, but still available via creative menu.
Every Item in this lootgroup will drop in all defined bags. This group is meant for "generic" / "trash" items, to make the actual good loots in the bags a bit more rare.
In order to calculate the rarity of your specific item, you have to add both the generic group 0 and your own groups weight-chances, as this is how it is done on the server.

Also, a pretty obvious fact, never add anything rare to this group; As these group 0 items might also drop for "Basic / Tier 0" bags. And you don't want a beginner to run around with netherstars, do you?

Scheduled for v1.1.23:
Groups will have an additional boolean switch value to disable generic loot. This is meant for "- unique" bags; As they really should only contain unique loot, and given to players in special cases



Finalized LootBag system implemented.

v1.1.25 contains the following, finalized changes:

LootBag GUI
Enter creative mode and Shift-Right click any LootBag you wish to inspect. A GUI will open with all possible drops for this particular Bag.
This will not contain trash loot. Only those items defined in the group will be shown.
The GUI will show enhanced tooltips if you hover the items, to make it easier to find the correct entry in the XML file, if you want to change something.
The GUI does not have any ability to interact with those items. You can't pull them out, nor can you place items in. Also normal players are unable to open that Interface

Automatic Trash-Group combination
Every LootGroup will be merged with the so called "TrashGroup" (Group ID 0); And a random loot will be chosen from the combined list.
You can enable/disable this behaviour by setting CombineTrashGroup to true or false in the group definition. Example:

Code:
<LootGroup GroupMetaID="14" GroupName="Legendary" Rarity="3" MinItems="1" MaxItems="1" CombineTrashGroup="true"/>
<LootGroup GroupMetaID="15" GroupName="Legendary - Unique" Rarity="3" MinItems="1" MaxItems="1" CombineTrashGroup="false"/>

Lootgroup 14 will have a combined loottable of GroupID 0 and 14, where Group 15 will only have its own list.
Keep that in mind when you selecting the weight-chances for your drop

ItemGroup or so called "Combined drops"
You can set the parameter ItemGroup for any drop. The default setting is empty, which means this drop is considered as a single drop.
If you wish to have multiple items to drop together, combine them like so:

<Loot Identifier="1" ItemName="Thaumcraft:ItemShard:1" Amount="8" NBTTag="" Chance="25" ItemGroup="basicshards" LimitedDropCount="0" RandomAmount="false"/>
<Loot Identifier="2" ItemName="Thaumcraft:ItemShard:2" Amount="8" NBTTag="" Chance="25" ItemGroup="basicshards" LimitedDropCount="0" RandomAmount="false"/>
<Loot Identifier="3" ItemName="Thaumcraft:ItemShard:3" Amount="8" NBTTag="" Chance="25" ItemGroup="basicshards" LimitedDropCount="0" RandomAmount="false"/>
<Loot Identifier="4" ItemName="Thaumcraft:ItemShard:4" Amount="8" NBTTag="" Chance="25" ItemGroup="basicshards" LimitedDropCount="0" RandomAmount="false"/>
<Loot Identifier="5" ItemName="Thaumcraft:ItemShard:5" Amount="8" NBTTag="" Chance="25" ItemGroup="basicshards" LimitedDropCount="0" RandomAmount="false"/>

What this does is:
If the random selection algorithm chooses one of those drops (ID 1 - 5), it will detect the ItemGroup setting basicshards. It will now search for every item with the same ItemGroup setting in the same Group, or the trash group (Remember: Groups might be merged with the trash group, thus the entire configuration will follow!).
Each individual item will still follow the rules for LimitedDropCount. Let's say you want 5 items to always drop together, but one of those items should only drop once. Then just set the LimitedDropCount to 1 (Or the amount if drops which are allowed)


So far. If something is missing, ask me please.
