package com.arcaneorigins.data;

import com.arcaneorigins.classes.ClassPerk;
import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.spell.ArcaneSpell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            Codec.INT.optionalFieldOf("skill_points", 1).forGetter(PlayerClassData::getSkillPoints),
            Codec.STRING.fieldOf("selected_spell_id").forGetter(PlayerClassData::getSelectedSpellId),
            Codec.INT.fieldOf("patron_favor").forGetter(PlayerClassData::getPatronFavor),
            Codec.STRING.listOf().optionalFieldOf("unlocked_spells", List.of()).forGetter(PlayerClassData::getUnlockedSpellIds),
            Codec.STRING.listOf().optionalFieldOf("unlocked_perks", List.of()).forGetter(PlayerClassData::getUnlockedPerkIds)
    ).apply(instance, PlayerClassData::new));

    private String classId;
    private String subclassId;
    private boolean sorcererRolled;
    private boolean sorcererUnlocked;
    private float mana;
    private float maxMana;
    private int level;
    private int classXp;
    private int skillPoints;
    private String selectedSpellId;
    private int patronFavor;
    private final Set<String> unlockedSpells = new LinkedHashSet<>();
    private final Set<String> unlockedPerks = new LinkedHashSet<>();

    public PlayerClassData() {
        this("", "", false, false, 100.0f, 100.0f, 1, 0, 1, "", 0, List.of(), List.of());
    }

    public PlayerClassData(String classId, String subclassId, boolean sorcererRolled, boolean sorcererUnlocked,
                           float mana, float maxMana, int level, int classXp, int skillPoints,
                           String selectedSpellId, int patronFavor,
                           List<String> unlockedSpellIds, List<String> unlockedPerkIds) {
        this.classId = classId == null ? "" : classId;
        this.subclassId = subclassId == null ? "" : subclassId;
        this.sorcererRolled = sorcererRolled;
        this.sorcererUnlocked = sorcererUnlocked;
        this.mana = mana;
        this.maxMana = Math.max(20.0f, maxMana);
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.classXp = Math.max(0, classXp);
        this.skillPoints = Math.max(0, skillPoints);
        this.selectedSpellId = selectedSpellId == null ? "" : selectedSpellId;
        this.patronFavor = Math.max(0, Math.min(100, patronFavor));
        if (unlockedSpellIds != null) {
            for (String id : unlockedSpellIds) {
                if (ArcaneSpell.byId(id) != null) this.unlockedSpells.add(id);
            }
        }
        if (unlockedPerkIds != null) {
            for (String id : unlockedPerkIds) {
                if (ClassPerk.byId(id) != null) this.unlockedPerks.add(id);
            }
        }
    }

    public boolean hasChosenClass() { return !classId.isEmpty() && RpgClass.byId(classId) != null; }
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

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = Math.max(0, skillPoints); }
    public void addSkillPoints(int pts) { this.skillPoints = Math.max(0, this.skillPoints + pts); }

    public int getXpForNextLevel() { return 55 + (level - 1) * 40; }

    public boolean addXp(int amount) {
        if (level >= MAX_LEVEL) return false;
        this.classXp += amount;
        boolean leveledUp = false;
        while (level < MAX_LEVEL && classXp >= getXpForNextLevel()) {
            classXp -= getXpForNextLevel();
            level++;
            skillPoints++;
            leveledUp = true;
        }
        return leveledUp;
    }

    public List<String> getUnlockedSpellIds() { return new ArrayList<>(unlockedSpells); }
    public List<ArcaneSpell> getUnlockedSpells() {
        List<ArcaneSpell> list = new ArrayList<>();
        for (String id : unlockedSpells) {
            ArcaneSpell sp = ArcaneSpell.byId(id);
            if (sp != null) list.add(sp);
        }
        return list;
    }
    public boolean hasSpellUnlocked(ArcaneSpell spell) { return spell != null && unlockedSpells.contains(spell.getId()); }
    public boolean unlockSpell(ArcaneSpell spell) {
        if (spell == null) return false;
        boolean added = unlockedSpells.add(spell.getId());
        if (added && (selectedSpellId.isEmpty() || ArcaneSpell.byId(selectedSpellId) == null)) {
            selectedSpellId = spell.getId();
        }
        return added;
    }

    public List<String> getUnlockedPerkIds() { return new ArrayList<>(unlockedPerks); }
    public boolean hasPerk(ClassPerk perk) { return perk != null && unlockedPerks.contains(perk.getId()); }
    public boolean unlockPerk(ClassPerk perk) { return perk != null && unlockedPerks.add(perk.getId()); }

    public void clearProgression() {
        unlockedSpells.clear();
        unlockedPerks.clear();
        selectedSpellId = "";
        skillPoints = 1;
    }

    public String getSelectedSpellId() {
        if (!unlockedSpells.isEmpty() && (selectedSpellId.isEmpty() || !unlockedSpells.contains(selectedSpellId))) {
            selectedSpellId = unlockedSpells.iterator().next();
        }
        return selectedSpellId;
    }
    public void setSelectedSpellId(String id) { this.selectedSpellId = id == null ? "" : id; }

    public ArcaneSpell getSelectedSpell() {
        List<ArcaneSpell> list = getUnlockedSpells();
        if (list.isEmpty()) return null;
        ArcaneSpell current = ArcaneSpell.byId(getSelectedSpellId());
        return current != null && list.contains(current) ? current : list.get(0);
    }

    public void cycleSelectedSpell(int delta) {
        List<ArcaneSpell> list = getUnlockedSpells();
        if (list.isEmpty()) { selectedSpellId = ""; return; }
        ArcaneSpell cur = getSelectedSpell();
        int idx = cur == null ? 0 : list.indexOf(cur);
        int next = ((idx + delta) % list.size() + list.size()) % list.size();
        selectedSpellId = list.get(next).getId();
    }

    public int getPatronFavor() { return patronFavor; }
    public void setPatronFavor(int patronFavor) { this.patronFavor = Math.max(0, Math.min(100, patronFavor)); }
    public void addPatronFavor(int delta) { setPatronFavor(this.patronFavor + delta); }
}
