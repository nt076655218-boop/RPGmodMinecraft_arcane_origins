package com.arcaneorigins.classes;

import com.arcaneorigins.spell.ArcaneSpell;
import java.util.Arrays;
import java.util.List;

/**
 * Class Progression Tree Nodes (Unlocked with Skill Points earned per class level!)
 */
public enum ClassPerk {
    // ================= ВОИН (WARRIOR) =================
    WAR_IRON_SKIN("war_iron_skin", RpgClass.WARRIOR, 1, "Железная Кожа",
            "+3 к броне и +4 к макс. здоровью.", null),
    WAR_WEAPON_MASTERY("war_weapon_mastery", RpgClass.WARRIOR, 2, "Мастерство Оружия",
            "+2.5 к урону в ближнем бою и +12% к скорости атаки.", null),
    WAR_SECOND_WIND("war_second_wind", RpgClass.WARRIOR, 3, "Второе Дыхание",
            "Пассивная регенерация в бою (Восстановление I каждые 5 сек) и +4 HP.", null),
    WAR_EXECUTIONER("war_executioner", RpgClass.WARRIOR, 4, "Сокрушительный Удар",
            "+35% критического урона по раненым врагам (ниже 50% HP).", null),
    WAR_WARLORD("war_warlord", RpgClass.WARRIOR, 5, "Полководец",
            "+4 к броне, +6 к макс. HP и +30% сопротивление отбрасыванию.", null),

    // ================= ВАРВАР / БЕРСЕРК (BARBARIAN) =================
    BARB_THICK_HIDE("barb_thick_hide", RpgClass.BARBARIAN, 1, "Дублёная Шкура",
            "+6 к макс. здоровью и +2 к природной броне.", null),
    BARB_BLOODLUST("barb_bloodlust", RpgClass.BARBARIAN, 2, "Жажда Крови",
            "Убийство врага восстанавливает 4 HP и даёт Ярость (Сила II на 5 сек).", null),
    BARB_SEISMIC_SLAM("barb_seismic_slam", RpgClass.BARBARIAN, 3, "Сейсмический Удар",
            "Прыжковые атаки вызывают ударную волну, подбрасывающую врагов вокруг.", null),
    BARB_UNYIELDING("barb_unyielding", RpgClass.BARBARIAN, 4, "Неудержимый",
            "+18% к скорости бега, иммунитет к замедлению и +3 к урону.", null),
    BARB_TITAN_RAGE("barb_titan_rage", RpgClass.BARBARIAN, 5, "Гнев Титана",
            "+8 к макс. здоровью, +4 к урону и постоянный эффект Сопротивления I.", null),

    // ================= ЧАРОДЕЙ (SORCERER) =================
    SORC_ARCANE_PULSE("sorc_arcane_pulse", RpgClass.SORCERER, 1, "Пробуждение Искры",
            "Открывает [Дуга Молнии] и [Врата Пустоты], +25 макс. маны.", ArcaneSpell.ARC_LIGHTNING),
    SORC_TWIN_FLOW("sorc_twin_flow", RpgClass.SORCERER, 2, "Двойной Поток",
            "Открывает [Огненный Смерч] и [Кольцо Мерзлоты], +15% к силе всех чар.", ArcaneSpell.FIRE_TORNADO),
    SORC_OVERCHARGE("sorc_overcharge", RpgClass.SORCERER, 3, "Перегрузка Эфира",
            "Открывает [Шаровая Молния] и [Гравитационный Колодец], -15% расход маны.", ArcaneSpell.STORM_ORB),
    SORC_METAMAGIC("sorc_metamagic", RpgClass.SORCERER, 4, "Метамагия: Ускорение",
            "Открывает [Инверсия Гравитации] и [Взрыв Плазмы], -20% откат заклинаний.", ArcaneSpell.ZERO_G_SLAM),
    SORC_APOTHEOSIS("sorc_apotheosis", RpgClass.SORCERER, 5, "Апофеоз Чародея",
            "Открывает [Метеоритный Дождь] и [Коллапс Пустоты], +50 макс. маны.", ArcaneSpell.METEOR_SHOWER),

    // ================= КОЛДУН (WARLOCK) =================
    LOCK_PACT_BOON("lock_pact_boon", RpgClass.WARLOCK, 1, "Тёмный Обет",
            "Открывает [Гравитационный Колодец] и +15 к Благосклонности.", ArcaneSpell.GRAVITY_WELL),
    LOCK_SOUL_SIPHON("lock_soul_siphon", RpgClass.WARLOCK, 2, "Пожиратель Душ",
            "Открывает [Огненный Смерч]. Убийство врага даёт +25 маны и +3 HP.", ArcaneSpell.FIRE_TORNADO),
    LOCK_ELDRITCH_SIGHT("lock_eldritch_sight", RpgClass.WARLOCK, 3, "Взор Бездны",
            "Открывает [Притяжение Бездны] и [Ударная Волна], +35 макс. маны.", ArcaneSpell.VOID_PULL),
    LOCK_ABYSSAL_ARMOR("lock_abyssal_armor", RpgClass.WARLOCK, 4, "Доспех Покровителя",
            "Открывает [Инверсия Гравитации], +4 к броне и +25% к силе чар.", ArcaneSpell.ZERO_G_SLAM),
    LOCK_HERALD_DOOM("lock_herald_doom", RpgClass.WARLOCK, 5, "Вестник Рока",
            "Открывает [Коллапс Пустоты] и [Гнев Небес], макс. Благосклонность 100.", ArcaneSpell.ABYSSAL_METEOR),

    // ================= ВОЛШЕБНИК (WIZARD) =================
    WIZ_SCHOLAR("wiz_scholar", RpgClass.WIZARD, 1, "Эрудиция Архимага",
            "+35 к макс. мане и +25% к скорости регенерации маны.", null),
    WIZ_SPELL_MASTERY("wiz_spell_mastery", RpgClass.WIZARD, 2, "Отточенные Формулы",
            "+18% к урону всех заклинаний и -15% к расходу маны.", null),
    WIZ_ARCANE_WARD("wiz_arcane_ward", RpgClass.WIZARD, 3, "Магический Барьер",
            "+4 к магической броне и +4 к макс. здоровью.", null),
    WIZ_QUICK_CHANT("wiz_quick_chant", RpgClass.WIZARD, 4, "Беглое Чтение",
            "-20% к времени перезарядки всех заклинаний.", null),
    WIZ_ARCHMAGE("wiz_archmage", RpgClass.WIZARD, 5, "Верховный Архимаг",
            "+60 к макс. мане и +25% к силе всех школ магии.", null);

    private final String id;
    private final RpgClass rpgClass;
    private final int requiredLevel;
    private final String title;
    private final String description;
    private final ArcaneSpell primarySpell;

    ClassPerk(String id, RpgClass rpgClass, int requiredLevel, String title, String description, ArcaneSpell primarySpell) {
        this.id = id;
        this.rpgClass = rpgClass;
        this.requiredLevel = requiredLevel;
        this.title = title;
        this.description = description;
        this.primarySpell = primarySpell;
    }

    public String getId() { return id; }
    public RpgClass getRpgClass() { return rpgClass; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ArcaneSpell getPrimarySpell() { return primarySpell; }

    public static List<ClassPerk> forClass(RpgClass c) {
        return Arrays.stream(values()).filter(p -> p.rpgClass == c).toList();
    }

    public static ClassPerk byId(String id) {
        for (ClassPerk p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }
}
