package com.arcaneorigins.spell;

public enum ArcaneSpell {
    GLACIAL_LANCE(
            "glacial_lance",
            "Ледяное Копьё",
            SpellSchool.ICE,
            15.0f,
            24,
            "Запускает пронзающую спираль льда, замораживая и отбрасывая врагов."
    ),
    FROST_NOVA(
            "frost_nova",
            "Кольцо Мерзлоты",
            SpellSchool.ICE,
            30.0f,
            100,
            "Создаёт руническую печать под ногами и мощную волну заморозки вокруг."
    ),
    BLIZZARD_TEMPEST(
            "blizzard_tempest",
            "Ледяная Буря",
            SpellSchool.ICE,
            45.0f,
            160,
            "Призывает 3-секундный ледяной вихрь в точке взгляда, стягивающий врагов."
    ),
    ARC_LIGHTNING(
            "arc_lightning",
            "Дуга Молнии",
            SpellSchool.LIGHTNING,
            18.0f,
            30,
            "Выпускает фрактальную молнию, перескакивающую до 4 целей цепным разрядом."
    ),
    THUNDERSTEP(
            "thunderstep",
            "Грозовой Рывок",
            SpellSchool.LIGHTNING,
            25.0f,
            80,
            "Мгновенный рывок сквозь молнию с оглушающей ударной волной грома."
    ),
    TEMPEST_JUDGMENT(
            "tempest_judgment",
            "Гнев Небес",
            SpellSchool.LIGHTNING,
            50.0f,
            200,
            "Вызывает грозовой круг и серию небесных разрядов в выбранной области."
    );

    private final String id;
    private final String displayName;
    private final SpellSchool school;
    private final float manaCost;
    private final int cooldownTicks;
    private final String description;

    ArcaneSpell(String id, String displayName, SpellSchool school, float manaCost, int cooldownTicks, String description) {
        this.id = id;
        this.displayName = displayName;
        this.school = school;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public SpellSchool getSchool() { return school; }
    public float getManaCost() { return manaCost; }
    public int getCooldownTicks() { return cooldownTicks; }
    public String getDescription() { return description; }

    public static ArcaneSpell byIndex(int idx) {
        ArcaneSpell[] all = values();
        if (all.length == 0) return GLACIAL_LANCE;
        int safe = ((idx % all.length) + all.length) % all.length;
        return all[safe];
    }
}
