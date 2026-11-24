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
            player.displayClientMessage(Component.literal("У тебя пока нет изученных заклинаний! Найди или скрафти Свиток Заклинания.").withStyle(ChatFormatting.GOLD), true);
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
            case GLACIAL_LANCE -> castGlacialLance(level, player, power);
            case ICE_SPIKES -> castIceSpikes(level, player, power);
            case FROST_NOVA -> castFrostNova(level, player, power);
            case GLACIAL_AEGIS -> castGlacialAegis(level, player, power);
            case BLIZZARD_TEMPEST -> castBlizzardTempest(level, player, power);
            case ARC_LIGHTNING -> castArcLightning(level, player, power);
            case SPARK_BARRAGE -> castSparkBarrage(level, player, power);
            case THUNDERSTEP -> castThunderstep(level, player, power);
            case STATIC_NOVA -> castStaticNova(level, player, power);
            case TEMPEST_JUDGMENT -> castTempestJudgment(level, player, power);
        }

        if (subclass == RpgSubclass.WILD_MAGIC && level.getRandom().nextFloat() < 0.20f) {
            triggerWildMagicSurge(level, player, data, cost);
        }

        if (data.addXp(10)) {
            ClassManager.onClassLevelUp(player, data);
        }

        ModNetworking.syncToPlayer(player, false);
    }

    // ---------------- ICE SPELLS ----------------

    private static void castGlacialLance(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.15, 0);
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = 28.0;

        BlockHitResult blockHit = level.clip(new ClipContext(eye, eye.add(look.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? eye.add(look.scale(maxDist)) : blockHit.getLocation();

        SpellVfx.spawnHorizontalCircle(level, player.position().add(0, 0.15, 0), 1.3, 18, SpellVfx.ICE_CORE);
        SpellVfx.spawnDoubleHelixBeam(level, eye.add(look.scale(0.8)), end, 0.45, 4.5);
        SpellVfx.spawnCrystalBurst(level, end, 1.8);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9f, 1.6f);
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
                    ActiveSpellManager.freezeEntityInIce(level, target, 75);
                    SpellVfx.spawnCrystalBurst(level, target.getBoundingBox().getCenter(), 1.6);
                    onSpellHitTarget(player, target);
                }
            }
        }
    }

    private static void castIceSpikes(ServerLevel level, ServerPlayer player, float power) {
        Vec3 start = player.position();
        Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        if (flatLook.lengthSqr() < 0.01) flatLook = new Vec3(1, 0, 0);

        level.playSound(null, start.x, start.y, start.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.9f);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.2f);

        Set<UUID> hit = new HashSet<>();
        for (int step = 2; step <= 14; step += 2) {
            double sx = start.x + flatLook.x * step;
            double sz = start.z + flatLook.z * step;
            double sy = SpellVfx.snapYToGround(level, sx, start.y, sz);
            Vec3 spikeBase = new Vec3(sx, sy, sz);
            double spikeHeight = 1.6 + step * 0.14;

            SpellVfx.spawnIceSpikeCone(level, spikeBase, spikeHeight);

            AABB spikeBox = new AABB(sx - 1.3, sy - 0.5, sz - 1.3, sx + 1.3, sy + spikeHeight + 0.8, sz + 1.3);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, spikeBox, e -> e.isAlive() && e != player)) {
                if (hit.add(target.getUUID())) {
                    target.hurt(level.damageSources().freeze(), 8.0f * power);
                    target.setDeltaMovement(target.getDeltaMovement().add(0, 0.52, 0));
                    target.hurtMarked = true;
                    target.setTicksFrozen(220);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
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
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 1.1, 0), r * 0.85, (int) (r * 6), ParticleTypes.SNOWFLAKE);
        }
        level.sendParticles(SpellVfx.BLUE_ICE_SHARDS, c.x, c.y + 0.7, c.z, 40, 2.8, 0.5, 2.8, 0.12);

        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.3f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);

        AABB box = new AABB(c.x - 6.8, c.y - 1.5, c.z - 6.8, c.x + 6.8, c.y + 3.5, c.z + 6.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (target.distanceTo(player) <= 6.8) {
                target.hurt(level.damageSources().freeze(), 7.5f * power);
                ActiveSpellManager.freezeEntityInIce(level, target, 90);
                onSpellHitTarget(player, target);
            }
        }
    }

    private static void castGlacialAegis(ServerLevel level, ServerPlayer player, float power) {
        int amp = power >= 1.25f ? 2 : 1;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, amp));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        player.setTicksFrozen(0);

        Vec3 c = player.position();
        for (double yOff : new double[]{0.2, 1.0, 1.8}) {
            SpellVfx.spawnHorizontalCircle(level, c.add(0, yOff, 0), 1.4, 24, SpellVfx.ICE_CORE);
        }
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.15, 0), 2.1, 0.3, SpellVfx.ICE_DEEP);
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 1.0, c.z, 30, 0.9, 0.9, 0.9, 0.05);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.3f, 1.4f);

        // Chill nearby melee attackers on activation
        AABB box = player.getBoundingBox().inflate(3.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            target.hurt(level.damageSources().freeze(), 4.5f * power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            onSpellHitTarget(player, target);
        }
    }

    private static void castBlizzardTempest(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 22.0);
        ActiveSpellManager.addBlizzard(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    // ---------------- LIGHTNING SPELLS ----------------

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

    private static void castSparkBarrage(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.2, 0);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 0.01) right = new Vec3(1, 0, 0);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.85f);

        Set<UUID> hit = new HashSet<>();
        double[] angles = {-0.28, -0.14, 0.0, 0.14, 0.28};
        for (double yawOff : angles) {
            Vec3 rayDir = look.add(right.scale(yawOff)).normalize();
            double range = 17.0;
            BlockHitResult blockHit = level.clip(new ClipContext(eye, eye.add(rayDir.scale(range)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = blockHit.getType() == HitResult.Type.MISS ? eye.add(rayDir.scale(range)) : blockHit.getLocation();

            SpellVfx.spawnJaggedLightningArc(level, eye.add(rayDir.scale(0.6)), end, 6, 0.35);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 12, 0.25, 0.25, 0.25, 0.1);

            double dist = eye.distanceTo(end);
            AABB search = player.getBoundingBox().expandTowards(rayDir.scale(dist)).inflate(1.3);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != player)) {
                Vec3 toT = target.getBoundingBox().getCenter().subtract(eye);
                double proj = toT.dot(rayDir);
                if (proj > 0 && proj <= dist + 0.6) {
                    Vec3 closest = eye.add(rayDir.scale(proj));
                    if (closest.distanceTo(target.getBoundingBox().getCenter()) < 1.25 && hit.add(target.getUUID())) {
                        target.hurt(level.damageSources().lightningBolt(), 6.5f * power);
                        onSpellHitTarget(player, target);
                    }
                }
            }
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

        SpellVfx.spawnHorizontalCircle(level, start, 1.6, 20, ParticleTypes.ELECTRIC_SPARK);
        SpellVfx.spawnJaggedLightningArc(level, start, dest.add(0, 1.0, 0), 8, 0.65);

        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0.0f;

        SpellVfx.spawnHorizontalCircle(level, dest.add(0, 0.3, 0), 3.6, 28, SpellVfx.LIGHTNING_CORE);
        SpellVfx.spawnHorizontalCircle(level, dest.add(0, 0.7, 0), 2.2, 20, ParticleTypes.ELECTRIC_SPARK);
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

    private static void castStaticNova(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position();
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.2, 0), 3.4, 0.2, SpellVfx.LIGHTNING_CORE);
        for (double r : new double[]{2.5, 4.5, 6.5}) {
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.4, 0), r, (int) (r * 10), SpellVfx.LIGHTNING_BLUE);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 1.0, 0), r * 0.9, (int) (r * 7), ParticleTypes.ELECTRIC_SPARK);
        }

        // Radial lightning spokes outward
        for (int i = 0; i < 8; i++) {
            double a = (2.0 * Math.PI * i) / 8.0;
            Vec3 end = c.add(Math.cos(a) * 6.2, 0.7, Math.sin(a) * 6.2);
            SpellVfx.spawnJaggedLightningArc(level, c.add(0, 1.0, 0), end, 5, 0.4);
        }

        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.3f, 1.35f);

        AABB box = new AABB(c.x - 6.6, c.y - 1.5, c.z - 6.6, c.x + 6.6, c.y + 3.5, c.z + 6.6);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (target.distanceTo(player) <= 6.6) {
                target.hurt(level.damageSources().lightningBolt(), 8.0f * power);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                if (!ActiveSpellManager.isEntityFrozen(target.getUUID())) {
                    Vec3 push = target.position().subtract(c).normalize().scale(1.1);
                    target.setDeltaMovement(push.x, 0.35, push.z);
                    target.hurtMarked = true;
                }
                onSpellHitTarget(player, target);
            }
        }
    }

    private static void castTempestJudgment(ServerLevel level, ServerPlayer player, float power) {
        Vec3 groundedPos = SpellVfx.findGroundedTargetPosition(level, player, 24.0);
        ActiveSpellManager.addTempest(level, player.getUUID(), groundedPos, power);
        level.playSound(null, groundedPos.x, groundedPos.y, groundedPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.75f);
    }

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
