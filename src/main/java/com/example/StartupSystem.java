package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.List;

/**
 * İlk kez giriş yapan öğrencilere:
 * – Karşılama başlığı
 * – Görev kılavuzu (kitap, otomatik açılır)
 * – Harita
 * – Tüm görev araçları
 * gösterilip/verilir. Tekrar girişlerde gösterilmez (yd_baslandi tag).
 */
public final class StartupSystem {

    /** İlk giriş tag adı — kayıtlı oyuncular tekrar almaz. */
    public static final String STARTED_TAG = "yd_baslandi";

    public static void onFirstJoin(ServerPlayer player, ServerLevel level) {
        showWelcomeTitle(player);
        giveStartingInventory(player, level);
        // Kitap slot-0'da, açılmasını tetikle
        player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
        player.addTag(STARTED_TAG);
    }

    // ─── Karşılama başlığı ────────────────────────────────────────────────

    private static void showWelcomeTitle(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitleTextPacket(
            Component.literal("§6§l✦ Ereğli: Yeşil Dönüşüm ✦")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
            Component.literal("§7Yeşil Dönüşüm Mühendisi olarak göreve başlıyorsun!")));
        // fadeIn=20 tick, stay=100 tick, fadeOut=30 tick
        player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 30));

        player.sendSystemMessage(Component.literal(
            "\n§6══════════════════════════════════\n" +
            "§e  Envanterinde §aGörev Kılavuzu §eve §aHarita §evar!\n" +
            "§7  NPC görevlilere §fsağ tıkla§7 → görev al.\n" +
            "§7  Kirlilik her 30 sn §c-5 §7puan düşürüyor.\n" +
            "§6══════════════════════════════════\n"
        ));
    }

    // ─── Başlangıç envanteri ──────────────────────────────────────────────

    private static void giveStartingInventory(ServerPlayer player, ServerLevel level) {
        BlockPos pos = player.blockPosition();

        // Hotbar slot 0 → Kılavuz kitabı (açılmak için burada olmalı)
        player.getInventory().setItem(0, createGuideBook());

        // Hotbar slot 1 → Harita (ölçek 2 = 512×512 blok alanı görünür)
        player.getInventory().setItem(1,
            MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, false));

        // Hotbar slot 2-8 → Aşama 1 araçları (sık kullanılan)
        player.getInventory().setItem(2, new ItemStack(Items.IRON_TRAPDOOR,    3)); // Baca filtresi
        player.getInventory().setItem(3, new ItemStack(Items.DAYLIGHT_DETECTOR,3)); // Güneş paneli
        player.getInventory().setItem(4, new ItemStack(Items.WATER_BUCKET,     1)); // Su arıtma
        player.getInventory().setItem(5, new ItemStack(Items.COPPER_INGOT,     3)); // Isı geri kazanım
        player.getInventory().setItem(6, new ItemStack(Items.OAK_SAPLING,     3)); // Ağaçlandırma
        player.getInventory().setItem(7, new ItemStack(Items.IRON_INGOT,       3)); // Bisiklet şeridi
        player.getInventory().setItem(8, new ItemStack(Items.MOSS_BLOCK,       3)); // Yeşil çatı

        // Ana envanter → Aşama 2-3 araçları
        player.getInventory().add(new ItemStack(Items.LIGHTNING_ROD, 3)); // EV şarj
        player.getInventory().add(new ItemStack(Items.CHEST,         3)); // Geri dönüşüm
        player.getInventory().add(new ItemStack(Items.GOLD_INGOT,    3)); // Güneş panel çatısı
        player.getInventory().add(new ItemStack(Items.BARREL,        3)); // Yağmur tankı
    }

    // ─── Kılavuz kitabı ───────────────────────────────────────────────────

    private static ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<Filterable<Component>> pages = List.of(

            // Sayfa 1 — Hoş geldin
            Filterable.passThrough(Component.literal(
                "§6§lEreğli:\nYeşil Dönüşüm\n\n" +
                "§rMerhaba, genç mühendis!\n\n" +
                "Ereğli şehrine gelen bir Yeşil Dönüşüm Mühendisisin. Sanayi kirliliği şehri mahvediyor.\n\n" +
                "Göreve hazır mısın?"
            )),

            // Sayfa 2 — Problem
            Filterable.passThrough(Component.literal(
                "§c§lŞehirde Sorun Var!\n\n" +
                "§r☠ Erdemir Fabrikası hava kirletiyor\n\n" +
                "☠ Yollar çökmüş, parklar çorak\n\n" +
                "☠ Eski jeneratörler çalışıyor\n\n" +
                "§aSen bunları düzelteceksin!"
            )),

            // Sayfa 3 — Görev vericiler
            Filterable.passThrough(Component.literal(
                "§6§lGörev Vericiler\n\n" +
                "§cFabrika Müdürü\n" +
                "§7→ Fabrika görevleri\n\n" +
                "§9Belediye Başkanı\n" +
                "§7→ Şehir görevleri\n\n" +
                "§2Mahalle Muhtarı\n" +
                "§7→ Topluluk görevleri\n\n" +
                "§eHepsine §lsağ tıkla!"
            )),

            // Sayfa 4 — Nasıl oynanır
            Filterable.passThrough(Component.literal(
                "§e§lNasıl Oynarsın?\n\n" +
                "§r1) NPC'ye sağ tıkla\n" +
                "§7→ görev listesini al\n\n" +
                "§r2) Elinde doğru araç olsun\n\n" +
                "§r3) Hedef bloğa sağ tıkla\n" +
                "§7→ görevi tamamla ve puan kazan!"
            )),

            // Sayfa 5 — Araç listesi (A1)
            Filterable.passThrough(Component.literal(
                "§6§lAraç Listesi — Fabrika\n\n" +
                "§rDemir Kapak\n§7→ Kirli Baca\n\n" +
                "Güneş Sensörü\n§7→ Dizel Jeneratör\n\n" +
                "Su Kovası\n§7→ Kirli Su Tankı\n\n" +
                "Bakır Külçe\n§7→ Soğutma Bloku"
            )),

            // Sayfa 6 — Araç listesi (A2-3)
            Filterable.passThrough(Component.literal(
                "§6§lAraç Listesi — Kent\n\n" +
                "§rMeşe Fidesi → Çorak Toprak\n" +
                "Demir Külçe → Bozuk Asfalt\n" +
                "Yosun Bloğu → Beton Çatı\n" +
                "Yıldırım Çubuğu → Benzin İst.\n\n" +
                "§6§lMahalle\n" +
                "§rSandık → Atık Alanı\n" +
                "Altın Külçe → Düz Çatı\n" +
                "Fıçı → Kuru Zemin"
            )),

            // Sayfa 7 — Puanlama
            Filterable.passThrough(Component.literal(
                "§6§lÖdüller\n\n" +
                "§r300 Puan:\n" +
                "§6'Çevre Dostu'\n" +
                "§7+ 5 Zümrüt\n\n" +
                "700 Puan:\n" +
                "§b'Sürdürülebilir Şehir'\n" +
                "§7+ 3 Elmas\n\n" +
                "1100 Puan:\n" +
                "§5'Yeşil Dönüşüm Mühendisi'\n" +
                "§7+ Nether Yıldızı"
            )),

            // Sayfa 8 — İpuçları
            Filterable.passThrough(Component.literal(
                "§a§lİpuçları\n\n" +
                "§r★ Kirlilik her 30 saniyede\n" +
                "§c-5 §7puan düşürür!\n\n" +
                "★ Skor tablosu ekranın\n" +
                "sağında görünür.\n\n" +
                "★ Ek araçları başlangıç\n" +
                "meydanındaki sandıkta\n" +
                "bulabilirsin!\n\n" +
                "§eİyi şanslar!"
            ))
        );

        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(
                Filterable.passThrough("Görev Kılavuzu"),
                "BSB Airquest",
                0,
                pages,
                true
            )
        );
        return book;
    }

    // ─── Başlangıç sandığı (BuildMapCommand tarafından çağrılır) ──────────

    public static void placeStartingChest(ServerLevel level, BlockPos origin) {
        BlockPos pos = origin.offset(8, 1, 3);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);

        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setCustomName(Component.literal("§6Görev Araçları Sandığı"));
            chest.setItem( 0, new ItemStack(Items.IRON_TRAPDOOR,    6));
            chest.setItem( 1, new ItemStack(Items.DAYLIGHT_DETECTOR,6));
            chest.setItem( 2, new ItemStack(Items.WATER_BUCKET,     3));
            chest.setItem( 3, new ItemStack(Items.COPPER_INGOT,     6));
            chest.setItem( 4, new ItemStack(Items.OAK_SAPLING,      6));
            chest.setItem( 5, new ItemStack(Items.IRON_INGOT,       6));
            chest.setItem( 6, new ItemStack(Items.MOSS_BLOCK,       6));
            chest.setItem( 7, new ItemStack(Items.LIGHTNING_ROD,    6));
            chest.setItem( 8, new ItemStack(Items.CHEST,            6));
            chest.setItem( 9, new ItemStack(Items.GOLD_INGOT,       6));
            chest.setItem(10, new ItemStack(Items.BARREL,           6));
        }
    }
}
