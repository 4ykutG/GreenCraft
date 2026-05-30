package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTab {

    public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath(Bsbairquest.MOD_ID, "yesil_donusum")
    );

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.TEMIZ_JENERATOR))
                .title(Component.literal("Yeşil Dönüşüm"))
                .build()
        );

        ItemGroupEvents.modifyEntriesEvent(KEY).register(entries -> {

            // ── Aşama 1: Fabrika ────────────────────────────────────────
            entries.accept(ModBlocks.KIRLI_BACA);
            entries.accept(ModBlocks.TEMIZ_BACA);
            entries.accept(ModBlocks.DIZEL_JENERATOR);
            entries.accept(ModBlocks.TEMIZ_JENERATOR);
            entries.accept(ModBlocks.KIRLI_SU_TANKI);
            entries.accept(ModBlocks.ARITMA_TANKI);
            entries.accept(ModBlocks.SOGUTMA_BLOKU);
            entries.accept(ModBlocks.ISI_ESANJORU);

            // ── Aşama 2: Kentsel Dönüşüm ────────────────────────────────
            entries.accept(ModBlocks.CORAK_TOPRAK);
            entries.accept(ModBlocks.YESIL_ALAN);
            entries.accept(ModBlocks.BOZUK_ASFALT);
            entries.accept(ModBlocks.BISIKLET_SERIDI);
            entries.accept(ModBlocks.BETON_CATI);
            entries.accept(ModBlocks.YESIL_CATI);
            entries.accept(ModBlocks.BENZIN_ISTASYONU);
            entries.accept(ModBlocks.EV_SARJ_ISTASYONU);

            // ── NPC Spawn Yumurtaları ────────────────────────────────────
            entries.accept(ModEntities.FABRIKA_MUDURU_EGG);
            entries.accept(ModEntities.BELEDIYE_BASKANI_EGG);
            entries.accept(ModEntities.MAHALLE_MUHTARI_EGG);

            // ── Aşama 3: Topluluk ────────────────────────────────────────
            entries.accept(ModBlocks.ATIK_ALANI);
            entries.accept(ModBlocks.GERI_DONUSUM_ISTASYONU);
            entries.accept(ModBlocks.DUZ_CATI);
            entries.accept(ModBlocks.GUNES_PANEL_CATI);
            entries.accept(ModBlocks.KURU_ZEMIN);
            entries.accept(ModBlocks.YAGMUR_SUYU_TANKI);
        });
    }
}
