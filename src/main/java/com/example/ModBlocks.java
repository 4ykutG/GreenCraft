package com.example;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    // ═══════════════════════════════════════════════════════════════════════
    //  AŞAMA 1 — Erdemir Fabrika İyileştirmesi
    // ═══════════════════════════════════════════════════════════════════════

    public static final Block KIRLI_BACA = register("kirli_baca",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(2.0f).sound(SoundType.STONE).noOcclusion()));

    public static final Block TEMIZ_BACA = register("temiz_baca",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(2.0f).sound(SoundType.STONE).noOcclusion()));

    public static final Block DIZEL_JENERATOR = register("dizel_jenerator",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final Block TEMIZ_JENERATOR = register("temiz_jenerator",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).noOcclusion()));

    public static final Block KIRLI_SU_TANKI = register("kirli_su_tanki",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final Block ARITMA_TANKI = register("aritma_tanki",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final Block SOGUTMA_BLOKU = register("sogutma_bloku",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final Block ISI_ESANJORU = register("isi_esanjoru",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    // ═══════════════════════════════════════════════════════════════════════
    //  AŞAMA 2 — Kentsel Dönüşüm
    // ═══════════════════════════════════════════════════════════════════════

    public static final Block CORAK_TOPRAK = register("corak_toprak",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.GRAVEL)));

    public static final Block YESIL_ALAN = register("yesil_alan",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GRASS).strength(0.5f).sound(SoundType.GRASS)));

    public static final Block BOZUK_ASFALT = register("bozuk_asfalt",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(1.5f).sound(SoundType.STONE)));

    public static final Block BISIKLET_SERIDI = register("bisiklet_seridi",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(1.5f).sound(SoundType.STONE)));

    public static final Block BETON_CATI = register("beton_cati",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(2.0f).sound(SoundType.STONE)));

    public static final Block YESIL_CATI = register("yesil_cati",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GRASS).strength(2.0f).sound(SoundType.GRASS)));

    public static final Block BENZIN_ISTASYONU = register("benzin_istasyonu",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final Block EV_SARJ_ISTASYONU = register("ev_sarj_istasyonu",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    // ═══════════════════════════════════════════════════════════════════════
    //  AŞAMA 3 — Topluluk Katılımı
    // ═══════════════════════════════════════════════════════════════════════

    public static final Block ATIK_ALANI = register("atik_alani",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT).strength(1.0f).sound(SoundType.STONE)));

    public static final Block GERI_DONUSUM_ISTASYONU = register("geri_donusum_istasyonu",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GRASS).strength(2.0f).sound(SoundType.METAL)));

    public static final Block DUZ_CATI = register("duz_cati",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(2.0f).sound(SoundType.STONE)));

    public static final Block GUNES_PANEL_CATI = register("gunes_panel_cati",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final Block KURU_ZEMIN = register("kuru_zemin",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.GRAVEL)));

    public static final Block YAGMUR_SUYU_TANKI = register("yagmur_suyu_tanki",
        new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)));

    // ─────────────────────────────────────────────────────────────────────
    private static Block register(String name, Block block) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Bsbairquest.MOD_ID, name);
        Block registered = Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(registered, new Item.Properties()));
        return registered;
    }

    public static void init() {}
}
