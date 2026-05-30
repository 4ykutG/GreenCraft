package com.example.entity;

import com.example.ModEntities;
import com.example.ModTasks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;

public class NpcEntity extends PathfinderMob {

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    /** İlk spawn'da entity tipine göre isim otomatik atanır. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        if (this.getCustomName() == null) {
            if (this.getType() == ModEntities.FABRIKA_MUDURU) {
                this.setCustomName(Component.literal("Fabrika Müdürü"));
            } else if (this.getType() == ModEntities.BELEDIYE_BASKANI) {
                this.setCustomName(Component.literal("Belediye Başkanı"));
            } else if (this.getType() == ModEntities.MAHALLE_MUHTARI) {
                this.setCustomName(Component.literal("Mahalle Muhtarı"));
            }
        }
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.1, 2.5f, 16.0f));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && player instanceof ServerPlayer sp
                && hand == InteractionHand.MAIN_HAND) {

            Component nameComp = this.getCustomName();
            String name = nameComp != null ? nameComp.getString() : "";

            String dialog = switch (name) {
                case "Fabrika Müdürü"   -> ModTasks.NPC_FABRIKA_MUDURU;
                case "Belediye Başkanı" -> ModTasks.NPC_BELEDIYE_BASKANI;
                case "Mahalle Muhtarı"  -> ModTasks.NPC_MAHALLE_MUHTARI;
                default -> "§7Selam! Görevlerini tamamlamak için bana danış.";
            };

            for (String line : dialog.split("\n"))
                sp.sendSystemMessage(Component.literal(line));

            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean isPushable() { return false; }

    // ─────────────────────────────────────────────────────────────────────
    //  AI: En yakın oyuncuyu takip et
    // ─────────────────────────────────────────────────────────────────────
    private static class FollowPlayerGoal extends Goal {

        private final PathfinderMob mob;
        private final double speed;
        private final float minDist;
        private final float maxDist;
        private Player target;

        FollowPlayerGoal(PathfinderMob mob, double speed, float minDist, float maxDist) {
            this.mob = mob;
            this.speed = speed;
            this.minDist = minDist;
            this.maxDist = maxDist;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = mob.level().getNearestPlayer(mob, maxDist);
            return target != null && !target.isCreative()
                && mob.distanceTo(target) > minDist;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && !target.isRemoved()
                && mob.distanceTo(target) > minDist
                && mob.distanceTo(target) <= maxDist + 4;
        }

        @Override
        public void stop() {
            target = null;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (target == null) return;
            mob.getLookControl().setLookAt(target, 10f, 10f);
            if (mob.distanceTo(target) > minDist)
                mob.getNavigation().moveTo(target, speed);
            else
                mob.getNavigation().stop();
        }
    }
}
