package com.arcaneorigins.network;

import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.spell.ArcaneSpell;

import java.util.ArrayList;
import java.util.List;

public class ClientClassCache {
    public static String classId = "";
    public static String subclassId = "";
    public static boolean sorcererUnlocked = false;
    public static float mana = 100.0f;
    public static float maxMana = 100.0f;
    public static int level = 1;
    public static int classXp = 0;
    public static int skillPoints = 0;
    public static String selectedSpellId = "";
    public static int patronFavor = 0;
    public static List<String> unlockedSpells = new ArrayList<>();
    public static List<String> unlockedPerks = new ArrayList<>();
    public static boolean pendingOpenMenu = false;

    public static void update(SyncClassDataPayload p) {
        classId = p.classId();
        subclassId = p.subclassId();
        sorcererUnlocked = p.sorcererUnlocked();
        mana = p.mana();
        maxMana = Math.max(1.0f, p.maxMana());
        level = p.level();
        classXp = p.classXp();
        skillPoints = p.skillPoints();
        selectedSpellId = p.selectedSpellId();
        patronFavor = p.patronFavor();
        unlockedSpells = new ArrayList<>(p.unlockedSpells());
        unlockedPerks = new ArrayList<>(p.unlockedPerks());
        if (p.openMenu()) {
            pendingOpenMenu = true;
        }
    }

    public static boolean hasChosenClass() { return !classId.isEmpty() && RpgClass.byId(classId) != null; }
    public static RpgClass getRpgClass() { return RpgClass.byId(classId); }
    public static RpgSubclass getRpgSubclass() { return RpgSubclass.byId(subclassId); }
    public static int getXpForNextLevel() { return 55 + (level - 1) * 40; }

    public static ArcaneSpell getSelectedSpell() {
        if (unlockedSpells.isEmpty()) return null;
        ArcaneSpell sp = ArcaneSpell.byId(selectedSpellId);
        if (sp != null && unlockedSpells.contains(sp.getId())) return sp;
        return ArcaneSpell.byId(unlockedSpells.get(0));
    }
}
