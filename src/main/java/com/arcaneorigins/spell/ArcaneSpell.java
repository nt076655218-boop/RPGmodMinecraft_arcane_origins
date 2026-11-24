package com.arcaneorigins.spell;

public enum ArcaneSpell {
    // ================= 1. ШКОЛА ЛЬДА (5 уникальных заклинаний) =================
    GLACIAL_LANCE(
            "glacial_lance",
            "Ледяное Копьё",
            SpellSchool.ICE,
            1, 16.0f, 25,
            "Дальний спиральный луч, намертво вмораживающий цель в глыбу льда как статую."
    ),
    FROST_NOVA(
            "frost_nova",
            "Кольцо Мерзлоты",
            SpellSchool.ICE,
            2, 28.0f, 95,
            "Мгновенный взрыв мороза вокруг мага — вмораживает всех ближних врагов в лёд."
    ),
    GLACIAL_PRISON_WALL(
            "glacial_prison_wall",
            "Ледяная Стена",
            SpellSchool.ICE,
            2, 26.0f, 110,
            "Возводит перед магом барьер из ледяных глыб с острыми шипами, отбрасывающий врагов."
    ),
    FROST_SPIKE_WAVE(
            "frost_spike_wave",
            "Волна Ледяных Шипов",
            SpellSchool.ICE,
            2, 26.0f, 90,
            "Гряда вырастающих из земли острых ледяных пик, пронзающих и подбрасывающих врагов на пути."
    ),
    BLIZZARD_TEMPEST(
            "blizzard_tempest",
            "Ледяная Буря",
            SpellSchool.ICE,
            3, 45.0f, 165,
            "Арктический смерч в указанной зоне — затягивает в центр, рубит осколками и вмораживает."
    ),

    // ================= 2. ШКОЛА МОЛНИИ (5 уникальных заклинаний) =================
    ARC_LIGHTNING(
            "arc_lightning",
            "Дуга Молнии",
            SpellSchool.LIGHTNING,
            1, 18.0f, 28,
            "Фрактальная цепная молния, перескакивающая по 4 врагам подряд."
    ),
    THUNDERSTEP(
            "thunderstep",
            "Грозовой Рывок",
            SpellSchool.LIGHTNING,
            2, 24.0f, 75,
            "Мгновенный телепорт сквозь разряд молнии со взрывной волной на точке выхода."
    ),
    STORM_ORB(
            "storm_orb",
            "Шаровая Молния",
            SpellSchool.LIGHTNING,
            2, 32.0f, 115,
            "Медленно плывущая плазменная сфера, бьющая током всех рядом каждые 0.4 сек."
    ),
    SHOCKWAVE_NOVA(
            "shockwave_nova",
            "Электромагнитный Импульс",
            SpellSchool.LIGHTNING,
            2, 28.0f, 95,
            "Радиальный грозовой разряд вокруг мага — оглушает, отталкивает и бьёт током всех рядом."
    ),
    TEMPEST_JUDGMENT(
            "tempest_judgment",
            "Гнев Небес",
            SpellSchool.LIGHTNING,
            3, 50.0f, 195,
            "Формирует грозовую тучу над землёй и бьёт 4 ветвистыми молниями прямо из облака."
    ),

    // ================= 3. ШКОЛА ГРАВИТАЦИИ И ПУСТОТЫ (5 уникальных заклинаний) =================
    VOID_RIFT(
            "void_rift",
            "Врата Пустоты",
            SpellSchool.GRAVITY,
            1, 22.0f, 55,
            "Открывает прямо перед магом вертикальный портал-разлом Бездны, затягивающий врагов внутрь с уроном и Иссушением."
    ),
    GRAVITY_WELL(
            "gravity_well",
            "Гравитационный Колодец",
            SpellSchool.GRAVITY,
            2, 34.0f, 120,
            "Создаёт сингулярность чёрной дыры, затягивающую врагов в центр с финальным схлопыванием."
    ),
    ZERO_G_SLAM(
            "zero_g_slam",
            "Инверсия Гравитации",
            SpellSchool.GRAVITY,
            2, 30.0f, 110,
            "Подбрасывает врагов вокруг в невесомость, а затем с силой впечатывает их в землю."
    ),
    VOID_PULL(
            "void_pull",
            "Гравитационный Импульс",
            SpellSchool.GRAVITY,
            2, 25.0f, 80,
            "Сжимает пространство конусом перед магом и мгновенно притягивает всех врагов к себе, оглушая их."
    ),
    ABYSSAL_METEOR(
            "abyssal_meteor",
            "Коллапс Пустоты",
            SpellSchool.GRAVITY,
            3, 52.0f, 190,
            "Обрушивает сгусток тёмной материи из небесного разлома с мощнейшей гравитационной волной."
    ),

    // ================= 4. ШКОЛА ОГНЯ (5 уникальных заклинаний) =================
    FIREBALL(
            "fireball",
            "Огненный Шар",
            SpellSchool.FIRE,
            1, 22.0f, 34,
            "Запускает настоящий летящий огненный шар с хвостом пламени и искрами, взрывающийся при попадании!"
    ),
    FIRE_TORNADO(
            "fire_tornado",
            "Огненный Смерч",
            SpellSchool.FIRE,
            2, 32.0f, 115,
            "Призывает огненное торнадо прямо на маге — несколько секунд следует за ним, сжигая и подбрасывая врагов!"
    ),
    PLASMA_BLAST(
            "plasma_blast",
            "Взрыв Плазмы",
            SpellSchool.FIRE,
            2, 28.0f, 90,
            "Выпускает широкий конус раскалённого пламени прямо перед магом, сжигая и отбрасывая врагов."
    ),
    ERUPTION_PILLARS(
            "eruption_pillars",
            "Извержение Гейзеров",
            SpellSchool.FIRE,
            2, 34.0f, 105,
            "Поджигает землю под врагами в радиусе 8 блоков, вызывая 4 взрывных огненных столба из-под земли."
    ),
    METEOR_SHOWER(
            "meteor_shower",
            "Метеоритный Дождь",
            SpellSchool.FIRE,
            3, 52.0f, 195,
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
