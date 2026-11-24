package com.arcaneorigins.spell;

import com.arcaneorigins.classes.ClassManager;
import com.arcaneorigins.classes.ClassPerk;
import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.item.ModItems;
import com.arcaneorigins.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SpellExecutor {
    private static final Map<UUID, Map<ArcaneSpell, Long>> COOLDOWNS = new HashMap<>();

    public static void tryCastSelectedSpell(ServerPlayer player) {
        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        if (!data.hasChosenClass()) {
            player.displayClientMessage(Component.literal("Сначала выбери класс (нажми G)!").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        ArcaneSpell spell = data.getSelectedSpell();
        if (spell == null) {
            player.displayClientMessage(Component.literal("У тебя пока нет изученных заклинаний! Открой меню прокачки [G]!").withStyle(ChatFormatting.GOLD), true);
            return;
        }

        RpgClass rpgClass = data.getRpgClass();
        RpgSubclass subclass = data.getRpgSubclass();

        boolean hasBookOrCatalyst = isHoldingMagicGear(player);
        if (rpgClass == RpgClass.WARRIOR || rpgClass == RpgClass.BARBARIAN) {
            if (!hasBookOrCatalyst) {
                player.displayClientMessage(Component.literal("Воины и Варвары могут кастовать только держа Книгу Заклинаний или Посох!").withStyle(ChatFormatting.RED), true);
                return;
            }
        } else if (rpgClass == RpgClass.WIZARD) {
            if (!hasBookOrCatalyst) {
                player.displayClientMessage(Component.literal("Волшебнику нужна Книга Заклинаний или Посох в руке!").withStyle(ChatFormatting.AQUA), true);
                return;
            }
        } else if (rpgClass == RpgClass.WARLOCK) {
            if (data.getPatronFavor() <= 0 && !hasBookOrCatalyst && !isHoldingPatronSigil(player)) {
                player.displayClientMessage(Component.literal("Принеси подношение Покровителю Сферой или возьми фокус!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        Map<ArcaneSpell, Long> playerCds = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new EnumMap<>(ArcaneSpell.class));
        long expire = playerCds.getOrDefault(spell, 0L);
        if (now < expire) {
            float sec = (expire - now) / 20.0f;
            player.displayClientMessage(Component.literal(String.format("Откат: %.1f сек.", sec)).withStyle(ChatFormatting.GRAY), true);
            return;
        }

        float cost = spell.getManaCost();
        float power = 1.0f + (data.getLevel() - 1) * 0.08f;
        float cdFactor = 1.0f;

        if (subclass == RpgSubclass.SCHOOL_ICE && spell.getSchool() == SpellSchool.ICE) {
            cost *= 0.80f;
            power *= 1.25f;
        } else if (subclass == RpgSubclass.SCHOOL_LIGHTNING && spell.getSchool() == SpellSchool.LIGHTNING) {
            cost *= 0.80f;
            power *= 1.25f;
        } else if ((subclass == RpgSubclass.SCHOOL_GRAVITY || subclass == RpgSubclass.PATRON_ABYSS) && spell.getSchool() == SpellSchool.GRAVITY) {
            cost *= 0.80f;
            power *= 1.25f;
        } else if ((subclass == RpgSubclass.SCHOOL_FIRE || subclass == RpgSubclass.PATRON_INFERNO) && spell.getSchool() == SpellSchool.FIRE) {
            cost *= 0.80f;
            power *= 1.25f;
        } else if (subclass == RpgSubclass.WILD_MAGIC) {
            cost *= 0.75f;
        }

        // Perk bonuses
        if (data.hasPerk(ClassPerk.SORC_TWIN_FLOW)) power *= 1.15f;
        if (data.hasPerk(ClassPerk.SORC_OVERCHARGE)) cost *= 0.85f;
        if (data.hasPerk(ClassPerk.SORC_METAMAGIC)) cdFactor *= 0.80f;
        if (data.hasPerk(ClassPerk.LOCK_ABYSSAL_ARMOR)) power *= 1.25f;
        if (data.hasPerk(ClassPerk.WIZ_SPELL_MASTERY)) { power *= 1.18f; cost *= 0.85f; }
        if (data.hasPerk(ClassPerk.WIZ_QUICK_CHANT)) cdFactor *= 0.80f;
        if (data.hasPerk(ClassPerk.WIZ_ARCHMAGE)) power *= 1.25f;

        if (isHoldingItem(player, ModItems.FROST_STAFF.get()) && spell.getSchool() == SpellSchool.ICE) {
            power *= 1.20f;
            cdFactor *= 0.85f;
        }
        if (isHoldingItem(player, ModItems.STORM_CATALYST.get()) && spell.getSchool() == SpellSchool.LIGHTNING) {
            power *= 1.20f;
            cdFactor *= 0.85f;
        }

        if (rpgClass == RpgClass.WARLOCK && data.getPatronFavor() >= 5) {
            power *= 1.25f;
            data.addPatronFavor(-5);
        }

        if (data.getMana() < cost && !player.isCreative()) {
            player.displayClientMessage(Component.literal("Недостаточно маны! (" + (int) cost + ")").withStyle(ChatFormatting.BLUE), true);
            return;
        }

        if (!player.isCreative()) {
            data.setMana(data.getMana() - cost);
        }
        int finalCd = Math.max(8, (int) (spell.getCooldownTicks() * cdFactor));
        playerCds.put(spell, now + finalCd);

        switch (spell) {
            // Ice (5)
            case GLACIAL_LANCE -> castGlacialLance(level, player, power);
            case FROST_NOVA -> castFrostNova(level, player, power);
            case GLACIAL_PRISON_WALL -> castGlacialPrisonWall(level, player, power);
            case FROST_SPIKE_WAVE -> castFrostSpikeWave(level, player, power);
            case BLIZZARD_TEMPEST -> castBlizzardTempest(level, player, power);
            // Lightning (5)
            case ARC_LIGHTNING -> castArcLightning(level, player, power);
            case THUNDERSTEP -> castThunderstep(level, player, power);
            case STORM_ORB -> castStormOrb(level, player, power);
            case SHOCKWAVE_NOVA -> castShockwaveNova(level, player, power);
            case TEMPEST_JUDGMENT -> castTempestJudgment(level, player, power);
            // Gravity / Void (5)
            case VOID_RIFT -> castVoidRift(level, player, power);
            case GRAVITY_WELL -> castGravityWell(level, player, power);
            case ZERO_G_SLAM -> castZeroGSlam(level, player, power);
            case VOID_PULL -> castVoidPull(level, player, power);
            case ABYSSAL_METEOR -> castAbyssalMeteor(level, player, power);
            // Fire (5)
            case FIREBALL -> castFireball(level, player, power);
            case FIRE_TORNADO -> castFireTornado(level, player, power);
            case PLASMA_BLAST -> castPlasmaBlast(level, player, power);
            case ERUPTION_PILLARS -> castEruptionPillars(level, player, power);
            case METEOR_SHOWER -> castMeteorShower(level, player, power);
        }

        if (subclass == RpgSubclass.WILD_MAGIC && level.getRandom().nextFloat() < 0.20f) {
            triggerWildMagicSurge(level, player, data, cost);
        }

        if (data.addXp(10)) {
            ClassManager.onClassLevelUp(player, data);
        }

        ModNetworking.syncToPlayer(player, false);
    }

    // ==================== 1. ICE SPELLS ====================

    private static void castGlacialLance(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.15, 0);
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = 28.0;

        BlockHitResult blockHit = level.clip(new ClipContext(eye, eye.add(look.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? eye.add(look.scale(maxDist)) : blockHit.getLocation();

        SpellVfx.spawnDoubleHelixBeam(level, eye.add(look.scale(0.8)), end, 0.45, 4.5);
        SpellVfx.spawnCrystalBurst(level, end, 1.8);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);

        double dist = eye.distanceTo(end);
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(dist)).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && e != player)) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
            double proj = toTarget.dot(look);
            if (proj > 0 && proj <= dist + 0.8) {
                Vec3 closest = eye.add(look.scale(proj));
                if (closest.distanceTo(target.getBoundingBox().getCenter()) < 1.35) {
                    target.hurt(level.damageSources().freeze(), 9.0f * power);
                    ActiveSpellManager.freezeEntityInIce(level, target, 80);
                    onSpellHitTarget(player, target);
                }
            }
        }
    }

    private static void castFrostNova(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position();
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.15, 0), 3.2, 0.0, SpellVfx.ICE_CORE);
        for (double r : new double[]{2.5, 4.5, 6.5}) {
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.3, 0), r, (int) (r * 9), SpellVfx.ICE_DEEP);
        }
        level.sendParticles(SpellVfx.BLUE_ICE_SHARDS, c.x, c.y + 0.7, c.z, 40, 2.8, 0.5, 2.8, 0.12);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.3f, 0.7f);

        AABB box = new AABB(c.x - 6.8, c.y - 1.5, c.z - 6.8, c.x + 6.8, c.y + 3.5, c.z + 6.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (target.distanceTo(player) <= 6.8) {
                target.hurt(level.damageSources().freeze(), 7.5f * power);
                ActiveSpellManager.freezeEntityInIce(level, target, 90);
                onSpellHitTarget(player, target);
            }
        }
    }

    private static void castGlacialPrisonWall(ServerLevel level, ServerPlayer player, float power) {
        Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        if (flatLook.lengthSqr() < 0.01) flatLook = new Vec3(1, 0, 0);
        Vec3 side = new Vec3(-flatLook.z, 0, flatLook.x);

        Vec3 center = player.position().add(flatLook.scale(4.5));
        List<BlockPos> wallBlocks = new ArrayList<>();

        for (int w = -3; w <= 3; w++) {
            double bx = center.x + side.x * w;
            double bz = center.z + side.z * w;
            double by = SpellVfx.snapYToGround(level, bx, player.getY(), bz);
            int wallHeight = (Math.abs(w) <= 1) ? 4 : 3;
            for (int h = 0; h < wallHeight; h++) {
                BlockPos pos = BlockPos.containing(bx, by + h, bz);
                if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
                    wallBlocks.add(pos.immutable());
                }
            }
            level.sendParticles(SpellVfx.BLUE_ICE_SHARDS, bx, by + 1.5, bz, 8, 0.3, 0.8, 0.3, 0.08);
        }

        ActiveSpellManager.addTemporaryIceBlocks(level, wallBlocks, 120);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.3f, 0.8f);

        AABB hitBox = new AABB(center.x - 4.0, center.y - 1.0, center.z - 4.0, center.x + 4.0, center.y + 4.0, center.z + 4.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e.isAlive() && e != player)) {
            target.hurt(level.damageSources().freeze(), 7.0f * power);
            if (!ActiveSpellManager.isEntityFrozen(target.getUUID())) {
                target.setDeltaMovement(flatLook.x * 1.1, 0.45, flatLook.z * 1.1);
                target.hurtMarked = true;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            onSpellHitTarget(player, target);
        }
    }

    private static void castFrostSpikeWave(ServerLevel level, ServerPlayer player, float power) {
        Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        if (flatLook.lengthSqr() < 0.01) flatLook = new Vec3(1, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.3f, 0.75f);

        Set<UUID> hit = new HashSet<>();
        for (int step = 2; step <= 14; step += 2) {
            double px = player.getX() + flatLook.x * step;
            double pz = player.getZ() + flatLook.z * step;
            double py = SpellVfx.snapYToGround(level, px, player.getY(), pz);

            double spikeHeight = 1.6 + (step * 0.14);
            for (double y = 0; y <= spikeHeight; y += 0.35) {
                level.sendParticles(SpellVfx.ICE_CORE, px, py + y, pz, 2, 0.18, 0.1, 0.18, 0);
                level.sendParticles(SpellVfx.ICE_DEEP, px, py + y, pz, 2, 0.28, 0.1, 0.28, 0);
            }
            level.sendParticles(SpellVfx.BLUE_ICE_SHARDS, px, py + 0.8, pz, 12, 0.4, 0.6, 0.4, 0.1);

            AABB box = new AABB(px - 1.6, py - 0.5, pz - 1.6, px + 1.6, py + 3.0, pz + 1.6);
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !hit.contains(e.getUUID()))) {
                hit.add(t.getUUID());
                t.hurt(level.damageSources().freeze(), 8.5f * power);
                if (!ActiveSpellManager.isEntityFrozen(t.getUUID())) {
                    t.setDeltaMovement(flatLook.x * 0.4, 0.72, flatLook.z * 0.4);
                    t.hurtMarked = true;
                }
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
                onSpellHitTarget(player, t);
            }
        }
    }

    private static void castBlizzardTempest(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 22.0);
        ActiveSpellManager.addBlizzard(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    // ==================== 2. LIGHTNING SPELLS ====================

    private static void castArcLightning(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.2, 0);
        Vec3 look = player.getLookAngle().normalize();
        double range = 24.0;

        LivingEntity primary = null;
        double bestScore = Double.MAX_VALUE;
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.5);
        for (LivingEntity ent : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            Vec3 toEnt = ent.getBoundingBox().getCenter().subtract(eye);
            double dist = toEnt.length();
            if (dist > range || dist < 0.5) continue;
            double dot = toEnt.normalize().dot(look);
            if (dot > 0.92) {
                double score = dist * (1.1 - dot);
                if (score < bestScore) {
                    bestScore = score;
                    primary = ent;
                }
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.9f, 1.6f);

        if (primary == null) {
            BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(range)) : hit.getLocation();
            SpellVfx.spawnJaggedLightningArc(level, eye.add(look.scale(0.7)), end, 9, 0.55);
            return;
        }

        Set<UUID> hitIds = new HashSet<>();
        LivingEntity current = primary;
        Vec3 prevPos = eye.add(look.scale(0.6));
        float dmg = 8.5f * power;

        for (int bounce = 0; bounce < 4 && current != null; bounce++) {
            Vec3 curPos = current.getBoundingBox().getCenter();
            SpellVfx.spawnJaggedLightningArc(level, prevPos, curPos, 7, 0.48);
            current.hurt(level.damageSources().lightningBolt(), dmg);
            onSpellHitTarget(player, current);
            hitIds.add(current.getUUID());

            final Vec3 anchor = curPos;
            LivingEntity next = level.getEntitiesOfClass(LivingEntity.class,
                            current.getBoundingBox().inflate(8.0),
                            e -> e.isAlive() && e != player && !hitIds.contains(e.getUUID()))
                    .stream()
                    .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(anchor)))
                    .orElse(null);

            prevPos = curPos;
            current = next;
            dmg *= 0.85f;
        }
    }

    private static void castThunderstep(ServerLevel level, ServerPlayer player, float power) {
        Vec3 start = player.position().add(0, 1.0, 0);
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = 11.0;

        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(),
                player.getEyePosition().add(look.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 dest = hit.getType() == HitResult.Type.MISS
                ? player.position().add(look.scale(maxDist))
                : hit.getLocation().subtract(look.scale(1.1));

        BlockPos destPos = BlockPos.containing(dest);
        if (!level.getBlockState(destPos).getCollisionShape(level, destPos).isEmpty()) {
            dest = dest.add(0, 1.0, 0);
        }

        SpellVfx.spawnJaggedLightningArc(level, start, dest.add(0, 1.0, 0), 8, 0.65);
        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0.0f;

        SpellVfx.spawnHorizontalCircle(level, dest.add(0, 0.3, 0), 3.6, 28, SpellVfx.LIGHTNING_CORE);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.1f, 1.4f);

        AABB box = new AABB(dest.x - 4.2, dest.y - 1.0, dest.z - 4.2, dest.x + 4.2, dest.y + 3.0, dest.z + 4.2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            target.hurt(level.damageSources().lightningBolt(), 6.5f * power);
            if (!ActiveSpellManager.isEntityFrozen(target.getUUID())) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
                target.hurtMarked = true;
            }
            onSpellHitTarget(player, target);
        }
    }

    private static void castStormOrb(ServerLevel level, ServerPlayer player, float power) {
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        Vec3 velocity = player.getLookAngle().normalize().scale(0.32);
        ActiveSpellManager.addStormOrb(level, player.getUUID(), start, velocity, power);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    private static void castShockwaveNova(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position().add(0, 0.9, 0);
        for (double r : new double[]{2.0, 4.0, 6.2}) {
            SpellVfx.spawnHorizontalCircle(level, c, r, (int) (r * 10), SpellVfx.LIGHTNING_CORE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.3, 0), r * 0.9, (int) (r * 8), SpellVfx.LIGHTNING_BLUE);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 55, 3.5, 0.6, 3.5, 0.25);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.55f);

        AABB box = new AABB(c.x - 6.5, c.y - 2.0, c.z - 6.5, c.x + 6.5, c.y + 3.0, c.z + 6.5);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (t.distanceTo(player) <= 6.5) {
                SpellVfx.spawnJaggedLightningArc(level, c, t.getBoundingBox().getCenter(), 5, 0.3);
                t.hurt(level.damageSources().lightningBolt(), 8.0f * power);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                if (!ActiveSpellManager.isEntityFrozen(t.getUUID())) {
                    Vec3 push = t.position().subtract(player.position()).normalize().scale(1.25);
                    t.setDeltaMovement(push.x, 0.4, push.z);
                    t.hurtMarked = true;
                }
                onSpellHitTarget(player, t);
            }
        }
    }

    private static void castTempestJudgment(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 24.0);
        ActiveSpellManager.addTempest(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.75f);
    }

    // ==================== 3. GRAVITY & VOID SPELLS ====================

    /**
     * Void Rift: Opens an upright dimensional portal gate right in front of the caster!
     */
    private static void castVoidRift(ServerLevel level, ServerPlayer player, float power) {
        Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        if (flatLook.lengthSqr() < 0.01) flatLook = new Vec3(1, 0, 0);
        Vec3 portalPos = player.position().add(flatLook.scale(3.4));
        double groundY = SpellVfx.snapYToGround(level, portalPos.x, player.getY(), portalPos.z);
        Vec3 center = new Vec3(portalPos.x, groundY, portalPos.z);

        ActiveSpellManager.addVoidPortalGate(level, player.getUUID(), center, flatLook, power);
        level.playSound(null, center.x, center.y + 1.5, center.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.85f, 1.4f);
    }

    private static void castGravityWell(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 22.0);
        ActiveSpellManager.addGravityWell(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8f, 1.35f);
    }

    private static void castZeroGSlam(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position();
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.2, 0), 4.0, 0.4, SpellVfx.VOID_PURPLE);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.7f);
        ActiveSpellManager.addZeroGSlam(level, player.getUUID(), c, power);
    }

    private static void castVoidPull(ServerLevel level, ServerPlayer player, float power) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition().add(0, -0.3, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.6f);

        for (int dist = 3; dist <= 13; dist += 2) {
            Vec3 ringCenter = origin.add(look.scale(dist));
            SpellVfx.spawnHorizontalCircle(level, ringCenter, 0.6 + dist * 0.22, 18, SpellVfx.VOID_MAGENTA);
        }

        AABB box = player.getBoundingBox().expandTowards(look.scale(15.0)).inflate(3.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            Vec3 toT = target.getBoundingBox().getCenter().subtract(origin);
            double d = toT.length();
            if (d <= 15.0 && toT.normalize().dot(look) > 0.68) {
                target.hurt(level.damageSources().magic(), 7.0f * power);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                if (!ActiveSpellManager.isEntityFrozen(target.getUUID())) {
                    Vec3 pull = player.position().add(look.scale(2.0)).subtract(target.position()).scale(0.45);
                    target.setDeltaMovement(pull.x, 0.35, pull.z);
                    target.hurtMarked = true;
                }
                onSpellHitTarget(player, target);
            }
        }
    }

    private static void castAbyssalMeteor(ServerLevel level, ServerPlayer player, float power) {
        Vec3 impact = SpellVfx.findGroundedTargetPosition(level, player, 24.0);
        Vec3 riftTop = impact.add(0, 11.5, 0);

        SpellVfx.spawnRuneHexagram(level, riftTop, 3.8, 0.0, SpellVfx.VOID_PURPLE);
        for (int s = 0; s <= 22; s++) {
            Vec3 pt = riftTop.lerp(impact, s / 22.0);
            level.sendParticles(SpellVfx.VOID_BLACK, pt.x, pt.y, pt.z, 5, 0.45, 0.3, 0.45, 0);
            level.sendParticles(SpellVfx.VOID_MAGENTA, pt.x, pt.y, pt.z, 3, 0.35, 0.2, 0.35, 0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, pt.x, pt.y, pt.z, 4, 0.4, 0.4, 0.4, 0.05);
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y + 0.5, impact.z, 1, 0, 0, 0, 0);
        SpellVfx.spawnHorizontalCircle(level, impact.add(0, 0.25, 0), 3.0, 28, SpellVfx.VOID_MAGENTA);
        SpellVfx.spawnHorizontalCircle(level, impact.add(0, 0.35, 0), 5.6, 40, SpellVfx.VOID_PURPLE);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.55f);

        AABB blast = new AABB(impact.x - 5.8, impact.y - 1.5, impact.z - 5.8, impact.x + 5.8, impact.y + 4.0, impact.z + 5.8);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, blast, e -> e.isAlive() && e != player)) {
            victim.hurt(level.damageSources().magic(), 14.0f * power);
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            if (!ActiveSpellManager.isEntityFrozen(victim.getUUID())) {
                Vec3 pull = impact.subtract(victim.position()).scale(0.35);
                victim.setDeltaMovement(pull.x, 0.42, pull.z);
                victim.hurtMarked = true;
            }
            onSpellHitTarget(player, victim);
        }
    }

    // ==================== 4. FIRE SPELLS ====================

    /**
     * True Flying Fireball Projectile!
     */
    private static void castFireball(ServerLevel level, ServerPlayer player, float power) {
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(1.1));
        Vec3 velocity = player.getLookAngle().normalize().scale(1.15);
        ActiveSpellManager.addFlyingFireball(level, player.getUUID(), start, velocity, power);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.25f, 0.9f);
    }

    /**
     * Fire Tornado spawned on caster and following them!
     */
    private static void castFireTornado(ServerLevel level, ServerPlayer player, float power) {
        ActiveSpellManager.addFireTornado(level, player.getUUID(), player.position(), power);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.35f, 0.75f);
    }

    /**
     * Wide Cone Plasma Blast in front of caster!
     */
    private static void castPlasmaBlast(ServerLevel level, ServerPlayer player, float power) {
        Vec3 origin = player.getEyePosition().add(0, -0.3, 0);
        Vec3 look = player.getLookAngle().normalize();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4f, 0.95f);

        for (int d = 1; d <= 9; d++) {
            Vec3 center = origin.add(look.scale(d));
            double spread = d * 0.34;
            level.sendParticles(SpellVfx.FIRE_ORANGE, center.x, center.y, center.z, 6, spread, spread * 0.6, spread, 0.04);
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 8, spread, spread * 0.6, spread, 0.08);
            if (d % 2 == 0) {
                level.sendParticles(SpellVfx.FIRE_YELLOW, center.x, center.y, center.z, 4, spread * 0.7, spread * 0.4, spread * 0.7, 0.02);
            }
        }

        AABB box = player.getBoundingBox().expandTowards(look.scale(9.5)).inflate(2.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            Vec3 toT = target.getBoundingBox().getCenter().subtract(origin);
            if (toT.length() <= 9.5 && toT.normalize().dot(look) > 0.62) {
                target.hurt(level.damageSources().inFire(), 9.5f * power);
                target.igniteForSeconds(6);
                if (!ActiveSpellManager.isEntityFrozen(target.getUUID())) {
                    target.setDeltaMovement(look.x * 1.15, 0.38, look.z * 1.15);
                    target.hurtMarked = true;
                }
                onSpellHitTarget(player, target);
            }
        }
    }

    /**
     * Eruption Pillars: Geysers of flame erupting under enemies around the caster!
     */
    private static void castEruptionPillars(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position();
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.15, 0), 4.5, 0.2, SpellVfx.FIRE_CRIMSON);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1f, 1.1f);

        AABB search = new AABB(c.x - 8.5, c.y - 2.0, c.z - 8.5, c.x + 8.5, c.y + 4.0, c.z + 8.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != player);

        List<Vec3> geyserSpots = new ArrayList<>();
        for (int i = 0; i < Math.min(4, targets.size()); i++) {
            geyserSpots.add(targets.get(i).position());
        }
        while (geyserSpots.size() < 4) {
            double a = (Math.PI / 2.0) * geyserSpots.size() + 0.4;
            double gx = c.x + Math.cos(a) * 4.5;
            double gz = c.z + Math.sin(a) * 4.5;
            geyserSpots.add(new Vec3(gx, SpellVfx.snapYToGround(level, gx, c.y, gz), gz));
        }

        for (Vec3 spot : geyserSpots) {
            for (double y = 0; y <= 4.5; y += 0.35) {
                level.sendParticles(SpellVfx.FIRE_ORANGE, spot.x, spot.y + y, spot.z, 3, 0.3, 0.15, 0.3, 0.02);
                level.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + y, spot.z, 4, 0.25, 0.15, 0.25, 0.05);
            }
            level.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 1.0, spot.z, 8, 0.4, 0.8, 0.4, 0.1);
            AABB pillarBox = new AABB(spot.x - 2.1, spot.y - 0.5, spot.z - 2.1, spot.x + 2.1, spot.y + 4.5, spot.z + 2.1);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, pillarBox, e -> e.isAlive() && e != player)) {
                victim.hurt(level.damageSources().inFire(), 9.0f * power);
                victim.igniteForSeconds(5);
                if (!ActiveSpellManager.isEntityFrozen(victim.getUUID())) {
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.75, 0));
                    victim.hurtMarked = true;
                }
                onSpellHitTarget(player, victim);
            }
        }
    }

    private static void castMeteorShower(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 24.0);
        ActiveSpellManager.addMeteorShower(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // ==================== HELPERS ====================

    private static void triggerWildMagicSurge(ServerLevel level, ServerPlayer player, PlayerClassData data, float cost) {
        int roll = level.getRandom().nextInt(3);
        SpellVfx.spawnHorizontalCircle(level, player.position().add(0, 1.0, 0), 2.0, 24, SpellVfx.ARCANE_GOLD);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.8, 0.8, 0.8, 0.5);
        switch (roll) {
            case 0 -> {
                data.setMana(data.getMana() + cost * 1.5f);
                player.displayClientMessage(Component.literal("✦ Всплеск Дикой Магии: Прилив маны!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
                player.displayClientMessage(Component.literal("✦ Всплеск Дикой Магии: Эфирный щит и ускорение!").withStyle(ChatFormatting.GOLD), true);
            }
            default -> {
                player.heal(4.0f);
                player.displayClientMessage(Component.literal("✦ Всплеск Дикой Магии: Исцеляющий импульс!").withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    public static void onSpellHitTarget(ServerPlayer caster, LivingEntity target) {
        if (caster == null) return;
        PlayerClassData data = caster.getData(ModAttachments.CLASS_DATA);
        RpgSubclass sub = data.getRpgSubclass();
        if (sub == RpgSubclass.PATRON_ABYSS) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
        } else if (sub == RpgSubclass.PATRON_ELDER) {
            caster.heal(1.5f);
        } else if (sub == RpgSubclass.PATRON_INFERNO) {
            target.igniteForSeconds(4);
        }
    }

    public static boolean isHoldingMagicGear(ServerPlayer player) {
        return isHoldingItem(player, ModItems.ARCANE_SPELLBOOK.get())
                || isHoldingItem(player, ModItems.FROST_STAFF.get())
                || isHoldingItem(player, ModItems.STORM_CATALYST.get());
    }

    public static boolean isHoldingPatronSigil(ServerPlayer player) {
        return isHoldingItem(player, ModItems.PATRON_SIGIL.get());
    }

    private static boolean isHoldingItem(ServerPlayer player, Item item) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(item) || off.is(item);
    }
}
