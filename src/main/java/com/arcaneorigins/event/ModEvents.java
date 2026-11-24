package com.arcaneorigins.event;

import com.arcaneorigins.classes.ClassManager;
import com.arcaneorigins.classes.ClassPerk;
import com.arcaneorigins.classes.RpgClass;
import com.arcaneorigins.classes.RpgSubclass;
import com.arcaneorigins.data.ModAttachments;
import com.arcaneorigins.data.PlayerClassData;
import com.arcaneorigins.network.ModNetworking;
import com.arcaneorigins.spell.ActiveSpellManager;
import com.arcaneorigins.spell.SpellVfx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);

        if (!data.isSorcererRolled()) {
            data.setSorcererRolled(true);
            boolean lucky = player.getRandom().nextFloat() < 0.10f;
            data.setSorcererUnlocked(lucky);
            if (lucky) {
                player.sendSystemMessage(Component.literal("★ В вашей крови пробудилась древняя искра! Доступен редкий класс ЧАРОДЕЙ! ★")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }

        if (data.hasChosenClass()) {
            ClassManager.applyPassives(player);
            ModNetworking.syncToPlayer(player, false);
        } else {
            ModNetworking.syncToPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassManager.applyPassives(player);
            ModNetworking.syncToPlayer(player, false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ActiveSpellManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 != 0) return;

        PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
        if (!data.hasChosenClass()) return;

        RpgClass rpgClass = data.getRpgClass();
        RpgSubclass sub = data.getRpgSubclass();

        float regen = rpgClass != null && rpgClass.isCaster() ? 2.6f : 1.0f;
        if (sub == RpgSubclass.ASTRAL) regen *= 1.35f;
        if (data.hasPerk(ClassPerk.WIZ_SCHOLAR)) regen *= 1.25f;
        if (rpgClass == RpgClass.WARLOCK && data.getPatronFavor() > 0) regen *= 1.25f;

        if (data.getMana() < data.getMaxMana()) {
            data.setMana(data.getMana() + regen);
            ModNetworking.syncToPlayer(player, false);
        }

        if (sub == RpgSubclass.SCHOOL_ICE) {
            player.setTicksFrozen(0);
        } else if (sub == RpgSubclass.SCHOOL_GRAVITY) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, true, false));
        } else if (sub == RpgSubclass.SCHOOL_FIRE || sub == RpgSubclass.PATRON_INFERNO || sub == RpgSubclass.DRACONIC) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
        } else if (sub == RpgSubclass.PATRON_ELDER) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }

        if (data.hasPerk(ClassPerk.WAR_SECOND_WIND) && player.tickCount % 100 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.5f);
        }
        if (data.hasPerk(ClassPerk.BARB_UNYIELDING)) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (data.hasPerk(ClassPerk.BARB_TITAN_RAGE)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, false));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            PlayerClassData data = killer.getData(ModAttachments.CLASS_DATA);
            if (data.hasChosenClass()) {
                if (data.hasPerk(ClassPerk.BARB_BLOODLUST)) {
                    killer.heal(4.0f);
                    killer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
                }
                if (data.hasPerk(ClassPerk.LOCK_SOUL_SIPHON)) {
                    killer.heal(3.0f);
                    data.setMana(data.getMana() + 25.0f);
                }
                if (data.addXp(16)) {
                    ClassManager.onClassLevelUp(killer, data);
                }
                ModNetworking.syncToPlayer(killer, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerClassData data = player.getData(ModAttachments.CLASS_DATA);
            if (data.getRpgSubclass() == RpgSubclass.ASTRAL || data.getRpgSubclass() == RpgSubclass.SCHOOL_GRAVITY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.IN_WALL)
                && ActiveSpellManager.isEntityFrozen(event.getEntity().getUUID())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            PlayerClassData defData = defender.getData(ModAttachments.CLASS_DATA);
            RpgSubclass sub = defData.getRpgSubclass();
            if (sub == RpgSubclass.TOTEMIC) {
                event.setAmount(event.getAmount() * 0.85f);
            } else if (sub == RpgSubclass.BERSERKER) {
                defender.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
                defender.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0));
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() instanceof LivingEntity victim && victim != attacker) {
            PlayerClassData atkData = attacker.getData(ModAttachments.CLASS_DATA);
            RpgSubclass sub = atkData.getRpgSubclass();

            if (atkData.hasPerk(ClassPerk.WAR_EXECUTIONER) && victim.getHealth() < victim.getMaxHealth() * 0.5f) {
                event.setAmount(event.getAmount() * 1.35f);
            }

            if (atkData.hasPerk(ClassPerk.BARB_SEISMIC_SLAM) && attacker.fallDistance > 1.2f && !attacker.onGround()) {
                ServerLevel sl = attacker.serverLevel();
                Vec3 c = victim.position();
                SpellVfx.spawnHorizontalCircle(sl, c.add(0, 0.2, 0), 3.6, 26, SpellVfx.FIRE_ORANGE);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
                for (LivingEntity near : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c.x - 3.8, c.y - 1.0, c.z - 3.8, c.x + 3.8, c.y + 2.5, c.z + 3.8),
                        e -> e.isAlive() && e != attacker)) {
                    near.setDeltaMovement(near.getDeltaMovement().add(0, 0.52, 0));
                    near.hurtMarked = true;
                }
            }

            if (sub == RpgSubclass.BERSERKER) {
                attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
            } else if (sub == RpgSubclass.THUNDERBREAKER && attacker.getRandom().nextFloat() < 0.35f) {
                ServerLevel sl = attacker.serverLevel();
                SpellVfx.spawnJaggedLightningArc(sl, attacker.getEyePosition().add(0, -0.3, 0),
                        victim.getBoundingBox().getCenter(), 5, 0.35);
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.8f);
                event.setAmount(event.getAmount() + 2.5f);
            } else if (sub == RpgSubclass.PATRON_INFERNO) {
                victim.igniteForSeconds(4);
            } else if (sub == RpgSubclass.PATRON_ABYSS) {
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
        }
    }
}
