package com.example;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ModTasks {

    /**
     * Bir yeşil dönüşüm görevi tanımı.
     * consumeAsBucket: true ise su kovası boş kovaya dönüşür (itemi yok olmaz).
     */
    public record Task(
        Block targetBlock,
        Item triggerItem,
        Block resultBlock,
        int points,
        SoundEvent sound,
        float volume,
        float pitch,
        String message,
        boolean consumeAsBucket
    ) {
        // Kısaltma constructor — çoğu görev için su kovası dönüşümü yok
        public Task(Block target, Item trigger, Block result, int pts,
                    SoundEvent sound, float vol, float pitch, String msg) {
            this(target, trigger, result, pts, sound, vol, pitch, msg, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AŞAMA 1 — Erdemir Fabrika İyileştirmesi  (450 puan toplam)
    // ═══════════════════════════════════════════════════════════════════════
    public static final List<Task> PHASE_1 = List.of(

        new Task(ModBlocks.KIRLI_BACA, Items.IRON_TRAPDOOR, ModBlocks.TEMIZ_BACA,
            100, SoundEvents.STONE_BREAK, 1.0f, 0.8f,
            "§a[FABRIKA] Grafen filtre takıldı! Baca temizlendi. (+100 Puan)"),

        new Task(ModBlocks.DIZEL_JENERATOR, Items.DAYLIGHT_DETECTOR, ModBlocks.TEMIZ_JENERATOR,
            150, SoundEvents.ANVIL_LAND, 0.5f, 1.5f,
            "§b[FABRIKA] Güneş paneli devreye girdi! Temiz enerji aktif. (+150 Puan)"),

        new Task(ModBlocks.KIRLI_SU_TANKI, Items.WATER_BUCKET, ModBlocks.ARITMA_TANKI,
            120, SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f,
            "§3[FABRIKA] Su arıtma sistemi kuruldu! Su kalitesi arttı. (+120 Puan)",
            true),   // su kovası → boş kova

        new Task(ModBlocks.SOGUTMA_BLOKU, Items.COPPER_INGOT, ModBlocks.ISI_ESANJORU,
            80, SoundEvents.ANVIL_LAND, 0.6f, 1.3f,
            "§6[FABRIKA] Isı geri kazanım sistemi aktif! Enerji verimliliği arttı. (+80 Puan)")
    );

    // ═══════════════════════════════════════════════════════════════════════
    //  AŞAMA 2 — Kentsel Dönüşüm  (340 puan toplam)
    // ═══════════════════════════════════════════════════════════════════════
    public static final List<Task> PHASE_2 = List.of(

        new Task(ModBlocks.CORAK_TOPRAK, Items.OAK_SAPLING, ModBlocks.YESIL_ALAN,
            50, SoundEvents.GRASS_BREAK, 1.0f, 0.9f,
            "§2[KENT] Fidan dikildi! Yeşil alan oluşturuldu. (+50 Puan)"),

        new Task(ModBlocks.BOZUK_ASFALT, Items.IRON_INGOT, ModBlocks.BISIKLET_SERIDI,
            70, SoundEvents.STONE_BREAK, 1.0f, 1.1f,
            "§7[KENT] Bisiklet şeridi çizildi! Karbon emisyonları azaldı. (+70 Puan)"),

        new Task(ModBlocks.BETON_CATI, Items.MOSS_BLOCK, ModBlocks.YESIL_CATI,
            90, SoundEvents.GRASS_BREAK, 0.9f, 0.8f,
            "§a[KENT] Yeşil çatı kuruldu! Isı adası etkisi azaldı. (+90 Puan)"),

        new Task(ModBlocks.BENZIN_ISTASYONU, Items.LIGHTNING_ROD, ModBlocks.EV_SARJ_ISTASYONU,
            130, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f,
            "§e[KENT] EV şarj istasyonu kuruldu! Fosil yakıt emisyonları azaldı. (+130 Puan)")
    );

    // ═══════════════════════════════════════════════════════════════════════
    //  AŞAMA 3 — Topluluk Katılımı  (330 puan toplam)
    // ═══════════════════════════════════════════════════════════════════════
    public static final List<Task> PHASE_3 = List.of(

        new Task(ModBlocks.ATIK_ALANI, Items.CHEST, ModBlocks.GERI_DONUSUM_ISTASYONU,
            100, SoundEvents.CHEST_OPEN, 0.8f, 1.0f,
            "§d[TOPLULUK] Geri dönüşüm istasyonu açıldı! Atık miktarı azaldı. (+100 Puan)"),

        new Task(ModBlocks.DUZ_CATI, Items.GOLD_INGOT, ModBlocks.GUNES_PANEL_CATI,
            150, SoundEvents.ANVIL_LAND, 0.5f, 1.8f,
            "§e[TOPLULUK] Çatı güneş paneli kuruldu! Mahalle temiz enerji üretiyor. (+150 Puan)"),

        new Task(ModBlocks.KURU_ZEMIN, Items.BARREL, ModBlocks.YAGMUR_SUYU_TANKI,
            80, SoundEvents.BUCKET_FILL, 1.0f, 1.0f,
            "§9[TOPLULUK] Yağmur suyu toplama sistemi kuruldu! Su tasarrufu sağlandı. (+80 Puan)")
    );

    // Tüm görevler — tek listede (blok etkileşim döngüsü için)
    public static final List<Task> ALL;
    static {
        ALL = new java.util.ArrayList<>();
        ALL.addAll(PHASE_1);
        ALL.addAll(PHASE_2);
        ALL.addAll(PHASE_3);
    }

    // NPC diyalog metinleri
    public static final String NPC_FABRIKA_MUDURU =
        "§6══════════════════════════════════\n" +
        "§e  FABRIKA MÜDÜRÜ — Görev Listesi\n" +
        "§6══════════════════════════════════\n" +
        "§7▶ §aGörev 1: §fKirli bacalara Grafen Filtre tak\n" +
        "§7  Araç: §eDemir Kapak §7→ §aKirli Baca\n" +
        "§7▶ §aGörev 2: §fDizel jeneratörleri dönüştür\n" +
        "§7  Araç: §eGüneş Sensörü §7→ §aDizel Jeneratör\n" +
        "§7▶ §aGörev 3: §fSu arıtma sistemi kur\n" +
        "§7  Araç: §eSu Kovası §7→ §aKirli Su Tankı\n" +
        "§7▶ §aGörev 4: §fIsı geri kazanım sistemi\n" +
        "§7  Araç: §eBakır Külçe §7→ §aSoğutma Bloku\n" +
        "§6══════════════════════════════════";

    public static final String NPC_BELEDIYE_BASKANI =
        "§6══════════════════════════════════\n" +
        "§e  BELEDİYE BAŞKANI — Görev Listesi\n" +
        "§6══════════════════════════════════\n" +
        "§7▶ §aGörev 5: §fÇorak alanlara fidan dik\n" +
        "§7  Araç: §eMeşe Fidesi §7→ §aÇorak Toprak\n" +
        "§7▶ §aGörev 6: §fBisiklet şeridi çiz\n" +
        "§7  Araç: §eDemir Külçe §7→ §aBozuk Asfalt\n" +
        "§7▶ §aGörev 7: §fÇatılara yeşil örtü yap\n" +
        "§7  Araç: §eYosun Bloğu §7→ §aBeton Çatı\n" +
        "§7▶ §aGörev 8: §fBenzin istasyonunu dönüştür\n" +
        "§7  Araç: §eYıldırım Çubuğu §7→ §aBenzin İstasyonu\n" +
        "§6══════════════════════════════════";

    public static final String NPC_MAHALLE_MUHTARI =
        "§6══════════════════════════════════\n" +
        "§e  MAHALLE MUHTARI — Görev Listesi\n" +
        "§6══════════════════════════════════\n" +
        "§7▶ §aGörev 9:  §fGeri dönüşüm istasyonu kur\n" +
        "§7  Araç: §eSandık §7→ §aAtık Alanı\n" +
        "§7▶ §aGörev 10: §fÇatılara güneş paneli kur\n" +
        "§7  Araç: §eAltın Külçe §7→ §aDüz Çatı\n" +
        "§7▶ §aGörev 11: §fYağmur suyu toplayıcı kur\n" +
        "§7  Araç: §eFıçı §7→ §aKuru Zemin\n" +
        "§6══════════════════════════════════";
}
