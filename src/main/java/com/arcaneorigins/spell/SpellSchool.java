package com.arcaneorigins.spell;

public enum SpellSchool {
    ICE("ice", "Лёд", 0x38BDF8),
    LIGHTNING("lightning", "Молния", 0x60A5FA),
    GRAVITY("gravity", "Гравитация и Пустота", 0xA855F7),
    FIRE("fire", "Огонь", 0xF97316);

    private final String id;
    private final String displayName;
    private final int color;

    SpellSchool(String id, String displayName, int color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }
}
