package com.arcaneorigins.network;

import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.spell.ArcaneSpell;

public class ClientClassCache {
    public static String classId = "";
    public static String subclassId = "";
    public static boolean sorcererUnlocked = false;
    public static float mana = 100.0f;
    public static float maxMana = 100.0f;
    public static int level = 1;
    public static int classXp = 0;
    public static int selectedSpellIndex = 0;
    public static int patronFavor = 0;
    public static boolean pendingOpenMenu = false;

    public static void update(SyncClassDataPayload p) {
        classId = p.classId();
        subclassId = p.subclassId();
        sorcererUnlocked = p.sorcererUnlocked();
        mana = p.mana();
        maxMana = Math.max(1.0f, p.maxMana());
        level = p.level();
        classXp = p.classXp();
        selectedSpellIndex = p.selectedSpellIndex();
        patronFavor = p.patronFavor();
        if (p.openMenu()) {
            pendingOpenMenu = true;
        }
    }

    public static boolean hasChosenClass() {
        return !classId.isEmpty() && RpgClass.byId(classId) != null;
    }

    public static RpgClass getRpgClass() { return RpgClass.byId(classId); }
    public static RpgSubclass getRpgSubclass() { return RpgSubclass.byId(subclassId); }
    public static ArcaneSpell getSelectedSpell() { return ArcaneSpell.byIndex(selectedSpellIndex); }
}
