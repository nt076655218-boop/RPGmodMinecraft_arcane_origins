package com.arcaneorigins.spell;

import net.minecraft.core.BlockPos;
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
    public static final DustParticleOptions ICE_CORE = new DustParticleOptions(new Vector3f(0.80f, 0.97f, 1.0f), 1.35f);
    public static final DustParticleOptions ICE_DEEP = new DustParticleOptions(new Vector3f(0.12f, 0.58f, 0.96f), 1.65f);
    public static final DustParticleOptions LIGHTNING_CORE = new DustParticleOptions(new Vector3f(0.95f, 0.98f, 1.0f), 1.35f);
    public static final DustParticleOptions LIGHTNING_BLUE = new DustParticleOptions(new Vector3f(0.24f, 0.55f, 1.0f), 1.65f);
    public static final DustParticleOptions STORM_CLOUD_DARK = new DustParticleOptions(new Vector3f(0.16f, 0.20f, 0.30f), 2.6f);
    public static final DustParticleOptions ARCANE_GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.80f, 0.25f), 1.3f);
    public static final DustParticleOptions WARLOCK_VOID = new DustParticleOptions(new Vector3f(0.62f, 0.15f, 0.92f), 1.4f);

    public static final BlockParticleOption ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    public static final BlockParticleOption BLUE_ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState());

    /**
     * Finds a grounded target position along player's look vector — NEVER leaves ground AoEs floating mid-air!
     */
    public static Vec3 findGroundedTargetPosition(ServerLevel level, ServerPlayer player, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(maxDist));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 raw = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();

        // Snap downward to the nearest solid ground block (up to 24 blocks down)
        BlockPos startPos = BlockPos.containing(raw.x, raw.y + 1.0, raw.z);
        for (int dy = 0; dy <= 24; dy++) {
            BlockPos check = startPos.below(dy);
            if (check.getY() <= level.getMinBuildHeight()) break;
            if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
                double topY = check.getY() + level.getBlockState(check).getCollisionShape(level, check).max(net.minecraft.core.Direction.Axis.Y);
                return new Vec3(raw.x, topY, raw.z);
            }
        }
        return player.position();
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

            if (i % 2 == 0) {
                level.sendParticles(ICE_CORE, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            }
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 1, 0.02, 0.02, 0.02, 0.01);
            }

            double r = helixRadius * (1.0 - t * 0.35);
            Vec3 offset1 = right.scale(Math.cos(angle) * r).add(up.scale(Math.sin(angle) * r));
            Vec3 offset2 = offset1.scale(-1.0);
            Vec3 p1 = center.add(offset1);
            Vec3 p2 = center.add(offset2);

            level.sendParticles(ICE_DEEP, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0);
            if (i % 6 == 0) {
                level.sendParticles(ICE_SHARDS, center.x, center.y, center.z, 2, 0.15, 0.15, 0.15, 0.05);
            }
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
}
