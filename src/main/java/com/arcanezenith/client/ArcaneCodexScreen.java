package com.arcanezenith.client;

import com.arcanezenith.network.C2SSelectSpellPacket;
import com.arcanezenith.network.C2SUnlockSpellPacket;
import com.arcanezenith.progression.SpellRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The "Codex of the Astral": a real interactive grimoire screen with three tabs.
 *
 * - Skill Tree: every spell laid out by element (column) x tier (row), connected by glowing
 *   lines to its prerequisite (SpellRegistry.Definition#requires), clickable to unlock/select -
 *   same underlying network calls as the radial menu.
 * - Bestiary: text summaries of the mod's enemies/bosses. Real per-entry lore text (not
 *   live 3D model scanning/weak-point stats - that needs a render-to-texture entity preview
 *   pipeline, flagged honestly below rather than faked).
 * - Crafting: recipe reference, replacing the old flat book page for the same info.
 *
 * This intentionally does NOT attempt a literal 3D-rendered book model or live entity
 * scanning - both are separate, large rendering subsystems. What's real here: an actual
 * clickable, navigable, multi-tab UI backed by SpellRegistry data (not memorized static
 * text), which is the part of the design doc that matters for actual play (browsing your
 * progression, understanding prerequisites, unlocking spells) versus pure visual flourish.
 */
public class ArcaneCodexScreen extends Screen {

    private enum Tab { SKILL_TREE, SPELL_LORE, BESTIARY, CRAFTING }

    private static final int TAB_BAR_WIDTH = 90;

    private Tab activeTab = Tab.SKILL_TREE;
    private final Map<SpellRegistry.Element, Integer> columnOf = new HashMap<>();
    private SpellRegistry.Definition hovered;

    public ArcaneCodexScreen() {
        super(Component.literal("Codex of the Astral"));
        SpellRegistry.Element[] elements = SpellRegistry.Element.values();
        for (int i = 0; i < elements.length; i++) {
            columnOf.put(elements[i], i);
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int y = 20;
        for (Tab tab : Tab.values()) {
            Button button = Button.builder(Component.literal(tabLabel(tab)), b -> switchTab(tab))
                    .bounds(10, y, TAB_BAR_WIDTH, 20)
                    .build();
            this.addRenderableWidget(button);
            y += 24;
        }

        Button close = Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(10, this.height - 30, TAB_BAR_WIDTH, 20)
                .build();
        this.addRenderableWidget(close);
    }

    private String tabLabel(Tab tab) {
        return switch (tab) {
            case SKILL_TREE  -> "Skill Tree";
            case SPELL_LORE  -> "Spell Lore";
            case BESTIARY    -> "Bestiary";
            case CRAFTING    -> "Crafting";
        };
    }

    private void switchTab(Tab tab) {
        this.activeTab = tab;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xE6120A1E);
        graphics.fill(0, 0, TAB_BAR_WIDTH + 20, this.height, 0x30000000);

        graphics.drawString(this.font, Component.literal("Codex of the Astral"), 10, 6, 0xFFD9CCFF);

        int contentX = TAB_BAR_WIDTH + 30;
        switch (activeTab) {
            case SKILL_TREE  -> renderSkillTree(graphics, mouseX, mouseY, contentX);
            case SPELL_LORE  -> renderSpellLore(graphics, contentX);
            case BESTIARY    -> renderBestiary(graphics, contentX);
            case CRAFTING    -> renderCrafting(graphics, contentX);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ---------------------------------------------------------------- SKILL TREE ----

    private int nodeX(int contentX, int treeWidth, SpellRegistry.Definition def) {
        int col = columnOf.get(def.element());
        int cols = SpellRegistry.Element.values().length;
        return contentX + (int) ((col + 0.5) * (treeWidth / (double) cols));
    }

    private int nodeY(int topY, SpellRegistry.Definition def) {
        return topY + def.tier() * 55;
    }

    private void renderSkillTree(GuiGraphics graphics, int mouseX, int mouseY, int contentX) {
        List<SpellRegistry.Definition> all = SpellRegistry.all();
        int treeWidth = this.width - contentX - 20;
        int topY = 40;

        // Element column headers
        for (var element : SpellRegistry.Element.values()) {
            int col = columnOf.get(element);
            int cols = SpellRegistry.Element.values().length;
            int x = contentX + (int) ((col + 0.5) * (treeWidth / (double) cols));
            graphics.drawCenteredString(this.font, Component.literal(element.displayName), x, 22, element.color);
        }

        // Prerequisite lines first, so nodes draw on top
        for (var def : all) {
            if (def.requires() == null) continue;
            var reqDef = SpellRegistry.get(def.requires());
            if (reqDef == null) continue;

            int x1 = nodeX(contentX, treeWidth, reqDef);
            int y1 = nodeY(topY, reqDef);
            int x2 = nodeX(contentX, treeWidth, def);
            int y2 = nodeY(topY, def);

            boolean bothUnlocked = ClientProgressionCache.isUnlocked(reqDef.id()) && ClientProgressionCache.isUnlocked(def.id());
            int lineColor = bothUnlocked ? 0xFFB08CFF : 0x55554488;
            drawManaLine(graphics, x1, y1, x2, y2, lineColor);
        }

        hovered = null;
        for (var def : all) {
            int x = nodeX(contentX, treeWidth, def);
            int y = nodeY(topY, def);
            boolean unlocked = ClientProgressionCache.isUnlocked(def.id());
            boolean selected = def.id().equals(ClientProgressionCache.getSelected());
            boolean isHover = Math.abs(mouseX - x) < 14 && Math.abs(mouseY - y) < 14;
            if (isHover) hovered = def;

            int radius = selected ? 11 : 9;
            int fill = unlocked ? def.element().color : 0xFF33264D;
            if (isHover) fill = brighten(fill);

            graphics.fill(x - radius, y - radius, x + radius, y + radius, fill);
            if (selected) {
                graphics.fill(x - radius - 2, y - radius - 2, x + radius + 2, y - radius, 0xFFFFFFFF);
                graphics.fill(x - radius - 2, y + radius, x + radius + 2, y + radius + 2, 0xFFFFFFFF);
            }
        }

        // Tooltip for hovered node
        if (hovered != null) {
            boolean unlocked = ClientProgressionCache.isUnlocked(hovered.id());
            String status = unlocked ? "Unlocked" : ("Locked \u2014 " + hovered.unlockCost() + " pts");
            int tx = mouseX + 14;
            int ty = mouseY;
            int w = this.font.width(hovered.displayName()) + 10;
            graphics.fill(tx - 4, ty - 4, tx + Math.max(w, 90), ty + 26, 0xEE1A1030);
            graphics.drawString(this.font, hovered.displayName(), tx, ty, 0xFFFFFFFF);
            graphics.drawString(this.font, status, tx, ty + 11, unlocked ? 0xFF8FE38F : 0xFFCC6633);
        }

        graphics.drawString(this.font, "Arcane Points: " + ClientProgressionCache.getPoints(),
                contentX, this.height - 20, 0xFFD9CCFF);
    }

    private void drawManaLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, x1, y1, 0f).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0f).setColor(r, g, b, a);
        MeshData mesh = buffer.buildOrThrow();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
    }

    private int brighten(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 50);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 50);
        int b = Math.min(255, (argb & 0xFF) + 50);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == Tab.SKILL_TREE && button == 0 && hovered != null) {
            var def = hovered;
            if (ClientProgressionCache.isUnlocked(def.id())) {
                PacketDistributor.sendToServer(new C2SSelectSpellPacket(def.id()));
            } else {
                PacketDistributor.sendToServer(new C2SUnlockSpellPacket(def.id()));
            }
            return true;
        }
        // Spell Lore lapozás
        if (activeTab == Tab.SPELL_LORE && button == 0) {
            int pages = (SPELL_LORE.size() + SPELL_LORE_PER_PAGE - 1) / SPELL_LORE_PER_PAGE;
            int bottomY = this.height - 25;
            if (mouseY >= bottomY && mouseY <= this.height - 5) {
                int contentX = this.width / 6;
                if (mouseX < contentX + 40 && spellLorePage > 0) {
                    spellLorePage--;
                    return true;
                }
                if (mouseX > this.width - 70 && spellLorePage < pages - 1) {
                    spellLorePage++;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------------------------------------------------------------- SPELL LORE ----

    private record SpellLoreEntry(String name, String tier, String element,
                                   String lore, String mechanics) {}

    private static final List<SpellLoreEntry> SPELL_LORE = List.of(
        // ── TIER 0–1 ──────────────────────────────────────────────────────────
        new SpellLoreEntry("Arcane Bolt", "Tier 0", "Arcane",
            "The simplest expression of raw arcane will — a beam of pure magical force compressed into a needle of light. Ancient mages used it to light campfires. You use it to end lives.",
            "Hits stack Arcane Mark (3 stacks = detonation). 3-layer trail. Detonate deals 22 AoE in 4.5 blocks."),
        new SpellLoreEntry("Teleport", "Tier 1", "Umbra",
            "The caster tears a hole in space and steps through. The decoy left behind is not an illusion — it is a fragment of the caster's shadow, given temporary substance and a single explosive purpose.",
            "Leaves explosive shadow decoy at origin. 30 dmg on detonation. Shadow wisp dissolve VFX."),
        // ── TIER 2 ────────────────────────────────────────────────────────────
        new SpellLoreEntry("Horde of Blades", "Tier 2", "Cryo",
            "Twelve spectral ice blades, each carved from condensed grief and winter wind. They orbit you like a crown of frozen stars, eager to be loosed.",
            "12-blade orbit 3s, then 3 blades launch at nearest targets. Parry: 14 dmg to anything in 3.5 blocks. Glass-shatter impact."),
        new SpellLoreEntry("Thunder War", "Tier 2", "Fulgur",
            "A battlefield prayer answered by the sky itself. The mage calls down not one bolt but a cascade — each strike jumping to the nearest unchained foe like lightning seeking ground.",
            "22 dmg/strike, chains to 10 mobs. Calls real weather (storm activates). LIGHTNING_FLASH shader."),
        new SpellLoreEntry("Aether Wings", "Tier 2", "Sanctum",
            "The caster doesn't fly so much as refuse to fall. Aether Wings are a statement of defiance against gravity, against limits, against everything that keeps magnificent things earthbound.",
            "15s supersonic flight. Sonic boom ring at speed threshold. Collision: 16+velocity dmg. 24-point wing geometry."),
        // ── TIER 3 ────────────────────────────────────────────────────────────
        new SpellLoreEntry("God's Spear", "Tier 3", "Sanctum",
            "This is not a weapon. This is judgment delivered in the shape of a weapon. The spear does not pierce because it is sharp — it pierces because the universe steps aside.",
            "55 pierce dmg + armor bypass. Pins target (NoAI) 3s. 6-ring collapse VFX. Detonation: 40 AoE."),
        new SpellLoreEntry("Gravitational Collapse", "Tier 3", "Umbra",
            "At the heart of every black hole is a point where physics stopped arguing and simply surrendered. The Gravitational Collapse spell creates a temporary ambassador from that place.",
            "60 AoE pull. 1200 accretion stream particles. Terrain carve. GRAVITY_LENS shader."),
        new SpellLoreEntry("Plasma Annihilator", "Tier 3", "Ignis",
            "The beam begins cool — almost clinical. Then it finds its rhythm. By the time it reaches full temperature, the air itself has forgotten how to be solid.",
            "8→28 dmg ramp over 4s channel. 3-layer beam. HEAT_HAZE shader. Block surface splatter."),
        new SpellLoreEntry("Angel's Help", "Tier 3", "Sanctum",
            "The oldest healing spell in the arcane tradition. Not because it is simple, but because some wounds require the weight of genuine light to close.",
            "Full heal + mana restore. Cleanses all debuffs. 48-point wing geometry. HOLY_BLOOM shader."),
        // ── TIER 4 ────────────────────────────────────────────────────────────
        new SpellLoreEntry("Mage's Hammer", "Tier 4", "Sanctum",
            "Four rings of light descend from a hole punched through the sky. The hammer is not made of anything — it is made of consequence.",
            "120 crush dmg. 7-block crater. 5s lava afterburn. Fény-emission at impact. Deep bass impact sound."),
        new SpellLoreEntry("Time Silence", "Tier 4", "Chrono",
            "The world does not stop. It simply pauses to reconsider. In the silence between heartbeats, every injury ever dealt waits patiently to be delivered all at once.",
            "30s freeze (NoAI + Slowness 255). All damage buffered ×3 then detonates. TIME_STOP shader."),
        new SpellLoreEntry("Judgment of Heaven", "Tier 4", "Sanctum",
            "An orbital platform of light materializes above the target and delivers sixteen consecutive verdicts. There is no appeal. There is no mercy. There is only the next spike.",
            "100 initial + 15/spike × 16 waves over 8s. HOLY_BLOOM shader. Fény at impact."),
        new SpellLoreEntry("Cataclysmic Rift", "Tier 4", "Ignis",
            "The earth remembers every wrong done to it. The Cataclysmic Rift gives it permission to express those feelings.",
            "Progressive crack spread. Real fissure terrain. 5 lava geysers/wave × 30 dmg. Vihar aktiválás. HEAT_HAZE shader."),
        new SpellLoreEntry("Singularity Collapse", "Tier 4", "Umbra",
            "The endgame of all gravity: not a pull but an erasure. The singularity doesn't damage things so much as inform them they no longer have permission to exist.",
            "100 TRUE dmg. 3-ring accretion disk. 0.5s Time Silence before detonation. Blinding supernova. Lebegő töltés."),
        // ── TIER 5: LEGENDARY ─────────────────────────────────────────────────
        new SpellLoreEntry("Eldritch Tempest", "Tier 5 \u2605", "Sanctum",
            "Dr. Strange once said that the greatest power is knowing which fight to not be in. The Eldritch Tempest disagrees. Eight golden energy whips that disagree at the same time.",
            "8 arany energiaostor 360°-ban. 45 dmg/ostor. Max 360 dmg. ARCANE_OVERDRIVE shader. Eldritch_whip particle."),
        new SpellLoreEntry("Avada Curse", "Tier 5 \u2605", "Umbra",
            "There is no counter-curse. There is no defense. There is only the green light, and then there is nothing. The soul does not go quietly.",
            "200 TRUE dmg. Instant kill beam 60 blokk. Lélek-kiszakadás VFX halálakor. BLOOD_CURSE shader (zöld)."),
        new SpellLoreEntry("Starscourge Meteor", "Tier 5 \u2605", "Ignis",
            "Radahn carried the stars in place through sheer will. When he finally fell, they moved. You are choosing to become one of them.",
            "Caster kilövi magát Y+60, meteorként visszacsapódik. 280 TRUE dmg. 8 blokk kráter. 80-pontos shockwave gyűrű. Lebegő töltés."),
        new SpellLoreEntry("Excalibur Beam", "Tier 5 \u2605", "Sanctum",
            "Saber's noble phantasm rendered in pure light. The sword is invisible. The destruction is not. A promise made to a world that needed saving, fired as a single golden line.",
            "0.8s töltés (arany gyűrűk összehúzódnak). 320 TRUE dmg. 80 blokk hatótáv, 5 blokk széles. Blokkok olvadnak. HOLY_BLOOM + ARCANE_OVERDRIVE."),
        new SpellLoreEntry("Vortex Essence", "Tier 5 \u2605", "Umbra",
            "The universe spent thirteen billion years making matter. The Vortex Essence reminds it that the original state was simpler.",
            "3-gyűrűs accretion disk. 5s gravitációs pull (inverse-square). 260 dmg detonáció. Ködfal. VOID_RIFT shader."),
        new SpellLoreEntry("Blades of Chaos", "Tier 5 \u2605", "Ignis",
            "Kratos carries his past chained to his arms. You choose to carry the same weight. The difference is he had no choice. Make them count.",
            "6 láncos tűzpenge csapás. 55 dmg + égés/csapás. 330 összesített. Szikraeső. STELLAR_FIRE shader."),
        new SpellLoreEntry("Lightning Storm", "Tier 5 \u2605", "Fulgur",
            "The Grey Wizard called a storm over the field and the Empire's flank collapsed. You are not a grey wizard. Your storm does not care about flanks.",
            "30s viharfelhő Y+15-n. 12 villám/mp random célpontokra. 14 400 max dmg. ARCANE_OVERDRIVE + vihar. STORM_BOLT particle."),
        new SpellLoreEntry("Chidori", "Tier 5 \u2605", "Fulgur",
            "A thousand birds screaming at once — that is the sound of lightning compressed into a human palm. Kakashi created it. Sasuke perfected it. You inherited the scream.",
            "1s töltés (kék villám spirál). Villámgyors teleport-roham 20 blokk. 180 TRUE dmg. ELECTRIC_SPARK burst. CHIDORI_SPARK particle."),
        new SpellLoreEntry("Glintstone Phalanx", "Tier 5 \u2605", "Cryo",
            "Sellen's favorite formation — sixteen neon-blue daggers arranged in an arc overhead, each tracking a different target with patient, glittering precision.",
            "16 neonkék tőr ívben, 8s auto-tracking. 70 dmg/tőr, max 1120. Elegáns fénycsík kilövésnél. GLINT_DAGGER particle."),
        new SpellLoreEntry("Crimson Bands of Cyttorak", "Tier 5 \u2605", "Sanctum",
            "The Crimson Bands don't just restrain. They insist. They wrap around the target and explain, at great length and with considerable force, that movement is no longer an option.",
            "8 skarlát kötelék 20m körben. 6s NoAI + Slowness 255. 20 dmg/sec crush. Singularity összevonás: 150 collision dmg. BLOOD_CURSE shader.")
    );

    private int spellLorePage = 0;
    private static final int SPELL_LORE_PER_PAGE = 3;

    private void renderSpellLore(GuiGraphics graphics, int contentX) {
        int width = this.width - contentX - 20;
        int y = 10;

        int total  = SPELL_LORE.size();
        int pages  = (total + SPELL_LORE_PER_PAGE - 1) / SPELL_LORE_PER_PAGE;
        int start  = spellLorePage * SPELL_LORE_PER_PAGE;
        int end    = Math.min(start + SPELL_LORE_PER_PAGE, total);

        // Lapozás gomb
        if (spellLorePage > 0) {
            graphics.drawString(this.font, Component.literal("< Prev"),
                    contentX, this.height - 20, 0xFFAA88CC);
        }
        graphics.drawString(this.font,
                Component.literal((spellLorePage + 1) + "/" + pages),
                this.width / 2 - 8, this.height - 20, 0xFF998AAA);
        if (spellLorePage < pages - 1) {
            graphics.drawString(this.font, Component.literal("Next >"),
                    this.width - 60, this.height - 20, 0xFFAA88CC);
        }

        for (int i = start; i < end; i++) {
            SpellLoreEntry e = SPELL_LORE.get(i);
            // Cím + tier/elem
            String header = e.name() + "  \u00a77[" + e.tier() + " \u2014 " + e.element() + "]";
            graphics.drawString(this.font, Component.literal(header), contentX, y, 0xFFE6D0FF);
            y += 12;
            // Lore szöveg
            y = wrapText(graphics, e.lore(), contentX, y, width, 0xFFCCC0E8);
            y += 2;
            // Mechanics
            graphics.drawString(this.font, Component.literal("\u00a7e\u25ba "), contentX, y, 0xFFFFDD88);
            y = wrapText(graphics, e.mechanics(), contentX + 10, y, width - 10, 0xFFFFDD88);
            y += 12;
        }
    }

    // ---------------------------------------------------------------- BESTIARY ----

    private record BestiaryEntry(String name, String lore, String weakPoint) {}

    private static final List<BestiaryEntry> BESTIARY = List.of(
        new BestiaryEntry("Arcane Zealot",
            "A rogue battle-mage who blinks away from melee range when threatened, erects " +
            "elemental barriers that reflect 40% of melee damage, and fires slowing crowd-control " +
            "bolts every 5s. Drains 8 mana from each attacker on hit. Naturally spawns in overworld " +
            "caves and inside the Arcane Spire.",
            "Strike from range \u2014 its blink reacts to proximity, not projectiles. The barrier lasts " +
            "only 4s; burst it with magic damage. The mana drain hurts most at low mana \u2014 keep your bar above half."),
        new BestiaryEntry("Chrono-Weaver",
            "An elite dimension mage that tears dimensional rifts at your position (AoE damage + " +
            "Slowness) and rewinds its own health to a 5-second-ago snapshot when it drops below 20% HP. " +
            "Limited to 2 rewinds per fight. The rift fires twice, 0.5s apart.",
            "Apply two sustained damage windows 6+ seconds apart to exhaust both rewind charges before " +
            "the final kill. Never burst \u2014 a full combo into the rewind threshold is wasted damage."),
        new BestiaryEntry("Void-Walker",
            "Swaps positions with its nearest player after a 1-second telegraphed void vortex. " +
            "Below 50% HP it gains Resistance II (Void Shroud) and becomes slightly faster. " +
            "Tall, dark, and deeply unsettling \u2014 plays Enderman ambient sounds.",
            "Watch for purple vortex particles at your feet \u2014 you have 1 second to move sideways. " +
            "The swap still happens but landing in the open beats landing inside a wall. " +
            "Below 50% HP, switch to true damage or armor-piercing spells to bypass Void Shroud."),
        new BestiaryEntry("Mana-Leech Drake",
            "A flying predator that drains 12 mana from every nearby player every 2 seconds, " +
            "storing it as internal charge. At 40 charge it breathes a sustained hot-pink plasma " +
            "ray (6 dmg/tick + fire). Cannot breathe without charge. Spawns in groups of 1-2.",
            "Stay beyond 6 blocks \u2014 it cannot charge without your mana. If already charged, " +
            "close to melee range; the breath cannot aim at its feet. Kill it before it breathes " +
            "or you will be on fire for 4 seconds minimum."),
        new BestiaryEntry("Ethereal Familiar",
            "Your loyal companion. Floats beside you, vacuums item drops within 10 blocks every " +
            "second, fires homing bolts at nearby monsters every 8s (8 dmg), and boosts your mana " +
            "regeneration by +5 every 5 seconds. Can be leashed.",
            "Summon before entering loot-heavy areas. Keep it alive \u2014 it cannot be re-summoned " +
            "until it fully despawns. Position it away from area-of-effect attacks."),
        new BestiaryEntry("Archon of the Shattered Sky",
            "Three-phase ultimate boss. Phase 1 (100-66%): Lightning Shield reflects non-magic damage " +
            "(5 hits to break), 180\u00b0 sweep beams, Zealot summons every 30s. Phase 2 (65-33%): " +
            "Gravity inversion, arena floor shatters, meteor rain every 3s. " +
            "Phase 3 (32-0%): Rapid 5-bolt volleys, 15s Time Silence windows every 45s " +
            "\u2014 all damage accumulates and detonates when time resumes.",
            "P1: use only magic/true damage \u2014 melee bounces back. P2: never stand still, " +
            "meteors target your last known position. P3: when the world desaturates, STOP ATTACKING " +
            "\u2014 every hit you land will also detonate on you when time resumes. " +
            "Find the Archon in the Arcane Spire structure: /locate structure arcanezenith:arcane_spire")
    );

    private void renderBestiary(GuiGraphics graphics, int contentX) {
        int y = 24;
        int width = this.width - contentX - 20;
        for (var entry : BESTIARY) {
            graphics.drawString(this.font, Component.literal(entry.name()), contentX, y, 0xFFE6D9FF);
            y += 11;
            y = wrapText(graphics, entry.lore(), contentX, y, width, 0xFFCFC4E8);
            graphics.drawString(this.font, Component.literal("Weak point: " + entry.weakPoint()),
                    contentX, y, 0xFF88CC88);
            y += 18;
        }
    }

    // ---------------------------------------------------------------- CRAFTING ----

    private record RecipeEntry(String name, String pattern) {}

    private static final List<RecipeEntry> RECIPES = List.of(
            new RecipeEntry("Arcane Wand", "Amethyst Shard / Stick / Gold Ingot (vertical column)"),
            new RecipeEntry("Arcane Codex", "Amethyst Shard / Book / Amethyst Shard (single row)"),
            new RecipeEntry("Arcane Infusion Table",
                    "Amethyst x3 (top row) / Gold - Enchanting Table - Gold (middle) / Obsidian x3 (bottom row)")
    );

    private void renderCrafting(GuiGraphics graphics, int contentX) {
        int y = 24;
        int width = this.width - contentX - 20;
        for (var recipe : RECIPES) {
            graphics.drawString(this.font, Component.literal(recipe.name()), contentX, y, 0xFFE6D9FF);
            y += 11;
            y = wrapText(graphics, recipe.pattern(), contentX, y, width, 0xFFCFC4E8);
            y += 10;
        }
    }

    // ---------------------------------------------------------------- shared helpers ----

    private int wrapText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        for (var line : this.font.split(Component.literal(text), maxWidth)) {
            graphics.drawString(this.font, line, x, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
