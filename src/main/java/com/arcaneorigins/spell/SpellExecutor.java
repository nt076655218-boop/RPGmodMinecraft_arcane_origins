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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SpellExecutor {
    // Cooldown map: playerUUID -> (spell -> expireGameTick)
    private static final Map<UUID, Map<ArcaneSpell, Long>> COOLDOWNS = new HashMap<>();

    public static void tryCastSelectedSpell(ServerPlayer player) {
        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        if (!data.hasChosenClass()) {
            player.displayClientMessage(Component.literal("Сначала выбери класс (нажми G)!").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        RpgClass rpgClass = data.getRpgClass();
        RpgSubclass subclass = data.getRpgSubclass();

        // Check caster eligibility & equipment rules!
        boolean hasBookOrCatalyst = isHoldingMagicGear(player);
        if (rpgClass == RpgClass.WARRIOR || rpgClass == RpgClass.BARBARIAN) {
            if (!hasBookOrCatalyst) {
                player.displayClientMessage(Component.literal("Воины и Варвары могут кастовать только держа Книгу Заклинаний или Посох!").withStyle(ChatFormatting.RED), true);
                return;
            }
        } else if (rpgClass == RpgClass.WIZARD) {
            // Wizard NEEDS a Spellbook or Staff/Catalyst
            if (!hasBookOrCatalyst) {
                player.displayClientMessage(Component.literal("Волшебнику нужна Книга Заклинаний или Посох в руке!").withStyle(ChatFormatting.AQUA), true);
                return;
            }
        } else if (rpgClass == RpgClass.WARLOCK) {
            // Warlock uses Patron favor or Sigil/Book
            if (data.getPatronFavor() <= 0 && !hasBookOrCatalyst && !isHoldingPatronSigil(player)) {
                player.displayClientMessage(Component.literal("Принеси подношение Покровителю Печатью или возьми фокус!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
        }
        // SORCERER needs NOTHING — innate magic!

        ArcaneSpell spell = ArcaneSpell.byIndex(data.getSelectedSpellIndex());
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        Map<ArcaneSpell, Long> playerCds = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new EnumMap<>(ArcaneSpell.class));
        long expire = playerCds.getOrDefault(spell, 0L);
        if (now < expire) {
            float sec = (expire - now) / 20.0f;
            player.displayClientMessage(Component.literal(String.format("Откат: %.1f сек.", sec)).withStyle(ChatFormatting.GRAY), true);
            return;
        }

        // Compute Mana Cost & Power Multiplier
        float cost = spell.getManaCost();
        float power = 1.0f + (data.getLevel() - 1) * 0.08f; // +8% per class level
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

        // Equipment catalyst bonuses
        if (isHoldingItem(player, ModItems.FROST_STAFF.get()) && spell.getSchool() == SpellSchool.ICE) {
            power *= 1.20f;
            cdFactor *= 0.85f;
        }
        if (isHoldingItem(player, ModItems.STORM_CATALYST.get()) && spell.getSchool() == SpellSchool.LIGHTNING) {
            power *= 1.20f;
            cdFactor *= 0.85f;
        }

        // Warlock Patron Favor boosts damage & consumes 5 favor
        if (rpgClass == RpgClass.WARLOCK && data.getPatronFavor() >= 5) {
            power *= 1.25f;
            data.addPatronFavor(-5);
        }

        if (data.getMana() < cost && !player.isCreative()) {
            player.displayClientMessage(Component.literal("Недостаточно маны! (" + (int) cost + ")").withStyle(ChatFormatting.BLUE), true);
            return;
        }

        // Spend mana & set cooldown
        if (!player.isCreative()) {
            data.setMana(data.getMana() - cost);
        }
        int finalCd = Math.max(8, (int) (spell.getCooldownTicks() * cdFactor));
        playerCds.put(spell, now + finalCd);

        // Execute spell!
        switch (spell) {
            case GLACIAL_LANCE -> castGlacialLance(level, player, power);
            case FROST_NOVA -> castFrostNova(level, player, power);
            case BLIZZARD_TEMPEST -> castBlizzardTempest(level, player, power);
            case ARC_LIGHTNING -> castArcLightning(level, player, power);
            case THUNDERSTEP -> castThunderstep(level, player, power);
            case TEMPEST_JUDGMENT -> castTempestJudgment(level, player, power);
        }

        // Wild Magic Sorcerer surge (20% chance!)
        if (subclass == RpgSubclass.WILD_MAGIC && level.getRandom().nextFloat() < 0.20f) {
            triggerWildMagicSurge(level, player, data, cost);
        }

        // Award class XP
        if (data.addXp(10)) {
            ClassManager.applyPassives(player);
            player.sendSystemMessage(Component.literal("★ Уровень класса повышен до " + data.getLevel() + "!").withStyle(ChatFormatting.GOLD));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.2f);
        }

        ModNetworking.syncToPlayer(player, false);
    }

    private static void castGlacialLance(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.15, 0);
        Vec3 look = player.getLookAngle().normalize();
        double maxDist = 28.0;

        BlockHitResult blockHit = level.clip(new ClipContext(eye, eye.add(look.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? eye.add(look.scale(maxDist)) : blockHit.getLocation();

        // Caster hand circle + double helix beam
        SpellVfx.spawnHorizontalCircle(level, player.position().add(0, 0.15, 0), 1.2, 16, SpellVfx.ICE_CORE);
        SpellVfx.spawnDoubleHelixBeam(level, eye.add(look.scale(0.8)), end, 0.45, 4.5);
        SpellVfx.spawnCrystalBurst(level, end, 1.8);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9f, 1.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);

        // Hit all living entities along the lance path
        double dist = eye.distanceTo(end);
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(dist)).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && e != player)) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
            double proj = toTarget.dot(look);
            if (proj > 0 && proj <= dist + 0.8) {
                Vec3 closest = eye.add(look.scale(proj));
                if (closest.distanceTo(target.getBoundingBox().getCenter()) < 1.35) {
                    target.hurt(level.damageSources().freeze(), 9.0f * power);
                    target.setTicksFrozen(220);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    SpellVfx.spawnCrystalBurst(level, target.getBoundingBox().getCenter(), 1.6);
                    onSpellHitTarget(player, target);
                }
            }
        }
    }

    private static void castFrostNova(ServerLevel level, ServerPlayer player, float power) {
        Vec3 c = player.position();
        SpellVfx.spawnRuneHexagram(level, c.add(0, 0.15, 0), 3.0, 0.0, SpellVfx.ICE_CORE);
        for (double r : new double[]{2.5, 4.5, 6.5}) {
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 0.3, 0), r, (int) (r * 9), SpellVfx.ICE_DEEP);
            SpellVfx.spawnHorizontalCircle(level, c.add(0, 1.1, 0), r * 0.85, (int) (r * 6), ParticleTypes.SNOWFLAKE);
        }
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.5, c.z, 35, 2.5, 0.4, 2.5, 0.08);

        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.3f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);

        AABB box = new AABB(c.x - 6.8, c.y - 1.5, c.z - 6.8, c.x + 6.8, c.y + 3.5, c.z + 6.8);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (target.distanceTo(player) <= 6.8) {
                target.hurt(level.damageSources().freeze(), 7.5f * power);
                target.setTicksFrozen(300);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                Vec3 push = target.position().subtract(c).normalize().scale(0.85);
                target.setDeltaMovement(push.x, 0.32, push.z);
                target.hurtMarked = true;
                onSpellHitTarget(player, target);
            }
        }
    }

    private static void castBlizzardTempest(ServerLevel level, ServerPlayer player, float power) {
        Vec3 targetPos = findLookTargetPosition(level, player, 20.0);
        ActiveSpellManager.addBlizzard(level, player.getUUID(), targetPos, power);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    private static void castArcLightning(ServerLevel level, ServerPlayer player, float power) {
        Vec3 eye = player.getEyePosition().add(0, -0.2, 0);
        Vec3 look = player.getLookAngle().normalize();
        double range = 24.0;

        // Find primary target near crosshair
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
            Vec3 end = findLookTargetPosition(level, player, range);
            SpellVfx.spawnJaggedLightningArc(level, eye.add(look.scale(0.7)), end, 9, 0.55);
            return;
        }

        // Chain lightning up to 4 targets!
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

            // Find next closest unhit enemy within 8 blocks
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

        // Keep feet safe if destination is inside solid block
        BlockPos destPos = BlockPos.containing(dest);
        if (!level.getBlockState(destPos).getCollisionShape(level, destPos).isEmpty()) {
            dest = dest.add(0, 1.0, 0);
        }

        // Departure spark & trail lightning
        SpellVfx.spawnHorizontalCircle(level, start, 1.6, 20, ParticleTypes.ELECTRIC_SPARK);
        SpellVfx.spawnJaggedLightningArc(level, start, dest.add(0, 1.0, 0), 8, 0.65);

        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0.0f;

        // Arrival thunder shockwave
        SpellVfx.spawnHorizontalCircle(level, dest.add(0, 0.3, 0), 3.6, 28, SpellVfx.LIGHTNING_CORE);
        SpellVfx.spawnHorizontalCircle(level, dest.add(0, 0.7, 0), 2.2, 20, ParticleTypes.ELECTRIC_SPARK);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.1f, 1.4f);

        AABB box = new AABB(dest.x - 4.2, dest.y - 1.0, dest.z - 4.2, dest.x + 4.2, dest.y + 3.0, dest.z + 4.2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            target.hurt(level.damageSources().lightningBolt(), 6.5f * power);
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
            target.hurtMarked = true;
            onSpellHitTarget(player, target);
        }
    }

    private static void castTempestJudgment(ServerLevel level, ServerPlayer player, float power) {
        Vec3 targetPos = findLookTargetPosition(level, player, 24.0);
        ActiveSpellManager.addTempest(level, player.getUUID(), targetPos, power);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.75f);
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

    private static Vec3 findLookTargetPosition(ServerLevel level, ServerPlayer player, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(maxDist));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    public static boolean isHoldingMagicGear(ServerPlayer player) {
        return isHoldingItem(player, ModItems.ARCANE_SPELLBOOK.get())
                || isHoldingItem(player, ModItems.FROST_STAFF.get())
                || isHoldingItem(player, ModItems.STORM_CATALYST.get());
    }

    public static boolean isHoldingPatronSigil(ServerPlayer player) {
        return isHoldingItem(player, ModItems.PATRON_SIGIL.get());
    }

    private static boolean isHoldingItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(item) || off.is(item);
    }
}
