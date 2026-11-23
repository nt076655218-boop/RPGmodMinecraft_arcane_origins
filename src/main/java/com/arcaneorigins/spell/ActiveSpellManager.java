package com.arcaneorigins.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ActiveSpellManager {
    private static final List<ActiveEffect> EFFECTS = new ArrayList<>();

    public static void addBlizzard(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.BLIZZARD, level, casterId, center, 60, powerMultiplier));
    }

    public static void addTempest(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.TEMPEST, level, casterId, center, 50, powerMultiplier));
    }

    public static void tick(MinecraftServer server) {
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
        double spin = e.age * 0.42;

        // Swirling funnel of snow & ice dust
        for (int arm = 0; arm < 3; arm++) {
            double armAngle = spin + arm * (2.0 * Math.PI / 3.0);
            for (int h = 0; h < 6; h++) {
                double yOff = h * 0.75;
                double radius = 1.1 + h * 0.55;
                double a = armAngle + h * 0.55;
                double x = c.x + Math.cos(a) * radius;
                double z = c.z + Math.sin(a) * radius;
                level.sendParticles(h % 2 == 0 ? SpellVfx.ICE_CORE : SpellVfx.ICE_DEEP, x, c.y + yOff, z, 1, 0, 0, 0, 0);
                if (h % 2 == 0) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, x, c.y + yOff + 0.3, z, 1, 0.1, 0.1, 0.1, 0.04);
                }
            }
        }

        if (e.age % 6 == 0) {
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.15, 0), 4.2, 24, SpellVfx.ICE_DEEP);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.0f, 0.7f);

            AABB box = new AABB(c.x - 4.8, c.y - 1.0, c.z - 4.8, c.x + 4.8, c.y + 5.0, c.z + 4.8);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));
            for (LivingEntity target : targets) {
                // Gently pull towards vortex center
                Vec3 pull = c.subtract(target.position()).scale(0.14);
                target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.03, pull.z));
                target.hurtMarked = true;
                target.setTicksFrozen(Math.min(260, target.getTicksFrozen() + 60));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                target.hurt(level.damageSources().freeze(), 3.0f * e.powerMultiplier);
                SpellExecutor.onSpellHitTarget(caster, target);
            }
        }
    }

    private static void tickTempest(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center;
        double rot = e.age * 0.25;

        // Ground & sky arcane storm circles
        if (e.age % 2 == 0) {
            SpellVfx.spawnRuneHexagram(level, c.add(0, 0.15, 0), 5.5, rot, SpellVfx.LIGHTNING_BLUE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 7.5, 0), 5.0, 28, SpellVfx.LIGHTNING_CORE);
        }

        // Charge-up arcs before first strike
        if (e.age < 15 && e.age % 3 == 0) {
            double a = level.getRandom().nextDouble() * Math.PI * 2.0;
            Vec3 skyPt = c.add(Math.cos(a) * 4.5, 7.5, Math.sin(a) * 4.5);
            SpellVfx.spawnJaggedLightningArc(level, skyPt, c.add(0, 4.0, 0), 5, 0.6);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.5f);
        }

        // 4 massive lightning strikes at ticks 15, 25, 35, 45
        if (e.age == 15 || e.age == 25 || e.age == 35 || e.age == 45) {
            AABB box = new AABB(c.x - 6.5, c.y - 2.0, c.z - 6.5, c.x + 6.5, c.y + 6.0, c.z + 6.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));

            Vec3 strikePos;
            if (!targets.isEmpty()) {
                LivingEntity chosen = targets.get(level.getRandom().nextInt(targets.size()));
                strikePos = chosen.position();
            } else {
                double a = level.getRandom().nextDouble() * Math.PI * 2.0;
                double r = level.getRandom().nextDouble() * 4.0;
                strikePos = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            }

            // Visual bolt (doesn't burn player's house or destroy loot!)
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(strikePos.x, strikePos.y, strikePos.z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }

            // Extra jagged sky-to-ground arc + expanding electric shockwave ring
            SpellVfx.spawnJaggedLightningArc(level, strikePos.add(0, 8.5, 0), strikePos, 8, 0.85);
            SpellVfx.spawnHorizontalCircle(level, strikePos.add(0, 0.4, 0), 3.2, 24, ParticleTypes.ELECTRIC_SPARK);
            level.playSound(null, strikePos.x, strikePos.y, strikePos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.3f, 1.1f);

            AABB blastBox = new AABB(strikePos.x - 3.6, strikePos.y - 1.5, strikePos.z - 3.6,
                    strikePos.x + 3.6, strikePos.y + 3.5, strikePos.z + 3.6);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blastBox,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                victim.hurt(level.damageSources().lightningBolt(), 8.0f * e.powerMultiplier);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.35, 0));
                victim.hurtMarked = true;
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
}
