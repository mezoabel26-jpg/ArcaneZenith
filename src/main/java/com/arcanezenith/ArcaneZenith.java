package com.arcanezenith;

import com.arcanezenith.block.ModBlocks;
import com.arcanezenith.block.ModOres;
import com.arcanezenith.capability.ModAttachments;
import com.arcanezenith.client.particle.ModParticles;
import com.arcanezenith.event.ManaRegenHandler;
import com.arcanezenith.item.ModItems;
import com.arcanezenith.item.OreItems;
import com.arcanezenith.network.ModNetworking;
import com.arcanezenith.registry.ModCreativeTabs;
import com.arcanezenith.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ArcaneZenith.MOD_ID)
public class ArcaneZenith {

    public static final String MOD_ID = "arcanezenith";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ArcaneZenith(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, com.arcanezenith.config.ArcaneZenithConfig.SPEC, "arcanezenith-client.toml");
        // Force static init of ore classes before the registers fire
        ModOres.init();
        OreItems.init();

        // Items, blocks, sounds, attachments
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        // Original block registries
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        ModBlocks.MENU_TYPES.register(modEventBus);

        // Ore block registries
        ModOres.BLOCKS.register(modEventBus);

        // Entity types
        com.arcanezenith.entity.ModEntities.ENTITY_TYPES.register(modEventBus);

        // Worldgen — struktúrák és biomok
        com.arcanezenith.worldgen.ModStructures.STRUCTURE_TYPES.register(modEventBus);
        com.arcanezenith.worldgen.ModStructures.STRUCTURE_PIECE_TYPES.register(modEventBus);
        com.arcanezenith.worldgen.ModBiomes.BIOMES.register(modEventBus);

        // Custom particles
        ModParticles.PARTICLES.register(modEventBus);

        // Config regisztrálás
        // config registration via constructor
        modEventBus.addListener(com.arcanezenith.config.ArcaneZenithConfig::onLoad);
        modEventBus.addListener(com.arcanezenith.config.ArcaneZenithConfig::onReload);

        // Networking
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);

        // Server-side game events
        NeoForge.EVENT_BUS.register(new ManaRegenHandler());
        NeoForge.EVENT_BUS.register(new com.arcanezenith.event.TimeSilenceDamageHandler());
        NeoForge.EVENT_BUS.register(new com.arcanezenith.event.EnemyReactionHandler());
    }

    private void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        // Config screen accessible via G key in-game (Codex) or directly from mod
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Natural spawn placements — mobs spawn in dark caves/overworld like vanilla monsters
            net.minecraft.world.entity.SpawnPlacements.register(
                    com.arcanezenith.entity.ModEntities.ARCANE_ZEALOT.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
            net.minecraft.world.entity.SpawnPlacements.register(
                    com.arcanezenith.entity.ModEntities.CHRONO_WEAVER.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
            net.minecraft.world.entity.SpawnPlacements.register(
                    com.arcanezenith.entity.ModEntities.VOID_WALKER.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
            net.minecraft.world.entity.SpawnPlacements.register(
                    com.arcanezenith.entity.ModEntities.MANA_LEECH_DRAKE.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        });
        LOGGER.info("Arcane Zenith ready — 14 spells, 6 entities, 5 particles, 5 shaders, 4 arcane ores.");
    }

    private void onEntityAttributeCreation(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(com.arcanezenith.entity.ModEntities.ARCANE_ZEALOT.get(),
                com.arcanezenith.entity.ArcaneZealotEntity.createAttributes().build());
        event.put(com.arcanezenith.entity.ModEntities.CHRONO_WEAVER.get(),
                com.arcanezenith.entity.ChronoWeaverEntity.createAttributes().build());
        event.put(com.arcanezenith.entity.ModEntities.VOID_WALKER.get(),
                com.arcanezenith.entity.VoidWalkerEntity.createAttributes().build());
        event.put(com.arcanezenith.entity.ModEntities.MANA_LEECH_DRAKE.get(),
                com.arcanezenith.entity.ManaLeechDrakeEntity.createAttributes().build());
        event.put(com.arcanezenith.entity.ModEntities.ETHEREAL_FAMILIAR.get(),
                com.arcanezenith.entity.EtherealFamiliarEntity.createAttributes().build());
        event.put(com.arcanezenith.entity.ModEntities.ARCHON.get(),
                com.arcanezenith.entity.boss.ArchonEntity.createAttributes().build());
    }
}
