package com.arcaneorigins.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SpellVfx {
    // Ice
    public static final DustParticleOptions ICE_CORE = new DustParticleOptions(new Vector3f(0.84f, 0.98f, 1.0f), 1.4f);
    public static final DustParticleOptions ICE_DEEP = new DustParticleOptions(new Vector3f(0.12f, 0.58f, 0.96f), 1.7f);
    public static final BlockParticleOption ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    public static final BlockParticleOption BLUE_ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState());

    // Lightning
    public static final DustParticleOptions LIGHTNING_CORE = new DustParticleOptions(new Vector3f(0.96f, 0.99f, 1.0f), 1.55f);
    public static final DustParticleOptions LIGHTNING_BLUE = new DustParticleOptions(new Vector3f(0.24f, 0.58f, 1.0f), 1.9f);
    public static final DustParticleOptions STORM_CLOUD_DARK = new DustParticleOptions(new Vector3f(0.14f, 0.17f, 0.26f), 3.0f);
    public static final DustParticleOptions STORM_CLOUD_INNER = new DustParticleOptions(new Vector3f(0.24f, 0.30f, 0.45f), 2.5f);

    // Gravity / Void
    public static final DustParticleOptions VOID_BLACK = new DustParticleOptions(new Vector3f(0.06f, 0.02f, 0.12f), 2.4f);
    public static final DustParticleOptions VOID_PURPLE = new DustParticleOptions(new Vector3f(0.65f, 0.16f, 0.96f), 1.7f);
    public static final DustParticleOptions VOID_MAGENTA = new DustParticleOptions(new Vector3f(0.92f, 0.35f, 1.0f), 1.35f);

    // Fire
    public static final DustParticleOptions FIRE_ORANGE = new DustParticleOptions(new Vector3f(1.0f, 0.42f, 0.05f), 1.75f);
    public static final DustParticleOptions FIRE_YELLOW = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.20f), 1.4f);
    public static final DustParticleOptions FIRE_CRIMSON = new DustParticleOptions(new Vector3f(0.80f, 0.10f, 0.05f), 2.0f);

    // Misc
    public static final DustParticleOptions ARCANE_GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.80f, 0.25f), 1.3f);
    public static final DustParticleOptions WARLOCK_VOID = new DustParticleOptions(new Vector3f(0.62f, 0.15f, 0.92f), 1.4f);

    public static Vec3 findGroundedTargetPosition(ServerLevel level, ServerPlayer player, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(maxDist));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 raw = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();

        BlockPos startPos = BlockPos.containing(raw.x, raw.y + 1.0, raw.z);
        for (int dy = 0; dy <= 24; dy++) {
            BlockPos check = startPos.below(dy);
            if (check.getY() <= level.getMinBuildHeight()) break;
            if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
                double topY = check.getY() + level.getBlockState(check).getCollisionShape(level, check).max(Direction.Axis.Y);
                return new Vec3(raw.x, topY, raw.z);
            }
        }
        return player.position();
    }

    public static double snapYToGround(ServerLevel level, double x, double refY, double z) {
        BlockPos start = BlockPos.containing(x, refY + 2.0, z);
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos check = start.below(dy);
            if (check.getY() <= level.getMinBuildHeight()) break;
            if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
                return check.getY() + level.getBlockState(check).getCollisionShape(level, check).max(Direction.Axis.Y);
            }
        }
        return refY;
    }

    public static void spawnHorizontalCircle(ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnRuneHexagram(ServerLevel level, Vec3 center, double radius, double rotationOffset, ParticleOptions particle) {
        spawnHorizontalCircle(level, center, radius, 44, particle);
        for (int t = 0; t < 2; t++) {
            double baseAngle = rotationOffset + (t * Math.PI / 3.0);
            Vec3[] verts = new Vec3[3];
            for (int v = 0; v < 3; v++) {
                double a = baseAngle + v * (2.0 * Math.PI / 3.0);
                verts[v] = new Vec3(center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius);
            }
            for (int v = 0; v < 3; v++) {
                spawnLine(level, verts[v], verts[(v + 1) % 3], 12, particle);
            }
        }
    }

    public static void spawnLine(ServerLevel level, Vec3 from, Vec3 to, int steps, ParticleOptions particle) {
        Vec3 diff = to.subtract(from);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = from.add(diff.scale(t));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnDoubleHelixBeam(ServerLevel level, Vec3 start, Vec3 end, double helixRadius, double turns) {
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        if (dist < 0.1) return;
        Vec3 dir = diff.normalize();
        Vec3 arbitrary = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(arbitrary).normalize();
        Vec3 up = right.cross(dir).normalize();

        int steps = Math.max(14, (int) (dist * 5.5));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 center = start.add(dir.scale(t * dist));
            double angle = t * turns * Math.PI * 2.0;

            if (i % 2 == 0) level.sendParticles(ICE_CORE, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            if (i % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 1, 0.02, 0.02, 0.02, 0.01);

            double r = helixRadius * (1.0 - t * 0.35);
            Vec3 offset1 = right.scale(Math.cos(angle) * r).add(up.scale(Math.sin(angle) * r));
            Vec3 offset2 = offset1.scale(-1.0);
            Vec3 p1 = center.add(offset1);
            Vec3 p2 = center.add(offset2);

            level.sendParticles(ICE_DEEP, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnJaggedLightningArc(ServerLevel level, Vec3 start, Vec3 end, int segments, double jitter) {
        RandomSource rand = level.getRandom();
        Vec3 prev = start;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 next = start.lerp(end, t);
            if (i < segments) {
                double j = jitter * (1.0 - Math.abs(t - 0.5));
                next = next.add(
                        (rand.nextDouble() - 0.5) * 2.0 * j,
                        (rand.nextDouble() - 0.5) * 2.0 * j,
                        (rand.nextDouble() - 0.5) * 2.0 * j
                );
            }
            int sub = 6;
            for (int s = 0; s <= sub; s++) {
                Vec3 pt = prev.lerp(next, (double) s / sub);
                level.sendParticles(LIGHTNING_CORE, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                if (s % 2 == 0) {
                    level.sendParticles(LIGHTNING_BLUE, pt.x, pt.y, pt.z, 1, 0.04, 0.04, 0.04, 0);
                }
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, next.x, next.y, next.z, 4, 0.09, 0.09, 0.09, 0.06);
            prev = next;
        }
    }

    public static void spawnVolumetricCloudBolt(ServerLevel level, Vec3 cloudOrigin, Vec3 groundImpact) {
        RandomSource rand = level.getRandom();
        int segments = 10;
        Vec3 prevMain = cloudOrigin;
        Vec3 prevTwin = cloudOrigin.add(0.25, 0, -0.25);

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 base = cloudOrigin.lerp(groundImpact, t);
            double spread = (i == segments) ? 0.0 : 0.95 * Math.sin(t * Math.PI);

            Vec3 nextMain = base.add(
                    (rand.nextDouble() - 0.5) * 2.0 * spread,
                    (rand.nextDouble() - 0.5) * 0.45 * spread,
                    (rand.nextDouble() - 0.5) * 2.0 * spread
            );
            Vec3 nextTwin = base.add(
                    (rand.nextDouble() - 0.5) * 2.2 * spread,
                    (rand.nextDouble() - 0.5) * 0.45 * spread,
                    (rand.nextDouble() - 0.5) * 2.2 * spread
            );

            int sub = 8;
            for (int s = 0; s <= sub; s++) {
                double st = (double) s / sub;
                Vec3 p1 = prevMain.lerp(nextMain, st);
                Vec3 p2 = prevTwin.lerp(nextTwin, st);
                level.sendParticles(LIGHTNING_CORE, p1.x, p1.y, p1.z, 2, 0.04, 0.04, 0.04, 0);
                level.sendParticles(LIGHTNING_BLUE, p1.x, p1.y, p1.z, 2, 0.14, 0.14, 0.14, 0);
                if (s % 2 == 0) {
                    level.sendParticles(LIGHTNING_CORE, p2.x, p2.y, p2.z, 1, 0.03, 0.03, 0.03, 0);
                }
                if (s == 0 && i % 2 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            if (i >= 3 && i <= 7 && rand.nextFloat() < 0.65f) {
                Vec3 forkEnd = nextMain.add(
                        (rand.nextDouble() - 0.5) * 3.0,
                        -1.0 - rand.nextDouble() * 1.4,
                        (rand.nextDouble() - 0.5) * 3.0
                );
                spawnJaggedLightningArc(level, nextMain, forkEnd, 4, 0.35);
            }

            prevMain = nextMain;
            prevTwin = nextTwin;
        }

        level.sendParticles(ParticleTypes.FLASH, groundImpact.x, groundImpact.y + 0.5, groundImpact.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, groundImpact.x, groundImpact.y + 0.3, groundImpact.z, 35, 0.9, 0.3, 0.9, 0.18);
        spawnHorizontalCircle(level, groundImpact.add(0, 0.2, 0), 1.8, 22, LIGHTNING_CORE);
        spawnHorizontalCircle(level, groundImpact.add(0, 0.3, 0), 3.4, 30, LIGHTNING_BLUE);
    }

    public static void spawnCrystalBurst(ServerLevel level, Vec3 center, double radius) {
        int rays = 14;
        for (int i = 0; i < rays; i++) {
            double theta = (2.0 * Math.PI * i) / rays;
            double phi = ((i % 3) - 1) * 0.55;
            Vec3 dir = new Vec3(Math.cos(theta) * Math.cos(phi), Math.sin(phi), Math.sin(theta) * Math.cos(phi));
            Vec3 end = center.add(dir.scale(radius));
            spawnLine(level, center, end, 6, i % 2 == 0 ? ICE_CORE : ICE_DEEP);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 26, 0.65, 0.65, 0.65, 0.12);
        level.sendParticles(BLUE_ICE_SHARDS, center.x, center.y, center.z, 18, 0.45, 0.45, 0.45, 0.12);
    }

    public static void spawnVoidRiftSlash(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        if (dist < 0.1) return;
        Vec3 dir = diff.normalize();
        Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize();
        if (side.lengthSqr() < 0.01) side = new Vec3(1, 0, 0);

        int steps = Math.max(12, (int) (dist * 4.5));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = start.add(dir.scale(t * dist));
            double tear = Math.sin(t * Math.PI) * 0.75;
            level.sendParticles(VOID_BLACK, p.x, p.y, p.z, 2, 0.08, 0.25 * tear, 0.08, 0);
            level.sendParticles(VOID_PURPLE, p.x + side.x * tear * 0.5, p.y, p.z + side.z * tear * 0.5, 1, 0, 0.15, 0, 0);
            level.sendParticles(VOID_PURPLE, p.x - side.x * tear * 0.5, p.y, p.z - side.z * tear * 0.5, 1, 0, 0.15, 0, 0);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    public static void spawnFireExplosion(ServerLevel level, Vec3 center, double radius) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5, center.z, 55, radius * 0.45, 0.5, radius * 0.45, 0.16);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.4, center.z, 18, radius * 0.4, 0.4, radius * 0.4, 0.1);
        spawnHorizontalCircle(level, center.add(0, 0.2, 0), radius, 34, FIRE_ORANGE);
        spawnHorizontalCircle(level, center.add(0, 0.5, 0), radius * 0.65, 24, FIRE_YELLOW);
    }
}
