package com.arcaneorigins.spell;

public enum ArcaneSpell {
    // ================= 1. ШКОЛА ЛЬДА (4 уникальные механики) =================
    GLACIAL_LANCE(
            "glacial_lance",
            "Ледяное Копьё",
            SpellSchool.ICE,
            1,
            16.0f,
            25,
            "Дальний спиральный луч, намертво вмораживающий цель в глыбу льда как статую."
    ),
    FROST_NOVA(
            "frost_nova",
            "Кольцо Мерзлоты",
            SpellSchool.ICE,
            2,
            28.0f,
            95,
            "Мгновенный взрыв мороза вокруг мага — вмораживает всех ближних врагов в лёд."
    ),
    GLACIAL_PRISON_WALL(
            "glacial_prison_wall",
            "Ледяная Стена",
            SpellSchool.ICE,
            2,
            26.0f,
            110,
            "Возводит перед магом барьер из ледяных глыб с острыми шипами, отбрасывающий врагов."
    ),
    BLIZZARD_TEMPEST(
            "blizzard_tempest",
            "Ледяная Буря",
            SpellSchool.ICE,
            3,
            45.0f,
            165,
            "Арктический смерч в указанной зоне — затягивает в центр, рубит осколками и вмораживает."
    ),

    // ================= 2. ШКОЛА МОЛНИИ (4 уникальные механики) =================
    ARC_LIGHTNING(
            "arc_lightning",
            "Дуга Молнии",
            SpellSchool.LIGHTNING,
            1,
            18.0f,
            28,
            "Фрактальная цепная молния, перескакивающая по 4 врагам подряд."
    ),
    THUNDERSTEP(
            "thunderstep",
            "Грозовой Рывок",
            SpellSchool.LIGHTNING,
            2,
            24.0f,
            75,
            "Мгновенный телепорт сквозь разряд молнии со взрывной волной на точке выхода."
    ),
    STORM_ORB(
            "storm_orb",
            "Шаровая Молния",
            SpellSchool.LIGHTNING,
            2,
            32.0f,
            115,
            "Выпускает медленно летящую плазменную сферу, бьющую током всех вокруг каждые 0.4 сек."
    ),
    TEMPEST_JUDGMENT(
            "tempest_judgment",
            "Гнев Небес",
            SpellSchool.LIGHTNING,
            3,
            50.0f,
            195,
            "Формирует грозовую тучу над землёй и бьёт 4 концентрированными молниями прямо из облака."
    ),

    // ================= 3. ШКОЛА ГРАВИТАЦИИ И ПУСТОТЫ (4 уникальные механики) =================
    VOID_RIFT(
            "void_rift",
            "Разлом Пустоты",
            SpellSchool.GRAVITY,
            1,
            20.0f,
            35,
            "Разрезает пространство тёмной трещиной по линии вперёд, нанося урон и накладывая Иссушение."
    ),
    GRAVITY_WELL(
            "gravity_well",
            "Гравитационный Колодец",
            SpellSchool.GRAVITY,
            2,
            34.0f,
            120,
            "Создаёт сингулярность чёрной дыры, затягивающую всех врагов в центр с финальным схлопыванием."
    ),
    ZERO_G_SLAM(
            "zero_g_slam",
            "Инверсия Гравитации",
            SpellSchool.GRAVITY,
            2,
            30.0f,
            110,
            "Подбрасывает врагов вокруг в невесомость, а затем с силой впечатывает их в землю."
    ),
    ABYSSAL_METEOR(
            "abyssal_meteor",
            "Коллапс Пустоты",
            SpellSchool.GRAVITY,
            3,
            52.0f,
            190,
            "Обрушивает сгусток тёмной материи из разлома с мощнейшей гравитационной ударной волной."
    ),

    // ================= 4. ШКОЛА ОГНЯ (4 уникальные механики) =================
    FIREBALL(
            "fireball",
            "Огненный Шар",
            SpellSchool.FIRE,
            1,
            22.0f,
            32,
            "Запускает пылающий сгусток пламени с огненным шлейфом, взрывающийся при попадании."
    ),
    FLAME_DASH(
            "flame_dash",
            "Огненный Рывок",
            SpellSchool.FIRE,
            2,
            24.0f,
            75,
            "Стремительный рывок вперёд сквозь пламя, оставляющий за магом пылающую огненную тропу."
    ),
    INFERNO_RING(
            "inferno_ring",
            "Кольцо Инферно",
            SpellSchool.FIRE,
            2,
            30.0f,
            100,
            "Огненный вихрь вокруг мага — отбрасывает, поджигает и сжигает врагов в радиусе 6 блоков."
    ),
    METEOR_SHOWER(
            "meteor_shower",
            "Метеоритный Дождь",
            SpellSchool.FIRE,
            3,
            52.0f,
            195,
            "Обрушивает с небес 5 пылающих метеоров в указанную область с огненными взрывами."
    );

    private final String id;
    private final String displayName;
    private final SpellSchool school;
    private final int tier;
    private final float manaCost;
    private final int cooldownTicks;
    private final String description;

    ArcaneSpell(String id, String displayName, SpellSchool school, int tier, float manaCost, int cooldownTicks, String description) {
        this.id = id;
        this.displayName = displayName;
        this.school = school;
        this.tier = tier;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public SpellSchool getSchool() { return school; }
    public int getTier() { return tier; }
    public float getManaCost() { return manaCost; }
    public int getCooldownTicks() { return cooldownTicks; }
    public String getDescription() { return description; }

    public static ArcaneSpell byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (ArcaneSpell s : values()) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }
}
