package com.arcaneorigins.item;

import com.arcaneorigins.ArcaneOriginsMod;
import com.arcaneorigins.spell.ArcaneSpell;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneOriginsMod.MOD_ID);

    public static final DeferredItem<Item> ARCANE_SPELLBOOK = ITEMS.register("arcane_spellbook",
            () -> new MagicFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    "Фолиант Заклинаний: позволяет Волшебнику свободно творить изученную магию."));

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

    // 4 Ice Scrolls
    public static final DeferredItem<Item> SCROLL_GLACIAL_LANCE = ITEMS.register("scroll_glacial_lance",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ArcaneSpell.GLACIAL_LANCE));

    public static final DeferredItem<Item> SCROLL_FROST_NOVA = ITEMS.register("scroll_frost_nova",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.FROST_NOVA));

    public static final DeferredItem<Item> SCROLL_GLACIAL_WALL = ITEMS.register("scroll_glacial_wall",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.GLACIAL_PRISON_WALL));

    public static final DeferredItem<Item> SCROLL_BLIZZARD = ITEMS.register("scroll_blizzard",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), ArcaneSpell.BLIZZARD_TEMPEST));

    // 4 Lightning Scrolls
    public static final DeferredItem<Item> SCROLL_ARC_LIGHTNING = ITEMS.register("scroll_arc_lightning",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ArcaneSpell.ARC_LIGHTNING));

    public static final DeferredItem<Item> SCROLL_THUNDERSTEP = ITEMS.register("scroll_thunderstep",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.THUNDERSTEP));

    public static final DeferredItem<Item> SCROLL_STORM_ORB = ITEMS.register("scroll_storm_orb",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.STORM_ORB));

    public static final DeferredItem<Item> SCROLL_TEMPEST = ITEMS.register("scroll_tempest",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), ArcaneSpell.TEMPEST_JUDGMENT));

    // 4 Gravity & Void Scrolls
    public static final DeferredItem<Item> SCROLL_VOID_RIFT = ITEMS.register("scroll_void_rift",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ArcaneSpell.VOID_RIFT));

    public static final DeferredItem<Item> SCROLL_GRAVITY_WELL = ITEMS.register("scroll_gravity_well",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.GRAVITY_WELL));

    public static final DeferredItem<Item> SCROLL_ZERO_G_SLAM = ITEMS.register("scroll_zero_g_slam",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.ZERO_G_SLAM));

    public static final DeferredItem<Item> SCROLL_ABYSSAL_METEOR = ITEMS.register("scroll_abyssal_meteor",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), ArcaneSpell.ABYSSAL_METEOR));

    // 4 Fire Scrolls
    public static final DeferredItem<Item> SCROLL_FIREBALL = ITEMS.register("scroll_fireball",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ArcaneSpell.FIREBALL));

    public static final DeferredItem<Item> SCROLL_FLAME_DASH = ITEMS.register("scroll_flame_dash",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.FLAME_DASH));

    public static final DeferredItem<Item> SCROLL_INFERNO_RING = ITEMS.register("scroll_inferno_ring",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ArcaneSpell.INFERNO_RING));

    public static final DeferredItem<Item> SCROLL_METEOR_SHOWER = ITEMS.register("scroll_meteor_shower",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), ArcaneSpell.METEOR_SHOWER));
}
