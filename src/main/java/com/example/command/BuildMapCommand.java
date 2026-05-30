package com.example.command;

import com.example.ModBlocks;
import com.example.ModEntities;
import com.example.StartupSystem;
import com.example.entity.NpcEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * /yd_harita_olustur  —  OP yetkisi gerektirir.
 * Komutu çalıştıran oyuncunun ayakta durduğu Y seviyesi zemin kabul edilir.
 * Harita yaklaşık 400×300 blok, 3 bölgeden oluşur.
 */
public final class BuildMapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("yd_harita_olustur")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                ServerLevel level = src.getLevel();
                BlockPos origin = BlockPos.containing(src.getPosition());

                // 1. Haritayı inşa et
                int placed = new MapBuilder(level, origin).build();

                // 2. Başlangıç sandığını yerleştir
                StartupSystem.placeStartingChest(level, origin);

                // 2. NPC'leri doğrudan haritaya yerleştir — kod değiştirmene gerek yok!
                int groundY = origin.getY() + 1;
                BlockPos fabrikaPos   = new BlockPos(origin.getX(),       groundY, origin.getZ() - 155);
                BlockPos belediyePos  = new BlockPos(origin.getX() - 140, groundY, origin.getZ() -  90);
                BlockPos mahallePos   = new BlockPos(origin.getX() + 140, groundY, origin.getZ() -  90);

                // Varsa eski NPC'leri kaldır, yoksa yerleştir
                removeOldNpcs(level, origin);
                spawnNpc(level, ModEntities.FABRIKA_MUDURU,   fabrikaPos);
                spawnNpc(level, ModEntities.BELEDIYE_BASKANI, belediyePos);
                spawnNpc(level, ModEntities.MAHALLE_MUHTARI,  mahallePos);

                src.sendSuccess(() -> Component.literal(
                    "§a══════════════════════════════════════\n" +
                    "§a  Ereğli haritası oluşturuldu! (" + placed + " blok)\n" +
                    "§a  3 NPC görevli yerleştirildi.\n" +
                    "§a  Başlamak için meydana git ve NPC'lere sağ tıkla!\n" +
                    "§a══════════════════════════════════════"
                ), false);
                return placed;
            })
        );
    }

    // ─── NPC yardımcıları ─────────────────────────────────────────────────

    /** Harita alanındaki eski NPC'leri temizler (haritayı yeniden oluştururken). */
    private static void removeOldNpcs(ServerLevel level, BlockPos origin) {
        AABB area = new AABB(
            origin.getX() - 500, level.getMinBuildHeight(), origin.getZ() - 500,
            origin.getX() + 500, level.getMaxBuildHeight(), origin.getZ() + 500
        );
        level.getEntitiesOfClass(NpcEntity.class, area).forEach(e -> e.discard());
    }

    private static void spawnNpc(ServerLevel level, EntityType<NpcEntity> type, BlockPos pos) {
        NpcEntity npc = type.create(level);
        if (npc == null) return;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(npc);
    }

    // ══════════════════════════════════════════════════════════════════════
    private static final class MapBuilder {

        private final ServerLevel world;
        private final int ox, oy, oz;
        private int count;

        MapBuilder(ServerLevel world, BlockPos origin) {
            this.world = world;
            this.ox = origin.getX();
            this.oy = origin.getY();
            this.oz = origin.getZ();
        }

        int build() {
            buildStartingPlaza();
            buildMainRoad();
            buildWestRoad();
            buildEastRoad();
            buildFactory();
            buildCityCenter();
            buildNeighborhood();
            return count;
        }

        // ─── İnşa yardımcıları ────────────────────────────────────────────

        void set(int dx, int dy, int dz, Block b) {
            world.setBlock(new BlockPos(ox+dx, oy+dy, oz+dz), b.defaultBlockState(), 3);
            count++;
        }

        void set(int dx, int dy, int dz, BlockState s) {
            world.setBlock(new BlockPos(ox+dx, oy+dy, oz+dz), s, 3);
            count++;
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
            BlockState s = b.defaultBlockState();
            int minX = Math.min(x1,x2), maxX = Math.max(x1,x2);
            int minY = Math.min(y1,y2), maxY = Math.max(y1,y2);
            int minZ = Math.min(z1,z2), maxZ = Math.max(z1,z2);
            for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(new BlockPos(ox+x, oy+y, oz+z), s, 3);
                count++;
            }
        }

        void floor(int x1, int z1, int x2, int z2, Block b) { fill(x1, 0, z1, x2, 0, z2, b); }

        /** Hollow box: zemin + 4 duvar + tavan. */
        void box(int x1, int z1, int x2, int z2, int h, Block floor, Block wall, Block roof) {
            floor(x1, z1, x2, z2, floor);
            for (int y = 1; y <= h; y++) {
                for (int x = x1; x <= x2; x++) { set(x, y, z1, wall); set(x, y, z2, wall); }
                for (int z = z1+1; z < z2; z++) { set(x1, y, z, wall); set(x2, y, z, wall); }
            }
            fill(x1, h+1, z1, x2, h+1, z2, roof);
        }

        void column(int dx, int dz, int h, Block b) {
            for (int y = 1; y <= h; y++) set(dx, y, dz, b);
        }

        void lampPost(int dx, int dz) {
            set(dx, 1, dz, Blocks.STONE_BRICK_WALL);
            set(dx, 2, dz, Blocks.STONE_BRICK_WALL);
            set(dx, 3, dz, Blocks.LANTERN);
        }

        // ─── 1. BAŞLANGIÇ MEYDANI ─────────────────────────────────────────
        void buildStartingPlaza() {
            // Taş zemin
            floor(-16, -16, 16, 16, Blocks.STONE_BRICKS);
            // Granit çerçeve
            for (int x = -16; x <= 16; x++) {
                set(x, 0, -16, Blocks.POLISHED_GRANITE); set(x, 0, 16, Blocks.POLISHED_GRANITE);
            }
            for (int z = -15; z <= 15; z++) {
                set(-16, 0, z, Blocks.POLISHED_GRANITE); set(16, 0, z, Blocks.POLISHED_GRANITE);
            }
            // Merkez altın platform
            floor(-2, -2, 2, 2, Blocks.GOLD_BLOCK);

            // NPC bekleme alanları (renkli beton)
            floor(-7, -10, -4, -7, Blocks.ORANGE_CONCRETE);  // Fabrika Müdürü
            floor(-10,  2, -7,  5, Blocks.BLUE_CONCRETE);    // Belediye Başkanı
            floor(  4,  2,  7,  5, Blocks.GREEN_CONCRETE);   // Mahalle Muhtarı

            // Köşe lambalar
            lampPost(-14, -14); lampPost(14, -14);
            lampPost(-14,  14); lampPost(14,  14);
        }

        // ─── 2. YOLLAR ────────────────────────────────────────────────────
        void buildMainRoad() {
            // Kuzey ana yol (plaza → fabrika)
            for (int z = -17; z >= -172; z--) {
                for (int x = -4; x <= 4; x++) set(x, 0, z, Blocks.STONE_BRICKS);
                set(-5, 0, z, Blocks.SMOOTH_STONE); set(5, 0, z, Blocks.SMOOTH_STONE);
            }
            // Lamba direkleri
            for (int z = -25; z >= -165; z -= 16) { lampPost(-7, z); lampPost(7, z); }
        }

        void buildWestRoad() {
            for (int x = -17; x >= -200; x--) {
                for (int z = -4; z <= 4; z++) set(x, 0, z, Blocks.STONE_BRICKS);
                set(x, 0, -5, Blocks.SMOOTH_STONE); set(x, 0, 5, Blocks.SMOOTH_STONE);
            }
        }

        void buildEastRoad() {
            for (int x = 17; x <= 200; x++) {
                for (int z = -4; z <= 4; z++) set(x, 0, z, Blocks.STONE_BRICKS);
                set(x, 0, -5, Blocks.SMOOTH_STONE); set(x, 0, 5, Blocks.SMOOTH_STONE);
            }
        }

        // ─── 3. ERDEMİR FABRİKA KOMPLEKSİ ───────────────────────────────
        void buildFactory() {
            // Kirleşmiş sanayi zemini
            fill(-90, 0, -175, 90, 0, -320, Blocks.GRAY_CONCRETE);

            // Çevre duvarı
            for (int x = -90; x <= 90; x++) {
                set(x, 1, -175, Blocks.STONE_BRICKS); set(x, 2, -175, Blocks.STONE_BRICKS);
            }

            // Ana fabrika binası (giriş kuzey-güney)
            box(-65, -185, 65, -305, 14, Blocks.GRAY_CONCRETE, Blocks.STONE_BRICKS, Blocks.GRAY_CONCRETE);

            // İkinci kat (kontrol kulesi)
            box(-20, -220, 20, -265, 8, Blocks.GRAY_CONCRETE, Blocks.SMOOTH_STONE, Blocks.IRON_BLOCK);

            // Giriş kapısı açıklığı
            fill(-6, 1, -182, 6, 6, -186, Blocks.AIR);

            // ── Görev 1: 6 Kirli Baca ──
            int[][] bacaPos = {{-50,-195},{-25,-200},{10,-195},{45,-205},{-55,-248},{48,-250}};
            for (int[] p : bacaPos) {
                floor(p[0]-2, p[1]-2, p[0]+2, p[1]+2, Blocks.GRAY_CONCRETE);
                set(p[0], 1, p[1], ModBlocks.KIRLI_BACA);
                column(p[0]-1, p[1], 10, Blocks.STONE_BRICKS);
                column(p[0]+1, p[1], 10, Blocks.STONE_BRICKS);
                column(p[0], p[1]-1, 10, Blocks.STONE_BRICKS);
                column(p[0], p[1]+1, 10, Blocks.STONE_BRICKS);
            }

            // ── Görev 2: 4 Dizel Jeneratör ──
            int[][] jenPos = {{-40,-222},{-15,-232},{18,-222},{43,-235}};
            for (int[] p : jenPos) {
                floor(p[0]-2, p[1]-2, p[0]+2, p[1]+2, Blocks.GRAY_CONCRETE);
                set(p[0], 1, p[1], ModBlocks.DIZEL_JENERATOR);
            }

            // ── Görev 3: 3 Kirli Su Tankı ──
            int[][] tankPos = {{-60,-258},{-42,-272},{-62,-282}};
            for (int[] p : tankPos) {
                floor(p[0]-3, p[1]-3, p[0]+3, p[1]+3, Blocks.GRAY_CONCRETE);
                set(p[0], 1, p[1], ModBlocks.KIRLI_SU_TANKI);
                set(p[0], 2, p[1], ModBlocks.KIRLI_SU_TANKI);
            }

            // ── Görev 4: 3 Soğutma Bloku ──
            int[][] soguPos = {{45,-262},{58,-275},{45,-285}};
            for (int[] p : soguPos) {
                floor(p[0]-2, p[1]-2, p[0]+2, p[1]+2, Blocks.GRAY_CONCRETE);
                set(p[0], 1, p[1], ModBlocks.SOGUTMA_BLOKU);
            }
        }

        // ─── 4. ŞEHİR MERKEZİ ────────────────────────────────────────────
        void buildCityCenter() {
            // Çim zemin
            fill(-200, 0, -20, -70, 0, -200, Blocks.GRASS_BLOCK);

            // Sokaklar
            fill(-200, 0, -20, -70, 0, -12, Blocks.STONE_BRICKS); // bağlantı yolu
            fill(-200, 0, -55, -70, 0, -47, Blocks.SMOOTH_STONE); // kaldırım
            fill(-150, 0, -200, -142, 0, -55, Blocks.STONE_BRICKS); // merkez cadde

            // Belediye Binası
            box(-185, -65, -130, -140, 10, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
            // Bayrak direği
            column(-157, -52, 12, Blocks.OAK_FENCE);

            // ── Görev 5: 3 Çorak Toprak ──
            set(-100, 1, -90, ModBlocks.CORAK_TOPRAK);
            set(-120, 1, -105, ModBlocks.CORAK_TOPRAK);
            set(-165, 1, -120, ModBlocks.CORAK_TOPRAK);

            // ── Görev 6: 4 Bozuk Asfalt ──
            set(-100, 0, -44, ModBlocks.BOZUK_ASFALT);
            set(-115, 0, -44, ModBlocks.BOZUK_ASFALT);
            set(-130, 0, -44, ModBlocks.BOZUK_ASFALT);
            set(-145, 0, -44, ModBlocks.BOZUK_ASFALT);

            // ── Görev 7: 4 Beton Çatılı Bina ──
            int[][] binalar = {{-90,-105},{-90,-140},{-90,-170},{-185,-130}};
            for (int[] b : binalar) {
                box(b[0]-8, b[1]-8, b[0]+8, b[1]+8, 6, Blocks.OAK_PLANKS, Blocks.OAK_LOG, ModBlocks.BETON_CATI);
            }

            // ── Görev 8: Benzin İstasyonu ──
            floor(-108, -72, -82, -60, Blocks.GRAY_CONCRETE);
            // Kanopi
            fill(-110, 5, -74, -80, 5, -58, Blocks.IRON_BLOCK);
            column(-110, -74, 4, Blocks.IRON_BARS); column(-80, -74, 4, Blocks.IRON_BARS);
            column(-110, -58, 4, Blocks.IRON_BARS); column(-80, -58, 4, Blocks.IRON_BARS);
            set(-95, 1, -66, ModBlocks.BENZIN_ISTASYONU);

            // Lambalar
            lampPost(-170, -30); lampPost(-120, -30); lampPost(-80, -30);
        }

        // ─── 5. KONUT MAHALLESİ ───────────────────────────────────────────
        void buildNeighborhood() {
            // Çim zemin
            fill(70, 0, -20, 210, 0, -200, Blocks.GRASS_BLOCK);

            // Sokaklar
            fill(70, 0, -20, 210, 0, -12, Blocks.STONE_BRICKS);      // bağlantı yolu
            fill(105, 0, -200, 113, 0, -20, Blocks.STONE_BRICKS);    // ana mahalle caddesi
            fill(155, 0, -200, 163, 0, -20, Blocks.STONE_BRICKS);    // yan cadde

            // ── Görev 9: 2 Atık Alanı ──
            floor(75, -45, 95, -70, Blocks.COARSE_DIRT);
            set(85, 1, -57, ModBlocks.ATIK_ALANI);
            floor(170, -50, 190, -75, Blocks.COARSE_DIRT);
            set(180, 1, -62, ModBlocks.ATIK_ALANI);

            // ── Görev 10: 3 Düz Çatılı + 2 Güneş Panelli Ev ──
            int[][] evler = {{100,-75},{130,-75},{165,-75},{100,-145},{165,-145}};
            Block[] catiBloklari = {
                ModBlocks.DUZ_CATI, ModBlocks.DUZ_CATI, ModBlocks.DUZ_CATI,
                ModBlocks.GUNES_PANEL_CATI, ModBlocks.GUNES_PANEL_CATI
            };
            for (int i = 0; i < evler.length; i++) {
                int[] e = evler[i];
                box(e[0]-6, e[1]-6, e[0]+6, e[1]+6, 5, Blocks.COBBLESTONE, Blocks.COBBLESTONE, catiBloklari[i]);
                // Kapı açıklığı
                fill(e[0]-1, 1, e[1]+6, e[0]+1, 3, e[1]+7, Blocks.AIR);
            }

            // ── Görev 11: 2 Kuru Zemin ──
            floor(118, -120, 135, -140, Blocks.SAND);
            set(126, 1, -130, ModBlocks.KURU_ZEMIN);
            floor(170, -105, 188, -125, Blocks.SAND);
            set(179, 1, -115, ModBlocks.KURU_ZEMIN);

            // Lambalar
            lampPost(75, -30); lampPost(120, -30); lampPost(165, -30);
            lampPost(75, -100); lampPost(120, -100); lampPost(165, -100);
        }
    }
}
