package com.icedfoxstudios.forgottenjungle.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.joml.Vector3d;

import static com.icedfoxstudios.forgottenjungle.raptor.config.RaptorConfig.*;
import static com.icedfoxstudios.forgottenjungle.raptor.text.RaptorText.*;


final class RaptorRuntimeState {
    final String key;
    final String worldKey;
    Ref<EntityStore> ref;
    final RaptorRoleInfo roleInfo;
    final RaptorSex sex;
    final String[] meatSlotItems = new String[RAPTOR_CARE_SLOT_COUNT];
    final int[] meatSlotCounts = new int[RAPTOR_CARE_SLOT_COUNT];
    long startedAtMs;
    long completeAtMs;
    long lastSeenMs;
    long nextTransitionAttemptMs;
    long nextStarveDamageMs;
    long nextAutoFeedMs;
    long lastCareTickMs;
    double foodLevel;
    double careHealth;
    double careMaxHealth;
    boolean transitionPending;
    boolean needsAdultMate;
    boolean starving;
    boolean dead;
    boolean careHealthInitialized;
    boolean breedingEnabled;
    int level;
    int xp;
    int unspentPoints;
    int damageLevel;
    int speedLevel;
    int staminaLevel;
    double stamina;
    RaptorCommandMode commandMode = RaptorCommandMode.IDLE;
    UUID ownerUuid;
    Ref<EntityStore> ownerRef;
    boolean hasGuardPosition;
    double guardX;
    double guardY;
    double guardZ;
    long nextAdultXpMs;
    long lastAdultTickMs;
    long lastMountedAtMs;
    long nextCommandTargetRefreshMs;
    long nextTrainingAtMs;

    RaptorRuntimeState(String key, String worldKey, Ref<EntityStore> ref, RaptorRoleInfo roleInfo, RaptorSex sex, long nowMs) {
        this.key = key;
        this.worldKey = worldKey;
        this.ref = ref;
        this.roleInfo = roleInfo;
        this.sex = sex == null ? RaptorSex.FEMALE : sex;
        this.startedAtMs = nowMs;
        this.completeAtMs = nowMs + roleInfo.durationMs;
        this.lastSeenMs = nowMs;
        this.nextTransitionAttemptMs = this.completeAtMs;
        this.nextStarveDamageMs = 0L;
        this.nextAutoFeedMs = 0L;
        this.lastCareTickMs = nowMs;
        this.foodLevel = roleInfo.isGrowthStage() ? RAPTOR_FOOD_MAX : 0.0;
        this.careMaxHealth = ForgottenJungleRuntime.raptorCareMaxHealth(roleInfo);
        this.careHealth = this.careMaxHealth;
        this.breedingEnabled = false;
        this.level = 1;
        this.xp = 0;
        this.unspentPoints = 0;
        this.damageLevel = 0;
        this.speedLevel = 0;
        this.staminaLevel = 0;
        this.stamina = ForgottenJungleRuntime.raptorMaxStamina(this);
        this.nextAdultXpMs = nowMs + RAPTOR_ACTIVITY_XP_MS;
        this.lastAdultTickMs = nowMs;
        this.nextCommandTargetRefreshMs = nowMs;
        this.nextTrainingAtMs = 0L;
    }

    void inheritCareFrom(RaptorRuntimeState previous, long nowMs) {
        if (previous == null) {
            return;
        }
        for (int i = 0; i < RAPTOR_CARE_SLOT_COUNT; i++) {
            this.meatSlotItems[i] = previous.meatSlotItems[i];
            this.meatSlotCounts[i] = previous.meatSlotCounts[i];
        }
        this.careHealth = clamp(previous.careHealth, 1.0, Math.max(1.0, this.careMaxHealth));
        this.careHealthInitialized = previous.careHealthInitialized;
        this.foodLevel = clamp(previous.foodLevel, 0.0, RAPTOR_FOOD_MAX);
        this.starving = previous.starving;
        this.breedingEnabled = previous.breedingEnabled;
        this.level = Math.max(1, previous.level);
        this.xp = Math.max(0, previous.xp);
        this.unspentPoints = Math.max(0, previous.unspentPoints);
        this.damageLevel = clampInt(previous.damageLevel, 0, RAPTOR_MAX_UPGRADE_LEVEL);
        this.speedLevel = clampInt(previous.speedLevel, 0, RAPTOR_MAX_UPGRADE_LEVEL);
        this.staminaLevel = clampInt(previous.staminaLevel, 0, RAPTOR_MAX_UPGRADE_LEVEL);
        this.stamina = clamp(previous.stamina, 0.0, ForgottenJungleRuntime.raptorMaxStamina(this));
        this.commandMode = previous.commandMode == null ? RaptorCommandMode.IDLE : previous.commandMode;
        this.ownerUuid = previous.ownerUuid;
        this.ownerRef = previous.ownerRef;
        this.hasGuardPosition = previous.hasGuardPosition;
        this.guardX = previous.guardX;
        this.guardY = previous.guardY;
        this.guardZ = previous.guardZ;
        this.nextAdultXpMs = Math.max(nowMs + 1_000L, previous.nextAdultXpMs);
        this.lastAdultTickMs = nowMs;
        this.lastMountedAtMs = previous.lastMountedAtMs;
        this.nextCommandTargetRefreshMs = nowMs;
        this.nextTrainingAtMs = Math.max(0L, previous.nextTrainingAtMs);
        this.lastCareTickMs = nowMs;
        this.nextStarveDamageMs = Math.max(nowMs + 1_000L, previous.nextStarveDamageMs);
        this.nextAutoFeedMs = Math.max(0L, previous.nextAutoFeedMs);
    }
}
