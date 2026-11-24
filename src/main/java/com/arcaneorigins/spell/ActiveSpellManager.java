package com.arcaneorigins.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ActiveSpellManager {
    private static final List<ActiveEffect> EFFECTS = new ArrayList<>();
    private static final Map<UUID, FrozenPrison> FROZEN_TARGETS = new HashMap<>();

    /**
     * Actually freezes an enemy inside real translucent Ice blocks, paralyzing them until the ice thaws or breaks!
     */
    public static void freezeEntityInIce(ServerLevel level, LivingEntity target, int durationTicks) {
        if (target == null || !target.isAlive() || target instanceof ServerPlayer) return;

        UUID id = target.getUUID();
        FrozenPrison existing = FROZEN_TARGETS.get(id);
        if (existing != null) {
            existing.remainingTicks = Math.max(existing.remainingTicks, durationTicks);
            target.setTicksFrozen(300);
            return;
        }

        List<BlockPos> placedIce = new ArrayList<>();
        BlockPos base = target.blockPosition();
        int height = Math.max(2, (int) Math.ceil(target.getBbHeight()));
        int radius = target.getBbWidth() > 1.1f ? 1 : 0;

        for (int dy = 0; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.canBeReplaced()) {
                        level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                        placedIce.add(pos.immutable());
                    }
                }
            }
        }

        // Anchor entity cleanly inside the ice block so it doesn't suffocate or jitter out
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.setTicksFrozen(320);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks + 20, 6, false, false));
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.1f, 1.35f);
        level.sendParticles(SpellVfx.BLUE_ICE_SHARDS, target.getX(), target.getY() + 1.0, target.getZ(),
                22, 0.45, 0.75, 0.45, 0.08);

        FROZEN_TARGETS.put(id, new FrozenPrison(level, id, target.position(), placedIce, durationTicks));
    }

    public static void addBlizzard(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.BLIZZARD, level, casterId, center, 70, powerMultiplier));
    }

    public static void addTempest(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.TEMPEST, level, casterId, center, 60, powerMultiplier));
    }

    public static void tick(MinecraftServer server) {
        tickFrozenPrisons();
        tickAreaEffects(server);
    }

    private static void tickFrozenPrisons() {
        if (FROZEN_TARGETS.isEmpty()) return;
        Iterator<Map.Entry<UUID, FrozenPrison>> it = FROZEN_TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            FrozenPrison prison = it.next().getValue();
            prison.remainingTicks--;
            Entity ent = prison.level.getEntity(prison.entityId);

            boolean allIceBroken = !prison.iceBlocks.isEmpty() && prison.iceBlocks.stream()
                    .noneMatch(p -> prison.level.getBlockState(p).is(Blocks.ICE));

            if (ent == null || !ent.isAlive() || prison.remainingTicks <= 0 || allIceBroken) {
                // Shatter the ice block prison cleanly without dropping water!
                for (BlockPos pos : prison.iceBlocks) {
                    if (prison.level.getBlockState(pos).is(Blocks.ICE)) {
                        prison.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                prison.level.playSound(null, prison.anchorPos.x, prison.anchorPos.y, prison.anchorPos.z,
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.1f, 1.15f);
                prison.level.sendParticles(SpellVfx.ICE_SHARDS,
                        prison.anchorPos.x, prison.anchorPos.y + 1.0, prison.anchorPos.z,
                        25, 0.5, 0.8, 0.5, 0.12);
                it.remove();
                continue;
            }

            if (ent instanceof LivingEntity living) {
                // Keep frozen target immobilized & prevent block suffocation damage
                living.setAirSupply(living.getMaxAirSupply());
                living.setTicksFrozen(300);
                living.setDeltaMovement(Vec3.ZERO);
                living.teleportTo(prison.anchorPos.x, prison.anchorPos.y, prison.anchorPos.z);
                if (living instanceof Mob mob) {
                    mob.getNavigation().stop();
                }
                if (prison.remainingTicks % 10 == 0) {
                    prison.level.sendParticles(ParticleTypes.SNOWFLAKE,
                            prison.anchorPos.x, prison.anchorPos.y + 1.1, prison.anchorPos.z,
                            4, 0.5, 0.7, 0.5, 0.01);
                }
            }
        }
    }

    private static void tickAreaEffects(MinecraftServer server) {
        if (EFFECTS.isEmpty()) return;
        Iterator<ActiveEffect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            ActiveEffect effect = it.next();
            effect.age++;
            if (effect.level == null || effect.age > effect.durationTicks) {
                it.remove();
                continue;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(effect.casterId);
            if (effect.type == EffectType.BLIZZARD) {
                tickBlizzard(effect, caster);
            } else if (effect.type == EffectType.TEMPEST) {
                tickTempest(effect, caster);
            }
        }
    }

    private static void tickBlizzard(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center;
        double spin = e.age * 0.45;
        RandomSource rand = level.getRandom();

        // Ground frost rune ring + 4 helical storm arms + falling hail/ice shards
        if (e.age % 2 == 0) {
            SpellVfx.spawnRuneHexagram(level, c.add(0, 0.12, 0), 4.6, e.age * 0.08, SpellVfx.ICE_DEEP);
        }

        for (int arm = 0; arm < 4; arm++) {
            double armAngle = spin + arm * (Math.PI / 2.0);
            for (int h = 0; h < 7; h++) {
                double yOff = h * 0.68;
                double radius = 0.8 + h * 0.62;
                double a = armAngle + h * 0.52;
                double x = c.x + Math.cos(a) * radius;
                double z = c.z + Math.sin(a) * radius;
                level.sendParticles(h % 2 == 0 ? SpellVfx.ICE_CORE : SpellVfx.ICE_DEEP, x, c.y + yOff, z, 1, 0, 0, 0, 0);
                if (h % 2 == 1) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, x, c.y + yOff + 0.2, z, 2, 0.15, 0.15, 0.15, 0.04);
                }
            }
        }

        // Falling ice shard hail inside the storm
        for (int s = 0; s < 3; s++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 4.5;
            level.sendParticles(SpellVfx.ICE_SHARDS, c.x + Math.cos(a) * r, c.y + 4.5, c.z + Math.sin(a) * r,
                    1, 0.05, -0.4, 0.05, 0.15);
        }

        if (e.age % 7 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 0.85f, 1.45f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.7f);

            AABB box = new AABB(c.x - 5.0, c.y - 1.0, c.z - 5.0, c.x + 5.0, c.y + 4.5, c.z + 5.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));
            for (LivingEntity target : targets) {
                if (!FROZEN_TARGETS.containsKey(target.getUUID())) {
                    Vec3 pull = c.subtract(target.position()).scale(0.16);
                    target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.02, pull.z));
                    target.hurtMarked = true;
                }
                target.hurt(level.damageSources().freeze(), 3.2f * e.powerMultiplier);
                // Freeze targets solid in ice blocks on climax or heavy chill!
                if (e.age >= 28 && rand.nextFloat() < 0.45f) {
                    freezeEntityInIce(level, target, 80);
                } else {
                    target.setTicksFrozen(Math.min(280, target.getTicksFrozen() + 70));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                }
                SpellExecutor.onSpellHitTarget(caster, target);
            }
        }
    }

    private static void tickTempest(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center;
        double rot = e.age * 0.22;
        RandomSource rand = level.getRandom();
        double cloudHeight = 8.5;

        // Ground target rune + dense dark storm cloud overhead
        if (e.age % 2 == 0) {
            SpellVfx.spawnRuneHexagram(level, c.add(0, 0.12, 0), 5.6, rot, SpellVfx.LIGHTNING_BLUE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.16, 0), 3.2, 24, SpellVfx.LIGHTNING_CORE);
        }

        // Volumetric thundercloud canopy
        for (int p = 0; p < 12; p++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 5.2;
            double yOff = cloudHeight + (rand.nextDouble() - 0.5) * 1.1;
            double px = c.x + Math.cos(a) * r;
            double pz = c.z + Math.sin(a) * r;
            level.sendParticles(SpellVfx.STORM_CLOUD_DARK, px, c.y + yOff, pz, 1, 0.2, 0.1, 0.2, 0);
            if (p % 3 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, px, c.y + yOff + 0.3, pz, 1, 0.15, 0.05, 0.15, 0.01);
            }
        }

        // Intra-cloud static arcs before & between strikes
        if (e.age % 4 == 0) {
            double a1 = rand.nextDouble() * Math.PI * 2.0;
            double a2 = a1 + 2.2;
            Vec3 p1 = c.add(Math.cos(a1) * 4.2, cloudHeight, Math.sin(a1) * 4.2);
            Vec3 p2 = c.add(Math.cos(a2) * 4.2, cloudHeight, Math.sin(a2) * 4.2);
            SpellVfx.spawnJaggedLightningArc(level, p1, p2, 5, 0.45);
        }

        // 4 massive cloud-to-ground lightning strikes at ticks 16, 28, 40, 52
        if (e.age == 16 || e.age == 28 || e.age == 40 || e.age == 52) {
            AABB box = new AABB(c.x - 6.2, c.y - 1.5, c.z - 6.2, c.x + 6.2, c.y + 5.0, c.z + 6.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));

            Vec3 strikePos;
            if (!targets.isEmpty()) {
                LivingEntity chosen = targets.get(rand.nextInt(targets.size()));
                strikePos = chosen.position();
            } else {
                double a = rand.nextDouble() * Math.PI * 2.0;
                double r = rand.nextDouble() * 4.2;
                strikePos = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            }

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(strikePos.x, strikePos.y, strikePos.z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }

            // Connect storm cloud directly to the ground strike point
            Vec3 cloudOrigin = strikePos.add((rand.nextDouble() - 0.5) * 1.6, cloudHeight, (rand.nextDouble() - 0.5) * 1.6);
            SpellVfx.spawnJaggedLightningArc(level, cloudOrigin, strikePos, 9, 0.85);
            SpellVfx.spawnHorizontalCircle(level, strikePos.add(0, 0.25, 0), 3.4, 28, ParticleTypes.ELECTRIC_SPARK);
            SpellVfx.spawnHorizontalCircle(level, strikePos.add(0, 0.35, 0), 2.2, 20, SpellVfx.LIGHTNING_CORE);
            level.playSound(null, strikePos.x, strikePos.y, strikePos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.35f, 1.05f);

            AABB blastBox = new AABB(strikePos.x - 3.6, strikePos.y - 1.5, strikePos.z - 3.6,
                    strikePos.x + 3.6, strikePos.y + 3.5, strikePos.z + 3.6);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blastBox,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                victim.hurt(level.damageSources().lightningBolt(), 8.5f * e.powerMultiplier);
                if (!FROZEN_TARGETS.containsKey(victim.getUUID())) {
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.38, 0));
                    victim.hurtMarked = true;
                }
                SpellExecutor.onSpellHitTarget(caster, victim);
            }
        }
    }

    private enum EffectType { BLIZZARD, TEMPEST }

    private static class ActiveEffect {
        final EffectType type;
        final ServerLevel level;
        final UUID casterId;
        final Vec3 center;
        final int durationTicks;
        final float powerMultiplier;
        int age = 0;

        ActiveEffect(EffectType type, ServerLevel level, UUID casterId, Vec3 center, int durationTicks, float powerMultiplier) {
            this.type = type;
            this.level = level;
            this.casterId = casterId;
            this.center = center;
            this.durationTicks = durationTicks;
            this.powerMultiplier = powerMultiplier;
        }
    }

    private static class FrozenPrison {
        final ServerLevel level;
        final UUID entityId;
        final Vec3 anchorPos;
        final List<BlockPos> iceBlocks;
        int remainingTicks;

        FrozenPrison(ServerLevel level, UUID entityId, Vec3 anchorPos, List<BlockPos> iceBlocks, int remainingTicks) {
            this.level = level;
            this.entityId = entityId;
            this.anchorPos = anchorPos;
            this.iceBlocks = iceBlocks;
            this.remainingTicks = remainingTicks;
        }
    }
}
