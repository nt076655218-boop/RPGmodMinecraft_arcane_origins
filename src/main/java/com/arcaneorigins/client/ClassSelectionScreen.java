package com.arcaneorigins.client;

import com.arcaneorigins.classes.ClassPerk;
import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.network.ClientClassCache;
import com.arcaneorigins.network.SelectClassPayload;
import com.arcaneorigins.network.UnlockPerkPayload;
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
    private ClassPerk hoveredOrSelectedPerk = null;
    private int activeTab = 0; // 0 = Прокачка класса (Перки и Чары), 1 = Открытые заклинания

    private int panelX, panelY;
    private final int panelW = 436;
    private final int panelH = 278;
    private int lastKnownSkillPoints = -1;
    private int lastKnownPerkCount = -1;

    public ClassSelectionScreen() {
        super(Component.literal("Развитие персонажа — Arcane Origins"));
        if (ClientClassCache.hasChosenClass()) {
            selectedClass = ClientClassCache.getRpgClass();
            selectedSubclass = ClientClassCache.getRpgSubclass();
            List<ClassPerk> perks = ClassPerk.forClass(selectedClass);
            if (!perks.isEmpty()) hoveredOrSelectedPerk = perks.get(0);
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        boolean alreadyChosen = ClientClassCache.hasChosenClass();
        lastKnownSkillPoints = ClientClassCache.skillPoints;
        lastKnownPerkCount = ClientClassCache.unlockedPerks.size();

        if (!alreadyChosen) {
            // ================= INITIAL CLASS SELECTION MODE =================
            int btnY = panelY + 32;
            for (RpgClass rpgClass : RpgClass.values()) {
                boolean lockedRare = rpgClass == RpgClass.SORCERER && !ClientClassCache.sorcererUnlocked;
                String label = rpgClass == RpgClass.SORCERER
                        ? (lockedRare ? "🔒 Чародей (10%)" : "★ ЧАРОДЕЙ ★")
                        : rpgClass.getDisplayName();
                if (rpgClass == selectedClass) label = "> " + label + " <";

                Button btn = Button.builder(Component.literal(label), b -> {
                    if (!lockedRare) {
                        this.selectedClass = rpgClass;
                        List<RpgSubclass> subs = rpgClass.getSubclasses();
                        if (!subs.isEmpty()) this.selectedSubclass = subs.get(0);
                        this.rebuildWidgets();
                    }
                }).bounds(panelX + 12, btnY, 134, 22).build();
                btn.active = !lockedRare;
                this.addRenderableWidget(btn);
                btnY += 26;
            }

            List<RpgSubclass> subs = selectedClass.getSubclasses();
            int subY = panelY + 76;
            for (RpgSubclass sub : subs) {
                String lbl = (sub == selectedSubclass ? "✦ " : "") + sub.getDisplayName();
                Button subBtn = Button.builder(Component.literal(lbl), b -> {
                    this.selectedSubclass = sub;
                    this.rebuildWidgets();
                }).bounds(panelX + 160, subY, 262, 19).build();
                this.addRenderableWidget(subBtn);
                subY += 22;
            }

            Button confirm = Button.builder(Component.literal("Принять судьбу"), b -> {
                if (selectedClass != null && selectedSubclass != null) {
                    PacketDistributor.sendToServer(new SelectClassPayload(selectedClass.getId(), selectedSubclass.getId()));
                    this.onClose();
                }
            }).bounds(panelX + 160, panelY + panelH - 26, 262, 20).build();
            this.addRenderableWidget(confirm);

        } else {
            // ================= CLASS PROGRESSION & SKILL TREE MODE =================
            RpgClass myClass = ClientClassCache.getRpgClass();
            List<ClassPerk> perks = ClassPerk.forClass(myClass);
            if (hoveredOrSelectedPerk == null && !perks.isEmpty()) {
                hoveredOrSelectedPerk = perks.get(0);
            }

            // Tab buttons
            Button tabTree = Button.builder(Component.literal(activeTab == 0 ? "> Дерево Прокачки <" : "Дерево Прокачки"), b -> {
                activeTab = 0;
                this.rebuildWidgets();
            }).bounds(panelX + 14, panelY + 46, 130, 20).build();
            this.addRenderableWidget(tabTree);

            Button tabSpells = Button.builder(Component.literal(activeTab == 1 ? "> Заклинания (" + ClientClassCache.unlockedSpells.size() + ") <"
                    : "Заклинания (" + ClientClassCache.unlockedSpells.size() + ")"), b -> {
                activeTab = 1;
                this.rebuildWidgets();
            }).bounds(panelX + 14, panelY + 70, 130, 20).build();
            this.addRenderableWidget(tabSpells);

            if (activeTab == 0) {
                int nodeY = panelY + 46;
                for (ClassPerk perk : perks) {
                    boolean unlocked = ClientClassCache.unlockedPerks.contains(perk.getId());
                    boolean canUnlock = !unlocked && ClientClassCache.skillPoints > 0 && ClientClassCache.level >= perk.getRequiredLevel();

                    String statusPrefix = unlocked ? "✔ " : (canUnlock ? "★ " : "🔒 ");
                    String btnText = statusPrefix + "[Ур." + perk.getRequiredLevel() + "] " + perk.getTitle();

                    Button perkBtn = Button.builder(Component.literal(btnText), b -> {
                        this.hoveredOrSelectedPerk = perk;
                        if (canUnlock) {
                            PacketDistributor.sendToServer(new UnlockPerkPayload(perk.getId()));
                        } else {
                            this.rebuildWidgets();
                        }
                    }).bounds(panelX + 158, nodeY, 264, 21).build();
                    this.addRenderableWidget(perkBtn);
                    nodeY += 24;
                }
            }

            Button close = Button.builder(Component.literal("Закрыть"), b -> this.onClose())
                    .bounds(panelX + 14, panelY + panelH - 28, 130, 20).build();
            this.addRenderableWidget(close);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (ClientClassCache.hasChosenClass()
                && (ClientClassCache.skillPoints != lastKnownSkillPoints
                || ClientClassCache.unlockedPerks.size() != lastKnownPerkCount)) {
            this.rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        RpgClass activeClass = ClientClassCache.hasChosenClass() ? ClientClassCache.getRpgClass() : selectedClass;
        gg.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE0D121F);
        int border = (0xFF << 24) | (activeClass != null ? activeClass.getColor() : 0x3B82F6);
        gg.renderOutline(panelX, panelY, panelW, panelH, border);
        gg.fill(panelX + 150, panelY + 40, panelX + 151, panelY + panelH - 10, 0x66475569);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        boolean alreadyChosen = ClientClassCache.hasChosenClass();
        if (!alreadyChosen) {
            gg.drawCenteredString(this.font, "ВЫБЕРИ СВОЙ КЛАСС И ПОДКЛАСС", panelX + panelW / 2, panelY + 11, 0xFFFDE68A);
            gg.drawString(this.font, selectedClass.getDisplayName(), panelX + 160, panelY + 28,
                    (0xFF << 24) | selectedClass.getColor(), true);
            gg.drawWordWrap(this.font, Component.literal(selectedClass.getDescription()),
                    panelX + 160, panelY + 40, 262, 0xFFCBD5E1);

            if (selectedSubclass != null) {
                int infoY = panelY + 178;
                gg.drawString(this.font, "Пассивный бонус:", panelX + 160, infoY, 0xFFFBBF24, true);
                gg.drawWordWrap(this.font, Component.literal(selectedSubclass.getPassiveSummary()),
                        panelX + 160, infoY + 11, 262, 0xFFE2E8F0);
            }
            return;
        }

        // Header & XP Progress Bar
        RpgClass myClass = ClientClassCache.getRpgClass();
        RpgSubclass sub = ClientClassCache.getRpgSubclass();
        String header = myClass.getDisplayName() + " (" + (sub != null ? sub.getDisplayName() : "") + ")  •  Уровень " + ClientClassCache.level + "/10";
        gg.drawCenteredString(this.font, header, panelX + panelW / 2, panelY + 9, 0xFFFDE68A);

        int xpBarX = panelX + 14;
        int xpBarY = panelY + 23;
        int xpBarW = panelW - 28;
        int xpBarH = 10;
        int nextXp = ClientClassCache.getXpForNextLevel();
        float xpRatio = Math.max(0f, Math.min(1f, (float) ClientClassCache.classXp / Math.max(1, nextXp)));
        gg.fill(xpBarX, xpBarY, xpBarX + xpBarW, xpBarY + xpBarH, 0xFF1E293B);
        gg.fill(xpBarX, xpBarY, xpBarX + (int) (xpBarW * xpRatio), xpBarY + xpBarH, 0xFF10B981);
        gg.drawCenteredString(this.font, "Опыт класса: " + ClientClassCache.classXp + " / " + nextXp + " XP",
                panelX + panelW / 2, xpBarY + 1, 0xFFFFFFFF);

        // Left Sidebar Stats & Skill Points
        int leftX = panelX + 14;
        int statY = panelY + 100;
        int spColor = ClientClassCache.skillPoints > 0 ? 0xFFFDE047 : 0xFF94A3B8;
        gg.drawString(this.font, "Очки развития: " + ClientClassCache.skillPoints, leftX, statY, spColor, true);
        statY += 14;
        gg.drawString(this.font, "Мана: " + (int) ClientClassCache.mana + "/" + (int) ClientClassCache.maxMana, leftX, statY, 0xFF60A5FA, true);
        statY += 13;
        if (myClass == RpgClass.WARLOCK) {
            gg.drawString(this.font, "Благоскл.: " + ClientClassCache.patronFavor + "/100", leftX, statY, 0xFFD8B4FE, true);
            statY += 13;
        }
        if (sub != null) {
            gg.drawString(this.font, "Пассивка пути:", leftX, statY + 4, 0xFFFBBF24, true);
            gg.drawWordWrap(this.font, Component.literal(sub.getPassiveSummary()), leftX, statY + 16, 130, 0xFFCBD5E1);
        }

        // Right Content Area
        if (activeTab == 0) {
            int descY = panelY + 172;
            gg.fill(panelX + 158, descY - 4, panelX + 422, panelY + panelH - 12, 0x99162033);
            if (hoveredOrSelectedPerk != null) {
                boolean unlocked = ClientClassCache.unlockedPerks.contains(hoveredOrSelectedPerk.getId());
                boolean canUnlock = !unlocked && ClientClassCache.skillPoints > 0 && ClientClassCache.level >= hoveredOrSelectedPerk.getRequiredLevel();

                gg.drawString(this.font, hoveredOrSelectedPerk.getTitle() + " (Треб. Ур." + hoveredOrSelectedPerk.getRequiredLevel() + ")",
                        panelX + 164, descY + 2, 0xFFFDE68A, true);
                gg.drawWordWrap(this.font, Component.literal(hoveredOrSelectedPerk.getDescription()),
                        panelX + 164, descY + 15, 250, 0xFFE2E8F0);

                String hint = unlocked ? "✔ Талант уже изучен!"
                        : (canUnlock ? "★ Нажми на кнопку таланта выше, чтобы изучить за 1 очко!"
                        : "Нужен Ур." + hoveredOrSelectedPerk.getRequiredLevel() + " и 1 очко развития.");
                int hintCol = unlocked ? 0xFF4ADE80 : (canUnlock ? 0xFFFACC15 : 0xFF94A3B8);
                gg.drawString(this.font, hint, panelX + 164, panelY + panelH - 24, hintCol, true);
            }
        } else {
            int listY = panelY + 48;
            gg.drawString(this.font, "Твои открытые заклинания ([V] — сменить, [R] — каст):",
                    panelX + 160, listY, 0xFF38BDF8, true);
            listY += 15;
            List<String> knownIds = ClientClassCache.unlockedSpells;
            if (knownIds.isEmpty()) {
                gg.drawString(this.font, "Пока нет открытых заклинаний.", panelX + 160, listY, 0xFF64748B, true);
            } else {
                for (String id : knownIds) {
                    ArcaneSpell sp = ArcaneSpell.byId(id);
                    if (sp == null) continue;
                    String line = "✦ " + sp.getDisplayName() + " [" + sp.getSchool().getDisplayName() + "] — " + (int) sp.getManaCost() + " MP";
                    int col = (0xFF << 24) | sp.getSchool().getColor();
                    gg.drawString(this.font, line, panelX + 160, listY, col, true);
                    listY += 13;
                    if (listY > panelY + panelH - 18) break;
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
