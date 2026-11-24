package com.arcaneorigins.client;

import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.network.ClientClassCache;
import com.arcaneorigins.network.SelectClassPayload;
import com.arcaneorigins.spell.ArcaneSpell;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ClassSelectionScreen extends Screen {
    private RpgClass selectedClass = RpgClass.WIZARD;
    private RpgSubclass selectedSubclass = RpgSubclass.SCHOOL_ICE;

    private int panelX, panelY;
    private final int panelW = 376;
    private final int panelH = 230;

    public ClassSelectionScreen() {
        super(Component.literal("Выбор пути — Arcane Origins"));
        if (ClientClassCache.hasChosenClass()) {
            selectedClass = ClientClassCache.getRpgClass();
            selectedSubclass = ClientClassCache.getRpgSubclass();
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        boolean alreadyChosen = ClientClassCache.hasChosenClass();

        int btnY = panelY + 30;
        for (RpgClass rpgClass : RpgClass.values()) {
            boolean lockedRare = rpgClass == RpgClass.SORCERER && !ClientClassCache.sorcererUnlocked;
            String label = rpgClass == RpgClass.SORCERER
                    ? (lockedRare ? "🔒 Чародей (10%)" : "★ ЧАРОДЕЙ ★")
                    : rpgClass.getDisplayName();
            if (rpgClass == selectedClass) {
                label = "> " + label + " <";
            }

            Button btn = Button.builder(Component.literal(label), b -> {
                if (!lockedRare && !alreadyChosen) {
                    this.selectedClass = rpgClass;
                    List<RpgSubclass> subs = rpgClass.getSubclasses();
                    if (!subs.isEmpty()) this.selectedSubclass = subs.get(0);
                    this.rebuildWidgets();
                }
            }).bounds(panelX + 12, btnY, 130, 22).build();
            btn.active = !lockedRare && (!alreadyChosen || rpgClass == selectedClass);
            this.addRenderableWidget(btn);
            btnY += 26;
        }

        if (!alreadyChosen) {
            List<RpgSubclass> subs = selectedClass.getSubclasses();
            int subY = panelY + 78;
            for (RpgSubclass sub : subs) {
                String lbl = (sub == selectedSubclass ? "✦ " : "") + sub.getDisplayName();
                Button subBtn = Button.builder(Component.literal(lbl), b -> {
                    this.selectedSubclass = sub;
                    this.rebuildWidgets();
                }).bounds(panelX + 156, subY, 206, 20).build();
                this.addRenderableWidget(subBtn);
                subY += 24;
            }

            Button confirm = Button.builder(Component.literal("Принять судьбу"), b -> {
                if (selectedClass != null && selectedSubclass != null) {
                    PacketDistributor.sendToServer(new SelectClassPayload(selectedClass.getId(), selectedSubclass.getId()));
                    this.onClose();
                }
            }).bounds(panelX + 156, panelY + panelH - 30, 206, 22).build();
            this.addRenderableWidget(confirm);
        } else {
            Button close = Button.builder(Component.literal("Закрыть"), b -> this.onClose())
                    .bounds(panelX + 156, panelY + panelH - 30, 206, 22).build();
            this.addRenderableWidget(close);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        gg.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEB0F1422);
        int border = (0xFF << 24) | selectedClass.getColor();
        gg.renderOutline(panelX, panelY, panelW, panelH, border);
        gg.fill(panelX + 148, panelY + 26, panelX + 149, panelY + panelH - 10, 0x66475569);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        boolean alreadyChosen = ClientClassCache.hasChosenClass();
        String header = alreadyChosen
                ? "Персонаж: " + ClientClassCache.getRpgClass().getDisplayName()
                + " • " + (ClientClassCache.getRpgSubclass() != null ? ClientClassCache.getRpgSubclass().getDisplayName() : "")
                + " (Ур. " + ClientClassCache.level + ")"
                : "ВЫБЕРИ СВОЙ КЛАСС И ПОДКЛАСС";
        gg.drawCenteredString(this.font, header, panelX + panelW / 2, panelY + 10, 0xFFFDE68A);

        if (!alreadyChosen) {
            gg.drawString(this.font, selectedClass.getDisplayName(), panelX + 156, panelY + 28,
                    (0xFF << 24) | selectedClass.getColor(), true);
            gg.drawWordWrap(this.font, Component.literal(selectedClass.getDescription()),
                    panelX + 156, panelY + 40, 206, 0xFFCBD5E1);

            if (selectedSubclass != null) {
                int infoY = panelY + 156;
                gg.drawString(this.font, "Пассивный бонус:", panelX + 156, infoY, 0xFFFBBF24, true);
                gg.drawWordWrap(this.font, Component.literal(selectedSubclass.getPassiveSummary()),
                        panelX + 156, infoY + 11, 206, 0xFFE2E8F0);
            }
        } else {
            // Show passive bonus + Learned Spells grimoire list!
            RpgSubclass sub = ClientClassCache.getRpgSubclass();
            if (sub != null) {
                gg.drawString(this.font, "Пассивка: " + sub.getDisplayName(), panelX + 156, panelY + 28, 0xFFFBBF24, true);
                gg.drawWordWrap(this.font, Component.literal(sub.getPassiveSummary()),
                        panelX + 156, panelY + 39, 206, 0xFFCBD5E1);
            }

            int listY = panelY + 74;
            gg.drawString(this.font, "Изученные заклинания (" + ClientClassCache.unlockedSpells.size() + "/" + ArcaneSpell.values().length + "):",
                    panelX + 156, listY, 0xFF38BDF8, true);
            listY += 13;

            for (ArcaneSpell sp : ArcaneSpell.values()) {
                boolean known = ClientClassCache.unlockedSpells.contains(sp.getId());
                String line = (known ? "✦ " : "🔒 ") + sp.getDisplayName() + " [" + sp.getSchool().getDisplayName() + "]";
                int col = known ? ((0xFF << 24) | sp.getSchool().getColor()) : 0xFF64748B;
                gg.drawString(this.font, line, panelX + 158, listY, col, true);
                listY += 12;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
