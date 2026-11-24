package com.arcaneorigins.classes;

import java.util.Arrays;
import java.util.List;

public enum RpgClass {
    WARRIOR("warrior", "Воин", "Мастер оружия и брони. Надёжная защита и боевая выучка.", 0xD97736, false, false, 40.0f),
    BARBARIAN("barbarian", "Варвар", "Неудержимая ярость, мощные удары и огромная живучесть.", 0xC83232, false, false, 40.0f),
    WIZARD("wizard", "Волшебник", "Учёный маг. Единственный класс, способный изучать заклинания по Свиткам! Кастует через Книгу или Посох.", 0x3B82F6, false, true, 125.0f),
    WARLOCK("warlock", "Колдун", "Заключает пакт с Покровителем. Открывает чары Покровителя с ростом уровня и подношениями Сферой (свитки не читает).", 0x9333EA, false, true, 105.0f),
    SORCERER("sorcerer", "Чародей", "РЕДКИЙ ДАР (10% шанс). Врождённая магия — кастует без книг и свитков, открывая силы с уровнем класса!", 0xF59E0B, true, true, 135.0f);

    private final String id;
    private final String displayName;
    private final String description;
    private final int color;
    private final boolean rare;
    private final boolean caster;
    private final float baseMaxMana;

    RpgClass(String id, String displayName, String description, int color, boolean rare, boolean caster, float baseMaxMana) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.rare = rare;
        this.caster = caster;
        this.baseMaxMana = baseMaxMana;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getColor() { return color; }
    public boolean isRare() { return rare; }
    public boolean isCaster() { return caster; }
    public float getBaseMaxMana() { return baseMaxMana; }

    public List<RpgSubclass> getSubclasses() {
        return Arrays.stream(RpgSubclass.values())
                .filter(sub -> sub.getParent() == this)
                .toList();
    }

    public static RpgClass byId(String id) {
        for (RpgClass c : values()) {
            if (c.id.equals(id)) return c;
        }
        return null;
    }
}
