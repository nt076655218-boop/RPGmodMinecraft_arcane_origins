package com.arcaneorigins.event;

import com.arcaneorigins.classes.ClassManager;
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
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            PlayerClassData data = killer.getData(ModAttachments.CLASS_DATA);
            if (data.hasChosenClass()) {
                if (data.addXp(14)) {
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
