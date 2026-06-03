package com.example.command;

import com.example.ModBlocks;
import com.example.ModEntities;
import com.example.StartupSystem;
import com.example.entity.NpcEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

/**
 * /yd_harita_olustur — Hikaye odaklı Ereğli dünyasını inşa eder.
 *
 * Dünya akışı (oyuncu güneyde spawn olur, kuzeye doğru ilerler):
 *   Başlangıç Meydanı → Şehir Caddesi → Fabrika → Karadeniz Kıyısı
 *   Batı koldan Şehir Merkezi / Doğu koldan Konut Mahallesi
 */
public final class BuildMapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("yd_harita_olustur")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                ServerLevel level = src.getLevel();
                BlockPos origin = BlockPos.containing(src.getPosition());

                World world = new World(level, origin);
                world.build();

                removeOldNpcs(level, origin);
                int gy = origin.getY() + 1;
                spawnNpc(level, ModEntities.FABRIKA_MUDURU,
                    new BlockPos(origin.getX() - 5, gy, origin.getZ() - 8));
                spawnNpc(level, ModEntities.BELEDIYE_BASKANI,
                    new BlockPos(origin.getX() - 145, gy, origin.getZ() - 90));
                spawnNpc(level, ModEntities.MAHALLE_MUHTARI,
                    new BlockPos(origin.getX() + 135, gy, origin.getZ() - 90));

                StartupSystem.placeStartingChest(level, origin);

                src.sendSuccess(() -> Component.literal(
                    "§a✦ Ereğli haritası tamamlandı! (" + world.count + " blok)\n" +
                    "§7  3 NPC yerleştirildi, sandık hazır.\n" +
                    "§e  Başlamak için meydana git ve kılavuzu oku!"
                ), false);
                return world.count;
            })
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    private static final class World {

        final ServerLevel level;
        final int ox, oy, oz;
        int count;

        World(ServerLevel level, BlockPos o) {
            this.level = level; ox = o.getX(); oy = o.getY(); oz = o.getZ();
        }

        void build() {
            buildTerrain();
            buildStartingPlaza();
            buildMainRoad();
            buildCityCenter();        // batı kolu
            buildNeighborhood();      // doğu kolu
            buildIndustrialCorridor();
            buildFactory();
            buildCoastline();
        }

        // ─── Yardımcılar ──────────────────────────────────────────────────

        void set(int dx, int dy, int dz, Block b) {
            level.setBlock(new BlockPos(ox+dx, oy+dy, oz+dz), b.defaultBlockState(), 3);
            count++;
        }
        void set(int dx, int dy, int dz, BlockState s) {
            level.setBlock(new BlockPos(ox+dx, oy+dy, oz+dz), s, 3);
            count++;
        }
        void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
            BlockState s = b.defaultBlockState();
            for (int x=Math.min(x1,x2);x<=Math.max(x1,x2);x++)
            for (int y=Math.min(y1,y2);y<=Math.max(y1,y2);y++)
            for (int z=Math.min(z1,z2);z<=Math.max(z1,z2);z++) {
                level.setBlock(new BlockPos(ox+x,oy+y,oz+z), s, 3); count++;
            }
        }
        void floor(int x1, int z1, int x2, int z2, Block b) { fill(x1,0,z1,x2,0,z2,b); }
        void column(int dx, int dz, int h, Block b) { for(int y=1;y<=h;y++) set(dx,y,dz,b); }

        /** Duvar+zemin+tavan olan içi boş kutu. */
        void box(int x1, int z1, int x2, int z2, int h, Block fl, Block wall, Block roof) {
            floor(x1,z1,x2,z2,fl);
            for (int y=1;y<=h;y++) {
                for (int x=x1;x<=x2;x++) { set(x,y,z1,wall); set(x,y,z2,wall); }
                for (int z=z1+1;z<z2;z++) { set(x1,y,z,wall); set(x2,y,z,wall); }
            }
            fill(x1,h+1,z1,x2,h+1,z2,roof);
        }

        /** Tüm yüzleri farklı blok. */
        void boxFull(int x1,int z1,int x2,int z2,int h,Block fl,Block wall,Block roof) {
            fill(x1,0,z1,x2,0,z2,fl);
            fill(x1,1,z1,x2,h,z2,wall);
            fill(x1,h+1,z1,x2,h+1,z2,roof);
        }

        /** Bina içine tavan ışığı — her 4 blokta bir glowstone. */
        void lightInterior(int x1, int z1, int x2, int z2, int ceilY) {
            for (int x = x1+2; x <= x2-2; x += 4)
                for (int z = z1+2; z <= z2-2; z += 4)
                    set(x, ceilY, z, Blocks.GLOWSTONE);
        }

        /** Duvara monte tabelası. */
        void sign(int dx, int dy, int dz, Direction facing, String l1, String l2) {
            BlockPos pos = new BlockPos(ox+dx, oy+dy, oz+dz);
            BlockState state = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            level.setBlock(pos, state, 3); count++;
            if (level.getBlockEntity(pos) instanceof SignBlockEntity s) {
                SignText text = new SignText()
                    .setMessage(0, Component.literal(l1))
                    .setMessage(1, Component.literal(l2));
                s.setText(text, true);
            }
        }
        void sign4(int dx,int dy,int dz,Direction dir,String l1,String l2,String l3,String l4) {
            BlockPos pos = new BlockPos(ox+dx,oy+dy,oz+dz);
            level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, dir), 3); count++;
            if (level.getBlockEntity(pos) instanceof SignBlockEntity s) {
                SignText t = new SignText()
                    .setMessage(0,Component.literal(l1))
                    .setMessage(1,Component.literal(l2))
                    .setMessage(2,Component.literal(l3))
                    .setMessage(3,Component.literal(l4));
                s.setText(t,true);
            }
        }

        void lampPost(int dx, int dz) {
            column(dx,dz,3,Blocks.IRON_BARS);
            set(dx,4,dz,Blocks.LANTERN);
        }
        void deadTree(int dx, int dz, int h) { column(dx,dz,h,Blocks.OAK_LOG); }

        void chestAt(int dx,int dy,int dz,ItemStack... items) {
            BlockPos pos=new BlockPos(ox+dx,oy+dy,oz+dz);
            level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3); count++;
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity c)
                for(int i=0;i<items.length&&i<27;i++) c.setItem(i,items[i]);
        }

        // ─── 1. GENEL ARAZİ ───────────────────────────────────────────────
        void buildTerrain() {
            // Genel çim zemin (tüm harita alanı)
            fill(-220,0,-330, 220,0,30, Blocks.GRASS_BLOCK);

            // Fabrikaya yakın kirlenmiş alan (kuzey)
            fill(-95,0,-180, 95,0,-330, Blocks.COARSE_DIRT);

            // Fabrika-şehir geçiş bölgesi (kirlilik gradient)
            fill(-60,0,-160, 60,0,-180, Blocks.GRAVEL);
        }

        // ─── 2. BAŞLANGIÇ MEYDANI ─────────────────────────────────────────
        void buildStartingPlaza() {
            // Taş tuğla zemin
            floor(-18,-18,18,18, Blocks.STONE_BRICKS);
            // Granit çerçeve
            for (int x=-18;x<=18;x++) { set(x,0,-18,Blocks.POLISHED_GRANITE); set(x,0,18,Blocks.POLISHED_GRANITE); }
            for (int z=-17;z<=17;z++) { set(-18,0,z,Blocks.POLISHED_GRANITE); set(18,0,z,Blocks.POLISHED_GRANITE); }
            // Altın merkez + çevre dekorasyon
            floor(-1,-1,1,1,Blocks.GOLD_BLOCK);
            set(0,1,0,Blocks.BEACON);

            // Fıskiye havuzu (4 köşe)
            floor(-8,-8,-5,-5,Blocks.BLUE_CONCRETE);
            floor(5,-8,8,-5,Blocks.BLUE_CONCRETE);
            floor(-8,5,-5,8,Blocks.BLUE_CONCRETE);
            floor(5,5,8,8,Blocks.BLUE_CONCRETE);
            for (int[] p:new int[][]{{-6,-6},{6,-6},{-6,6},{6,6}}) {
                set(p[0],1,p[1],Blocks.WATER_CAULDRON); // havuz
            }

            // Ağaçlar meydan köşelerinde
            for (int[] p:new int[][]{{-14,-14},{14,-14},{-14,14},{14,14}}) {
                column(p[0],p[1],5,Blocks.OAK_LOG);
                fill(p[0]-2,5,p[1]-2,p[0]+2,7,p[1]+2,Blocks.OAK_LEAVES);
            }

            // Giriş tabelası
            sign4(0,2,-19,Direction.NORTH,
                "§6§lEreğli'ye", "§6§lHoş Geldiniz!",
                "§7Yeşil Mühendis", "§7olmaya hazır mısın?");

            // NPC bekleyiş noktaları (renkli yastık)
            set(-6,0,-10,Blocks.ORANGE_WOOL);   // Fabrika Müdürü
            set(-9,0,6, Blocks.BLUE_WOOL);      // Belediye Başkanı
            set(9,0,6,  Blocks.GREEN_WOOL);     // Mahalle Muhtarı

            // Köşe lambalar
            lampPost(-16,-16); lampPost(16,-16);
            lampPost(-16,16);  lampPost(16,16);

            // Sandık + bilgi tabelası
            chestAt(12,1,10,
                new ItemStack(Items.IRON_TRAPDOOR,3),
                new ItemStack(Items.DAYLIGHT_DETECTOR,3));
            sign(13,2,10,Direction.WEST,"§6Yedek","Araçlar");
        }

        // ─── 3. ANA YOL (güney→fabrika) ──────────────────────────────────
        void buildMainRoad() {
            // Ana cadde (kuzey-güney, 9 blok geniş)
            for (int z=-19;z>=-175;z--) {
                for (int x=-4;x<=4;x++) set(x,0,z,Blocks.STONE_BRICKS);
                set(-5,0,z,Blocks.SMOOTH_STONE); set(5,0,z,Blocks.SMOOTH_STONE);
                set(-6,0,z,Blocks.SMOOTH_STONE); set(6,0,z,Blocks.SMOOTH_STONE);
            }
            // Yol çizgisi
            for (int z=-25;z>=-170;z-=4) { set(0,0,z,Blocks.YELLOW_CONCRETE); }
            // Lamba direkleri
            for (int z=-28;z>=-170;z-=16) { lampPost(-8,z); lampPost(8,z); }

            // Batı yolu (meydandan şehir merkezine)
            for (int x=-19;x>=-205;x--) {
                for (int z=-4;z<=4;z++) set(x,0,z,Blocks.STONE_BRICKS);
                set(x,0,-5,Blocks.SMOOTH_STONE); set(x,0,5,Blocks.SMOOTH_STONE);
            }
            // Doğu yolu (meydandan mahalleye)
            for (int x=19;x<=205;x++) {
                for (int z=-4;z<=4;z++) set(x,0,z,Blocks.STONE_BRICKS);
                set(x,0,-5,Blocks.SMOOTH_STONE); set(x,0,5,Blocks.SMOOTH_STONE);
            }
        }

        // ─── 4. ŞEHİR MERKEZİ (batı) ─────────────────────────────────────
        void buildCityCenter() {
            fill(-220,0,-200,-60,0,-20, Blocks.GRASS_BLOCK);

            // Şehir içi yollar
            fill(-220,0,-30,-60,0,-22,Blocks.STONE_BRICKS);  // güney cadde
            fill(-155,0,-22,-147,0,-200,Blocks.STONE_BRICKS); // merkez cadde

            // ── Belediye Binası ──
            buildCityHall();

            // ── Kasvetli Park (corak_toprak görevi) ──
            buildDecayedPark();

            // ── Çarşı Sırası ──
            buildShopRow();

            // ── Bozuk Asfalt (bisiklet şeridi görevi) ──
            buildBrokenRoad();

            // ── Benzin İstasyonu ──
            buildGasStation();

            // ── Beton Çatılı Binalar ──
            buildConcreteRoofBuildings();

            // Lambalar
            for (int x=-205;x<=-70;x+=20) lampPost(x,-26);
            for (int z=-35;z>=-195;z-=20) lampPost(-153,z);
        }

        void buildCityHall() {
            // Dış cephe (2 katlı)
            box(-200,-70,-135,-115, 12, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
            lightInterior(-200,-115,-135,-70, 12);
            // Pencereler
            for (int y : new int[]{3,8}) {
                for (int x : new int[]{-195,-185,-175,-165,-155,-145,-135}) {
                    set(x,y,-70,Blocks.GLASS_PANE); set(x,y,-115,Blocks.GLASS_PANE);
                }
                for (int z=-75;z>=-110;z-=5) {
                    set(-200,y,z,Blocks.GLASS_PANE); set(-135,y,z,Blocks.GLASS_PANE);
                }
            }
            // Kapı
            fill(-169,1,-70,-165,3,-70,Blocks.AIR);
            // Saat kulesi (köşede)
            column(-200,-70,16,Blocks.STONE_BRICKS);
            set(-200,17,-70,Blocks.GOLD_BLOCK);
            // İÇİ: masalar + sandalyeler
            // Masa = çit + levha
            for (int[] t:new int[][]{{-185,-85},{-175,-85},{-165,-85}}) {
                set(t[0],1,t[1],Blocks.OAK_FENCE);
                set(t[0],2,t[1],Blocks.OAK_PRESSURE_PLATE);
            }
            // Kitaplık
            fill(-198,1,-90,-198,3,-80,Blocks.BOOKSHELF);
            // Tabela
            sign4(-167,4,-71,Direction.NORTH,
                "§9§lBELEDİYE","§9§lBAŞKANLIĞI",
                "Karadeniz Ereğli","Yeşil Dönüşüm");
        }

        void buildDecayedPark() {
            // Çorak park zemini
            floor(-130,-55,-80,-30, Blocks.COARSE_DIRT);
            // Yıkık çevre çiti
            for (int x=-130;x<=-80;x+=3) set(x,1,-55,Blocks.OAK_FENCE);
            for (int x=-130;x<=-80;x+=3) set(x,1,-30,Blocks.OAK_FENCE);
            // Kurumuş ağaçlar
            deadTree(-110,-50,4); deadTree(-100,-40,5); deadTree(-90,-45,3);
            // Dead bushes
            for (int[] p:new int[][]{{-120,-45},{-105,-48},{-95,-38},{-115,-35}})
                set(p[0],1,p[1],Blocks.DEAD_BUSH);
            // Kırık çeşme
            fill(-107,0,-42,-104,2,-38,Blocks.COBBLESTONE);
            set(-105,2,-40,Blocks.AIR);  // kırık üst kısım
            // Görev blokları
            set(-115,1,-48, ModBlocks.CORAK_TOPRAK);
            set(-100,1,-42, ModBlocks.CORAK_TOPRAK);
            set(-88, 1,-35, ModBlocks.CORAK_TOPRAK);
            // Park tabelası
            sign4(-130,2,-43,Direction.EAST,
                "§2Şehir Parkı","§8(Yenileme","§8Bekliyor)","§aFidan Dik!");
        }

        void buildShopRow() {
            // 4 dükkan sıralı
            int[][] shops = {{-215,-80},{-215,-100},{-215,-120},{-215,-140}};
            for (int i=0;i<shops.length;i++) {
                int x=shops[i][0], z=shops[i][1];
                box(x,z,x+20,z+15, 5, Blocks.OAK_PLANKS, Blocks.BRICKS, Blocks.OAK_PLANKS);
                lightInterior(x,z,x+20,z+15, 5);
                // Vitrin penceresi
                fill(x+5,2,z,x+15,4,z,Blocks.GLASS_PANE);
                // Kapı aralığı
                fill(x+9,1,z,x+11,3,z,Blocks.AIR);
                // Tezgah içinde
                set(x+2,1,z+7,Blocks.CRAFTING_TABLE);
                set(x+17,1,z+7,Blocks.BARREL);
            }
            // Dükkan tabelaları
            sign(-210,6,-73,Direction.EAST,"§6Ereğli","Bakkalı");
            sign(-210,6,-93,Direction.EAST,"§cKapalı",":(");
            sign(-210,6,-113,Direction.EAST,"§aKahvehane","Açık");
            sign(-210,6,-133,Direction.EAST,"§9Eczane","Devam");
        }

        void buildBrokenRoad() {
            // Bozuk asfalt bölümü (görev blokları)
            for (int x=-130;x>=-205;x-=10) {
                set(x,0,-27, ModBlocks.BOZUK_ASFALT);
                // Çukur efekti
                set(x-2,0,-27,Blocks.GRAVEL);
                set(x+2,0,-27,Blocks.GRAVEL);
            }
        }

        void buildGasStation() {
            // Zemin
            floor(-115,-185,-85,-165, Blocks.GRAY_CONCRETE);
            // Kanopi
            fill(-118,5,-188,-82,5,-162, Blocks.IRON_BLOCK);
            // Direkler
            column(-117,-187,4,Blocks.IRON_BARS); column(-83,-187,4,Blocks.IRON_BARS);
            column(-117,-163,4,Blocks.IRON_BARS); column(-83,-163,4,Blocks.IRON_BARS);
            // Pompa + görev bloğu
            set(-100,1,-176, ModBlocks.BENZIN_ISTASYONU);
            set(-100,2,-176, Blocks.DISPENSER); // pompa başlığı
            // Tabela
            sign4(-100,6,-187,Direction.NORTH,"§cBenzin","İstasyonu","§8EV Şarj","§7dönüşümü?");
        }

        void buildConcreteRoofBuildings() {
            int[][] b={{-80,-100},{-80,-140},{-80,-175},{-190,-165}};
            for (int i=0;i<b.length;i++) {
                int x=b[i][0], z=b[i][1];
                // Bina gövdesi
                box(x-9,z-9,x+9,z+9,6,Blocks.COBBLESTONE,Blocks.COBBLESTONE,ModBlocks.BETON_CATI);
                lightInterior(x-9,z-9,x+9,z+9, 6);
                // Pencereler
                fill(x-7,2,z-9,x-3,4,z-9,Blocks.GLASS_PANE);
                fill(x+3,2,z-9,x+7,4,z-9,Blocks.GLASS_PANE);
                // Kapı
                fill(x-1,1,z-9,x+1,3,z-9,Blocks.AIR);
                // İç: yataklar
                level.setBlock(new BlockPos(ox+x-5,oy+1,oz+z+2),
                    Blocks.RED_BED.defaultBlockState()
                        .setValue(BlockStateProperties.BED_PART, BedPart.FOOT)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3); count++;
                set(x+3,1,z-2,Blocks.CRAFTING_TABLE);
                set(x+3,1,z+4,Blocks.FURNACE);
            }
        }

        // ─── 5. KONUT MAHALLESİ (doğu) ───────────────────────────────────
        void buildNeighborhood() {
            fill(60,0,-200,220,0,-20, Blocks.GRASS_BLOCK);

            // Mahalle yolları
            fill(60,0,-22,220,0,-20,Blocks.STONE_BRICKS);
            fill(108,0,-22,116,0,-200,Blocks.STONE_BRICKS);
            fill(158,0,-22,166,0,-200,Blocks.STONE_BRICKS);

            buildMuhtarlik();
            buildResidentialHouses();
            buildWasteDumps();
            buildDryAreas();
            buildOldPlayground();

            for (int x=70;x<=205;x+=20) lampPost(x,-26);
        }

        void buildMuhtarlik() {
            // Muhtarlık binası (görev vericinin ofisi)
            box(70,-65,100,-40, 6, Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
            lightInterior(70,-65,100,-40, 6);
            fill(79,2,-65,91,4,-65,Blocks.GLASS_PANE); // vitrin
            fill(83,1,-65,87,3,-65,Blocks.AIR);        // kapı
            // İç: muhtarın masası
            set(85,1,-50,Blocks.OAK_FENCE); set(85,2,-50,Blocks.OAK_PRESSURE_PLATE);
            fill(72,1,-62,72,3,-44,Blocks.BOOKSHELF);
            // Tabela
            sign4(85,7,-66,Direction.NORTH,
                "§2MAHALLE","§2MUHTARLIĞI",
                "Topluluk","Görevleri");
        }

        void buildResidentialHouses() {
            // 5 farklı ev stili
            buildHouseA(100,-80);   // küçük ev
            buildHouseB(155,-75);   // orta ev
            buildHouseA(205,-80);   // küçük ev
            buildHouseB(100,-140);  // orta ev
            buildHouseC(160,-145);  // büyük ev
        }

        void buildHouseA(int cx, int cz) {
            box(cx-7,cz-6,cx+7,cz+6, 5, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, ModBlocks.DUZ_CATI);
            lightInterior(cx-7,cz-6,cx+7,cz+6, 5);
            fill(cx-5,2,cz-6,cx-1,4,cz-6,Blocks.GLASS_PANE);
            fill(cx+1,2,cz-6,cx+5,4,cz-6,Blocks.GLASS_PANE);
            fill(cx-1,1,cz-6,cx+1,3,cz-6,Blocks.AIR);
            // İç detay
            set(cx-4,1,cz+3,Blocks.FURNACE);
            set(cx+4,1,cz+3,Blocks.CHEST);
            level.setBlock(new BlockPos(ox+cx-3,oy+1,oz+cz-3),
                Blocks.WHITE_BED.defaultBlockState()
                    .setValue(BlockStateProperties.BED_PART,BedPart.FOOT)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH),3); count++;
            // Kirli bahçe
            for (int[] p:new int[][]{{cx-9,cz-2},{cx-9,cz+2},{cx+9,cz-2}})
                set(p[0],0,p[1],Blocks.COARSE_DIRT);
            set(cx-10,1,cz+1,Blocks.DEAD_BUSH);
        }

        void buildHouseB(int cx, int cz) {
            box(cx-9,cz-8,cx+9,cz+8, 7, Blocks.BRICKS, Blocks.BRICKS, ModBlocks.GUNES_PANEL_CATI);
            lightInterior(cx-9,cz-8,cx+9,cz+8, 7);
            fill(cx-7,2,cz-8,cx-2,5,cz-8,Blocks.GLASS_PANE);
            fill(cx+2,2,cz-8,cx+7,5,cz-8,Blocks.GLASS_PANE);
            fill(cx-1,1,cz-8,cx+1,4,cz-8,Blocks.AIR);
            // İç: oturma odası + mutfak
            set(cx-5,1,cz+3,Blocks.CRAFTING_TABLE);
            set(cx+5,1,cz+3,Blocks.BOOKSHELF);
            fill(cx-6,1,cz-5,cx-3,1,cz-3,Blocks.OAK_PLANKS); // halı
            // Bahçe çiti
            for (int x=cx-11;x<=cx+11;x++) { set(x,1,cz-10,Blocks.OAK_FENCE); set(x,1,cz+10,Blocks.OAK_FENCE); }
            for (int z=cz-9;z<=cz+9;z++) { set(cx-11,1,z,Blocks.OAK_FENCE); set(cx+11,1,z,Blocks.OAK_FENCE); }
        }

        void buildHouseC(int cx, int cz) {
            // İki katlı büyük ev
            box(cx-11,cz-10,cx+11,cz+10, 5, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.OAK_PLANKS);
            box(cx-9, cz-8, cx+9, cz+8, 5, Blocks.AIR, Blocks.STONE_BRICKS, ModBlocks.DUZ_CATI);
            fill(cx-8,2,cz-10,cx-3,4,cz-10,Blocks.GLASS_PANE);
            fill(cx+3,2,cz-10,cx+8,4,cz-10,Blocks.GLASS_PANE);
            fill(cx-1,1,cz-10,cx+1,4,cz-10,Blocks.AIR);
            // İç: sofalar
            set(cx,1,cz+5,Blocks.OAK_STAIRS);
            fill(cx-7,1,cz-8,cx-4,1,cz-5,Blocks.OAK_PLANKS);
            set(cx+5,1,cz-5,Blocks.CHEST);
            // Kuru bahçe
            fill(cx-13,0,cz-12,cx+13,0,cz+12,Blocks.SAND);
            for (int[] p:new int[][]{{cx-10,cz-8},{cx+10,cz-8},{cx-10,cz+8}})
                set(p[0],1,p[1],Blocks.DEAD_BUSH);
            set(cx,1,cz-13, ModBlocks.KURU_ZEMIN);
        }

        void buildWasteDumps() {
            // 2 atık toplama noktası
            floor(75,-50,95,-70,Blocks.COARSE_DIRT);
            set(85,1,-60, ModBlocks.ATIK_ALANI);
            // Atık yığını görünümü
            for (int i=0;i<8;i++)
                set(75+(int)(Math.random()*18),1,-50-(int)(Math.random()*18),Blocks.GRAVEL);
            set(80,2,-55,Blocks.BARREL); set(90,2,-55,Blocks.BARREL);
            sign4(85,2,-49,Direction.SOUTH,
                "§cAtık Alanı","§7Geri Dönüşüm",
                "§7istasyonu","§akur!");

            floor(170,-50,190,-70,Blocks.COARSE_DIRT);
            set(180,1,-60, ModBlocks.ATIK_ALANI);
            set(175,2,-55,Blocks.BARREL); set(185,2,-55,Blocks.BARREL);
        }

        void buildDryAreas() {
            floor(115,-120,135,-140,Blocks.SAND);
            set(125,1,-130, ModBlocks.KURU_ZEMIN);
            set(120,1,-132,Blocks.DEAD_BUSH); set(130,1,-128,Blocks.DEAD_BUSH);

            floor(165,-105,185,-125,Blocks.SAND);
            set(175,1,-115, ModBlocks.KURU_ZEMIN);
        }

        void buildOldPlayground() {
            // Eski ve bakımsız oyun parkı
            floor(130,-55,170,-90,Blocks.COARSE_DIRT);
            // Kırık tahterevalli
            set(140,1,-72,Blocks.OAK_SLAB); set(150,1,-72,Blocks.OAK_SLAB);
            column(145,-72,2,Blocks.OAK_FENCE);
            // Pas tutmuş salıncak direkleri
            column(135,-65,3,Blocks.IRON_BARS); column(145,-65,3,Blocks.IRON_BARS);
            set(140,4,-65,Blocks.CHAIN);
            column(155,-65,3,Blocks.IRON_BARS); column(165,-65,3,Blocks.IRON_BARS);
            set(160,4,-65,Blocks.CHAIN);
            sign(145,4,-62,Direction.SOUTH,"§cBakımsız","Park :(");
        }

        // ─── 6. ENDÜSTRİYEL KORIDOR (fabrika girişi) ─────────────────────
        void buildIndustrialCorridor() {
            // Geniş sanayi yolu
            fill(-8,0,-162, 8,0,-180, Blocks.STONE_BRICKS);
            fill(-40,0,-162,-8,0,-180, Blocks.GRAY_CONCRETE);
            fill(8,0,-162, 40,0,-180, Blocks.GRAY_CONCRETE);

            // Depolar (her iki yantta)
            buildWarehouse(-55,-165,-30,-178);
            buildWarehouse(30,-165,55,-178);

            // Bekçi kulübesi
            box(-4,-160,4,-174, 4, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
            fill(-2,1,-160,2,3,-160,Blocks.GLASS_PANE);
            // Bariyer
            set(-8,1,-170,Blocks.IRON_BARS); set(-8,2,-170,Blocks.IRON_BARS);
            set(8,1,-170,Blocks.IRON_BARS);  set(8,2,-170,Blocks.IRON_BARS);

            // Kirlilik gradient (yol boyunca)
            for (int z=-162;z>=-178;z--) {
                set(-9,0,z,Blocks.GRAVEL); set(9,0,z,Blocks.GRAVEL);
            }
            // Dead bushes
            set(-20,1,-167,Blocks.DEAD_BUSH); set(20,1,-167,Blocks.DEAD_BUSH);
            set(-35,1,-172,Blocks.DEAD_BUSH); set(35,1,-172,Blocks.DEAD_BUSH);

            // Tabela
            sign4(0,5,-162,Direction.NORTH,
                "§8ERDEMİR A.Ş.","§8Fabrika Girişi",
                "§cDikkat: Kirli","§7Bölge");
        }

        void buildWarehouse(int x1, int z1, int x2, int z2) {
            boxFull(x1,z1,x2,z2,5,Blocks.GRAY_CONCRETE,Blocks.GRAY_CONCRETE,Blocks.IRON_BLOCK);
            // Büyük kapı aralığı
            int cx=(x1+x2)/2, cz=Math.max(z1,z2);
            fill(cx-3,1,cz,cx+3,4,cz,Blocks.AIR);
            // İç: variller
            set(x1+2,1,z1+2,Blocks.BARREL); set(x1+2,1,z1+4,Blocks.BARREL);
            set(x2-2,1,z1+2,Blocks.BARREL); set(x2-2,1,z1+4,Blocks.BARREL);
        }

        // ─── 7. ERDEMİR FABRİKA KOMPLEKSİ ───────────────────────────────
        void buildFactory() {
            // Kirlenmiş sanayi zemini
            fill(-92,0,-182, 92,0,-320, Blocks.NETHERRACK);
            // Kısmen beton yüzey
            fill(-92,0,-182, 92,0,-200, Blocks.GRAY_CONCRETE);
            fill(-70,0,-200, 70,0,-320, Blocks.GRAY_CONCRETE);

            // Çevre beton duvarı
            for (int x=-92;x<=92;x++) {
                set(x,1,-182,Blocks.STONE_BRICKS); set(x,2,-182,Blocks.STONE_BRICKS);
                set(x,3,-182,Blocks.STONE_BRICK_WALL);
            }
            // Lav çukurları (sanayi atığı)
            for (int[] p:new int[][]{{-80,-193},{-60,-215},{70,-210},{80,-240}})
                fill(p[0]-2,0,p[1]-2,p[0]+2,0,p[1]+2,Blocks.LAVA);

            // Ana fabrika binası
            buildMainFactoryBuilding();

            // Görev blokları
            placeChimneys();
            placeGenerators();
            placeWaterTreatment();
            placeCoolingTowers();

            // Çevre dead bushes ve ağaç gövdeleri
            for (int[] p:new int[][]{{-85,-195},{-75,-220},{75,-200},{85,-235},{-50,-310},{50,-308}}) {
                set(p[0],1,p[1],Blocks.DEAD_BUSH);
            }
            for (int[] p:new int[][]{{-88,-205},{88,-208},{-88,-250},{88,-255}})
                deadTree(p[0],p[1],6);
        }

        void buildMainFactoryBuilding() {
            // Büyük ana yapı
            box(-65,-190, 65,-305, 15, Blocks.GRAY_CONCRETE, Blocks.STONE_BRICKS, Blocks.GRAY_CONCRETE);
            lightInterior(-65,-305,65,-190, 15);  // fabrika tavan ışıkları
            // Siyah cam pencereler (kirli ortam)
            for (int y : new int[]{4,9}) {
                for (int x=-60;x<=60;x+=8) {
                    set(x,y,-190,Blocks.BLACK_STAINED_GLASS_PANE);
                    set(x,y,-305,Blocks.BLACK_STAINED_GLASS_PANE);
                }
            }
            // Giriş kapısı
            fill(-5,1,-190, 5,6,-191, Blocks.AIR);
            // Kontrol kulesi (üst yapı)
            box(-20,-225, 20,-265, 8, Blocks.GRAY_CONCRETE, Blocks.STONE_BRICKS, Blocks.IRON_BLOCK);
            fill(-15,2,-225,15,6,-225,Blocks.GRAY_STAINED_GLASS_PANE); // kontrol camı

            // İÇ DETAYLAR: Üretim hattı
            // Konveyör bant simülasyonu
            for (int x=-50;x<=50;x+=4) {
                set(x,1,-230,Blocks.SMOOTH_STONE_SLAB);
                set(x,1,-240,Blocks.SMOOTH_STONE_SLAB);
                set(x,1,-250,Blocks.SMOOTH_STONE_SLAB);
            }
            // Makine gövdeleri
            for (int[] m:new int[][]{{-40,-235},{-20,-235},{0,-235},{20,-235},{40,-235}}) {
                set(m[0],2,m[1],Blocks.DISPENSER);
                set(m[0],3,m[1],Blocks.PISTON);
            }
            // Boru hatları
            for (int z=-200;z>=-300;z-=3) {
                set(-62,8,z,Blocks.IRON_BARS); set(62,8,z,Blocks.IRON_BARS);
            }
            // Kontrol odası içi
            fill(-15,1,-240,-5,1,-245,Blocks.OAK_PLANKS); // zemin
            set(-12,2,-243,Blocks.OBSERVER); // panel
            set(-8,2,-243,Blocks.COMPARATOR);
            set(-5,2,-243,Blocks.DAYLIGHT_DETECTOR); // ironi!

            // Fabrika tabelaları
            sign4(0,10,-191,Direction.NORTH,
                "§8ERDEMİR A.Ş.","§8Demir Çelik",
                "§cÜretim Alanı","§8Giriş Yasak");
            sign4(0,3,-265,Direction.NORTH,
                "§8Kontrol","§8Odası",
                "§cFabrika","§8Müdürü");
        }

        void placeChimneys() {
            // 6 kirli baca — farklı boy direklere monte
            int[][] pos={{-50,-198},{-25,-202},{12,-198},{45,-208},{-55,-252},{48,-254}};
            for (int[] p:pos) {
                floor(p[0]-3,p[1]-3,p[0]+3,p[1]+3,Blocks.GRAY_CONCRETE);
                // Baca tabanı
                column(p[0]-1,p[1]-1,4,Blocks.STONE_BRICKS);
                column(p[0]+1,p[1]-1,4,Blocks.STONE_BRICKS);
                column(p[0]-1,p[1]+1,4,Blocks.STONE_BRICKS);
                column(p[0]+1,p[1]+1,4,Blocks.STONE_BRICKS);
                // Görev bloku
                set(p[0],5,p[1], ModBlocks.KIRLI_BACA);
                set(p[0],6,p[1], ModBlocks.KIRLI_BACA);
                // Baca tepesi halkası
                for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++)
                    set(p[0]+dx,9,p[1]+dz,Blocks.STONE_BRICKS);
            }
        }

        void placeGenerators() {
            // 4 dizel jeneratör — jeneratör odasında
            int[][] pos={{-42,-225},{-16,-235},{18,-225},{44,-238}};
            for (int[] p:pos) {
                floor(p[0]-3,p[1]-3,p[0]+3,p[1]+3,Blocks.GRAY_CONCRETE);
                set(p[0],1,p[1], ModBlocks.DIZEL_JENERATOR);
                // Egzoz borusu
                column(p[0]+2,p[1],5,Blocks.IRON_BARS);
            }
        }

        void placeWaterTreatment() {
            // Su arıtma tesisi (ayrı yapı, fabrika batısı)
            box(-90,-255,-68,-285, 8, Blocks.GRAY_CONCRETE, Blocks.STONE_BRICKS, Blocks.IRON_BLOCK);
            fill(-85,2,-255,-73,6,-255,Blocks.GLASS_PANE);
            // Görev blokları
            set(-85,1,-265, ModBlocks.KIRLI_SU_TANKI);
            set(-85,2,-265, ModBlocks.KIRLI_SU_TANKI);
            set(-75,1,-272, ModBlocks.KIRLI_SU_TANKI);
            set(-75,2,-272, ModBlocks.KIRLI_SU_TANKI);
            set(-80,1,-280, ModBlocks.KIRLI_SU_TANKI);
            // Kirli su görünümü
            fill(-87,-1,-258,-68,-1,-288,Blocks.WATER);
            sign4(-79,9,-256,Direction.NORTH,
                "§9Su Arıtma","§9Tesisi",
                "§c[KİRLİ]","§aArıtma Kur!");
        }

        void placeCoolingTowers() {
            // Soğutma kuleleri (fabrika doğusu)
            int[][] pos={{55,-262},{70,-278},{55,-290}};
            for (int[] p:pos) {
                // Kule gövdesi (silindir hissi için köşeli)
                box(p[0]-4,p[1]-4,p[0]+4,p[1]+4, 10, Blocks.GRAY_CONCRETE, Blocks.SMOOTH_STONE, Blocks.AIR);
                set(p[0],11,p[1], ModBlocks.SOGUTMA_BLOKU);
                // Su buharı efekti (beyaz cam)
                set(p[0],12,p[1],Blocks.WHITE_STAINED_GLASS);
                set(p[0],13,p[1],Blocks.WHITE_STAINED_GLASS);
            }
        }

        // ─── 8. KARADENİZ KIYISI ─────────────────────────────────────────
        void buildCoastline() {
            // Deniz
            fill(-100,-1,-300,100,-1,-330,Blocks.WATER);
            fill(-100,0,-300,100,0,-330,Blocks.WATER);
            // Kumsalı kıyı
            fill(-100,0,-296,100,0,-300,Blocks.SAND);
            fill(-100,0,-291,100,0,-296,Blocks.GRAVEL);

            // Balıkçı iskelesi (kirlilik nedeniyle terk edilmiş)
            for (int z=-300;z>=-320;z--) {
                set(-30,1,z,Blocks.OAK_PLANKS); set(-20,1,z,Blocks.OAK_PLANKS);
                set(-10,1,z,Blocks.OAK_PLANKS); set(0,1,z,Blocks.OAK_PLANKS);
            }
            for (int z=-300;z>=-320;z-=4) {
                column(-30,z,2,Blocks.OAK_FENCE); column(0,z,2,Blocks.OAK_FENCE);
            }
            // Kırık tekneler (birkaç sandık)
            set(-25,2,-312,Blocks.BARREL); set(-15,2,-312,Blocks.BARREL);
            set(-5,2,-312,Blocks.CHEST);
            // Terk edilmiş işaret
            sign4(-15,3,-299,Direction.NORTH,
                "§8Ereğli","§8Balıkçı","§8İskelesi","§c[KAPALI]");

            // Kıyıda dead bushes + kirlilik izi
            for (int x=-90;x<=90;x+=10) set(x,1,-297,Blocks.DEAD_BUSH);
            // Fabrika atık kanalı (denize akan kirlilik)
            fill(-5,-1,-288,-5,-1,-300,Blocks.LAVA);

            sign4(0,3,-296,Direction.NORTH,
                "§4UYARI","§cKirlenmiş","§cSahil","§7Denize girme!");
        }
    }

    // ─── NPC yardımcıları ──────────────────────────────────────────────────

    private static void removeOldNpcs(ServerLevel level, BlockPos origin) {
        AABB area=new AABB(origin.getX()-500,level.getMinBuildHeight(),origin.getZ()-500,
                           origin.getX()+500,level.getMaxBuildHeight(),origin.getZ()+500);
        level.getEntitiesOfClass(NpcEntity.class,area).forEach(e->e.discard());
    }

    private static void spawnNpc(ServerLevel level, EntityType<NpcEntity> type, BlockPos pos) {
        NpcEntity npc=type.create(level); if(npc==null)return;
        npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0f,0f);
        npc.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.MOB_SUMMONED,null);
        level.addFreshEntity(npc);
    }
}
