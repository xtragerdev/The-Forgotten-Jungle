package com.icedfoxstudios.forgottenjungle.raptor.config;

import java.util.Set;

public final class RaptorConfig {
    public static final String HUD_ID = "TFJRaptorIncubationHud";
    public static final long INCUBATION_MS = 1_440_000L;
    public static final long HATCHLING_GROWTH_MS = 900_000L;
    public static final long YOUNGLING_GROWTH_MS = 900_000L;
    public static final long JUVENILE_GROWTH_MS = 1_260_000L;
    public static final long SUBADULT_GROWTH_MS = 1_260_000L;
    public static final long ADULT_BREEDING_COOLDOWN_MS = 1_800_000L;
    public static final long LOOK_UI_INTERVAL_MS = 125L;
    public static final long NEARBY_SCAN_INTERVAL_MS = 1_000L;
    public static final long STALE_EGG_REMOVE_MS = 90_000L;
    public static final long DEBUG_INTERVAL_MS = 2_000L;
    public static final double LOOK_DISTANCE = 5.5;
    public static final String RAPTOR_CARE_INTERACTION_HINT = "server.interactionHints.open";
    public static final double BREEDING_MATE_RADIUS_BLOCKS = 8.0;
    public static final int NEARBY_SCAN_RADIUS = 6;
    public static final int SET_BLOCK_UPDATE_REASON = 198;
    public static final long HATCH_RETRY_INTERVAL_MS = 1_000L;
    public static final long RAPTOR_TRANSITION_RETRY_INTERVAL_MS = 1_000L;
    public static final long STALE_RAPTOR_REMOVE_MS = 120_000L;
    public static final String RAPTOR_CARE_PAGE_PATH = "Pages/TFJRaptorCarePage.ui";
    public static final String RAPTOR_ADULT_PAGE_PATH = "Pages/TFJRaptorAdultPage.ui";
    public static final String RAPTOR_GUIDE_PAGE_PATH = "Pages/TFJRaptorGuidePage.ui";
    public static final int RAPTOR_GUIDE_ROWS = 10;
    public static final double RAPTOR_FOOD_MAX = 100.0;
    public static final double RAPTOR_FOOD_DRAIN_PER_SECOND = 0.75;
    public static final double RAPTOR_MEAT_FOOD_VALUE = 40.0;
    public static final double RAPTOR_AUTO_EAT_THRESHOLD = 25.0;
    public static final long RAPTOR_AUTO_EAT_DELAY_MS = 6_000L;
    public static final long RAPTOR_STARVE_DAMAGE_INTERVAL_MS = 3_000L;
    public static final double RAPTOR_STARVE_DAMAGE = 2.0;
    public static final double RAPTOR_MEAT_HEAL = 2.0;
    public static final int RAPTOR_CARE_SLOT_COUNT = 3;
    public static final int RAPTOR_CARE_SLOT_CAPACITY = 64;
    public static final long RAPTOR_CARE_OPEN_COOLDOWN_MS = 250L;
    public static final long RAPTOR_CARE_PAGE_REFRESH_MS = 250L;
    public static final int RAPTOR_MAX_LEVEL = 30;
    public static final int RAPTOR_MAX_UPGRADE_LEVEL = 10;
    public static final double RAPTOR_ADULT_BASE_DAMAGE = 8.0;
    public static final double RAPTOR_MOUNT_STAMINA_COST = 15.0;
    public static final double RAPTOR_STAMINA_REGEN_PER_SECOND = 4.0;
    public static final double RAPTOR_MOUNT_STAMINA_DRAIN_PER_SECOND = 1.5;
    public static final long RAPTOR_ACTIVITY_XP_MS = 60_000L;
    public static final int RAPTOR_FOLLOW_XP = 4;
    public static final int RAPTOR_GUARD_XP = 3;
    public static final int RAPTOR_MOUNTED_XP = 10;
    public static final long RAPTOR_TRAINING_COOLDOWN_MS = 5L * 60L * 60L * 1_000L;
    public static final long RAPTOR_COMMAND_TARGET_REFRESH_MS = 1_000L;
    public static final String RAPTOR_MASTER_TARGET_SLOT = "MasterTarget";
    public static final String RAPTOR_LOCKED_TARGET_SLOT = "LockedTarget";
    public static final String RAPTOR_IDLE_STATE = "Idle";
    public static final String RAPTOR_FOLLOW_STATE = "Follow";

    public static final Set<String> MEAT_ITEM_TOKENS = Set.of(
        "meat", "beef", "steak", "pork", "bacon", "chicken", "mutton", "lamb", "rabbit",
        "fish", "cod", "salmon", "trout", "tuna", "carp", "venison", "turkey", "ham",
        "sausage", "drumstick", "ribs", "crab", "lobster", "shrimp", "squid", "calamari"
    );

    public static final double[][] HATCHLING_SPAWN_OFFSETS = new double[][] {
        {0.0, 0.0},
        {1.10, 0.0},
        {-1.10, 0.0},
        {0.0, 1.10},
        {0.0, -1.10},
        {0.78, 0.78},
        {-0.78, 0.78},
        {0.78, -0.78},
        {-0.78, -0.78},
        {1.55, 0.0},
        {-1.55, 0.0},
        {0.0, 1.55},
        {0.0, -1.55}
    };

    public static final double[] HATCHLING_SPAWN_Y_OFFSETS = new double[] {1.10, 0.65, 1.55, 0.25};

    public static final int[][] EGG_LAYING_OFFSETS = new int[][] {
        {0, 0},
        {1, 0},
        {-1, 0},
        {0, 1},
        {0, -1},
        {1, 1},
        {-1, 1},
        {1, -1},
        {-1, -1}
    };

    public static final int[] EGG_LAYING_Y_OFFSETS = new int[] {0, 1, -1};

    private RaptorConfig() {
    }
}
