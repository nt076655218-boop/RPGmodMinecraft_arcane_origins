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

    public static boolean isEntityFrozen(UUID uuid) {
        return FROZEN_TARGETS.containsKey(uuid);
    }

    /**
     * Freezes an enemy completely still like an ice statue inside translucent Ice blocks:
     * - Disables Mob AI & head rotation so it doesn't twitch or look around
     * - Cancels IN_WALL suffocation damage in ModEvents
     * - Shatters ice cleanly on expiration without leaving water
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

        // Snap entity neatly to the center of its block column so it sits symmetrically inside the ice cube
        BlockPos base = target.blockPosition();
        Vec3 centeredPos = new Vec3(base.getX() + 0.5, target.getY(), base.getZ() + 0.5);
        target.teleportTo(centeredPos.x, centeredPos.y, centeredPos.z);

        List<BlockPos> placedIce = new ArrayList<>();
        int height = Math.max(2, (int) Math.ceil(target.getBbHeight()));
        int radius = target.getBbWidth() > 1.05f ? 1 : 0;

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

        boolean hadNoAi = false;
        boolean wasSilent = target.isSilent();
        if (target instanceof Mob mob) {
            hadNoAi = mob.isNoAi();
            mob.getNavigation().stop();
            mob.setNoAi(true);
            mob.setSilent(true);
        }

        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.setTicksFrozen(320);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks + 20, 6, false, false));

        level.playSound(null, centeredPos.x, centeredPos.y, centeredPos.z,
                SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.1f, 1.35f);
        level.sendParticles(SpellVfx.BLUE_ICE_SHARDS, centeredPos.x, centeredPos.y + 1.0, centeredPos.z,
                22, 0.45, 0.75, 0.45, 0.08);

        FROZEN_TARGETS.put(id, new FrozenPrison(
                level, id, centeredPos, target.getYRot(), target.getXRot(),
                placedIce, durationTicks, hadNoAi, wasSilent
        ));
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
                for (BlockPos pos : prison.iceBlocks) {
                    if (prison.level.getBlockState(pos).is(Blocks.ICE)) {
                        prison.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                if (ent instanceof Mob mob && mob.isAlive()) {
                    mob.setNoAi(prison.hadNoAi);
                    mob.setSilent(prison.wasSilent);
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
                living.setAirSupply(living.getMaxAirSupply());
                living.setTicksFrozen(300);
                living.setDeltaMovement(Vec3.ZERO);
                living.setYRot(prison.frozenYaw);
                living.setXRot(prison.frozenPitch);
                living.yHeadRot = prison.frozenYaw;
                living.yBodyRot = prison.frozenYaw;
                living.setPos(prison.anchorPos.x, prison.anchorPos.y, prison.anchorPos.z);

                if (prison.remainingTicks % 12 == 0) {
                    prison.level.sendParticles(ParticleTypes.SNOWFLAKE,
                            prison.anchorPos.x, prison.anchorPos.y + 1.1, prison.anchorPos.z,
                            3, 0.55, 0.75, 0.55, 0.01);
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
        double cloudHeight = 7.5;

        // Ground rune circle
        if (e.age % 2 == 0) {
            SpellVfx.spawnRuneHexagram(level, c.add(0, 0.12, 0), 5.6, rot, SpellVfx.LIGHTNING_BLUE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.16, 0), 3.2, 24, SpellVfx.LIGHTNING_CORE);
        }

        // Dense multi-layered storm cloud at y = c.y + 7.5
        for (int p = 0; p < 16; p++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 5.2;
            double yOff = cloudHeight + (rand.nextDouble() - 0.5) * 1.2;
            double px = c.x + Math.cos(a) * r;
            double pz = c.z + Math.sin(a) * r;
            level.sendParticles(p % 2 == 0 ? SpellVfx.STORM_CLOUD_DARK : SpellVfx.STORM_CLOUD_INNER,
                    px, c.y + yOff, pz, 1, 0.25, 0.12, 0.25, 0);
            if (p % 4 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, px, c.y + yOff + 0.35, pz, 1, 0.15, 0.05, 0.15, 0.01);
            }
        }

        // Intra-cloud static arcs inside the thundercloud
        if (e.age % 4 == 0) {
            double a1 = rand.nextDouble() * Math.PI * 2.0;
            double a2 = a1 + 2.2;
            Vec3 p1 = c.add(Math.cos(a1) * 4.2, cloudHeight, Math.sin(a1) * 4.2);
            Vec3 p2 = c.add(Math.cos(a2) * 4.2, cloudHeight, Math.sin(a2) * 4.2);
            SpellVfx.spawnJaggedLightningArc(level, p1, p2, 5, 0.45);
        }

        // 4 massive volumetric cloud-to-ground lightning strikes — NO sky-piercing vanilla LightningBolt!
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
                double sx = c.x + Math.cos(a) * r;
                double sz = c.z + Math.sin(a) * r;
                strikePos = new Vec3(sx, SpellVfx.snapYToGround(level, sx, c.y, sz), sz);
            }

            // Originates INSIDE the storm cloud (c.y + cloudHeight - 0.3), NEVER above it!
            Vec3 cloudOrigin = new Vec3(
                    strikePos.x + (rand.nextDouble() - 0.5) * 1.4,
                    c.y + cloudHeight - 0.3,
                    strikePos.z + (rand.nextDouble() - 0.5) * 1.4
            );
            SpellVfx.spawnVolumetricCloudBolt(level, cloudOrigin, strikePos);
            level.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.35f, 1.05f);
            level.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.1f, 1.2f);

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
        final float frozenYaw;
        final float frozenPitch;
        final List<BlockPos> iceBlocks;
        int remainingTicks;
        final boolean hadNoAi;
        final boolean wasSilent;

        FrozenPrison(ServerLevel level, UUID entityId, Vec3 anchorPos, float frozenYaw, float frozenPitch,
                     List<BlockPos> iceBlocks, int remainingTicks, boolean hadNoAi, boolean wasSilent) {
            this.level = level;
            this.entityId = entityId;
            this.anchorPos = anchorPos;
            this.frozenYaw = frozenYaw;
            this.frozenPitch = frozenPitch;
            this.iceBlocks = iceBlocks;
            this.remainingTicks = remainingTicks;
            this.hadNoAi = hadNoAi;
            this.wasSilent = wasSilent;
        }
    }
}
