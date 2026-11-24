package com.arcaneorigins.spell;

public enum ArcaneSpell {
    GLACIAL_LANCE(
            "glacial_lance",
            "Ледяное Копьё",
            SpellSchool.ICE,
            1,
            15.0f,
            24,
            "Пронзающая ледяная спираль, сковывающая врагов в глыбу льда."
    ),
    FROST_NOVA(
            "frost_nova",
            "Кольцо Мерзлоты",
            SpellSchool.ICE,
            2,
            30.0f,
            100,
            "Волна глубокого мороза, замораживающая всех врагов вокруг в ледяные кристаллы."
    ),
    BLIZZARD_TEMPEST(
            "blizzard_tempest",
            "Ледяная Буря",
            SpellSchool.ICE,
            3,
            45.0f,
            160,
            "Призывает на землю арктический смерч из снега и осколков льда, замораживающий врагов."
    ),
    ARC_LIGHTNING(
            "arc_lightning",
            "Дуга Молнии",
            SpellSchool.LIGHTNING,
            1,
            18.0f,
            30,
            "Фрактальная молния, перескакивающая цепным разрядом до 4 врагов."
    ),
    THUNDERSTEP(
            "thunderstep",
            "Грозовой Рывок",
            SpellSchool.LIGHTNING,
            2,
            25.0f,
            80,
            "Мгновенный перенос сквозь разряд молнии с громовой волной при приземлении."
    ),
    TEMPEST_JUDGMENT(
            "tempest_judgment",
            "Гнев Небес",
            SpellSchool.LIGHTNING,
            3,
            50.0f,
            200,
            "Формирует грозовое облако в небе и обрушивает 4 молнии точно на землю под прицелом."
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
