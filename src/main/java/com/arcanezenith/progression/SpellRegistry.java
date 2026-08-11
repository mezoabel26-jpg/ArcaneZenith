package com.arcanezenith.progression;

import com.arcanezenith.spell.*;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central table of every spell: logic, display name, tier, point cost, element.
 * Tier 0 = free starter | Tier 1-3 = point unlock | Tier 4+ = scroll-only (no point path)
 *
 * Adding a new spell: implement Spell, add one line here. Done.
 */
public final class SpellRegistry {

    /** The 7 magic schools from the design doc, used to lay out the Codex skill tree. */
    public enum Element {
        IGNIS("Ignis", 0xFFFF6A3D),
        FULGUR("Fulgur", 0xFFFFE066),
        UMBRA("Umbra", 0xFF9B59B6),
        SYLVA("Sylva", 0xFF6ABE30),
        CRYO("Cryo", 0xFF6EC6FF),
        CHRONO("Chrono", 0xFFB0A0FF),
        SANCTUM("Sanctum", 0xFFFFF2C0);

        public final String displayName;
        public final int color;

        Element(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    public record Definition(Spell spell, String displayName, int tier, int unlockCost, Element element,
                              ResourceLocation requires) {
        public ResourceLocation id() { return spell.id(); }
    }

    private static final Map<ResourceLocation, Definition> BY_ID = new LinkedHashMap<>();

    private static void add(Spell spell, String name, int tier, int cost, Element element, ResourceLocation requires) {
        BY_ID.put(spell.id(), new Definition(spell, name, tier, cost, element, requires));
    }

    static {
        // --- TIER 0: FREE STARTER ---
        add(new ArcaneBoltSpell(),          "Arcane Bolt",          0,   0, Element.FULGUR,  null);

        // --- TIER 1: 15 pts ---
        add(new TeleportSpell(),            "Teleport",             1,  15, Element.UMBRA,   ArcaneBoltSpell.ID);

        // --- TIER 2: 40 pts ---
        add(new ThunderWarSpell(),          "Thunder War",          2,  40, Element.FULGUR,  TeleportSpell.ID);
        add(new HordeOfBladesSpell(),       "Horde of Blades",      2,  40, Element.SANCTUM, TeleportSpell.ID);
        add(new AetherWingsSpell(),         "Aether Wings",         2,  35, Element.SYLVA,   TeleportSpell.ID);

        // --- TIER 3: 80 pts ---
        add(new GodsSpearSpell(),           "God's Spear",          3,  80, Element.SANCTUM, HordeOfBladesSpell.ID);
        add(new AngelsHelpSpell(),          "Angel's Help",         3,  80, Element.SANCTUM, AetherWingsSpell.ID);
        add(new GravitationalCollapseSpell(),"Gravitational Collapse",3, 90, Element.UMBRA,  ThunderWarSpell.ID);
        add(new PlasmaAnnihilatorSpell(),   "Plasma Annihilator",   3,  90, Element.IGNIS,   ThunderWarSpell.ID);

        // ── TIER 4: ULTIMATE ────────────────────────────────────────────
        add(new MagesHammerSpell(),         "Mage's Hammer",        4, 150, Element.SANCTUM, GodsSpearSpell.ID);
        add(new TimeSilenceSpell(),         "Time Silence",         4, 200, Element.CHRONO,  GravitationalCollapseSpell.ID);
        add(new JudgmentOfHeavenSpell(),    "Judgment of Heaven",   4, 180, Element.SANCTUM, AngelsHelpSpell.ID);
        add(new CataclysmicRiftSpell(),     "Cataclysmic Rift",     4, 180, Element.IGNIS,   PlasmaAnnihilatorSpell.ID);
        add(new SingularityCollapseSpell(), "Singularity Collapse", 4, 300, Element.UMBRA,   GravitationalCollapseSpell.ID);

        // ── TIER 5: LEGENDARY (10 új OP varázslat) ───────────────────
        add(new EldritchTempestSpell(),     "Eldritch Tempest",     5, 400, Element.SANCTUM, MagesHammerSpell.ID);
        add(new AvadaCurseSpell(),          "Avada Curse",          5, 500, Element.UMBRA,   SingularityCollapseSpell.ID);
        add(new StarscourgeMeteoSpell(),    "Starscourge Meteor",   5, 450, Element.IGNIS,   CataclysmicRiftSpell.ID);
        add(new ExcaliburBeamSpell(),       "Excalibur Beam",       5, 420, Element.SANCTUM, JudgmentOfHeavenSpell.ID);
        add(new VortexEssenceSpell(),       "Vortex Essence",       5, 380, Element.UMBRA,   TimeSilenceSpell.ID);
        add(new BladesOfChaosSpell(),       "Blades of Chaos",      5, 360, Element.IGNIS,   PlasmaAnnihilatorSpell.ID);
        add(new LightningStormSpell(),      "Lightning Storm",      5, 480, Element.FULGUR,  ThunderWarSpell.ID);
        add(new ChidoriSpell(),             "Chidori",              5, 350, Element.FULGUR,  GodsSpearSpell.ID);
        add(new GlintstonePhalanxSpell(),   "Glintstone Phalanx",   5, 340, Element.CRYO,    HordeOfBladesSpell.ID);
        add(new CrimsonBandsSpell(),        "Crimson Bands",        5, 440, Element.SANCTUM, AngelsHelpSpell.ID);
    }

    private SpellRegistry() {}

    public static Definition get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Spell getSpell(ResourceLocation id) {
        Definition def = BY_ID.get(id);
        return def == null ? null : def.spell();
    }

    public static List<Definition> all() {
        return BY_ID.values().stream()
                .sorted((a, b) -> Integer.compare(a.tier(), b.tier()))
                .toList();
    }

    /** Get all spell definitions of a given tier. */
    public static List<Definition> ofTier(int tier) {
        return all().stream().filter(d -> d.tier() == tier).toList();
    }

    /** Get all spell definitions belonging to one of the 7 elements, sorted by tier for tree layout. */
    public static List<Definition> ofElement(Element element) {
        return all().stream().filter(d -> d.element() == element).toList();
    }
}
