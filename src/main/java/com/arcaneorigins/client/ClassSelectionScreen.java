package com.arcaneorigins.client;

import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.network.ClientClassCache;
import com.arcaneorigins.network.SelectClassPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class ClassSelectionScreen extends Screen {
    private RpgClass selectedClass = RpgClass.WIZARD;
    private RpgSubclass selectedSubclass = RpgSubclass.SCHOOL_ICE;

    private int panelX, panelY, panelW = 360, panelH = 224;

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

        // Left column: 5 classes
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
                if (!lockedRare) {
                    this.selectedClass = rpgClass;
                    List<RpgSubclass> subs = rpgClass.getSubclasses();
                    if (!subs.isEmpty()) this.selectedSubclass = subs.get(0);
                    this.rebuildWidgets();
                }
            }).bounds(panelX + 12, btnY, 128, 22).build();
            btn.active = !lockedRare;
            this.addRenderableWidget(btn);
            btnY += 26;
        }

        // Right column: Subclasses for selected class
        List<RpgSubclass> subs = selectedClass.getSubclasses();
        int subY = panelY + 78;
        for (RpgSubclass sub : subs) {
            String lbl = (sub == selectedSubclass ? "✦ " : "") + sub.getDisplayName();
            Button subBtn = Button.builder(Component.literal(lbl), b -> {
                this.selectedSubclass = sub;
                this.rebuildWidgets();
            }).bounds(panelX + 154, subY, 192, 20).build();
            this.addRenderableWidget(subBtn);
            subY += 24;
        }

        // Confirm / Close button
        if (!alreadyChosen) {
            Button confirm = Button.builder(Component.literal("Принять судьбу"), b -> {
                if (selectedClass != null && selectedSubclass != null) {
                    PacketDistributor.sendToServer(new SelectClassPayload(selectedClass.getId(), selectedSubclass.getId()));
                    this.onClose();
                }
            }).bounds(panelX + 154, panelY + panelH - 30, 192, 22).build();
            this.addRenderableWidget(confirm);
        } else {
            Button close = Button.builder(Component.literal("Закрыть"), b -> this.onClose())
                    .bounds(panelX + 154, panelY + panelH - 30, 192, 22).build();
            this.addRenderableWidget(close);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        // Dark fantasy parchment/slate backdrop
        gg.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEB0F1422);
        int border = (0xFF << 24) | selectedClass.getColor();
        gg.renderOutline(panelX, panelY, panelW, panelH, border);
        gg.fill(panelX + 146, panelY + 26, panelX + 147, panelY + panelH - 10, 0x66475569);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        String header = ClientClassCache.hasChosenClass()
                ? "Персонаж: " + ClientClassCache.getRpgClass().getDisplayName() + " (Ур. " + ClientClassCache.level + ")"
                : "ВЫБЕРИ СВОЙ КЛАСС И ПОДКЛАСС";
        gg.drawCenteredString(this.font, header, panelX + panelW / 2, panelY + 10, 0xFFFDE68A);

        // Class description
        gg.drawString(this.font, selectedClass.getDisplayName(), panelX + 154, panelY + 28,
                (0xFF << 24) | selectedClass.getColor(), true);
        gg.drawWordWrap(this.font, Component.literal(selectedClass.getDescription()),
                panelX + 154, panelY + 40, 194, 0xFFCBD5E1);

        // Subclass passive preview
        if (selectedSubclass != null) {
            int infoY = panelY + 156;
            gg.drawString(this.font, "Пассивный бонус:", panelX + 154, infoY, 0xFFFBBF24, true);
            gg.drawWordWrap(this.font, Component.literal(selectedSubclass.getPassiveSummary()),
                    panelX + 154, infoY + 11, 194, 0xFFE2E8F0);
        }

        // Wizard note about upcoming schools
        if (selectedClass == RpgClass.WIZARD) {
            gg.drawWordWrap(this.font, Component.literal("Этап 1: Лёд и Молния. Огонь, Время, Гравитация и др. — в след. обновлении!"),
                    panelX + 12, panelY + 166, 128, 0xFF94A3B8);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
