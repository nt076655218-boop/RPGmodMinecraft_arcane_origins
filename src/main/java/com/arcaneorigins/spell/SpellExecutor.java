package com.arcaneorigins.spell;

import com.arcaneorigins.classes.ClassManager;
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
            player.displayClientMessage(Component.literal("У тебя пока нет изученных заклинаний!").withStyle(ChatFormatting.GOLD), true);
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
            // Ice
            case GLACIAL_LANCE -> castGlacialLance(level, player, power);
            case FROST_NOVA -> castFrostNova(level, player, power);
            case GLACIAL_PRISON_WALL -> castGlacialPrisonWall(level, player, power);
            case BLIZZARD_TEMPEST -> castBlizzardTempest(level, player, power);
            // Lightning
            case ARC_LIGHTNING -> castArcLightning(level, player, power);
            case THUNDERSTEP -> castThunderstep(level, player, power);
            case STORM_ORB -> castStormOrb(level, player, power);
            case TEMPEST_JUDGMENT -> castTempestJudgment(level, player, power);
            // Gravity / Void
            case VOID_RIFT -> castVoidRift(level, player, power);
            case GRAVITY_WELL -> castGravityWell(level, player, power);
            case ZERO_G_SLAM -> castZeroGSlam(level, player, power);
            case ABYSSAL_METEOR -> castAbyssalMeteor(level, player, power);
            // Fire
            case FIREBALL -> castFireball(level, player, power);
            case FLAME_DASH -> castFlameDash(level, player, power);
            case INFERNO_RING -> castInfernoRing(level, player, power);
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

    private static void castTempestJudgment(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 24.0);
        ActiveSpellManager.addTempest(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.75f);
    }

    // ==================== 3. GRAVITY & VOID SPELLS ====================

    private static void castVoidRift(ServerLevel level, ServerPlayer player, float power) {
        Vec3 start = player.getEyePosition().add(0, -0.35, 0);
        Vec3 look = player.getLookAngle().normalize();
        double range = 20.0;

        BlockHitResult hit = level.clip(new ClipContext(start, start.add(look.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? start.add(look.scale(range)) : hit.getLocation();

        SpellVfx.spawnVoidRiftSlash(level, start.add(look.scale(1.0)), end);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.65f);

        double dist = start.distanceTo(end);
        AABB box = player.getBoundingBox().expandTowards(look.scale(dist)).inflate(1.6);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            Vec3 toT = target.getBoundingBox().getCenter().subtract(start);
            double proj = toT.dot(look);
            if (proj > 0 && proj <= dist + 0.8) {
                Vec3 closest = start.add(look.scale(proj));
                if (closest.distanceTo(target.getBoundingBox().getCenter()) < 1.55) {
                    target.hurt(level.damageSources().magic(), 9.5f * power);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 90, 1));
                    onSpellHitTarget(player, target);
                }
            }
        }
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

    private static void castFireball(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.15, 0);
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = 26.0;

        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(maxDist)) : hit.getLocation();

        double dist = eye.distanceTo(end);
        int steps = Math.max(12, (int) (dist * 4.0));
        for (int i = 2; i <= steps; i++) {
            Vec3 p = eye.lerp(end, (double) i / steps);
            level.sendParticles(SpellVfx.FIRE_ORANGE, p.x, p.y, p.z, 2, 0.14, 0.14, 0.14, 0);
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0.01);
            }
        }

        SpellVfx.spawnFireExplosion(level, end, 3.6);
        level.playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.15f, 1.0f);

        AABB blast = new AABB(end.x - 3.8, end.y - 2.0, end.z - 3.8, end.x + 3.8, end.y + 3.0, end.z + 3.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blast, e -> e.isAlive() && e != player)) {
            target.hurt(level.damageSources().inFire(), 9.5f * power);
            target.igniteForSeconds(5);
            onSpellHitTarget(player, target);
        }
    }

    private static void castFlameDash(ServerLevel level, ServerPlayer player, float power) {
        Vec3 start = player.position();
        Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        if (flatLook.lengthSqr() < 0.01) flatLook = new Vec3(1, 0, 0);

        double maxDist = 9.0;
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(),
                player.getEyePosition().add(flatLook.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 dest = hit.getType() == HitResult.Type.MISS
                ? start.add(flatLook.scale(maxDist))
                : hit.getLocation().subtract(flatLook.scale(1.1));

        for (int i = 0; i <= 18; i++) {
            Vec3 p = start.lerp(dest, i / 18.0);
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.4, p.z, 8, 0.35, 0.35, 0.35, 0.04);
            level.sendParticles(SpellVfx.FIRE_ORANGE, p.x, p.y + 0.2, p.z, 4, 0.3, 0.1, 0.3, 0);
        }

        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0.0f;
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.9f);

        AABB trailBox = new AABB(Math.min(start.x, dest.x) - 1.8, start.y - 1.0, Math.min(start.z, dest.z) - 1.8,
                Math.max(start.x, dest.x) + 1.8, start.y + 2.5, Math.max(start.z, dest.z) + 1.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, trailBox, e -> e.isAlive() && e != player)) {
            target.hurt(level.damageSources().inFire(), 7.5f * power);
            target.igniteForSeconds(5);
            onSpellHitTarget(player, target);
        }
    }

    private static void castInfernoRing(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position();
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.2, 0), 3.5, 0.15, SpellVfx.FIRE_YELLOW);
        for (double r : new double[]{2.5, 4.5, 6.2}) {
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.45, 0), r, (int) (r * 11), SpellVfx.FIRE_ORANGE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 1.0, 0), r * 0.85, (int) (r * 8), ParticleTypes.FLAME);
        }
        level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4f, 0.8f);

        AABB box = new AABB(c.x - 6.5, c.y - 1.5, c.z - 6.5, c.x + 6.5, c.y + 3.5, c.z + 6.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (target.distanceTo(player) <= 6.5) {
                target.hurt(level.damageSources().inFire(), 8.5f * power);
                target.igniteForSeconds(6);
                if (!ActiveSpellManager.isEntityFrozen(target.getUUID())) {
                    Vec3 push = target.position().subtract(c).normalize().scale(1.15);
                    target.setDeltaMovement(push.x, 0.35, push.z);
                    target.hurtMarked = true;
                }
                onSpellHitTarget(player, target);
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
