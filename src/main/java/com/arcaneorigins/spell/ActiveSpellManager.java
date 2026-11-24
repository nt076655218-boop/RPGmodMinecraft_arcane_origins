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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ActiveSpellManager {
    private static final List<ActiveEffect> EFFECTS = new ArrayList<>();
    private static final Map<UUID, FrozenPrison> FROZEN_TARGETS = new HashMap<>();
    private static final List<TempBlockCluster> TEMP_BLOCKS = new ArrayList<>();

    public static boolean isEntityFrozen(UUID uuid) {
        return FROZEN_TARGETS.containsKey(uuid);
    }

    public static void freezeEntityInIce(ServerLevel level, LivingEntity target, int durationTicks) {
        if (target == null || !target.isAlive() || target instanceof ServerPlayer) return;

        UUID id = target.getUUID();
        FrozenPrison existing = FROZEN_TARGETS.get(id);
        if (existing != null) {
            existing.remainingTicks = Math.max(existing.remainingTicks, durationTicks);
            target.setTicksFrozen(300);
            return;
        }

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

    public static void addTemporaryIceBlocks(ServerLevel level, List<BlockPos> positions, int durationTicks) {
        if (!positions.isEmpty()) {
            TEMP_BLOCKS.add(new TempBlockCluster(level, positions, durationTicks));
        }
    }

    public static void addFlyingFireball(ServerLevel level, UUID casterId, Vec3 start, Vec3 velocity, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.FLYING_FIREBALL, level, casterId, start, velocity, Vec3.ZERO, 45, powerMultiplier));
    }

    public static void addFireTornado(ServerLevel level, UUID casterId, Vec3 start, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.FIRE_TORNADO, level, casterId, start, Vec3.ZERO, Vec3.ZERO, 95, powerMultiplier));
    }

    public static void addVoidPortalGate(ServerLevel level, UUID casterId, Vec3 center, Vec3 facingDir, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.VOID_PORTAL_GATE, level, casterId, center, Vec3.ZERO, facingDir, 70, powerMultiplier));
    }

    public static void addBlizzard(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.BLIZZARD, level, casterId, center, Vec3.ZERO, Vec3.ZERO, 70, powerMultiplier));
    }

    public static void addTempest(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.TEMPEST, level, casterId, center, Vec3.ZERO, Vec3.ZERO, 60, powerMultiplier));
    }

    public static void addStormOrb(ServerLevel level, UUID casterId, Vec3 start, Vec3 velocity, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.STORM_ORB, level, casterId, start, velocity, Vec3.ZERO, 55, powerMultiplier));
    }

    public static void addGravityWell(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.GRAVITY_WELL, level, casterId, center, Vec3.ZERO, Vec3.ZERO, 65, powerMultiplier));
    }

    public static void addZeroGSlam(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.ZERO_G_SLAM, level, casterId, center, Vec3.ZERO, Vec3.ZERO, 26, powerMultiplier));
    }

    public static void addMeteorShower(ServerLevel level, UUID casterId, Vec3 center, float powerMultiplier) {
        EFFECTS.add(new ActiveEffect(EffectType.METEOR_SHOWER, level, casterId, center, Vec3.ZERO, Vec3.ZERO, 62, powerMultiplier));
    }

    public static void tick(MinecraftServer server) {
        tickFrozenPrisons();
        tickTempBlocks();
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

    private static void tickTempBlocks() {
        if (TEMP_BLOCKS.isEmpty()) return;
        Iterator<TempBlockCluster> it = TEMP_BLOCKS.iterator();
        while (it.hasNext()) {
            TempBlockCluster c = it.next();
            c.remainingTicks--;
            if (c.remainingTicks <= 0) {
                for (BlockPos pos : c.blocks) {
                    BlockState st = c.level.getBlockState(pos);
                    if (st.is(Blocks.PACKED_ICE) || st.is(Blocks.ICE)) {
                        c.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        c.level.sendParticles(SpellVfx.ICE_SHARDS, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                6, 0.3, 0.3, 0.3, 0.08);
                    }
                }
                if (!c.blocks.isEmpty()) {
                    BlockPos first = c.blocks.get(0);
                    c.level.playSound(null, first.getX(), first.getY(), first.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9f, 1.2f);
                }
                it.remove();
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
            boolean done = switch (effect.type) {
                case FLYING_FIREBALL -> tickFlyingFireball(effect, caster);
                case FIRE_TORNADO -> tickFireTornado(effect, caster);
                case VOID_PORTAL_GATE -> tickVoidPortalGate(effect, caster);
                case BLIZZARD -> { tickBlizzard(effect, caster); yield false; }
                case TEMPEST -> { tickTempest(effect, caster); yield false; }
                case STORM_ORB -> { tickStormOrb(effect, caster); yield false; }
                case GRAVITY_WELL -> { tickGravityWell(effect, caster); yield false; }
                case ZERO_G_SLAM -> { tickZeroGSlam(effect, caster); yield false; }
                case METEOR_SHOWER -> { tickMeteorShower(effect, caster); yield false; }
            };
            if (done) {
                it.remove();
            }
        }
    }

    /**
     * True Flying Fireball Projectile with blazing core, ember trail, and explosion on impact!
     */
    private static boolean tickFlyingFireball(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 prev = e.center;
        Vec3 next = prev.add(e.velocity);

        // Render dense blazing sphere + trailing flames along step
        for (int s = 0; s <= 4; s++) {
            Vec3 p = prev.lerp(next, s / 4.0);
            level.sendParticles(SpellVfx.FIRE_YELLOW, p.x, p.y, p.z, 3, 0.14, 0.14, 0.14, 0);
            level.sendParticles(SpellVfx.FIRE_ORANGE, p.x, p.y, p.z, 5, 0.28, 0.28, 0.28, 0);
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 4, 0.22, 0.22, 0.22, 0.03);
            if (s % 2 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.01);
            }
        }

        // Check block collision
        BlockHitResult blockHit = level.clip(new ClipContext(prev, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 impactPos = null;
        if (blockHit.getType() != HitResult.Type.MISS) {
            impactPos = blockHit.getLocation();
        } else {
            // Check entity collision
            AABB hitBox = new AABB(prev, next).inflate(0.95);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));
            if (!hits.isEmpty()) {
                impactPos = hits.get(0).getBoundingBox().getCenter();
            }
        }

        if (impactPos != null || e.age >= e.durationTicks) {
            Vec3 boom = impactPos != null ? impactPos : next;
            SpellVfx.spawnFireExplosion(level, boom, 4.0);
            level.playSound(null, boom.x, boom.y, boom.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.35f, 0.95f);

            AABB blast = new AABB(boom.x - 4.2, boom.y - 2.5, boom.z - 4.2, boom.x + 4.2, boom.y + 3.5, boom.z + 4.2);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blast,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                target.hurt(level.damageSources().inFire(), 10.5f * e.powerMultiplier);
                target.igniteForSeconds(6);
                if (!FROZEN_TARGETS.containsKey(target.getUUID())) {
                    Vec3 push = target.position().subtract(boom).normalize().scale(0.85);
                    target.setDeltaMovement(push.x, 0.35, push.z);
                    target.hurtMarked = true;
                }
                SpellExecutor.onSpellHitTarget(caster, target);
            }
            return true;
        }

        e.center = next;
        return false;
    }

    /**
     * Fire Tornado: Spawns on the caster and follows them for ~4.8 seconds, burning & lifting nearby enemies!
     */
    private static boolean tickFireTornado(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        if (caster != null && caster.isAlive() && caster.serverLevel() == level) {
            e.center = e.center.lerp(caster.position(), 0.45);
        }
        Vec3 c = e.center;
        double spin = e.age * 0.55;

        // Funnel vortex of flame, orange & yellow dust, and smoke
        for (int arm = 0; arm < 3; arm++) {
            double baseAngle = spin + arm * (2.0 * Math.PI / 3.0);
            for (int h = 0; h < 8; h++) {
                double yOff = h * 0.55;
                double radius = 1.2 + h * 0.38;
                double a = baseAngle + h * 0.58;
                double x = c.x + Math.cos(a) * radius;
                double z = c.z + Math.sin(a) * radius;
                level.sendParticles(h % 2 == 0 ? SpellVfx.FIRE_ORANGE : SpellVfx.FIRE_YELLOW, x, c.y + yOff, z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.FLAME, x, c.y + yOff + 0.15, z, 1, 0.1, 0.1, 0.1, 0.02);
                if (h == 7) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, x, c.y + yOff + 0.3, z, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }

        if (e.age % 6 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.75f, 1.35f);
            AABB box = new AABB(c.x - 4.4, c.y - 1.0, c.z - 4.4, c.x + 4.4, c.y + 4.2, c.z + 4.4);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                target.hurt(level.damageSources().inFire(), 3.2f * e.powerMultiplier);
                target.igniteForSeconds(4);
                if (!FROZEN_TARGETS.containsKey(target.getUUID())) {
                    // Swirling updraft around the caster
                    Vec3 rel = target.position().subtract(c);
                    Vec3 tangent = new Vec3(-rel.z, 0, rel.x).normalize().scale(0.35);
                    target.setDeltaMovement(tangent.x, 0.26, tangent.z);
                    target.hurtMarked = true;
                }
                SpellExecutor.onSpellHitTarget(caster, target);
            }
        }
        return false;
    }

    /**
     * Void Portal Gate: Opens an upright dark dimensional portal right in front of the caster,
     * pulling nearby enemies into the abyssal gateway with Wither & void damage!
     */
    private static boolean tickVoidPortalGate(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center.add(0, 1.8, 0);
        Vec3 forward = e.facingDir;
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        if (right.lengthSqr() < 0.01) right = new Vec3(1, 0, 0);

        double width = 1.45;
        double height = 2.15;
        double spin = e.age * 0.25;

        // Vertical oval portal frame + swirling abyss core
        int rimPoints = 28;
        for (int i = 0; i < rimPoints; i++) {
            double a = (2.0 * Math.PI * i) / rimPoints;
            Vec3 pt = c.add(right.scale(Math.cos(a) * width)).add(0, Math.sin(a) * height, 0);
            level.sendParticles(i % 2 == 0 ? SpellVfx.VOID_PURPLE : SpellVfx.VOID_MAGENTA, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }

        // Inner dark event horizon + spiraling portal particles
        for (int k = 0; k < 10; k++) {
            double rFrac = (k / 10.0) * 0.85;
            double a = spin + k * 0.9;
            Vec3 inner = c.add(right.scale(Math.cos(a) * width * rFrac)).add(0, Math.sin(a) * height * rFrac, 0);
            level.sendParticles(SpellVfx.VOID_BLACK, inner.x, inner.y, inner.z, 2, 0.12, 0.12, 0.12, 0);
            if (k % 2 == 0) {
                level.sendParticles(ParticleTypes.PORTAL, inner.x, inner.y, inner.z, 2, 0.25, 0.25, 0.25, 0.35);
            }
        }

        if (e.age % 6 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.7f, 0.55f);
            AABB pullBox = new AABB(c.x - 5.2, c.y - 2.5, c.z - 5.2, c.x + 5.2, c.y + 3.0, c.z + 5.2);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, pullBox,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                if (!FROZEN_TARGETS.containsKey(target.getUUID())) {
                    Vec3 pull = c.subtract(target.position().add(0, 1.0, 0)).normalize().scale(0.38);
                    target.setDeltaMovement(pull.x, 0.12, pull.z);
                    target.hurtMarked = true;
                }
                if (target.distanceToSqr(c) <= 9.0) {
                    target.hurt(level.damageSources().magic(), 3.2f * e.powerMultiplier);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                    SpellExecutor.onSpellHitTarget(caster, target);
                }
            }
        }
        return false;
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

        if (e.age % 7 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 0.85f, 1.45f);
            AABB box = new AABB(c.x - 5.0, c.y - 1.0, c.z - 5.0, c.x + 5.0, c.y + 4.5, c.z + 5.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
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

        if (e.age % 2 == 0) {
            SpellVfx.spawnRuneHexagram(level, c.add(0, 0.12, 0), 5.6, rot, SpellVfx.LIGHTNING_BLUE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.16, 0), 3.2, 24, SpellVfx.LIGHTNING_CORE);
        }

        for (int p = 0; p < 16; p++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 5.2;
            double yOff = cloudHeight + (rand.nextDouble() - 0.5) * 1.2;
            double px = c.x + Math.cos(a) * r;
            double pz = c.z + Math.sin(a) * r;
            level.sendParticles(p % 2 == 0 ? SpellVfx.STORM_CLOUD_DARK : SpellVfx.STORM_CLOUD_INNER,
                    px, c.y + yOff, pz, 1, 0.25, 0.12, 0.25, 0);
        }

        if (e.age % 4 == 0) {
            double a1 = rand.nextDouble() * Math.PI * 2.0;
            double a2 = a1 + 2.2;
            Vec3 p1 = c.add(Math.cos(a1) * 4.2, cloudHeight, Math.sin(a1) * 4.2);
            Vec3 p2 = c.add(Math.cos(a2) * 4.2, cloudHeight, Math.sin(a2) * 4.2);
            SpellVfx.spawnJaggedLightningArc(level, p1, p2, 5, 0.45);
        }

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

            Vec3 cloudOrigin = new Vec3(
                    strikePos.x + (rand.nextDouble() - 0.5) * 1.4,
                    c.y + cloudHeight - 0.3,
                    strikePos.z + (rand.nextDouble() - 0.5) * 1.4
            );
            SpellVfx.spawnVolumetricCloudBolt(level, cloudOrigin, strikePos);
            level.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.35f, 1.05f);

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

    private static void tickStormOrb(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        e.center = e.center.add(e.velocity);
        Vec3 c = e.center;

        level.sendParticles(SpellVfx.LIGHTNING_CORE, c.x, c.y, c.z, 4, 0.12, 0.12, 0.12, 0);
        level.sendParticles(SpellVfx.LIGHTNING_BLUE, c.x, c.y, c.z, 6, 0.32, 0.32, 0.32, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 5, 0.4, 0.4, 0.4, 0.08);
        if (e.age % 3 == 0) {
            SpellVfx.spawnHorizontalCircle(level, c, 0.9, 14, SpellVfx.LIGHTNING_CORE);
        }

        if (e.age % 8 == 0) {
            AABB zapBox = new AABB(c.x - 4.5, c.y - 3.0, c.z - 4.5, c.x + 4.5, c.y + 3.0, c.z + 4.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, zapBox,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));
            if (!targets.isEmpty()) {
                level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.65f, 1.9f);
            }
            for (LivingEntity t : targets) {
                SpellVfx.spawnJaggedLightningArc(level, c, t.getBoundingBox().getCenter(), 5, 0.35);
                t.hurt(level.damageSources().lightningBolt(), 4.2f * e.powerMultiplier);
                SpellExecutor.onSpellHitTarget(caster, t);
            }
        }
    }

    private static void tickGravityWell(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center.add(0, 1.1, 0);
        double spin = e.age * 0.38;

        level.sendParticles(SpellVfx.VOID_BLACK, c.x, c.y, c.z, 6, 0.35, 0.35, 0.35, 0);
        level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 12, 1.6, 0.8, 1.6, 0.4);
        SpellVfx.spawnHorizontalCircle(level, c, 2.2, 22, SpellVfx.VOID_PURPLE);
        SpellVfx.spawnHorizontalCircle(level, c, 4.4, 30, SpellVfx.VOID_MAGENTA);

        for (int arm = 0; arm < 3; arm++) {
            double a = spin + arm * (2.0 * Math.PI / 3.0);
            double r = 4.6 * (1.0 - (e.age % 15) / 16.0);
            level.sendParticles(SpellVfx.VOID_MAGENTA, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 2, 0.05, 0.05, 0.05, 0);
        }

        if (e.age % 5 == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.7f, 0.6f);
            AABB pullBox = new AABB(c.x - 6.8, c.y - 2.5, c.z - 6.8, c.x + 6.8, c.y + 3.5, c.z + 6.8);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, pullBox,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                if (!FROZEN_TARGETS.containsKey(target.getUUID())) {
                    Vec3 pull = c.subtract(target.position().add(0, 0.8, 0)).normalize().scale(0.42);
                    target.setDeltaMovement(pull.x, 0.14, pull.z);
                    target.hurtMarked = true;
                }
                target.hurt(level.damageSources().magic(), 2.4f * e.powerMultiplier);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                SpellExecutor.onSpellHitTarget(caster, target);
            }
        }

        if (e.age == e.durationTicks) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.65f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            SpellVfx.spawnHorizontalCircle(level, c, 5.2, 40, SpellVfx.VOID_PURPLE);
            AABB blast = new AABB(c.x - 5.5, c.y - 2.5, c.z - 5.5, c.x + 5.5, c.y + 3.5, c.z + 5.5);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blast,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                target.hurt(level.damageSources().magic(), 9.0f * e.powerMultiplier);
                SpellExecutor.onSpellHitTarget(caster, target);
            }
        }
    }

    private static void tickZeroGSlam(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center;
        AABB box = new AABB(c.x - 6.5, c.y - 1.5, c.z - 6.5, c.x + 6.5, c.y + 8.0, c.z + 6.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())));

        if (e.age <= 16) {
            if (e.age % 3 == 0) {
                SpellVfx.spawnHorizontalCircle(level, c.add(0, e.age * 0.22, 0), 5.8, 32, SpellVfx.VOID_MAGENTA);
            }
            for (LivingEntity t : targets) {
                if (!FROZEN_TARGETS.containsKey(t.getUUID())) {
                    t.setDeltaMovement(0, 0.24, 0);
                    t.hurtMarked = true;
                }
            }
        } else if (e.age == 20) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.3f, 0.6f);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.25, 0), 6.2, 42, SpellVfx.VOID_PURPLE);
            for (LivingEntity t : targets) {
                if (!FROZEN_TARGETS.containsKey(t.getUUID())) {
                    t.setDeltaMovement(0, -1.65, 0);
                    t.hurtMarked = true;
                }
                t.hurt(level.damageSources().magic(), 9.5f * e.powerMultiplier);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                SpellExecutor.onSpellHitTarget(caster, t);
            }
        }
    }

    private static void tickMeteorShower(ActiveEffect e, ServerPlayer caster) {
        ServerLevel level = e.level;
        Vec3 c = e.center;
        RandomSource rand = level.getRandom();

        if (e.age % 2 == 0) {
            SpellVfx.spawnRuneHexagram(level, c.add(0, 0.14, 0), 5.6, e.age * 0.1, SpellVfx.FIRE_ORANGE);
        }

        if (e.age == 10 || e.age == 22 || e.age == 34 || e.age == 46 || e.age == 58) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 4.4;
            double ix = c.x + Math.cos(a) * r;
            double iz = c.z + Math.sin(a) * r;
            double iy = SpellVfx.snapYToGround(level, ix, c.y, iz);
            Vec3 impact = new Vec3(ix, iy, iz);
            Vec3 skyStart = impact.add((rand.nextDouble() - 0.5) * 4.0, 11.0, (rand.nextDouble() - 0.5) * 4.0);

            int steps = 18;
            for (int s = 0; s <= steps; s++) {
                Vec3 pt = skyStart.lerp(impact, (double) s / steps);
                level.sendParticles(SpellVfx.FIRE_CRIMSON, pt.x, pt.y, pt.z, 3, 0.25, 0.25, 0.25, 0);
                level.sendParticles(ParticleTypes.FLAME, pt.x, pt.y, pt.z, 4, 0.2, 0.2, 0.2, 0.03);
                if (s % 2 == 0) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, pt.x, pt.y, pt.z, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            SpellVfx.spawnFireExplosion(level, impact, 3.2);
            level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.15f, 0.9f);

            AABB blast = new AABB(impact.x - 3.6, impact.y - 1.5, impact.z - 3.6, impact.x + 3.6, impact.y + 3.5, impact.z + 3.6);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blast,
                    ent -> ent.isAlive() && (caster == null || !ent.getUUID().equals(caster.getUUID())))) {
                victim.hurt(level.damageSources().inFire(), 8.0f * e.powerMultiplier);
                victim.igniteForSeconds(5);
                SpellExecutor.onSpellHitTarget(caster, victim);
            }
        }
    }

    private enum EffectType {
        FLYING_FIREBALL, FIRE_TORNADO, VOID_PORTAL_GATE,
        BLIZZARD, TEMPEST, STORM_ORB, GRAVITY_WELL, ZERO_G_SLAM, METEOR_SHOWER
    }

    private static class ActiveEffect {
        final EffectType type;
        final ServerLevel level;
        final UUID casterId;
        Vec3 center;
        final Vec3 velocity;
        final Vec3 facingDir;
        final int durationTicks;
        final float powerMultiplier;
        int age = 0;

        ActiveEffect(EffectType type, ServerLevel level, UUID casterId, Vec3 center, Vec3 velocity, Vec3 facingDir, int durationTicks, float powerMultiplier) {
            this.type = type;
            this.level = level;
            this.casterId = casterId;
            this.center = center;
            this.velocity = velocity;
            this.facingDir = facingDir;
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

    private static class TempBlockCluster {
        final ServerLevel level;
        final List<BlockPos> blocks;
        int remainingTicks;

        TempBlockCluster(ServerLevel level, List<BlockPos> blocks, int remainingTicks) {
            this.level = level;
            this.blocks = blocks;
            this.remainingTicks = remainingTicks;
        }
    }
}
