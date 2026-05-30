package com.example;

import com.example.command.BuildMapCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.example.entity.NpcEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bsbairquest implements ModInitializer {

    public static final String MOD_ID = "bsb-airquest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String OBJECTIVE_ID = "ogrenci_puan";

    // Duman partikülü: her kaç tick'te bir kontrol edilsin
    private static final int SMOKE_INTERVAL_TICKS = 20;   // 1 saniye
    // Kirlilik puanı düşümü: her kaç tick'te bir
    private static final int POLLUTION_INTERVAL_TICKS = 600; // 30 saniye
    // Kirlilik düşüş miktarı
    private static final int POLLUTION_DECAY = 5;

    // NPC'ler zaten spawn edildi mi (sunucu oturumu başına bir kez)
    private boolean npcsSpawned = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Ereğli: Yeşil Dönüşüm — mod başlatılıyor...");

        ModBlocks.init();
        ModEntities.init();
        ModCreativeTab.init();

        ServerLifecycleEvents.SERVER_STARTED.register(this::setupScoreboard);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            initPlayerScore(server, player);

            // İlk kez katılan öğrencilere başlangıç ekranı + envanter
            if (!player.getTags().contains(StartupSystem.STARTED_TAG)) {
                server.execute(() -> StartupSystem.onFirstJoin(player, server.overworld()));
            }

            // İlk oyuncu bağlandığında NPC kontrolü yap
            if (!npcsSpawned) {
                npcsSpawned = true;
                server.execute(() -> checkAndSpawnNpcs(server.overworld()));
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
            BuildMapCommand.register(dispatcher));

        registerBlockInteractions();
        registerTickSystems();

        LOGGER.info("Tüm sistemler aktif. Toplam {} görev yüklendi.", ModTasks.ALL.size());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BLOK ETKİLEŞİM — Görev sistemi
    // ─────────────────────────────────────────────────────────────────────
    private void registerBlockInteractions() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND)         return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp))      return InteractionResult.PASS;

            BlockPos pos       = hitResult.getBlockPos();
            BlockState state   = world.getBlockState(pos);
            ItemStack held     = player.getItemInHand(hand);
            Scoreboard sb      = serverLevel.getServer().getScoreboard();
            Objective obj      = sb.getObjective(OBJECTIVE_ID);

            for (ModTasks.Task task : ModTasks.ALL) {
                if (!state.is(task.targetBlock()) || !held.is(task.triggerItem())) continue;

                // Eşyayı tüket
                if (task.consumeAsBucket()) {
                    held.shrink(1);
                    if (!sp.getAbilities().instabuild) {
                        sp.getInventory().add(new ItemStack(Items.BUCKET));
                    }
                } else {
                    held.shrink(1);
                }

                // Bloğu dönüştür, ses çal, mesaj gönder
                world.setBlock(pos, task.resultBlock().defaultBlockState(), 3);
                world.playSound((Player) null, pos, task.sound(), SoundSource.BLOCKS, task.volume(), task.pitch());
                sp.sendSystemMessage(Component.literal(task.message()));

                // Puan ekle + eşik ödülleri kontrol et
                int newScore = addScore(sb, obj, sp, task.points());
                checkRewards(sp, newScore);

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  TICK SİSTEMLERİ — Duman + Kirlilik
    // ─────────────────────────────────────────────────────────────────────
    private void registerTickSystems() {

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            long tick = world.getGameTime();

            // ── Duman partikülü: kirli bloklardan duman çıkar ────────────
            if (tick % SMOKE_INTERVAL_TICKS == 0) {
                for (ServerPlayer sp : world.players()) {
                    BlockPos center = sp.blockPosition();
                    BlockPos.betweenClosedStream(
                        center.offset(-7, -2, -7),
                        center.offset( 7,  4,  7)
                    ).forEach(checkPos -> {
                        BlockState s = world.getBlockState(checkPos);
                        if (s.is(ModBlocks.KIRLI_BACA) ||
                            s.is(ModBlocks.DIZEL_JENERATOR) ||
                            s.is(ModBlocks.KIRLI_SU_TANKI)) {

                            world.sendParticles(
                                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                checkPos.getX() + 0.5,
                                checkPos.getY() + 1.3,
                                checkPos.getZ() + 0.5,
                                2, 0.05, 0.15, 0.05, 0.005
                            );
                        }
                    });
                }
            }

            // ── Kirlilik düşümü: her 30 saniyede kirlilik skoru azaltır ──
            if (tick % POLLUTION_INTERVAL_TICKS == 0 && !world.players().isEmpty()) {
                Scoreboard sb  = world.getServer().getScoreboard();
                Objective obj  = sb.getObjective(OBJECTIVE_ID);
                if (obj == null) return;

                for (ServerPlayer sp : world.players()) {
                    ScoreAccess access = sb.getOrCreatePlayerScore(sp, obj);
                    int current = access.get();
                    if (current > 0) {
                        access.set(Math.max(0, current - POLLUTION_DECAY));
                        // Action bar mesajı (chat'i doldurmaz)
                        sp.displayClientMessage(
                            Component.literal("§c☠ Endüstriyel kirlilik devam ediyor! (-"
                                + POLLUTION_DECAY + " Puan) — Görevleri tamamla!"),
                            true
                        );
                    }
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  YARDIMCI METODLAR
    // ─────────────────────────────────────────────────────────────────────

    private void setupScoreboard(MinecraftServer server) {
        ServerScoreboard sb = server.getScoreboard();
        if (sb.getObjective(OBJECTIVE_ID) == null) {
            sb.addObjective(OBJECTIVE_ID, ObjectiveCriteria.DUMMY,
                Component.literal("Sürdürülebilirlik Skoru"),
                ObjectiveCriteria.RenderType.INTEGER, false, null);
        }
        sb.setDisplayObjective(DisplaySlot.SIDEBAR, sb.getObjective(OBJECTIVE_ID));
        LOGGER.info("Scoreboard hazır.");
    }

    private void initPlayerScore(MinecraftServer server, ServerPlayer player) {
        Scoreboard sb  = server.getScoreboard();
        Objective obj  = sb.getObjective(OBJECTIVE_ID);
        if (obj == null) return;
        if (sb.getPlayerScoreInfo(player, obj) == null) {
            sb.getOrCreatePlayerScore(player, obj).set(0);
        }
    }

    /** Skoru artırır, yeni değeri döndürür. */
    private int addScore(Scoreboard sb, Objective obj, ServerPlayer player, int amount) {
        if (obj == null) return 0;
        ScoreAccess access = sb.getOrCreatePlayerScore(player, obj);
        int newVal = access.get() + amount;
        access.set(newVal);
        return newVal;
    }

    /** Puan eşiklerine ulaşınca rozet + ödül verir (player tag ile tekrar engellenmiş). */
    private void checkRewards(ServerPlayer player, int score) {
        if (score >= 300 && player.addTag("yd_rozet_1")) {
            player.sendSystemMessage(Component.literal(
                "§6★ ══════════════════════════════\n" +
                "§6★ [ROZET] 'Çevre Dostu' unvanını kazandın!\n" +
                "§6★ ══════════════════════════════"));
            player.getInventory().add(new ItemStack(Items.EMERALD, 5));
        }
        if (score >= 700 && player.addTag("yd_rozet_2")) {
            player.sendSystemMessage(Component.literal(
                "§b★ ══════════════════════════════\n" +
                "§b★ [ROZET] 'Sürdürülebilir Şehir' unvanını kazandın!\n" +
                "§b★ ══════════════════════════════"));
            player.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        }
        if (score >= 1100 && player.addTag("yd_rozet_3")) {
            player.sendSystemMessage(Component.literal(
                "§5★ ══════════════════════════════\n" +
                "§5★ [ROZET] 'Yeşil Dönüşüm Mühendisi' unvanını kazandın!\n" +
                "§5★ Tüm görevler tamamlandı. Ereğli dönüştürüldü!\n" +
                "§5★ ══════════════════════════════"));
            player.getInventory().add(new ItemStack(Items.NETHER_STAR));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NPC OTO-SPAWN
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Overworld'da NPC var mı diye kontrol eder.
     * Yoksa spawn noktasının yakınına üç NPC yerleştirir.
     */
    private void checkAndSpawnNpcs(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        AABB searchArea = new AABB(
            spawn.getX() - 500, level.getMinBuildHeight(), spawn.getZ() - 500,
            spawn.getX() + 500, level.getMaxBuildHeight(), spawn.getZ() + 500
        );

        boolean hasFabrika   = !level.getEntitiesOfClass(NpcEntity.class, searchArea,
            e -> e.getType() == ModEntities.FABRIKA_MUDURU).isEmpty();
        boolean hasBelediye  = !level.getEntitiesOfClass(NpcEntity.class, searchArea,
            e -> e.getType() == ModEntities.BELEDIYE_BASKANI).isEmpty();
        boolean hasMahalle   = !level.getEntitiesOfClass(NpcEntity.class, searchArea,
            e -> e.getType() == ModEntities.MAHALLE_MUHTARI).isEmpty();

        // Haritada önceden tanımlı pozisyonlar (spawn noktasına göre offset)
        // Harita tasarımına göre bu değerleri güncelle!
        if (!hasFabrika)  spawnNpc(level, ModEntities.FABRIKA_MUDURU,  spawn.offset(  0, 1, -30));
        if (!hasBelediye) spawnNpc(level, ModEntities.BELEDIYE_BASKANI, spawn.offset(-20, 1,  10));
        if (!hasMahalle)  spawnNpc(level, ModEntities.MAHALLE_MUHTARI,  spawn.offset( 20, 1,  10));

        LOGGER.info("NPC görevliler haritaya yerleştirildi (spawn: {}).", spawn);
    }

    private void spawnNpc(ServerLevel level, net.minecraft.world.entity.EntityType<NpcEntity> type, BlockPos pos) {
        NpcEntity npc = type.create(level);
        if (npc == null) return;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
            MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(npc);
    }
}
