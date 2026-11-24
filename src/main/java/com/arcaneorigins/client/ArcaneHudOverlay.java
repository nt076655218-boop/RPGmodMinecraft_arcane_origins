package com.arcaneorigins.client;

import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.network.ClientClassCache;
import com.arcaneorigins.spell.ArcaneSpell;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ArcaneHudOverlay {
    public static void render(GuiGraphics gg, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!ClientClassCache.hasChosenClass()) return;

        Font font = mc.font;
        int screenH = gg.guiHeight();
        int x = 10;
        int y = screenH - 58;

        RpgClass rpgClass = ClientClassCache.getRpgClass();
        ArcaneSpell spell = ClientClassCache.getSelectedSpell();

        int boxW = 184;
        int boxH = rpgClass == RpgClass.WARLOCK ? 52 : 44;

        int accent = (0xFF << 24) | (rpgClass != null ? rpgClass.getColor() : 0x3B82F6);
        gg.fill(x, y, x + boxW, y + boxH, 0xB80E111B);
        gg.fill(x, y, x + boxW, y + 1, accent);
        gg.fill(x, y + boxH - 1, x + boxW, y + boxH, 0x88334155);
        gg.fill(x, y, x + 2, y + boxH, accent);

        String title = (rpgClass != null ? rpgClass.getDisplayName() : "?")
                + " • Ур." + ClientClassCache.level;
        gg.drawString(font, title, x + 6, y + 4, 0xFFF8FAFC, true);

        int barX = x + 6;
        int barY = y + 16;
        int barW = boxW - 12;
        int barH = 8;
        float ratio = Math.max(0f, Math.min(1f, ClientClassCache.mana / ClientClassCache.maxMana));
        gg.fill(barX, barY, barX + barW, barY + barH, 0xFF1E293B);
        gg.fill(barX, barY, barX + (int) (barW * ratio), barY + barH, 0xFF2563EB);
        gg.fill(barX, barY, barX + (int) (barW * ratio), barY + 2, 0xFF60A5FA);

        String manaTxt = (int) ClientClassCache.mana + "/" + (int) ClientClassCache.maxMana + " MP";
        gg.drawString(font, manaTxt, barX + 3, barY, 0xFFFFFFFF, true);

        if (spell != null) {
            int spellColor = (0xFF << 24) | spell.getSchool().getColor();
            String spellLine = "[R] " + spell.getDisplayName() + " (" + (int) spell.getManaCost() + ")";
            gg.drawString(font, spellLine, x + 6, y + 29, spellColor, true);
        } else {
            gg.drawString(font, "Заклинаний нет", x + 6, y + 29, 0xFF94A3B8, true);
        }

        if (rpgClass == RpgClass.WARLOCK) {
            String favorTxt = "Благосклонность: " + ClientClassCache.patronFavor + "/100";
            gg.drawString(font, favorTxt, x + 6, y + 40, 0xFFD8B4FE, true);
        }
    }
}
