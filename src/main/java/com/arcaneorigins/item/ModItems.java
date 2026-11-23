package com.arcaneorigins.item;

import com.arcaneorigins.ArcaneOriginsMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneOriginsMod.MOD_ID);

    public static final DeferredItem<Item> ARCANE_SPELLBOOK = ITEMS.register("arcane_spellbook",
            () -> new MagicFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    "Книга Заклинаний: позволяет Волшебнику свободно творить магию."));

    public static final DeferredItem<Item> FROST_STAFF = ITEMS.register("frost_staff",
            () -> new MagicFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    "Посох Вечного Льда: +20% к урону Льдом и -15% к откату."));

    public static final DeferredItem<Item> STORM_CATALYST = ITEMS.register("storm_catalyst",
            () -> new MagicFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    "Грозовой Фокус: +20% к урону Молнией и -15% к откату."));

    public static final DeferredItem<Item> PATRON_SIGIL = ITEMS.register("patron_sigil",
            () -> new PatronSigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> SCROLL_OF_REBIRTH = ITEMS.register("scroll_of_rebirth",
            () -> new ScrollOfRebirthItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
}
