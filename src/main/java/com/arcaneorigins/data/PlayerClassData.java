package com.arcaneorigins.data;

import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerClassData {
    public static final int MAX_LEVEL = 10;

    public static final Codec<PlayerClassData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("class_id").forGetter(PlayerClassData::getClassId),
            Codec.STRING.fieldOf("subclass_id").forGetter(PlayerClassData::getSubclassId),
            Codec.BOOL.fieldOf("sorcerer_rolled").forGetter(PlayerClassData::isSorcererRolled),
            Codec.BOOL.fieldOf("sorcerer_unlocked").forGetter(PlayerClassData::isSorcererUnlocked),
            Codec.FLOAT.fieldOf("mana").forGetter(PlayerClassData::getMana),
            Codec.FLOAT.fieldOf("max_mana").forGetter(PlayerClassData::getMaxMana),
            Codec.INT.fieldOf("level").forGetter(PlayerClassData::getLevel),
            Codec.INT.fieldOf("class_xp").forGetter(PlayerClassData::getClassXp),
            Codec.INT.fieldOf("selected_spell").forGetter(PlayerClassData::getSelectedSpellIndex),
            Codec.INT.fieldOf("patron_favor").forGetter(PlayerClassData::getPatronFavor)
    ).apply(instance, PlayerClassData::new));

    private String classId;
    private String subclassId;
    private boolean sorcererRolled;
    private boolean sorcererUnlocked;
    private float mana;
    private float maxMana;
    private int level;
    private int classXp;
    private int selectedSpellIndex;
    private int patronFavor;

    public PlayerClassData() {
        this("", "", false, false, 100.0f, 100.0f, 1, 0, 0, 0);
    }

    public PlayerClassData(String classId, String subclassId, boolean sorcererRolled, boolean sorcererUnlocked,
                           float mana, float maxMana, int level, int classXp, int selectedSpellIndex, int patronFavor) {
        this.classId = classId == null ? "" : classId;
        this.subclassId = subclassId == null ? "" : subclassId;
        this.sorcererRolled = sorcererRolled;
        this.sorcererUnlocked = sorcererUnlocked;
        this.mana = mana;
        this.maxMana = Math.max(20.0f, maxMana);
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.classXp = Math.max(0, classXp);
        this.selectedSpellIndex = Math.max(0, selectedSpellIndex);
        this.patronFavor = Math.max(0, patronFavor);
    }

    public boolean hasChosenClass() {
        return !classId.isEmpty() && RpgClass.byId(classId) != null;
    }

    public RpgClass getRpgClass() { return RpgClass.byId(classId); }
    public RpgSubclass getRpgSubclass() { return RpgSubclass.byId(subclassId); }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId == null ? "" : classId; }

    public String getSubclassId() { return subclassId; }
    public void setSubclassId(String subclassId) { this.subclassId = subclassId == null ? "" : subclassId; }

    public boolean isSorcererRolled() { return sorcererRolled; }
    public void setSorcererRolled(boolean sorcererRolled) { this.sorcererRolled = sorcererRolled; }

    public boolean isSorcererUnlocked() { return sorcererUnlocked; }
    public void setSorcererUnlocked(boolean sorcererUnlocked) { this.sorcererUnlocked = sorcererUnlocked; }

    public float getMana() { return mana; }
    public void setMana(float mana) { this.mana = Math.max(0.0f, Math.min(maxMana, mana)); }

    public float getMaxMana() { return maxMana; }
    public void setMaxMana(float maxMana) {
        this.maxMana = Math.max(20.0f, maxMana);
        if (this.mana > this.maxMana) this.mana = this.maxMana;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(MAX_LEVEL, level)); }

    public int getClassXp() { return classXp; }
    public void setClassXp(int classXp) { this.classXp = Math.max(0, classXp); }

    public int getXpForNextLevel() {
        return 60 + (level - 1) * 45;
    }

    public boolean addXp(int amount) {
        if (level >= MAX_LEVEL) return false;
        this.classXp += amount;
        boolean leveledUp = false;
        while (level < MAX_LEVEL && classXp >= getXpForNextLevel()) {
            classXp -= getXpForNextLevel();
            level++;
            leveledUp = true;
        }
        return leveledUp;
    }

    public int getSelectedSpellIndex() { return selectedSpellIndex; }
    public void setSelectedSpellIndex(int idx) { this.selectedSpellIndex = idx; }

    public int getPatronFavor() { return patronFavor; }
    public void setPatronFavor(int patronFavor) { this.patronFavor = Math.max(0, Math.min(100, patronFavor)); }

    public void addPatronFavor(int delta) {
        setPatronFavor(this.patronFavor + delta);
    }
}
