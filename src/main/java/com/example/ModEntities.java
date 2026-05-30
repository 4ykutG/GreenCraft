package com.example;

import com.example.entity.NpcEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModEntities {

    // ── 3 ayrı entity tipi — her biri kendi isim/diyaloğunu taşır ──────────

    public static final EntityType<NpcEntity> FABRIKA_MUDURU = registerEntity("fabrika_muduru");
    public static final EntityType<NpcEntity> BELEDIYE_BASKANI = registerEntity("belediye_baskani");
    public static final EntityType<NpcEntity> MAHALLE_MUHTARI = registerEntity("mahalle_muhtari");

    // ── Spawn yumurtaları (Yaratıcı sekmede görünür) ──────────────────────

    // Renk: koyu kahve (bg) + turuncu (highlight) → endüstriyel
    public static final Item FABRIKA_MUDURU_EGG = registerEgg(
        "fabrika_muduru_spawn_egg", FABRIKA_MUDURU, 0x5C3D1B, 0xE07000);

    // Renk: lacivert (bg) + altın (highlight) → resmi
    public static final Item BELEDIYE_BASKANI_EGG = registerEgg(
        "belediye_baskani_spawn_egg", BELEDIYE_BASKANI, 0x1A3A6B, 0xD4A900);

    // Renk: koyu yeşil (bg) + beyaz (highlight) → topluluk
    public static final Item MAHALLE_MUHTARI_EGG = registerEgg(
        "mahalle_muhtari_spawn_egg", MAHALLE_MUHTARI, 0x2D6E3E, 0xF0F0F0);

    // ─────────────────────────────────────────────────────────────────────

    private static EntityType<NpcEntity> registerEntity(String name) {
        return Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Bsbairquest.MOD_ID, name),
            EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.95f)
                .clientTrackingRange(10)
                .build()
        );
    }

    private static Item registerEgg(String name, EntityType<NpcEntity> type,
                                     int bgColor, int highlightColor) {
        return Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Bsbairquest.MOD_ID, name),
            new SpawnEggItem(type, bgColor, highlightColor, new Item.Properties())
        );
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(FABRIKA_MUDURU,  NpcEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BELEDIYE_BASKANI, NpcEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MAHALLE_MUHTARI, NpcEntity.createAttributes());
    }
}
