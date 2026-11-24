package com.arcaneorigins.classes;

public enum RpgSubclass {
    // --- ВОИН ---
    KNIGHT("knight", RpgClass.WARRIOR, "Рыцарь",
            "+4 к броне, +4 макс. HP. Несокрушимый щит авангарда.", 0x94A3B8),
    BLADEMASTER("blademaster", RpgClass.WARRIOR, "Мастер клинка",
            "+18% к скорости атаки, +2 к урону в ближнем бою.", 0xF97316),
    GUARDIAN("guardian", RpgClass.WARRIOR, "Страж",
            "+2 к броне, +6 макс. HP, +40% защита от отбрасывания.", 0x64748B),

    // --- ВАРВАР ---
    BERSERKER("berserker", RpgClass.BARBARIAN, "Берсерк",
            "+3 к урону. В бою впадает в Ярость (Сила + Скорость).", 0xEF4444),
    TOTEMIC("totemic", RpgClass.BARBARIAN, "Тотемный",
            "+6 макс. HP и постоянное снижение входящего урона на 15%.", 0x10B981),
    THUNDERBREAKER("thunderbreaker", RpgClass.BARBARIAN, "Громобой",
            "+15% скорость бега. Удары бьют врагов разрядом молнии.", 0x38BDF8),

    // --- ВОЛШЕБНИК ---
    SCHOOL_ICE("school_ice", RpgClass.WIZARD, "Школа Льда",
            "Старт: Ледяное Копьё + свиток Кольцо Мерзлоты. +25% урон Льдом, -20% мана.", 0x38BDF8),
    SCHOOL_LIGHTNING("school_lightning", RpgClass.WIZARD, "Школа Молнии",
            "Старт: Дуга Молнии + свиток Грозовой Рывок. +25% урон Молнией, -20% мана.", 0x60A5FA),
    SCHOOL_GRAVITY("school_gravity", RpgClass.WIZARD, "Школа Гравитации и Пустоты",
            "Старт: Врата Пустоты + свиток Гравитационный Колодец. +25% урон Пустотой, -20% мана.", 0xA855F7),
    SCHOOL_FIRE("school_fire", RpgClass.WIZARD, "Школа Огня",
            "Старт: Огненный Шар + свиток Огненный Смерч. +25% урон Огнём, -20% мана.", 0xF97316),

    // --- КОЛДУН ---
    PATRON_ABYSS("patron_abyss", RpgClass.WARLOCK, "Покровитель Бездны",
            "+20% макс. маны. Заклинания Пустоты и удары накладывают Иссушение.", 0x7E22CE),
    PATRON_ELDER("patron_elder", RpgClass.WARLOCK, "Древний Покровитель",
            "Вампиризм (+1.5 HP за попадание чарами) и Ночное зрение.", 0x14B8A6),
    PATRON_INFERNO("patron_inferno", RpgClass.WARLOCK, "Инфернальный Покровитель",
            "Огнестойкость, +25% урон Огнём, +2 к урону и поджог врагов.", 0xEA580C),

    // --- ЧАРОДЕЙ ---
    DRACONIC("draconic", RpgClass.SORCERER, "Драконья кровь",
            "Драконья чешуя (+4 броня, +4 HP, огнестойкость) и +20% макс. маны.", 0xF59E0B),
    WILD_MAGIC("wild_magic", RpgClass.SORCERER, "Дикая магия",
            "-25% цена маны. 20% шанс всплеска: возврат маны или мощный бафф!", 0xEC4899),
    ASTRAL("astral", RpgClass.SORCERER, "Звёздное наследие",
            "Нет урона от падения, +15% скорость, +35% регенерация маны.", 0x818CF8);

    private final String id;
    private final RpgClass parent;
    private final String displayName;
    private final String passiveSummary;
    private final int color;

    RpgSubclass(String id, RpgClass parent, String displayName, String passiveSummary, int color) {
        this.id = id;
        this.parent = parent;
        this.displayName = displayName;
        this.passiveSummary = passiveSummary;
        this.color = color;
    }

    public String getId() { return id; }
    public RpgClass getParent() { return parent; }
    public String getDisplayName() { return displayName; }
    public String getPassiveSummary() { return passiveSummary; }
    public int getColor() { return color; }

    public static RpgSubclass byId(String id) {
        for (RpgSubclass s : values()) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }
}
