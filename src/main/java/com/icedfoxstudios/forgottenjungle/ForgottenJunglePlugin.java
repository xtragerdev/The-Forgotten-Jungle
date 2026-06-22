package com.icedfoxstudios.forgottenjungle;

import com.google.gson.JsonElement;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.mounts.MountSystems;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.AnimationSlot;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import org.joml.Vector3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.icedfoxstudios.forgottenjungle.RaptorConfig.*;
import static com.icedfoxstudios.forgottenjungle.RaptorText.*;

public final class ForgottenJunglePlugin extends JavaPlugin {
    private static final List<EggInfo> EGG_INFOS = new ArrayList<>();
    private static final Map<Integer, EggInfo> EGG_BY_BLOCK_ID = new HashMap<>();
    private static final Map<String, EggInfo> EGG_BY_BLOCK_KEY = new HashMap<>();
    private static final Map<String, EggInfo> EGG_BY_SUFFIX = new HashMap<>();
    private static final Map<String, EggRuntimeState> KNOWN_EGGS = new HashMap<>();
    private static final Map<String, RaptorRoleInfo> RAPTOR_BY_ROLE = new HashMap<>();
    private static final Map<String, RaptorRuntimeState> KNOWN_RAPTORS = new HashMap<>();
    private static final Map<String, RaptorSex> RAPTOR_SEX_BY_KEY = new ConcurrentHashMap<>();
    private static final Map<UUID, RaptorCarePage> RAPTOR_CARE_PAGE_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_SCAN_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> NEXT_UI_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> NEXT_DEBUG_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> PENDING_EGG_FAST_FORWARD_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PENDING_GROWTH_FAST_FORWARD_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PENDING_BREEDING_FAST_FORWARD_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PENDING_GUIDE_OPEN_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<String, Long> RAPTOR_CARE_LAST_OPEN_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Ref<EntityStore>> RAPTOR_CARE_PROMPT_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Set<Integer> PTERODACTYL_FLIGHT_ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> PTERODACTYL_FLIGHT_ANIMATION_ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> PTERODACTYL_JUMP_HELD = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Long> PTERODACTYL_JUMP_STARTED_MS = new ConcurrentHashMap<>();
    private static final Map<Integer, Double> PTERODACTYL_LAST_Y = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> PTERODACTYL_LANDING_SINCE_MS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> PTERODACTYL_NEXT_ANIMATION_MS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> PTERODACTYL_LAST_DEBUG = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PTERODACTYL_RIDER_ACTIVE_MOUNT_ID = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PTERODACTYL_RIDER_ACTIVE_MOUNT_SEEN_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PTERODACTYL_RIDER_ACTIVE_MOUNT_STARTED_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, PterodactylLookInput> PTERODACTYL_RIDER_LAST_LOOK = new ConcurrentHashMap<>();
    private static final Map<UUID, PterodactylMoveInput> PTERODACTYL_RIDER_LAST_MOVEMENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PTERODACTYL_RIDER_LAST_INPUT_MS = new ConcurrentHashMap<>();
    private static final String TFJ_INSTANCE_TEMPLATE = "The_Forgotten_Jungle";
    private static final String TFJ_INSTANCE_WORLD_KEY = "the_forgotten_jungle_world";
    private static final long TFJ_PORTAL_COOLDOWN_MS = 6500L;
    private static final Map<UUID, Long> TFJ_PORTAL_ENTRY_COOLDOWN_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TFJ_PORTAL_RETURN_COOLDOWN_MS = new ConcurrentHashMap<>();
    private static final Set<String> TFJ_PORTAL_SPAWN_BUILT = ConcurrentHashMap.newKeySet();
    private static final Set<String> TFJ_RETURN_PORTAL_SPAWN_BUILT = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> TFJ_PORTAL_SPAWN_ATTEMPT_MS = new ConcurrentHashMap<>();
    private static volatile int tfjPortalBlockId = Integer.MIN_VALUE;
    private static volatile int tfjReturnPortalBlockId = Integer.MIN_VALUE;
    private static volatile int tfjPterodactylRoleIndex = Integer.MIN_VALUE;
    private static volatile int tfjPterodactylMountRoleIndex = Integer.MIN_VALUE;
    private static volatile long lastPterodactylFlightDebugSecond = -1L;
    private static long nextGlobalDebugMs;

    static {
        registerEggInfo(new EggInfo("Red", "Red Raptor", "TFJ_Raptor_Red_Hatchling", true, 1.00, 1.00, "Balanced"));
        registerEggInfo(new EggInfo("Blue", "Blue Raptor", "TFJ_Raptor_Blue_Hatchling", true, 0.92, 1.12, "Fast"));
        registerEggInfo(new EggInfo("Yellow", "Yellow Raptor", "TFJ_Raptor_Yellow_Hatchling", true, 0.95, 1.07, "Agile"));
        registerEggInfo(new EggInfo("Green", "Green Raptor", "TFJ_Raptor_Green_Hatchling", true, 1.08, 0.96, "Hardy"));
        registerEggInfo(new EggInfo("White", "White Raptor", "TFJ_Raptor_White_Hatchling", true, 1.05, 1.02, "Rare wild"));
        registerEggInfo(new EggInfo("Cyan", "Cyan Raptor", "TFJ_Raptor_Cyan_Hatchling", true, 0.88, 1.18, "Very fast"));
        registerEggInfo(new EggInfo("Black", "Black Raptor", "TFJ_Raptor_Black_Hatchling", false, 0.90, 1.20, "Fragile sprinter"));
        registerEggInfo(new EggInfo("Gold", "Gold Raptor", "TFJ_Raptor_Gold_Hatchling", false, 1.18, 1.08, "Prime"));
        registerEggInfo(new EggInfo("Violet", "Violet Raptor", "TFJ_Raptor_Violet_Hatchling", false, 0.96, 1.15, "Quick"));
        registerEggInfo(new EggInfo("Emerald", "Emerald Raptor", "TFJ_Raptor_Emerald_Hatchling", false, 1.22, 0.94, "Bulky"));
        registerEggInfo(new EggInfo("Amber", "Amber Raptor", "TFJ_Raptor_Amber_Hatchling", false, 1.12, 1.00, "Sturdy"));
        registerEggInfo(new EggInfo("Rose", "Rose Raptor", "TFJ_Raptor_Rose_Hatchling", false, 0.90, 1.10, "Light"));
        registerEggInfo(new EggInfo("Jade", "Jade Raptor", "TFJ_Raptor_Jade_Hatchling", false, 1.15, 1.02, "Resilient"));
        registerEggInfo(new EggInfo("Ivory", "Ivory Raptor", "TFJ_Raptor_Ivory_Hatchling", false, 1.28, 0.88, "Heavy"));
        registerEggInfo(new EggInfo("Azure", "Azure Raptor", "TFJ_Raptor_Azure_Hatchling", false, 0.86, 1.24, "Swift"));
        registerEggInfo(new EggInfo("Crimson", "Crimson Raptor", "TFJ_Raptor_Crimson_Hatchling", false, 1.10, 1.16, "Apex strain"));
        registerEggInfo(new EggInfo("Pterodactyl", "Pterodactyl", "TFJ_Pterodactyl_Egg", "TFJ_Pterodactyl", "TFJ_Pterodactyl_Hatchling", true, 1.14, 1.28, "Flying mount"));
        for (EggInfo info : EGG_INFOS) {
            registerRaptorRoleInfo(info);
        }
    }

    private static void registerEggInfo(EggInfo info) {
        if (info == null) {
            return;
        }
        EGG_INFOS.add(info);
        EGG_BY_SUFFIX.put(normalizeSuffix(info.suffix), info);
    }

    private static EggInfo getEggInfoBySuffix(String suffix) {
        if (suffix == null) {
            return null;
        }
        return EGG_BY_SUFFIX.get(normalizeSuffix(suffix));
    }

    public ForgottenJunglePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        resolveEggBlockIds();
        registerRaptorCareInteractionAction();
        getEntityStoreRegistry().registerSystem(new RaptorIncubationSystem());
        getEntityStoreRegistry().registerSystem(new RaptorLifecycleSystem());
        getEntityStoreRegistry().registerSystem(new PterodactylMountInputSystem());
        getEntityStoreRegistry().registerSystem(new PterodactylFlightSystem());
        getEntityStoreRegistry().registerSystem(new PterodactylMountCleanupSystem());
        getEntityStoreRegistry().registerSystem(new ForgottenJunglePortalSpawnSystem());
        getEntityStoreRegistry().registerSystem(new ForgottenJunglePortalEntrySystem());
        getEntityStoreRegistry().registerSystem(new ForgottenJunglePortalReturnSystem());
        registerRaptorCommands();
        System.out.println("[TFJ] ForgottenJunglePlugin setup: eggInfos=" + EGG_INFOS.size() + ", blockIds=" + EGG_BY_BLOCK_ID.size() + ", blockKeys=" + EGG_BY_BLOCK_KEY.size());
    }

    private void registerRaptorCareInteractionAction() {
        try {
            NPCPlugin plugin = NPCPlugin.get();
            if (plugin == null) {
                System.out.println("[TFJ] NPCPlugin unavailable; raptor care interaction action not registered.");
                return;
            }
            plugin.registerCoreComponentType("OpenTFJRaptorCareUI", BuilderActionOpenRaptorCareUI::new);
            System.out.println("[TFJ] Registered OpenTFJRaptorCareUI interaction action.");
        } catch (Throwable throwable) {
            System.out.println("[TFJ] Could not register OpenTFJRaptorCareUI action: " + describeThrowable(throwable));
        }
    }


    private void registerRaptorCommands() {
        registerRaptorCommand(new RaptorEggTenSecondsCommand(), "/tfjegg10");
        registerRaptorCommand(new RaptorGrowthTenSecondsCommand(), "/tfjgrow10");
        registerRaptorCommand(new RaptorBreedingTenSecondsCommand(), "/tfjbreed10");
        registerRaptorCommand(new RaptorAllTenSecondsCommand(), "/tfjraptor10");
        registerRaptorCommand(new RaptorGuideCommand(), "/tfjraptorguide");
    }

    private void registerRaptorCommand(CommandBase command, String label) {
        try {
            Object registry = getCommandRegistry();
            if (registry == null) {
                System.out.println("[TFJ] Could not register " + label + " command: registry is null.");
                return;
            }

            java.lang.reflect.Method fallback = null;
            StringBuilder candidates = new StringBuilder();
            for (java.lang.reflect.Method method : registry.getClass().getMethods()) {
                if (!"registerCommand".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (candidates.length() > 0) {
                    candidates.append(", ");
                }
                candidates.append(parameter.getName());
                if (parameter.isAssignableFrom(command.getClass())) {
                    method.invoke(registry, command);
                    System.out.println("[TFJ] Registered " + label + " command.");
                    return;
                }
                if (fallback == null) {
                    fallback = method;
                }
            }

            if (fallback != null) {
                fallback.invoke(registry, command);
                System.out.println("[TFJ] Registered " + label + " command through fallback signature " + fallback.getParameterTypes()[0].getName() + ".");
                return;
            }

            System.out.println("[TFJ] Could not register " + label + " command: no registerCommand method found. candidates=" + candidates);
        } catch (Throwable throwable) {
            System.out.println("[TFJ] Could not register " + label + " command: " + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static final class RaptorEggTenSecondsCommand extends CommandBase {
        private RaptorEggTenSecondsCommand() {
            super("tfjegg10", "Set nearby TFJ raptor eggs to the final 10 seconds.");
            addAliases("tfjeggtest");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            queueFastForwardRequest(ctx, true, false, false, "TFJ: pedido recibido. Ponte cerca del huevo; se pondra a 10 segundos en el siguiente tick.");
        }
    }

    private static final class RaptorGrowthTenSecondsCommand extends CommandBase {
        private RaptorGrowthTenSecondsCommand() {
            super("tfjgrow10", "Set nearby TFJ hatchling/juvenile growth timers to the final 10 seconds.");
            addAliases("tfjraptorgrow10", "tfjgrowth10");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            queueFastForwardRequest(ctx, false, true, false, "TFJ: pedido recibido. Mira o acercate a un raptor joven; su crecimiento bajara a 10 segundos.");
        }
    }

    private static final class RaptorBreedingTenSecondsCommand extends CommandBase {
        private RaptorBreedingTenSecondsCommand() {
            super("tfjbreed10", "Set nearby adult TFJ raptor breeding cooldowns to the final 10 seconds.");
            addAliases("tfjraptorbreed10", "tfjcooldown10");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            queueFastForwardRequest(ctx, false, false, true, "TFJ: pedido recibido. Mira o acercate a un raptor adulto; su cooldown bajara a 10 segundos.");
        }
    }

    private static final class RaptorAllTenSecondsCommand extends CommandBase {
        private RaptorAllTenSecondsCommand() {
            super("tfjraptor10", "Set nearby TFJ raptor breeding lifecycle timers to the final 10 seconds.");
            addAliases("tfjall10", "tfjbreedall10");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            queueFastForwardRequest(ctx, true, true, true, "TFJ: pedido recibido. Huevos, crecimiento y cooldowns cercanos bajaran a 10 segundos.");
        }
    }

    private static final class RaptorGuideCommand extends CommandBase {
        private RaptorGuideCommand() {
            super("tfjraptorguide", "Open the TFJ raptor breeding and mutation guide.");
            addAliases("tfjguide", "tfjbreeding", "tfjmutations");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            queueGuideOpenRequest(ctx);
        }
    }

    private static void queueFastForwardRequest(CommandContext ctx, boolean eggs, boolean growth, boolean breeding, String queuedMessage) {
        if (ctx == null || !ctx.isPlayer() || ctx.sender() == null || ctx.sender().getUuid() == null) {
            if (ctx != null) {
                ctx.sendMessage(Message.raw("TFJ: este comando solo puede usarlo un jugador."));
            }
            return;
        }

        UUID playerUuid = ctx.sender().getUuid();
        long nowMs = System.currentTimeMillis();
        if (eggs) {
            PENDING_EGG_FAST_FORWARD_BY_PLAYER.put(playerUuid, nowMs);
        }
        if (growth) {
            PENDING_GROWTH_FAST_FORWARD_BY_PLAYER.put(playerUuid, nowMs);
        }
        if (breeding) {
            PENDING_BREEDING_FAST_FORWARD_BY_PLAYER.put(playerUuid, nowMs);
        }
        ctx.sendMessage(Message.raw(queuedMessage));
    }

    private static void queueGuideOpenRequest(CommandContext ctx) {
        if (ctx == null || !ctx.isPlayer() || ctx.sender() == null || ctx.sender().getUuid() == null) {
            if (ctx != null) {
                ctx.sendMessage(Message.raw("TFJ: este comando solo puede usarlo un jugador."));
            }
            return;
        }

        PENDING_GUIDE_OPEN_BY_PLAYER.put(ctx.sender().getUuid(), System.currentTimeMillis());
        ctx.sendMessage(Message.raw("TFJ: abriendo guia de crianza de raptores."));
    }

    private static final class ForgottenJunglePortalSpawnSystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            World world = getWorld(store);
            if (world == null || commandBuffer == null) {
                return;
            }

            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || playerRef.getUuid() == null || playerRef.getTransform() == null) {
                return;
            }

            Vector3d position = playerRef.getTransform().getPosition();
            if (position == null) {
                return;
            }

            if (isForgottenJungleWorld(world)) {
                ensureManagedPortalNearPlayer(world, playerRef.getUuid(), position, true, commandBuffer);
            } else {
                ensureManagedPortalNearPlayer(world, playerRef.getUuid(), position, false, commandBuffer);
            }
        }
    }

    private static final class ForgottenJunglePortalEntrySystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            World world = getWorld(store);
            if (world == null || commandBuffer == null) {
                return;
            }

            if (isForgottenJungleWorld(world)) {
                return;
            }

            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || playerRef.getUuid() == null || playerRef.getReference() == null || playerRef.getTransform() == null) {
                return;
            }

            long nowMs = System.currentTimeMillis();
            UUID playerUuid = playerRef.getUuid();
            if (nowMs < TFJ_PORTAL_ENTRY_COOLDOWN_MS.getOrDefault(playerUuid, 0L)) {
                return;
            }

            Vector3d position = playerRef.getTransform().getPosition();
            if (position == null) {
                return;
            }

            int[] portalPos = findNearestBlock(world, position, getTFJPortalBlockId(), "TFJ_Jungle_Portal", 2, 3);
            if (portalPos == null) {
                return;
            }

            TFJ_PORTAL_ENTRY_COOLDOWN_MS.put(playerUuid, nowMs + TFJ_PORTAL_COOLDOWN_MS);
            Transform returnTransform = playerRef.getTransform().clone();
            Rotation3f rotation = returnTransform.getRotation();
            if (rotation == null) {
                rotation = new Rotation3f();
            }
            Transform entryTransform = new Transform(new Vector3d(0.5, 96.0, 0.5), rotation);

            try {
                CompletableFuture<World> jungleWorld = getOrCreateForgottenJungleWorld(world, returnTransform);
                jungleWorld.whenComplete((createdWorld, throwable) -> {
                    if (throwable != null || createdWorld == null) {
                        TFJ_PORTAL_ENTRY_COOLDOWN_MS.put(playerUuid, System.currentTimeMillis() + 15000L);
                    }
                });
                System.out.println("[TFJ][Portal] Enter player=" + playerUuid + " portal=" + portalPos[0] + "," + portalPos[1] + "," + portalPos[2] + " instance=" + TFJ_INSTANCE_WORLD_KEY);
                InstancesPlugin.teleportPlayerToLoadingInstance(playerRef.getReference(), commandBuffer, jungleWorld, entryTransform);
            } catch (Throwable throwable) {
                TFJ_PORTAL_ENTRY_COOLDOWN_MS.remove(playerUuid);
                System.out.println("[TFJ][Portal] Entry failed player=" + playerUuid + ": " + describeThrowable(throwable));
            }
        }
    }

    private static final class ForgottenJunglePortalReturnSystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            World world = getWorld(store);
            if (world == null || commandBuffer == null) {
                return;
            }

            if (!isForgottenJungleWorld(world)) {
                return;
            }

            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || playerRef.getUuid() == null || playerRef.getReference() == null || playerRef.getTransform() == null) {
                return;
            }

            long nowMs = System.currentTimeMillis();
            UUID playerUuid = playerRef.getUuid();
            if (nowMs < TFJ_PORTAL_RETURN_COOLDOWN_MS.getOrDefault(playerUuid, 0L)) {
                return;
            }

            Vector3d position = playerRef.getTransform().getPosition();
            if (position == null) {
                return;
            }

            int[] portalPos = findNearestBlock(world, position, getTFJReturnPortalBlockId(), "TFJ_Return_Portal", 2, 3);
            if (portalPos == null) {
                return;
            }

            TFJ_PORTAL_RETURN_COOLDOWN_MS.put(playerUuid, nowMs + 9000L);
            TFJ_PORTAL_ENTRY_COOLDOWN_MS.put(playerUuid, nowMs + 8000L);
            try {
                System.out.println("[TFJ][Portal] Return player=" + playerUuid + " portal=" + portalPos[0] + "," + portalPos[1] + "," + portalPos[2]);
                InstancesPlugin.exitInstance(playerRef.getReference(), commandBuffer);
            } catch (Throwable throwable) {
                TFJ_PORTAL_RETURN_COOLDOWN_MS.remove(playerUuid);
                System.out.println("[TFJ][Portal] Return failed player=" + playerUuid + ": " + describeThrowable(throwable));
            }
        }
    }

    private static void ensureManagedPortalNearPlayer(
        World world,
        UUID playerUuid,
        Vector3d position,
        boolean returnPortal,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        if (world == null || position == null || commandBuffer == null) {
            return;
        }

        String portalName = returnPortal ? "TFJ_Return_Portal" : "TFJ_Jungle_Portal";
        int portalBlockId = returnPortal ? getTFJReturnPortalBlockId() : getTFJPortalBlockId();
        Set<String> builtSet = returnPortal ? TFJ_RETURN_PORTAL_SPAWN_BUILT : TFJ_PORTAL_SPAWN_BUILT;
        String buildKey = worldKey(world) + ":" + (returnPortal ? "return_portal" : "entry_portal");
        if (builtSet.contains(buildKey)) {
            return;
        }

        int[] existing = findNearestBlock(world, position, portalBlockId, portalName, 80, 48);
        if (existing != null) {
            builtSet.add(buildKey);
            return;
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs < TFJ_PORTAL_SPAWN_ATTEMPT_MS.getOrDefault(buildKey, 0L)) {
            return;
        }
        TFJ_PORTAL_SPAWN_ATTEMPT_MS.put(buildKey, nowMs + 5000L);

        int centerX = (int) Math.floor(position.x());
        int centerY = Math.max(2, (int) Math.floor(position.y()) - 1);
        int centerZ = (int) Math.floor(position.z()) + (returnPortal ? 4 : -5);
        commandBuffer.run(commandStore -> {
            int changed = buildManagedPortal(world, centerX, centerY, centerZ, portalName);
            if (changed > 0) {
                builtSet.add(buildKey);
                System.out.println("[TFJ][Portal] Built " + portalName + " for player=" + playerUuid + " world=" + worldKey(world) + " at " + centerX + "," + centerY + "," + centerZ + " changed=" + changed);
            } else {
                System.out.println("[TFJ][Portal] Could not build " + portalName + " yet for player=" + playerUuid + " world=" + worldKey(world) + " at " + centerX + "," + centerY + "," + centerZ);
            }
        });
    }

    private static int buildManagedPortal(World world, int centerX, int y, int centerZ, String portalBlockName) {
        if (world == null || portalBlockName == null || portalBlockName.isBlank()) {
            return 0;
        }

        int changed = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (setAny(world, centerX + dx, y, centerZ + dz, "Rock_Stone", "Stone", "Hytale:Rock_Stone", "Hytale:Stone", "Soil_Dirt", "Hytale:Soil_Dirt")) {
                    changed++;
                }
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            if (setAny(world, centerX, y + dy, centerZ, "Empty", "Air", "Hytale:Empty", "Hytale:Air")) {
                changed++;
            }
        }
        boolean portalPlaced = setAny(world, centerX, y + 1, centerZ, portalBlockName, "IcedFoxStudios:" + portalBlockName, "IcedFoxStudios.TheForgottenJungle:" + portalBlockName);
        if (portalPlaced) {
            changed++;
        }
        return portalPlaced ? changed : 0;
    }

    private static CompletableFuture<World> getOrCreateForgottenJungleWorld(World sourceWorld, Transform returnTransform) {
        Universe universe = Universe.get();
        World loadedWorld = universe == null ? null : universe.getWorld(TFJ_INSTANCE_WORLD_KEY);
        if (loadedWorld != null && loadedWorld.isAlive()) {
            return CompletableFuture.completedFuture(loadedWorld);
        }
        if (universe != null && hasInstanceWorldFolder(sourceWorld, TFJ_INSTANCE_WORLD_KEY)) {
            return universe.loadWorld(TFJ_INSTANCE_WORLD_KEY);
        }
        return InstancesPlugin.get().spawnInstance(
            TFJ_INSTANCE_TEMPLATE,
            TFJ_INSTANCE_WORLD_KEY,
            sourceWorld,
            returnTransform
        );
    }

    private static boolean hasInstanceWorldFolder(World sourceWorld, String instanceWorldKey) {
        if (sourceWorld == null || sourceWorld.getSavePath() == null || instanceWorldKey == null || instanceWorldKey.isBlank()) {
            return false;
        }
        try {
            Path worldsDirectory = sourceWorld.getSavePath().getParent();
            return worldsDirectory != null && Files.exists(worldsDirectory.resolve(instanceWorldKey));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isForgottenJungleWorld(World world) {
        if (world == null) {
            return false;
        }
        String worldName = "";
        String displayName = "";
        try {
            worldName = world.getName() == null ? "" : world.getName().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
        }
        try {
            displayName = world.getWorldConfig() == null || world.getWorldConfig().getDisplayName() == null
                ? ""
                : world.getWorldConfig().getDisplayName().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
        }
        return worldName.contains("the_forgotten_jungle")
            || worldName.contains("forgotten jungle")
            || worldName.contains("forgottenjungle")
            || displayName.contains("the_forgotten_jungle")
            || displayName.contains("forgotten jungle")
            || displayName.contains("forgottenjungle");
    }

    private static int getTFJPortalBlockId() {
        int cached = tfjPortalBlockId;
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }
        int blockId = resolveCustomBlockId("TFJ_Jungle_Portal");
        if (blockId != BlockType.UNKNOWN_ID && blockId != BlockType.EMPTY_ID) {
            tfjPortalBlockId = blockId;
        } else {
            debugGlobal(System.currentTimeMillis(), "TFJ_Jungle_Portal block id is not ready; name fallback remains active.");
        }
        return blockId;
    }

    private static int getTFJReturnPortalBlockId() {
        int cached = tfjReturnPortalBlockId;
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }
        int blockId = resolveCustomBlockId("TFJ_Return_Portal");
        if (blockId != BlockType.UNKNOWN_ID && blockId != BlockType.EMPTY_ID) {
            tfjReturnPortalBlockId = blockId;
        } else {
            debugGlobal(System.currentTimeMillis(), "TFJ_Return_Portal block id is not ready; name fallback remains active.");
        }
        return blockId;
    }

    private static int resolveCustomBlockId(String blockName) {
        int blockId = BlockType.getBlockIdOrUnknown(blockName, "IcedFoxStudios:" + blockName);
        if (blockId == BlockType.UNKNOWN_ID) {
            blockId = BlockType.getBlockIdOrUnknown("IcedFoxStudios.TheForgottenJungle:" + blockName, blockName);
        }
        if (blockId != BlockType.UNKNOWN_ID && blockId != BlockType.EMPTY_ID) {
            return blockId;
        }

        BlockType blockType = resolveCustomBlockType(blockName);
        if (blockType == null || blockType.isUnknown()) {
            return BlockType.UNKNOWN_ID;
        }
        try {
            return BlockType.getAssetMap().getIndex(blockType.getId());
        } catch (Throwable ignored) {
            return BlockType.UNKNOWN_ID;
        }
    }

    private static int[] findNearestBlock(
        World world,
        Vector3d position,
        int targetBlockId,
        String targetBlockName,
        int radiusXZ,
        int radiusY
    ) {
        if (world == null || position == null) {
            return null;
        }

        int centerX = (int) Math.floor(position.x());
        int centerY = (int) Math.floor(position.y());
        int centerZ = (int) Math.floor(position.z());
        int[] best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int x = centerX - radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int y = centerY - radiusY; y <= centerY + radiusY; y++) {
                for (int z = centerZ - radiusXZ; z <= centerZ + radiusXZ; z++) {
                    if (!blockMatches(world, x, y, z, targetBlockId, targetBlockName)) {
                        continue;
                    }
                    double dx = position.x() - (x + 0.5);
                    double dy = position.y() - (y + 0.5);
                    double dz = position.z() - (z + 0.5);
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq < bestDistanceSq) {
                        bestDistanceSq = distanceSq;
                        best = new int[] { x, y, z };
                    }
                }
            }
        }
        return best;
    }

    private static boolean blockMatches(World world, int x, int y, int z, int targetBlockId, String targetBlockName) {
        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return false;
        }
        if (targetBlockId != BlockType.UNKNOWN_ID && targetBlockId != BlockType.EMPTY_ID && getBlockSafely(chunk, x, y, z) == targetBlockId) {
            return true;
        }
        if (targetBlockName == null || targetBlockName.isBlank()) {
            return false;
        }
        try {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType != null && normalizeBlockKey(blockType.getId()).equals(normalizeBlockKey(targetBlockName))) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean setAny(World world, int x, int y, int z, String... blockIds) {
        if (blockIds == null) {
            return false;
        }
        for (String blockId : blockIds) {
            if (trySetBlockId(world, x, y, z, blockId)) {
                return true;
            }
            BlockType blockType = resolveCustomBlockType(blockId);
            if (blockType != null && trySetBlock(world, x, y, z, blockType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean trySetBlockId(World world, int x, int y, int z, String blockId) {
        if (world == null || blockId == null || blockId.isBlank()) {
            return false;
        }
        try {
            world.setBlock(x, y, z, blockId);
            return true;
        } catch (Throwable ignored) {
        }

        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return false;
        }
        int localX = ChunkUtil.localCoordinate(x);
        int localY = ChunkUtil.localCoordinate(y);
        int localZ = ChunkUtil.localCoordinate(z);
        try {
            if (chunk.setBlock(localX, y, localZ, blockId)) {
                chunk.markNeedsSaving();
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (chunk.setBlock(localX, localY, localZ, blockId)) {
                chunk.markNeedsSaving();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static final class RaptorCarePage extends InteractiveCustomUIPage<RaptorCarePage.RaptorCareEventData> {
        private final PlayerRef viewerRef;
        private final UUID viewerUuid;
        private final String raptorKey;
        private String status = "Click a meat slot to store one meat from your inventory.";
        private long nextAutoRefreshMs;

        private RaptorCarePage(PlayerRef playerRef, String raptorKey) {
            super(playerRef, CustomPageLifetime.CanDismiss, RaptorCareEventData.CODEC);
            this.viewerRef = playerRef;
            this.viewerUuid = playerRef == null ? null : playerRef.getUuid();
            this.raptorKey = raptorKey == null ? "" : raptorKey;
            if (this.viewerUuid != null) {
                RAPTOR_CARE_PAGE_BY_PLAYER.put(this.viewerUuid, this);
            }
        }

        @Override
        public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
            commandBuilder.append(RAPTOR_CARE_PAGE_PATH);
            bind(eventBuilder, "#TFJRaptorCareCloseButton", "close");
            bind(eventBuilder, "#TFJRaptorCareBreedingToggleButton", "toggle_breeding");
            for (int slot = 0; slot < RAPTOR_CARE_SLOT_COUNT; slot++) {
                bind(eventBuilder, "#TFJRaptorCareSlot" + slot + "Button", "slot:" + slot);
            }

            RaptorRuntimeState state = KNOWN_RAPTORS.get(raptorKey);
            if (state == null || state.dead) {
                commandBuilder.set("#TFJRaptorCareTitle.Text", "Raptor unavailable");
                commandBuilder.set("#TFJRaptorCareStatus.Text", "Move close to the raptor and interact again.");
                commandBuilder.set("#TFJRaptorCareHealthText.Text", "0 / 0 HP");
                commandBuilder.set("#TFJRaptorCareHealthBar.Value", 0.0);
                commandBuilder.set("#TFJRaptorCareFoodBar.Value", 0.0);
                commandBuilder.set("#TFJRaptorCareGrowthBar.Value", 0.0);
                commandBuilder.set("#TFJRaptorCareTime.Text", "--:--");
                commandBuilder.set("#TFJRaptorCareMutation.Text", "Mutation: --");
                commandBuilder.set("#TFJRaptorCareBreedingToggleButton.Text", "Breeding: --");
                for (EggInfo info : EGG_INFOS) {
                    commandBuilder.set("#TFJRaptorCareIcon" + info.suffix + ".Visible", false);
                }
                for (int slot = 0; slot < RAPTOR_CARE_SLOT_COUNT; slot++) {
                    commandBuilder.set("#TFJRaptorCareSlot" + slot + "Icon.Visible", false);
                    commandBuilder.set("#TFJRaptorCareSlot" + slot + "Count.Text", "+");
                    commandBuilder.set("#TFJRaptorCareSlot" + slot + "Name.Text", "Empty");
                }
                return;
            }

            long nowMs = System.currentTimeMillis();
            long remainingMs = Math.max(0L, state.completeAtMs - nowMs);
            commandBuilder.set("#TFJRaptorCareTitle.Text", raptorDisplayName(state));
            commandBuilder.set("#TFJRaptorCareStage.Text", state.roleInfo.stageLabel);
            commandBuilder.set("#TFJRaptorCareStatus.Text", status);
            commandBuilder.set("#TFJRaptorCareState.Text", raptorStatus(state, remainingMs));
            commandBuilder.set("#TFJRaptorCareMutation.Text", mutationLabel(state.roleInfo.info));
            commandBuilder.set("#TFJRaptorCareBreedingToggleButton.Text", raptorBreedingToggleText(state));
            commandBuilder.set("#TFJRaptorCareTime.Text", state.roleInfo.isBreedingStage() ? "Adult" : formatRemaining(remainingMs));
            commandBuilder.set("#TFJRaptorCareSexFemale.Visible", state.sex == RaptorSex.FEMALE);
            commandBuilder.set("#TFJRaptorCareSexMale.Visible", state.sex == RaptorSex.MALE);
            commandBuilder.set("#TFJRaptorCareHealthText.Text", formatRaptorHealth(state));
            commandBuilder.set("#TFJRaptorCareHealthBar.Value", raptorHealthProgress(state));
            commandBuilder.set("#TFJRaptorCareFoodBar.Value", raptorFoodProgress(state));
            commandBuilder.set("#TFJRaptorCareGrowthBar.Value", raptorGrowthProgress(state, nowMs));
            commandBuilder.set("#TFJRaptorCareFoodText.Text", state.roleInfo.isBreedingStage() ? "No food required" : formatRaptorFood(state));
            commandBuilder.set("#TFJRaptorCareNeedText.Text", raptorFoodStatus(state));
            for (EggInfo info : EGG_INFOS) {
                commandBuilder.set("#TFJRaptorCareIcon" + info.suffix + ".Visible", info.suffix.equals(state.roleInfo.info.suffix));
            }
            for (int slot = 0; slot < RAPTOR_CARE_SLOT_COUNT; slot++) {
                String itemId = state.meatSlotItems[slot];
                int count = state.meatSlotCounts[slot];
                boolean hasItem = itemId != null && !itemId.isBlank() && count > 0;
                commandBuilder.set("#TFJRaptorCareSlot" + slot + "Icon.Visible", hasItem);
                if (hasItem) {
                    commandBuilder.set("#TFJRaptorCareSlot" + slot + "Icon.ItemId", itemId);
                }
                commandBuilder.set("#TFJRaptorCareSlot" + slot + "Count.Text", hasItem ? "x" + count : "+");
                commandBuilder.set("#TFJRaptorCareSlot" + slot + "Name.Text", hasItem ? displayItemName(itemId) : "Empty");
            }
        }

        @Override
        public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, RaptorCareEventData data) {
            if (data == null || data.Action == null || data.Action.isBlank()) {
                return;
            }
            String action = data.Action.trim().toLowerCase(Locale.ROOT);
            if ("close".equals(action)) {
                if (viewerUuid != null) {
                    RAPTOR_CARE_PAGE_BY_PLAYER.remove(viewerUuid, this);
                }
                close();
                return;
            }
            if ("toggle_breeding".equals(action)) {
                RaptorRuntimeState state = KNOWN_RAPTORS.get(raptorKey);
                if (state == null || state.dead) {
                    status = "This raptor is no longer available.";
                } else if (state.roleInfo == null || !state.roleInfo.isBreedingStage()) {
                    status = "Only adult raptors can breed.";
                } else {
                    state.breedingEnabled = !state.breedingEnabled;
                    status = state.breedingEnabled
                        ? "Breeding enabled. Pair with another enabled adult nearby."
                        : "Breeding disabled.";
                    if (state.breedingEnabled && state.completeAtMs > System.currentTimeMillis() + 10_000L) {
                        state.completeAtMs = System.currentTimeMillis() + 10_000L;
                        state.nextTransitionAttemptMs = state.completeAtMs;
                    }
                }
                rebuild();
                return;
            }
            if (action.startsWith("slot:")) {
                int slot = 0;
                try {
                    slot = Integer.parseInt(action.substring("slot:".length()));
                } catch (Exception ignored) {
                }
                Player player = resolveViewerPlayer(playerEntityRef, store);
                status = depositOneMeatFromPlayer(player, KNOWN_RAPTORS.get(raptorKey), slot);
                rebuild();
            }
        }

        private Player resolveViewerPlayer(Ref<EntityStore> fallbackRef, Store<EntityStore> fallbackStore) {
            Ref<EntityStore> ref = viewerRef == null ? fallbackRef : viewerRef.getReference();
            Store<EntityStore> store = ref == null ? fallbackStore : ref.getStore();
            if (store == null || ref == null || !ref.isValid()) {
                return null;
            }
            return store.getComponent(ref, Player.getComponentType());
        }

        private void bind(UIEventBuilder eventBuilder, String selector, String action) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), true);
        }

        private void requestAutoRefresh(long nowMs) {
            if (nowMs < nextAutoRefreshMs) {
                return;
            }
            nextAutoRefreshMs = nowMs + RAPTOR_CARE_PAGE_REFRESH_MS;
            try {
                rebuild();
            } catch (Throwable throwable) {
                if (viewerUuid != null) {
                    RAPTOR_CARE_PAGE_BY_PLAYER.remove(viewerUuid, this);
                }
                debugGlobal(nowMs, "Could not auto-refresh TFJ raptor care page: " + describeThrowable(throwable));
            }
        }

        public static final class RaptorCareEventData {
            private static final BuilderCodec.Builder<RaptorCareEventData> BUILDER = BuilderCodec.builder(RaptorCareEventData.class, RaptorCareEventData::new);
            public static final BuilderCodec<RaptorCareEventData> CODEC =
                BUILDER
                    .addField(
                        new KeyedCodec<>("Action", new StringCodec()),
                        (instance, value) -> instance.Action = value,
                        instance -> instance.Action
                    )
                    .build();

            public String Action;
        }
    }

    private static final class RaptorAdultPage extends InteractiveCustomUIPage<RaptorAdultPage.RaptorAdultEventData> {
        private final PlayerRef viewerRef;
        private final UUID viewerUuid;
        private final String raptorKey;
        private String status = "Adult raptor ready.";

        private RaptorAdultPage(PlayerRef playerRef, String raptorKey) {
            super(playerRef, CustomPageLifetime.CanDismiss, RaptorAdultEventData.CODEC);
            this.viewerRef = playerRef;
            this.viewerUuid = playerRef == null ? null : playerRef.getUuid();
            this.raptorKey = raptorKey == null ? "" : raptorKey;
        }

        @Override
        public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
            commandBuilder.append(RAPTOR_ADULT_PAGE_PATH);
            bind(eventBuilder, "#TFJRaptorAdultCloseButton", "close");
            bind(eventBuilder, "#TFJRaptorAdultMountButton", "mount");
            bind(eventBuilder, "#TFJRaptorAdultFollowButton", "order_follow");
            bind(eventBuilder, "#TFJRaptorAdultGuardButton", "order_guard");
            bind(eventBuilder, "#TFJRaptorAdultIdleButton", "order_idle");
            bind(eventBuilder, "#TFJRaptorAdultBreedingToggleButton", "toggle_breeding");
            bind(eventBuilder, "#TFJRaptorAdultTrainButton", "train_xp");
            bind(eventBuilder, "#TFJRaptorAdultDamageButton", "upgrade_damage");
            bind(eventBuilder, "#TFJRaptorAdultSpeedButton", "upgrade_speed");
            bind(eventBuilder, "#TFJRaptorAdultStaminaButton", "upgrade_stamina");

            RaptorRuntimeState state = KNOWN_RAPTORS.get(raptorKey);
            if (state == null || state.dead || state.roleInfo == null || !state.roleInfo.isBreedingStage()) {
                commandBuilder.set("#TFJRaptorAdultTitle.Text", "Adult raptor unavailable");
                commandBuilder.set("#TFJRaptorAdultStatus.Text", "Move close to the adult raptor and interact again.");
                commandBuilder.set("#TFJRaptorAdultHealthText.Text", "0 / 0 HP");
                commandBuilder.set("#TFJRaptorAdultHealthBar.Value", 0.0);
                commandBuilder.set("#TFJRaptorAdultXpText.Text", "Level --");
                commandBuilder.set("#TFJRaptorAdultXpBar.Value", 0.0);
                commandBuilder.set("#TFJRaptorAdultPoints.Text", "Points: 0");
                commandBuilder.set("#TFJRaptorAdultStaminaText.Text", "Stamina --");
                commandBuilder.set("#TFJRaptorAdultStaminaBar.Value", 0.0);
                commandBuilder.set("#TFJRaptorAdultTrainButton.Text", "Train");
                commandBuilder.set("#TFJRaptorAdultCommand.Text", "Command: --");
                commandBuilder.set("#TFJRaptorAdultBreedingToggleButton.Text", "Breeding: --");
                setAdultUpgradeTexts(commandBuilder, null);
                for (EggInfo info : EGG_INFOS) {
                    commandBuilder.set("#TFJRaptorAdultIcon" + info.suffix + ".Visible", false);
                }
                return;
            }

            syncRaptorHealthFromStats(store, state.ref, state);
            commandBuilder.set("#TFJRaptorAdultTitle.Text", raptorDisplayName(state));
            commandBuilder.set("#TFJRaptorAdultStatus.Text", status);
            commandBuilder.set("#TFJRaptorAdultSexFemale.Visible", state.sex == RaptorSex.FEMALE);
            commandBuilder.set("#TFJRaptorAdultSexMale.Visible", state.sex == RaptorSex.MALE);
            commandBuilder.set("#TFJRaptorAdultMutation.Text", mutationLabel(state.roleInfo.info));
            commandBuilder.set("#TFJRaptorAdultHealthText.Text", formatRaptorHealth(state));
            commandBuilder.set("#TFJRaptorAdultHealthBar.Value", raptorHealthProgress(state));
            commandBuilder.set("#TFJRaptorAdultXpText.Text", formatRaptorLevel(state));
            commandBuilder.set("#TFJRaptorAdultXpBar.Value", raptorXpProgress(state));
            commandBuilder.set("#TFJRaptorAdultPoints.Text", "Points: " + state.unspentPoints);
            commandBuilder.set("#TFJRaptorAdultTrainButton.Text", raptorTrainButtonText(state, System.currentTimeMillis()));
            commandBuilder.set("#TFJRaptorAdultCommand.Text", "Command: " + state.commandMode.label);
            commandBuilder.set("#TFJRaptorAdultBreedingToggleButton.Text", raptorBreedingToggleText(state));
            commandBuilder.set("#TFJRaptorAdultStaminaText.Text", formatRaptorStamina(state));
            commandBuilder.set("#TFJRaptorAdultStaminaBar.Value", raptorStaminaProgress(state));
            setAdultUpgradeTexts(commandBuilder, state);
            for (EggInfo info : EGG_INFOS) {
                commandBuilder.set("#TFJRaptorAdultIcon" + info.suffix + ".Visible", info.suffix.equals(state.roleInfo.info.suffix));
            }
        }

        @Override
        public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, RaptorAdultEventData data) {
            if (data == null || data.Action == null || data.Action.isBlank()) {
                return;
            }
            String action = data.Action.trim().toLowerCase(Locale.ROOT);
            if ("close".equals(action)) {
                close();
                return;
            }

            RaptorRuntimeState state = KNOWN_RAPTORS.get(raptorKey);
            if (state == null || state.dead || state.roleInfo == null || !state.roleInfo.isBreedingStage()) {
                status = "This adult raptor is no longer available.";
                rebuild();
                return;
            }

            Player player = resolveViewerPlayer(playerEntityRef, store);
            if ("mount".equals(action)) {
                status = tryMountRaptor(playerEntityRef, store, player, viewerRef, state);
                rebuild();
                return;
            }
            if ("order_follow".equals(action)) {
                setRaptorOwner(state, playerEntityRef);
                state.commandMode = RaptorCommandMode.FOLLOW;
                boolean applied = applyRaptorCommandState(store, state);
                status = applied ? "Following you." : "Follow failed. Check TFJ logs.";
                rebuild();
                return;
            }
            if ("order_guard".equals(action)) {
                setRaptorOwner(state, playerEntityRef);
                rememberGuardPosition(store, state);
                state.commandMode = RaptorCommandMode.GUARD;
                applyRaptorCommandState(store, state);
                status = "Guarding this place.";
                rebuild();
                return;
            }
            if ("order_idle".equals(action)) {
                state.commandMode = RaptorCommandMode.IDLE;
                state.ownerRef = null;
                state.ownerUuid = null;
                state.hasGuardPosition = false;
                applyRaptorCommandState(store, state);
                status = "Command cleared.";
                rebuild();
                return;
            }
            if ("toggle_breeding".equals(action)) {
                state.breedingEnabled = !state.breedingEnabled;
                status = state.breedingEnabled
                    ? "Breeding enabled. Pair with another enabled adult nearby."
                    : "Breeding disabled.";
                if (state.breedingEnabled && state.completeAtMs > System.currentTimeMillis() + 10_000L) {
                    state.completeAtMs = System.currentTimeMillis() + 10_000L;
                    state.nextTransitionAttemptMs = state.completeAtMs;
                }
                rebuild();
                return;
            }
            if ("train_xp".equals(action)) {
                status = trainRaptor(state, System.currentTimeMillis());
                rebuild();
                return;
            }
            if ("upgrade_damage".equals(action)) {
                status = upgradeRaptorStat(state, "damage");
                rebuild();
                return;
            }
            if ("upgrade_speed".equals(action)) {
                status = upgradeRaptorStat(state, "speed");
                rebuild();
                return;
            }
            if ("upgrade_stamina".equals(action)) {
                status = upgradeRaptorStat(state, "stamina");
                rebuild();
            }
        }

        private Player resolveViewerPlayer(Ref<EntityStore> fallbackRef, Store<EntityStore> fallbackStore) {
            Ref<EntityStore> ref = viewerRef == null ? fallbackRef : viewerRef.getReference();
            Store<EntityStore> store = ref == null ? fallbackStore : ref.getStore();
            if (store == null || ref == null || !ref.isValid()) {
                return null;
            }
            return store.getComponent(ref, Player.getComponentType());
        }

        private void bind(UIEventBuilder eventBuilder, String selector, String action) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), true);
        }

        public static final class RaptorAdultEventData {
            private static final BuilderCodec.Builder<RaptorAdultEventData> BUILDER = BuilderCodec.builder(RaptorAdultEventData.class, RaptorAdultEventData::new);
            public static final BuilderCodec<RaptorAdultEventData> CODEC =
                BUILDER
                    .addField(
                        new KeyedCodec<>("Action", new StringCodec()),
                        (instance, value) -> instance.Action = value,
                        instance -> instance.Action
                    )
                    .build();

            public String Action;
        }
    }

    private static final class RaptorGuidePage extends InteractiveCustomUIPage<RaptorGuidePage.RaptorGuideEventData> {
        private final PlayerRef viewerRef;
        private int pageIndex;

        private RaptorGuidePage(PlayerRef playerRef) {
            super(playerRef, CustomPageLifetime.CanDismiss, RaptorGuideEventData.CODEC);
            this.viewerRef = playerRef;
        }

        @Override
        public void build(Ref<EntityStore> playerEntityRef, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
            commandBuilder.append(RAPTOR_GUIDE_PAGE_PATH);
            bind(eventBuilder, "#TFJRaptorGuideCloseButton", "close");
            bind(eventBuilder, "#TFJRaptorGuidePreviousButton", "previous");
            bind(eventBuilder, "#TFJRaptorGuideNextButton", "next");

            List<RaptorGuidePageData> pages = buildRaptorGuidePages();
            if (pages.isEmpty()) {
                pages.add(new RaptorGuidePageData("Raptor Guide", "No data available yet.", new ArrayList<>()));
            }
            pageIndex = Math.max(0, Math.min(pageIndex, pages.size() - 1));
            RaptorGuidePageData page = pages.get(pageIndex);
            commandBuilder.set("#TFJRaptorGuideTitle.Text", page.title);
            commandBuilder.set("#TFJRaptorGuideSubtitle.Text", page.subtitle);
            commandBuilder.set("#TFJRaptorGuidePageLabel.Text", (pageIndex + 1) + " / " + pages.size());
            commandBuilder.set("#TFJRaptorGuidePreviousButton.Text", pageIndex <= 0 ? "-" : "Prev");
            commandBuilder.set("#TFJRaptorGuideNextButton.Text", pageIndex >= pages.size() - 1 ? "-" : "Next");

            for (int row = 0; row < RAPTOR_GUIDE_ROWS; row++) {
                boolean visible = row < page.rows.size();
                commandBuilder.set("#TFJRaptorGuideRow" + row + ".Visible", visible);
                if (!visible) {
                    commandBuilder.set("#TFJRaptorGuideRow" + row + "Name.Text", "");
                    commandBuilder.set("#TFJRaptorGuideRow" + row + "Detail.Text", "");
                    commandBuilder.set("#TFJRaptorGuideRow" + row + "Chance.Text", "");
                    continue;
                }
                RaptorGuideRow guideRow = page.rows.get(row);
                commandBuilder.set("#TFJRaptorGuideRow" + row + "Name.Text", guideRow.name);
                commandBuilder.set("#TFJRaptorGuideRow" + row + "Detail.Text", guideRow.detail);
                commandBuilder.set("#TFJRaptorGuideRow" + row + "Chance.Text", guideRow.chance);
            }
        }

        @Override
        public void handleDataEvent(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, RaptorGuideEventData data) {
            if (data == null || data.Action == null || data.Action.isBlank()) {
                return;
            }
            String action = data.Action.trim().toLowerCase(Locale.ROOT);
            if ("close".equals(action)) {
                close();
                return;
            }
            List<RaptorGuidePageData> pages = buildRaptorGuidePages();
            int lastPage = Math.max(0, pages.size() - 1);
            if ("previous".equals(action)) {
                pageIndex = Math.max(0, pageIndex - 1);
            } else if ("next".equals(action)) {
                pageIndex = Math.min(lastPage, pageIndex + 1);
            }
            rebuild();
        }

        private void bind(UIEventBuilder eventBuilder, String selector, String action) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), true);
        }

        public static final class RaptorGuideEventData {
            private static final BuilderCodec.Builder<RaptorGuideEventData> BUILDER = BuilderCodec.builder(RaptorGuideEventData.class, RaptorGuideEventData::new);
            public static final BuilderCodec<RaptorGuideEventData> CODEC =
                BUILDER
                    .addField(
                        new KeyedCodec<>("Action", new StringCodec()),
                        (instance, value) -> instance.Action = value,
                        instance -> instance.Action
                    )
                    .build();

            public String Action;
        }
    }

    private static List<RaptorGuidePageData> buildRaptorGuidePages() {
        List<RaptorGuidePageData> pages = new ArrayList<>();
        addGuidePages(pages, "Raptor Species", "Stats shown as adult values. Natural species can spawn wild.", buildSpeciesGuideRows());
        addGuidePages(pages, "Same Species", "Same parents mostly repeat their own strain, with rare mutation rolls.", buildSameSpeciesGuideRows());
        addGuidePages(pages, "Natural Crosses", "Base species combinations. Remaining chance is split between both parents.", buildNaturalCrossGuideRows());
        addGuidePages(pages, "Reinforced Lines", "Natural species paired with an evolved strain pushes that family line.", buildLineReinforcementGuideRows());
        addGuidePages(pages, "Advanced Mutations", "Breeding-only strains can now produce rarer advanced lines.", buildAdvancedMutationGuideRows());
        addGuidePages(pages, "Fallback Rules", "These cover every pair not listed on the earlier pages.", buildFallbackGuideRows());
        return pages;
    }

    private static void addGuidePages(List<RaptorGuidePageData> pages, String title, String subtitle, List<RaptorGuideRow> rows) {
        if (pages == null || rows == null || rows.isEmpty()) {
            return;
        }
        for (int start = 0; start < rows.size(); start += RAPTOR_GUIDE_ROWS) {
            int end = Math.min(rows.size(), start + RAPTOR_GUIDE_ROWS);
            List<RaptorGuideRow> pageRows = new ArrayList<>();
            for (int index = start; index < end; index++) {
                pageRows.add(rows.get(index));
            }
            String pageTitle = title;
            if (rows.size() > RAPTOR_GUIDE_ROWS) {
                pageTitle = title + " " + ((start / RAPTOR_GUIDE_ROWS) + 1);
            }
            pages.add(new RaptorGuidePageData(pageTitle, subtitle, pageRows));
        }
    }

    private static List<RaptorGuideRow> buildSpeciesGuideRows() {
        List<RaptorGuideRow> rows = new ArrayList<>();
        for (EggInfo info : EGG_INFOS) {
            String source = info.naturalSpawn ? "Natural" : "Breeding-only";
            rows.add(new RaptorGuideRow(
                info.displayName,
                source + " | " + info.traitLabel,
                formatAdultStats(info)
            ));
        }
        return rows;
    }

    private static List<RaptorGuideRow> buildSameSpeciesGuideRows() {
        List<RaptorGuideRow> rows = new ArrayList<>();
        for (EggInfo info : EGG_INFOS) {
            List<MutationOption> options = sameColorMutationOptions(info.suffix);
            int inherited = Math.max(0, 100 - mutationWeight(options));
            rows.add(new RaptorGuideRow(
                info.suffix + " + " + info.suffix,
                describeMutationOptions(options),
                inherited + "% " + info.suffix
            ));
        }
        return rows;
    }

    private static List<RaptorGuideRow> buildNaturalCrossGuideRows() {
        return buildPairGuideRows(new String[][] {
            {"Red", "Blue"},
            {"Red", "Yellow"},
            {"Red", "White"},
            {"Red", "Green"},
            {"Red", "Cyan"},
            {"Blue", "Cyan"},
            {"Blue", "Green"},
            {"Blue", "White"},
            {"Yellow", "Green"},
            {"Yellow", "White"},
            {"Yellow", "Cyan"},
            {"Green", "Cyan"},
            {"Green", "White"},
            {"White", "Cyan"}
        });
    }

    private static List<RaptorGuideRow> buildLineReinforcementGuideRows() {
        return buildPairGuideRows(new String[][] {
            {"Red", "Crimson"},
            {"Red", "Rose"},
            {"Red", "Violet"},
            {"Blue", "Azure"},
            {"Blue", "Violet"},
            {"Cyan", "Azure"},
            {"Cyan", "Emerald"},
            {"Green", "Emerald"},
            {"Green", "Jade"},
            {"Yellow", "Amber"},
            {"Yellow", "Gold"},
            {"White", "Ivory"},
            {"White", "Gold"},
            {"White", "Rose"}
        });
    }

    private static List<RaptorGuideRow> buildAdvancedMutationGuideRows() {
        return buildPairGuideRows(new String[][] {
            {"Black", "Gold"},
            {"Black", "Violet"},
            {"Black", "Emerald"},
            {"Black", "Amber"},
            {"Black", "Rose"},
            {"Black", "Jade"},
            {"Black", "Ivory"},
            {"Black", "Azure"},
            {"Black", "Crimson"},
            {"Gold", "Violet"},
            {"Gold", "Emerald"},
            {"Gold", "Amber"},
            {"Gold", "Rose"},
            {"Gold", "Jade"},
            {"Gold", "Ivory"},
            {"Gold", "Azure"},
            {"Gold", "Crimson"},
            {"Violet", "Emerald"},
            {"Violet", "Amber"},
            {"Violet", "Rose"},
            {"Violet", "Jade"},
            {"Violet", "Ivory"},
            {"Violet", "Azure"},
            {"Violet", "Crimson"},
            {"Emerald", "Amber"},
            {"Emerald", "Rose"},
            {"Emerald", "Jade"},
            {"Emerald", "Ivory"},
            {"Emerald", "Azure"},
            {"Emerald", "Crimson"},
            {"Amber", "Rose"},
            {"Amber", "Jade"},
            {"Amber", "Ivory"},
            {"Amber", "Azure"},
            {"Amber", "Crimson"},
            {"Rose", "Jade"},
            {"Rose", "Ivory"},
            {"Rose", "Azure"},
            {"Rose", "Crimson"},
            {"Jade", "Ivory"},
            {"Jade", "Azure"},
            {"Jade", "Crimson"},
            {"Ivory", "Azure"},
            {"Ivory", "Crimson"},
            {"Azure", "Crimson"}
        });
    }

    private static List<RaptorGuideRow> buildFallbackGuideRows() {
        List<RaptorGuideRow> rows = new ArrayList<>();
        rows.add(new RaptorGuideRow("Unlisted + Black", "Black 8%, Crimson 3%", "89% parents"));
        rows.add(new RaptorGuideRow("Unlisted + Gold", "Gold 7%, Ivory 3%", "90% parents"));
        rows.add(new RaptorGuideRow("Any other pair", "No special roll", "50% / 50% parents"));
        rows.add(new RaptorGuideRow("Adult requirement", "Both adults must have Breeding: On", "Near female"));
        rows.add(new RaptorGuideRow("Cooldown testing", "/tfjbreed10 or /tfjraptor10", "10 sec"));
        return rows;
    }

    private static List<RaptorGuideRow> buildPairGuideRows(String[][] pairs) {
        List<RaptorGuideRow> rows = new ArrayList<>();
        if (pairs == null) {
            return rows;
        }
        for (String[] pair : pairs) {
            if (pair == null || pair.length < 2) {
                continue;
            }
            List<MutationOption> options = crossColorMutationOptions(pair[0], pair[1]);
            rows.add(new RaptorGuideRow(
                pair[0] + " + " + pair[1],
                describeMutationOptions(options),
                Math.max(0, 100 - mutationWeight(options)) + "% parents"
            ));
        }
        return rows;
    }

    private static String describeMutationOptions(List<MutationOption> options) {
        if (options == null || options.isEmpty()) {
            return "No special roll";
        }
        StringBuilder builder = new StringBuilder();
        for (MutationOption option : options) {
            if (option == null || option.weight <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(option.suffix).append(' ').append(option.weight).append('%');
        }
        return builder.length() == 0 ? "No special roll" : builder.toString();
    }

    private static int mutationWeight(List<MutationOption> options) {
        if (options == null || options.isEmpty()) {
            return 0;
        }
        int weight = 0;
        for (MutationOption option : options) {
            if (option != null) {
                weight += Math.max(0, option.weight);
            }
        }
        return weight;
    }

    private static String formatAdultStats(EggInfo info) {
        if (info == null) {
            return "--";
        }
        long health = Math.round(84.0 * info.healthMultiplier);
        double speed = Math.round(10.0 * info.speedMultiplier * 10.0) / 10.0;
        return health + " HP | " + formatCompactDecimal(speed) + " speed";
    }

    private static final class ActionOpenRaptorCareUI extends ActionBase {
        private ActionOpenRaptorCareUI(BuilderActionOpenRaptorCareUI builder, BuilderSupport builderSupport) {
            super(builder);
        }

        @Override
        public boolean canExecute(Ref<EntityStore> entityRef, Role role, InfoProvider infoProvider, double dt, Store<EntityStore> store) {
            if (getRaptorRoleInfo(getNpcSafely(store, entityRef)) == null) {
                return false;
            }
            Object stateSupport = getStateSupportSafely(role, System.currentTimeMillis(), false);
            return super.canExecute(entityRef, role, infoProvider, dt, store)
                && role != null
                && getInteractionIterationTargetSafely(stateSupport, System.currentTimeMillis(), false) != null;
        }

        @Override
        public boolean execute(Ref<EntityStore> entityRef, Role role, InfoProvider infoProvider, double dt, Store<EntityStore> store) {
            super.execute(entityRef, role, infoProvider, dt, store);
            Object stateSupport = getStateSupportSafely(role, System.currentTimeMillis(), false);
            if (role == null || stateSupport == null || store == null) {
                return false;
            }
            if (getRaptorRoleInfo(getNpcSafely(store, entityRef)) == null) {
                return false;
            }
            Ref<EntityStore> targetRef = getInteractionIterationTargetSafely(stateSupport, System.currentTimeMillis(), false);
            if (targetRef == null) {
                return false;
            }
            PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
            Player player = store.getComponent(targetRef, Player.getComponentType());
            if (playerRef == null || player == null) {
                return false;
            }
            String openKey = String.valueOf(playerRef.getUuid()) + "|" + String.valueOf(entityRef);
            long nowMs = System.currentTimeMillis();
            Long previous = RAPTOR_CARE_LAST_OPEN_MS.put(openKey, nowMs);
            if (previous != null && nowMs - previous < RAPTOR_CARE_OPEN_COOLDOWN_MS) {
                return true;
            }
            return openRaptorCarePage(targetRef, store, player, playerRef, entityRef);
        }
    }

    private static final class BuilderActionOpenRaptorCareUI extends BuilderActionBase {
        @Override
        public String getShortDescription() {
            return "Open the TFJ raptor care UI.";
        }

        @Override
        public String getLongDescription() {
            return getShortDescription();
        }

        @Override
        public Action build(BuilderSupport builderSupport) {
            return new ActionOpenRaptorCareUI(this, builderSupport);
        }

        @Override
        public BuilderDescriptorState getBuilderDescriptorState() {
            return BuilderDescriptorState.Stable;
        }

        @Override
        public BuilderActionOpenRaptorCareUI readConfig(JsonElement jsonElement) {
            requireInstructionType(EnumSet.of(InstructionType.Interaction));
            return this;
        }
    }

    private static final class MeatInventoryMatch {
        private final String itemId;
        private final int quantity;

        private MeatInventoryMatch(String itemId, int quantity) {
            this.itemId = itemId == null ? "" : itemId;
            this.quantity = Math.max(0, quantity);
        }
    }



    private static final class RaptorIncubationHud extends CustomUIHud {
        private boolean visible;
        private String species = "";
        private String status = "";
        private String remaining = "";
        private String iconSuffix = "";
        private String mutation = "";
        private boolean sexFemaleVisible;
        private boolean sexMaleVisible;
        private double progress;

        private RaptorIncubationHud(PlayerRef playerRef) {
            super(playerRef, HUD_ID);
        }

        private void setTimed(String species, String iconSuffix, String status, long remainingMs, long startedAtMs, long durationMs, long nowMs) {
            this.visible = true;
            this.species = species;
            this.iconSuffix = iconSuffix;
            this.mutation = mutationLabel(getEggInfoBySuffix(iconSuffix));
            this.status = status;
            this.sexFemaleVisible = false;
            this.sexMaleVisible = false;
            this.remaining = formatRemaining(remainingMs);
            this.progress = durationMs <= 0L ? 1.0 : clamp((double) (nowMs - startedAtMs) / (double) durationMs, 0.0, 1.0);
        }

        private void setEgg(EggRuntimeState egg, long nowMs) {
            long remainingMs = Math.max(0L, egg.hatchAtMs - nowMs);
            setTimed(
                egg.info.displayName,
                egg.info.suffix,
                remainingMs <= 0L ? "Hatching" : "Incubating",
                remainingMs,
                egg.startedAtMs,
                INCUBATION_MS,
                nowMs
            );
        }

        private void setRaptor(RaptorRuntimeState raptor, long nowMs) {
            long remainingMs = Math.max(0L, raptor.completeAtMs - nowMs);
            setTimed(
                raptorDisplayName(raptor),
                raptor.roleInfo.info.suffix,
                raptorStatus(raptor, remainingMs),
                remainingMs,
                raptor.startedAtMs,
                raptor.roleInfo.durationMs,
                nowMs
            );
            this.sexFemaleVisible = raptor.sex == RaptorSex.FEMALE;
            this.sexMaleVisible = raptor.sex == RaptorSex.MALE;
        }

        private void refresh(EggRuntimeState egg, long nowMs) {
            setEgg(egg, nowMs);
            UICommandBuilder commandBuilder = new UICommandBuilder();
            appendIncubationHud(commandBuilder);
            update(true, commandBuilder);
        }

        private void refresh(RaptorRuntimeState raptor, long nowMs) {
            setRaptor(raptor, nowMs);
            UICommandBuilder commandBuilder = new UICommandBuilder();
            appendIncubationHud(commandBuilder);
            update(true, commandBuilder);
        }

        private void appendIncubationHud(UICommandBuilder commandBuilder) {
            commandBuilder.append("TheForgottenJungle/TFJRaptorIncubationHud.ui");
            commandBuilder.set("#TFJRaptorIncubationHud.Visible", this.visible);
            commandBuilder.set("#TFJIncubatorSpecies.Text", this.species);
            commandBuilder.set("#TFJIncubatorMutation.Text", this.mutation);
            commandBuilder.set("#TFJIncubatorStatus.Text", this.status);
            commandBuilder.set("#TFJIncubatorSexFemale.Visible", this.visible && this.sexFemaleVisible);
            commandBuilder.set("#TFJIncubatorSexMale.Visible", this.visible && this.sexMaleVisible);
            commandBuilder.set("#TFJIncubatorTimeRemaining.Text", this.remaining);
            commandBuilder.set("#TFJIncubatorProgressBar.Value", this.progress);
            for (EggInfo info : EGG_INFOS) {
                commandBuilder.set("#TFJIncubatorIcon" + info.suffix + ".Visible", this.visible && info.suffix.equals(this.iconSuffix));
            }
        }

        @Override
        protected void build(UICommandBuilder commandBuilder) {
            appendIncubationHud(commandBuilder);
        }
    }

    private static final class RaptorIncubationSystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            if (store == null) {
                return;
            }
            long nowMs = System.currentTimeMillis();
            if (EGG_INFOS.isEmpty()) {
                debugGlobal(nowMs, "No raptor egg metadata was generated into the Java plugin.");
                return;
            }
            if (EGG_BY_BLOCK_ID.isEmpty()) {
                resolveEggBlockIds();
            }
            if (EGG_BY_BLOCK_ID.isEmpty()) {
                debugGlobal(nowMs, "TFJ raptor egg numeric block ids are not ready; using BlockType name fallback. eggInfos=" + EGG_INFOS.size() + ", blockKeys=" + EGG_BY_BLOCK_KEY.size());
            }

            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }
            UUID playerUuid = playerRef.getUuid();
            boolean debugNow = shouldDebugPlayer(playerUuid, nowMs);
            Ref<EntityStore> playerEntityRef = playerRef.getReference();
            if (playerEntityRef == null || !playerEntityRef.isValid()) {
                RAPTOR_CARE_PAGE_BY_PLAYER.remove(playerUuid);
                if (debugNow) {
                    debug("PlayerRef has no valid entity reference; player=" + playerLabel(playerRef));
                }
                return;
            }

            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) {
                player = store.getComponent(playerEntityRef, Player.getComponentType());
            }
            if (player == null || player.getHudManager() == null) {
                return;
            }

            World world = getWorld(store);
            if (world == null) {
                if (debugNow) {
                    debug("World externalData is not available; clearing HUD for player=" + playerLabel(playerRef));
                }
                clearOwnHud(playerRef, player);
                return;
            }

            TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
            if (transform != null && shouldScan(playerUuid, nowMs)) {
                scanNearbyEggs(world, transform, nowMs);
                scanNearbyRaptors(store, commandBuffer, world, transform, nowMs);
            }
            boolean eggFastForward = consumeFastForwardRequest(PENDING_EGG_FAST_FORWARD_BY_PLAYER, playerUuid);
            boolean growthFastForward = consumeFastForwardRequest(PENDING_GROWTH_FAST_FORWARD_BY_PLAYER, playerUuid);
            boolean breedingFastForward = consumeFastForwardRequest(PENDING_BREEDING_FAST_FORWARD_BY_PLAYER, playerUuid);
            boolean guideOpen = consumeFastForwardRequest(PENDING_GUIDE_OPEN_BY_PLAYER, playerUuid);
            if (guideOpen) {
                boolean opened = openRaptorGuidePage(playerEntityRef, store, player, playerRef);
                playerRef.sendMessage(Message.raw(opened ? "TFJ: guia de raptores abierta." : "TFJ: no se pudo abrir la guia ahora mismo."));
            }
            if (eggFastForward) {
                if (transform != null) {
                    scanNearbyEggs(world, transform, nowMs);
                }
                int affected = fastForwardEggsInWorld(world, nowMs, 10_000L);
                playerRef.sendMessage(Message.raw("TFJ: " + affected + " huevo(s) de raptor puestos a 10 segundos."));
                debug("Egg fast-forward command consumed for player=" + playerLabel(playerRef) + ", affected=" + affected + ".");
            }
            if (growthFastForward || breedingFastForward) {
                if (transform != null) {
                    scanNearbyRaptors(store, commandBuffer, world, transform, nowMs);
                }
                int affected = fastForwardRaptorsInWorld(worldKey(world), growthFastForward, breedingFastForward, nowMs, 10_000L);
                playerRef.sendMessage(Message.raw("TFJ: " + affected + " estado(s) de raptor puestos a 10 segundos."));
                debug("Raptor lifecycle fast-forward consumed for player=" + playerLabel(playerRef) + ", growth=" + growthFastForward + ", breeding=" + breedingFastForward + ", affected=" + affected + ".");
            }
            processKnownEggs(store, world, commandBuffer, nowMs);
            if (debugNow) {
                debug("Tick player=" + playerLabel(playerRef) + ", world=" + worldKey(world) + ", knownEggs=" + KNOWN_EGGS.size() + ", blockIds=" + EGG_BY_BLOCK_ID.size() + ", blockKeys=" + EGG_BY_BLOCK_KEY.size());
            }

            LookedEgg lookedEgg = findLookedEgg(world, index, archetypeChunk, commandBuffer, nowMs, debugNow);
            CustomUIHud currentHud = player.getHudManager().getCustomHud(HUD_ID);
            if (lookedEgg != null) {
                RAPTOR_CARE_PAGE_BY_PLAYER.remove(playerUuid);
                clearRaptorCarePrompt(playerUuid, playerEntityRef, store, null, nowMs);
                EggRuntimeState egg = getOrCreateEgg(world, lookedEgg.x, lookedEgg.y, lookedEgg.z, lookedEgg.info, nowMs);
                if (currentHud != null && !(currentHud instanceof RaptorIncubationHud)) {
                    if (debugNow) {
                        debug("Custom HUD id is already occupied by " + currentHud.getClass().getName() + "; cannot show TFJ egg HUD.");
                    }
                    return;
                }
                if (!(currentHud instanceof RaptorIncubationHud)) {
                    RaptorIncubationHud hud = new RaptorIncubationHud(playerRef);
                    hud.setEgg(egg, nowMs);
                    player.getHudManager().addCustomHud(playerRef, hud);
                    if (debugNow) {
                        debug("Added incubation HUD for " + egg.info.displayName + " at " + egg.x + "," + egg.y + "," + egg.z);
                    }
                    return;
                }
                if (shouldRefreshUi(playerUuid, nowMs)) {
                    ((RaptorIncubationHud) currentHud).refresh(egg, nowMs);
                }
                return;
            }

            LookedRaptor lookedRaptor = findLookedRaptor(playerEntityRef, store, commandBuffer, world, nowMs, debugNow);
            if (lookedRaptor != null) {
                refreshOpenRaptorCarePage(playerRef, player, lookedRaptor.state, nowMs);
                if (currentHud != null && !(currentHud instanceof RaptorIncubationHud)) {
                    if (debugNow) {
                        debug("Custom HUD id is already occupied by " + currentHud.getClass().getName() + "; cannot show TFJ raptor HUD.");
                    }
                    return;
                }
                if (!(currentHud instanceof RaptorIncubationHud)) {
                    RaptorIncubationHud hud = new RaptorIncubationHud(playerRef);
                    hud.setRaptor(lookedRaptor.state, nowMs);
                    player.getHudManager().addCustomHud(playerRef, hud);
                    if (debugNow) {
                        debug("Added lifecycle HUD for " + lookedRaptor.state.roleInfo.info.displayName + " role=" + lookedRaptor.state.roleInfo.role + ".");
                    }
                    return;
                }
                if (shouldRefreshUi(playerUuid, nowMs)) {
                    ((RaptorIncubationHud) currentHud).refresh(lookedRaptor.state, nowMs);
                }
                return;
            }

            RAPTOR_CARE_PAGE_BY_PLAYER.remove(playerUuid);
            clearRaptorCarePrompt(playerUuid, playerEntityRef, store, null, nowMs);
            if (currentHud instanceof RaptorIncubationHud) {
                if (debugNow) {
                    debug("Removing incubation HUD because the player is no longer looking at a TFJ egg or raptor.");
                }
                player.getHudManager().removeCustomHud(playerRef, HUD_ID);
            }
        }
    }

    private static final class RaptorLifecycleSystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            if (store == null || archetypeChunk == null) {
                return;
            }
            NPCEntity npc = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
            RaptorRoleInfo roleInfo = getRaptorRoleInfo(npc);
            if (roleInfo == null) {
                return;
            }
            long nowMs = System.currentTimeMillis();
            World world = getWorld(store);
            Ref<EntityStore> raptorRef = archetypeChunk.getReferenceTo(index);
            RaptorRuntimeState state = getOrCreateRaptorState(store, worldKey(world), raptorRef, npc, roleInfo, nowMs);
            tickRaptorCare(store, raptorRef, npc, state, nowMs);
            tickRaptorAdultRuntime(store, raptorRef, npc, state, nowMs);
            if (state.dead) {
                return;
            }
            if (!roleInfo.canAdvance()) {
                if (roleInfo.isBreedingStage() && nowMs >= state.completeAtMs && nowMs >= state.nextTransitionAttemptMs) {
                    state.transitionPending = true;
                    if (commandBuffer != null) {
                        commandBuffer.run(commandStore -> {
                            long layNowMs = System.currentTimeMillis();
                            boolean laid = tryLayRaptorEgg(commandStore == null ? store : commandStore, world, raptorRef, state, layNowMs);
                            state.transitionPending = false;
                            state.nextTransitionAttemptMs = laid ? state.completeAtMs : layNowMs + RAPTOR_TRANSITION_RETRY_INTERVAL_MS;
                        });
                    } else {
                        boolean laid = tryLayRaptorEgg(store, world, raptorRef, state, nowMs);
                        state.transitionPending = false;
                        state.nextTransitionAttemptMs = laid ? state.completeAtMs : nowMs + RAPTOR_TRANSITION_RETRY_INTERVAL_MS;
                    }
                }
                return;
            }
            if (nowMs < state.completeAtMs || nowMs < state.nextTransitionAttemptMs) {
                return;
            }
            state.transitionPending = true;
            if (commandBuffer != null) {
                commandBuffer.run(commandStore -> {
                    long hatchNowMs = System.currentTimeMillis();
                    boolean advanced = advanceRaptorRole(commandStore == null ? store : commandStore, raptorRef, roleInfo, state, hatchNowMs);
                    state.transitionPending = advanced;
                    state.nextTransitionAttemptMs = hatchNowMs + (advanced ? 5_000L : RAPTOR_TRANSITION_RETRY_INTERVAL_MS);
                });
            } else {
                boolean advanced = advanceRaptorRole(store, raptorRef, roleInfo, state, nowMs);
                state.transitionPending = advanced;
                state.nextTransitionAttemptMs = nowMs + (advanced ? 5_000L : RAPTOR_TRANSITION_RETRY_INTERVAL_MS);
            }
        }
    }

    private static final class PterodactylMountInputSystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Set.of(
                new SystemDependency<>(Order.BEFORE, MountSystems.HandleMountInput.class),
                new SystemDependency<>(Order.BEFORE, PlayerSystems.ProcessPlayerInput.class)
            );
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || playerRef.getUuid() == null || playerRef.getReference() == null) {
                return;
            }
            Object playerInput = getComponentReflect(store, playerRef.getReference(), getComponentTypeReflect("com.hypixel.hytale.server.core.modules.entity.player.PlayerInput"));
            if (playerInput == null) {
                return;
            }
            boolean updated = false;
            Object queue = invokeReflect(playerInput, "getMovementUpdateQueue");
            if (queue instanceof Iterable<?> iterable) {
                for (Object update : iterable) {
                    PterodactylLookInput look = readPterodactylLookInput(update);
                    if (look != null) {
                        PTERODACTYL_RIDER_LAST_LOOK.put(playerRef.getUuid(), look);
                        updated = true;
                    }
                    PterodactylMoveInput movement = readPterodactylMoveInput(update);
                    if (movement != null) {
                        PTERODACTYL_RIDER_LAST_MOVEMENT.put(playerRef.getUuid(), movement);
                        updated = true;
                    }
                }
            }
            if (updated) {
                PTERODACTYL_RIDER_LAST_INPUT_MS.put(playerRef.getUuid(), System.currentTimeMillis());
            }
        }
    }

    private static final class PterodactylFlightSystem extends EntityTickingSystem<EntityStore> {
        @Override
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Set.of(
                new SystemDependency<>(Order.AFTER, MountSystems.PlayerMount.class),
                new SystemDependency<>(Order.AFTER, MountSystems.HandleMountInput.class),
                new SystemDependency<>(Order.AFTER, PlayerSystems.ProcessPlayerInput.class)
            );
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            if (store == null || archetypeChunk == null) {
                return;
            }
            Ref<EntityStore> mountRef = archetypeChunk.getReferenceTo(index);
            Object npcMount = getComponentReflect(store, mountRef, getComponentTypeReflect("com.hypixel.hytale.builtin.mounts.NPCMountComponent"));
            if (!isTfjPterodactylMountComponent(npcMount)) {
                return;
            }

            int mountNetworkId = getNetworkId(store, mountRef);
            PlayerRef riderRef = asPlayerRef(invokeReflect(npcMount, "getOwnerPlayerRef"));
            NPCEntity mountNpc = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
            if (mountNpc == null || mountNpc.isDespawning()) {
                if (riderRef != null) {
                    resetPterodactylRiderMountState(riderRef.getReference(), store);
                }
                clearPterodactylMountState(mountNetworkId);
                return;
            }
            if (riderRef == null || riderRef.getReference() == null || !riderRef.getReference().isValid()) {
                clearPterodactylMountState(mountNetworkId);
                return;
            }

            rememberActivePterodactylMount(riderRef, mountNetworkId);
            Object mountMovementStatesComponent = getComponentReflect(store, mountRef, getComponentTypeReflect("com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent"));
            TransformComponent mountTransform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            TransformComponent riderTransform = store.getComponent(riderRef.getReference(), TransformComponent.getComponentType());
            HeadRotation riderHeadRotation = store.getComponent(riderRef.getReference(), HeadRotation.getComponentType());
            PterodactylLookInput capturedLook = riderRef.getUuid() == null ? null : PTERODACTYL_RIDER_LAST_LOOK.get(riderRef.getUuid());
            PterodactylMoveInput capturedMovement = recentPterodactylMoveInput(riderRef);

            tickPterodactylFlightMotion(
                mountRef,
                commandBuffer == null ? store : commandBuffer,
                mountMovementStatesComponent,
                mountTransform,
                riderTransform,
                riderHeadRotation,
                capturedLook,
                capturedMovement,
                mountNetworkId,
                deltaTime
            );

            Object states = getMovementStates(mountMovementStatesComponent);
            boolean riderFlightMovementEnabled = states != null
                && (getBooleanMember(states, "flying")
                    || getBooleanMember(states, "gliding")
                    || getBooleanMember(states, "jumping")
                    || getBooleanMember(states, "falling")
                    || getBooleanMember(states, "fallingFar")
                    || !getBooleanMember(states, "onGround"));
            setPterodactylRiderFlightMovement(riderRef.getReference(), store, riderRef, riderFlightMovementEnabled);
            playPterodactylFlightAnimation(mountRef, mountNpc, store, mountNetworkId, states);
            logPterodactylFlightDebug(
                store,
                "TFJ pterodactyl flight: rider=%s mountId=%d flying=%s jumping=%s crouching=%s %s",
                riderRef.getUsername(),
                mountNetworkId,
                states != null && getBooleanMember(states, "flying"),
                states != null && getBooleanMember(states, "jumping"),
                states != null && getBooleanMember(states, "crouching"),
                PTERODACTYL_LAST_DEBUG.getOrDefault(mountNetworkId, "no-motion-debug")
            );
        }
    }

    private static final class PterodactylMountCleanupSystem extends EntityTickingSystem<EntityStore> {
        private static final long STALE_MOUNT_MS = 2500L;

        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }

        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return Set.of(new SystemDependency<>(Order.AFTER, PterodactylFlightSystem.class));
        }

        @Override
        public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
        ) {
            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null || playerRef.getUuid() == null || playerRef.getReference() == null) {
                return;
            }
            UUID riderUuid = playerRef.getUuid();
            Integer activeMountId = PTERODACTYL_RIDER_ACTIVE_MOUNT_ID.get(riderUuid);
            Long lastSeenMs = PTERODACTYL_RIDER_ACTIVE_MOUNT_SEEN_MS.get(riderUuid);
            if (activeMountId == null || lastSeenMs == null) {
                return;
            }
            long staleMs = System.currentTimeMillis() - lastSeenMs;
            if (staleMs < STALE_MOUNT_MS) {
                return;
            }

            Object playerInput = getComponentReflect(store, playerRef.getReference(), getComponentTypeReflect("com.hypixel.hytale.server.core.modules.entity.player.PlayerInput"));
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) {
                player = store.getComponent(playerRef.getReference(), Player.getComponentType());
            }
            int inputMountId = asIntegerOrZero(invokeReflect(playerInput, "getMountId"));
            int playerMountId = asIntegerOrZero(invokeReflect(player, "getMountEntityId"));
            if (inputMountId == activeMountId || playerMountId == activeMountId) {
                resetPterodactylRiderMountState(playerRef.getReference(), store);
                removeComponentReflect(commandBuffer, playerRef.getReference(), getComponentTypeReflect("com.hypixel.hytale.builtin.mounts.MountedComponent"));
                clearPterodactylMountState(activeMountId);
                logPterodactylFlightDebug(
                    store,
                    "TFJ cleaned stale pterodactyl mount: rider=%s mountId=%d staleMs=%d inputMountId=%d playerMountId=%d",
                    playerRef.getUsername(),
                    activeMountId,
                    staleMs,
                    inputMountId,
                    playerMountId
                );
            }
            clearPterodactylRiderActiveMountState(riderUuid);
        }
    }

    private static boolean consumeFastForwardRequest(Map<UUID, Long> pendingRequests, UUID playerUuid) {
        if (pendingRequests == null || playerUuid == null) {
            return false;
        }
        return pendingRequests.remove(playerUuid) != null;
    }

    private static void refreshOpenRaptorCarePage(PlayerRef playerRef, Player player, RaptorRuntimeState lookedState, long nowMs) {
        if (playerRef == null || playerRef.getUuid() == null || player == null || lookedState == null) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();
        RaptorCarePage page = RAPTOR_CARE_PAGE_BY_PLAYER.get(playerUuid);
        if (page == null) {
            return;
        }
        if (!safeEquals(page.raptorKey, lookedState.key)) {
            RAPTOR_CARE_PAGE_BY_PLAYER.remove(playerUuid, page);
            return;
        }
        try {
            CustomUIPage currentPage = player.getPageManager() == null ? null : player.getPageManager().getCustomPage();
            if (currentPage != page) {
                RAPTOR_CARE_PAGE_BY_PLAYER.remove(playerUuid, page);
                return;
            }
        } catch (Throwable throwable) {
            RAPTOR_CARE_PAGE_BY_PLAYER.remove(playerUuid, page);
            debugGlobal(nowMs, "Could not verify active TFJ raptor care page: " + describeThrowable(throwable));
            return;
        }
        page.requestAutoRefresh(nowMs);
    }

    private static boolean shouldScan(UUID playerUuid, long nowMs) {
        if (playerUuid == null) {
            return true;
        }
        long nextScan = NEXT_SCAN_BY_PLAYER.getOrDefault(playerUuid, 0L);
        if (nowMs < nextScan) {
            return false;
        }
        NEXT_SCAN_BY_PLAYER.put(playerUuid, nowMs + NEARBY_SCAN_INTERVAL_MS);
        return true;
    }

    private static boolean shouldRefreshUi(UUID playerUuid, long nowMs) {
        if (playerUuid == null) {
            return true;
        }
        long nextUi = NEXT_UI_BY_PLAYER.getOrDefault(playerUuid, 0L);
        if (nowMs < nextUi) {
            return false;
        }
        NEXT_UI_BY_PLAYER.put(playerUuid, nowMs + LOOK_UI_INTERVAL_MS);
        return true;
    }

    private static World getWorld(Store<EntityStore> store) {
        if (store == null) {
            return null;
        }
        Object externalData;
        try {
            externalData = store.getExternalData();
        } catch (Throwable ignored) {
            return null;
        }
        if (externalData instanceof World) {
            return (World) externalData;
        }
        if (externalData == null) {
            return null;
        }
        try {
            Object world = externalData.getClass().getMethod("getWorld").invoke(externalData);
            return world instanceof World ? (World) world : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void clearOwnHud(PlayerRef playerRef, Player player) {
        if (player == null || player.getHudManager() == null) {
            return;
        }
        if (player.getHudManager().getCustomHud(HUD_ID) instanceof RaptorIncubationHud) {
            player.getHudManager().removeCustomHud(playerRef, HUD_ID);
        }
    }


    private static void scanNearbyRaptors(Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, World world, TransformComponent transform, long nowMs) {
        if (store == null || commandBuffer == null || world == null || transform == null || transform.getPosition() == null) {
            return;
        }
        try {
            List<Ref<EntityStore>> refs = TargetUtil.getAllEntitiesInSphere(transform.getPosition(), NEARBY_SCAN_RADIUS + 2.0, commandBuffer);
            if (refs == null) {
                return;
            }
            String currentWorldKey = worldKey(world);
            for (Ref<EntityStore> ref : refs) {
                trackRaptorIfKnown(store, currentWorldKey, ref, nowMs);
            }
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Nearby raptor scan failed: " + describeThrowable(throwable));
        }
    }

    private static RaptorRuntimeState trackRaptorIfKnown(Store<EntityStore> store, String worldKey, Ref<EntityStore> ref, long nowMs) {
        NPCEntity npc = getNpcSafely(store, ref);
        RaptorRoleInfo roleInfo = getRaptorRoleInfo(npc);
        if (roleInfo == null) {
            return null;
        }
        return getOrCreateRaptorState(store, worldKey, ref, npc, roleInfo, nowMs);
    }

    private static LookedRaptor findLookedRaptor(
        Ref<EntityStore> playerEntityRef,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer,
        World world,
        long nowMs,
        boolean debugNow
    ) {
        if (playerEntityRef == null || store == null || commandBuffer == null) {
            return null;
        }
        Ref<EntityStore> targetRef;
        try {
            targetRef = TargetUtil.getTargetEntity(playerEntityRef, (float) LOOK_DISTANCE, commandBuffer);
        } catch (Throwable throwable) {
            if (debugNow) {
                debug("TargetUtil.getTargetEntity failed: " + describeThrowable(throwable));
            }
            return null;
        }
        if (targetRef == null || !targetRef.isValid()) {
            return null;
        }
        RaptorRuntimeState state = trackRaptorIfKnown(store, worldKey(world), targetRef, nowMs);
        if (state == null) {
            if (debugNow) {
                NPCEntity npc = getNpcSafely(store, targetRef);
                debug("Looked entity is not a TFJ breeding raptor: role=" + resolveRoleName(npc) + ".");
            }
            return null;
        }
        if (debugNow) {
            debug("Looking at TFJ raptor " + state.roleInfo.role + " state=" + state.roleInfo.stageLabel + ", remaining=" + formatRemaining(Math.max(0L, state.completeAtMs - nowMs)) + ".");
        }
        return new LookedRaptor(targetRef, state);
    }

    private static NPCEntity getNpcSafely(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        try {
            return store.getComponent(ref, NPCEntity.getComponentType());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static RaptorRuntimeState getOrCreateRaptorState(Store<EntityStore> store, String worldKey, Ref<EntityStore> ref, NPCEntity npc, RaptorRoleInfo roleInfo, long nowMs) {
        String key = raptorKey(ref, npc);
        RaptorRuntimeState state = KNOWN_RAPTORS.get(key);
        if (state == null || state.roleInfo != roleInfo || !safeEquals(state.worldKey, worldKey)) {
            RaptorSex sex = getOrCreateRaptorSex(key);
            state = new RaptorRuntimeState(key, worldKey, ref, roleInfo, sex, nowMs);
            KNOWN_RAPTORS.put(key, state);
            forceRaptorAppearance(store, ref, roleInfo.role, nowMs);
            debug("Tracking TFJ raptor lifecycle key=" + key + ", role=" + roleInfo.role + ", stage=" + roleInfo.stageLabel + ", sex=" + sex.label + ", duration=" + (roleInfo.durationMs / 1000L) + "s.");
        }
        state.ref = ref;
        state.lastSeenMs = nowMs;
        return state;
    }

    private static void tickRaptorCare(Store<EntityStore> store, Ref<EntityStore> raptorRef, NPCEntity npc, RaptorRuntimeState state, long nowMs) {
        if (store == null || raptorRef == null || state == null || state.dead || state.roleInfo == null || state.roleInfo.isBreedingStage()) {
            return;
        }

        long elapsedMs = markRaptorCareTick(state, nowMs);
        syncRaptorHealthFromStats(store, raptorRef, state);
        drainRaptorFood(state, elapsedMs);
        int storedMeat = totalStoredMeat(state);
        if (state.foodLevel <= RAPTOR_AUTO_EAT_THRESHOLD && storedMeat > 0 && nowMs >= state.nextAutoFeedMs) {
            String consumed = consumeStoredMeat(state);
            state.foodLevel = clamp(state.foodLevel + RAPTOR_MEAT_FOOD_VALUE, 0.0, RAPTOR_FOOD_MAX);
            healRaptorCare(store, raptorRef, state, RAPTOR_MEAT_HEAL, nowMs);
            state.nextAutoFeedMs = nowMs + RAPTOR_AUTO_EAT_DELAY_MS;
            debug("TFJ raptor " + state.roleInfo.role + " ate " + displayItemName(consumed) + "; food=" + formatRaptorFood(state) + ", storedMeat=" + totalStoredMeat(state) + ", hp=" + formatRaptorHealth(state) + ".");
        }

        if (state.foodLevel > 0.01) {
            state.starving = false;
            state.nextStarveDamageMs = 0L;
            return;
        }

        state.starving = true;
        pauseRaptorGrowthForHunger(state, elapsedMs);
        if (storedMeat > 0 && nowMs < state.nextAutoFeedMs) {
            return;
        }
        if (state.nextStarveDamageMs <= 0L) {
            state.nextStarveDamageMs = nowMs;
        }
        if (nowMs >= state.nextStarveDamageMs) {
            damageRaptorCare(store, raptorRef, npc, state, RAPTOR_STARVE_DAMAGE, nowMs);
            state.nextStarveDamageMs = nowMs + RAPTOR_STARVE_DAMAGE_INTERVAL_MS;
        }
    }

    private static void tickRaptorAdultRuntime(Store<EntityStore> store, Ref<EntityStore> raptorRef, NPCEntity npc, RaptorRuntimeState state, long nowMs) {
        if (store == null || raptorRef == null || state == null || state.dead || state.roleInfo == null || !state.roleInfo.isBreedingStage()) {
            return;
        }
        long elapsedMs = markRaptorAdultTick(state, nowMs);
        syncRaptorHealthFromStats(store, raptorRef, state);
        applyRaptorUpgradeStats(store, raptorRef, state, nowMs);
        boolean mounted = isRaptorMounted(store, raptorRef);
        tickRaptorStamina(state, elapsedMs, mounted);
        tickRaptorActivityXp(state, mounted, nowMs);
        if (state.commandMode == RaptorCommandMode.FOLLOW) {
            followRaptorOwner(store, raptorRef, state, nowMs);
        } else if (state.commandMode == RaptorCommandMode.GUARD) {
            guardRaptorPosition(store, raptorRef, state, nowMs);
        }
    }

    private static long markRaptorAdultTick(RaptorRuntimeState state, long nowMs) {
        if (state == null) {
            return 0L;
        }
        long previous = state.lastAdultTickMs <= 0L ? nowMs : state.lastAdultTickMs;
        state.lastAdultTickMs = nowMs;
        return Math.max(0L, nowMs - previous);
    }

    private static void tickRaptorStamina(RaptorRuntimeState state, long elapsedMs, boolean mounted) {
        if (state == null || elapsedMs <= 0L) {
            return;
        }
        double max = raptorMaxStamina(state);
        double delta = ((double) elapsedMs / 1_000.0) * (mounted ? -RAPTOR_MOUNT_STAMINA_DRAIN_PER_SECOND : RAPTOR_STAMINA_REGEN_PER_SECOND);
        state.stamina = clamp(state.stamina + delta, 0.0, max);
    }

    private static void tickRaptorActivityXp(RaptorRuntimeState state, boolean mounted, long nowMs) {
        if (state == null || state.level >= RAPTOR_MAX_LEVEL || nowMs < state.nextAdultXpMs) {
            return;
        }
        int amount = 0;
        if (mounted) {
            amount += RAPTOR_MOUNTED_XP;
        }
        if (state.commandMode == RaptorCommandMode.FOLLOW) {
            amount += RAPTOR_FOLLOW_XP;
        } else if (state.commandMode == RaptorCommandMode.GUARD) {
            amount += RAPTOR_GUARD_XP;
        }
        if (amount <= 0) {
            return;
        }
        addRaptorXp(state, amount);
        state.nextAdultXpMs = nowMs + RAPTOR_ACTIVITY_XP_MS;
    }

    private static boolean isRaptorMounted(Store<EntityStore> store, Ref<EntityStore> raptorRef) {
        if (store == null || raptorRef == null || !raptorRef.isValid()) {
            return false;
        }
        try {
            Object mountType = getComponentTypeReflect("com.hypixel.hytale.builtin.mounts.NPCMountComponent");
            return mountType != null && getComponentReflect(store, raptorRef, mountType) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void followRaptorOwner(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRuntimeState state, long nowMs) {
        Ref<EntityStore> owner = state == null ? null : state.ownerRef;
        if (owner == null || !owner.isValid()) {
            if (state != null) {
                state.commandMode = RaptorCommandMode.IDLE;
                state.ownerRef = null;
                state.ownerUuid = null;
                applyRaptorCommandState(store, state);
            }
            return;
        }
        refreshRaptorFollowTarget(store, state, nowMs);
    }

    private static void refreshRaptorFollowTarget(Store<EntityStore> store, RaptorRuntimeState state, long nowMs) {
        if (store == null || state == null || state.ref == null || !state.ref.isValid()) {
            return;
        }
        if (nowMs < state.nextCommandTargetRefreshMs) {
            return;
        }
        state.nextCommandTargetRefreshMs = nowMs + RAPTOR_COMMAND_TARGET_REFRESH_MS;
        NPCEntity npc = getNpcSafely(store, state.ref);
        Role role = npc == null ? null : npc.getRole();
        if (role == null) {
            debug("TFJ raptor follow refresh failed: role unavailable.");
            return;
        }
        boolean cleared = setRaptorMarkedTarget(role, RAPTOR_LOCKED_TARGET_SLOT, null);
        boolean targetSet = setRaptorMarkedTarget(role, RAPTOR_MASTER_TARGET_SLOT, state.ownerRef);
        if (!targetSet) {
            debug("TFJ raptor follow refresh failed: clearedLocked=" + cleared + ", owner=" + state.ownerRef);
        }
    }

    private static void guardRaptorPosition(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRuntimeState state, long nowMs) {
        if (state == null || !state.hasGuardPosition) {
            rememberGuardPosition(store, state);
            return;
        }
        applyRaptorCommandState(store, state);
    }

    private static boolean applyRaptorCommandState(Store<EntityStore> store, RaptorRuntimeState state) {
        if (store == null || state == null || state.ref == null || !state.ref.isValid()) {
            return false;
        }
        NPCEntity npc = getNpcSafely(store, state.ref);
        Role role = npc == null ? null : npc.getRole();
        if (role == null) {
            debug("TFJ raptor command failed: role unavailable.");
            return false;
        }
        if (state.commandMode == RaptorCommandMode.FOLLOW && state.ownerRef != null && state.ownerRef.isValid()) {
            boolean cleared = setRaptorMarkedTarget(role, RAPTOR_LOCKED_TARGET_SLOT, null);
            boolean targetSet = setRaptorMarkedTarget(role, RAPTOR_MASTER_TARGET_SLOT, state.ownerRef);
            boolean stateSet = setRaptorRoleState(state.ref, role, RAPTOR_FOLLOW_STATE, null, store);
            state.nextCommandTargetRefreshMs = System.currentTimeMillis() + RAPTOR_COMMAND_TARGET_REFRESH_MS;
            debug("TFJ raptor follow command: role=" + state.roleInfo.role + ", owner=" + state.ownerRef + ", clearedLocked=" + cleared + ", targetSet=" + targetSet + ", stateSet=" + stateSet);
            if (!targetSet || !stateSet) {
                debug("TFJ raptor follow command failed: clearedLocked=" + cleared + ", targetSet=" + targetSet + ", stateSet=" + stateSet);
            }
            return targetSet && stateSet;
        }
        setRaptorMarkedTarget(role, RAPTOR_MASTER_TARGET_SLOT, null);
        return setRaptorRoleState(state.ref, role, RAPTOR_IDLE_STATE, null, store);
    }

    private static boolean setRaptorMarkedTarget(Role role, String slot, Ref<EntityStore> target) {
        if (role == null || slot == null || slot.isBlank()) {
            return false;
        }
        Object support = invokeReflect(role, "getMarkedEntitySupport");
        boolean supportApplied = false;
        if (support != null) {
            supportApplied = invokeReflectSuccess(support, "setMarkedEntity", slot, target);
        }
        boolean roleApplied = invokeReflectSuccess(role, "setMarkedTarget", slot, target);
        boolean applied = supportApplied || roleApplied;
        if (!applied) {
            debug("TFJ raptor marked target failed: slot=" + slot + ", target=" + target + ", support=" + (support != null));
        } else if (target != null && !supportApplied) {
            debug("TFJ raptor marked target only applied through role wrapper: slot=" + slot + ", support=" + (support != null));
        }
        return applied;
    }

    private static boolean setRaptorRoleState(Ref<EntityStore> raptorRef, Role role, String stateName, String subState, Store<EntityStore> store) {
        if (raptorRef == null || role == null || stateName == null || stateName.isBlank() || store == null) {
            return false;
        }
        Object stateSupport = role.getStateSupport();
        if (stateSupport == null) {
            debug("TFJ raptor state failed: state support unavailable for " + stateName);
            return false;
        }
        String resolvedSubState = resolveRaptorSubState(stateSupport, stateName, subState);
        boolean applied = false;
        if (resolvedSubState != null) {
            applied = invokeReflectSuccess(stateSupport, "setState", raptorRef, stateName, resolvedSubState, store);
        }
        if (!applied) {
            debug("TFJ raptor state failed: could not set " + stateName + " subState=" + subState + " with support=" + stateSupport.getClass().getName());
        }
        return applied;
    }

    private static String resolveRaptorSubState(Object stateSupport, String stateName, String subState) {
        if (stateSupport == null || stateName == null || stateName.isBlank()) {
            return null;
        }
        Object stateHelper = invokeReflect(stateSupport, "getStateHelper");
        if (stateHelper == null) {
            return subState == null ? "" : subState.trim();
        }
        Integer stateIndex = asInteger(invokeReflect(stateHelper, "getStateIndex", stateName));
        if (stateIndex == null || stateIndex < 0) {
            return null;
        }
        String resolved = subState == null ? "" : subState.trim();
        if (resolved.isBlank()) {
            String defaultSubState = asString(invokeReflect(stateHelper, "getDefaultSubState"));
            resolved = defaultSubState == null ? "" : defaultSubState.trim();
        }
        Integer subStateIndex = resolved.isBlank() ? 0 : asInteger(invokeReflect(stateHelper, "getSubStateIndex", stateIndex, resolved));
        if (subStateIndex == null || subStateIndex < 0) {
            return null;
        }
        return resolved;
    }

    private static TransformComponent getTransformSafely(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        try {
            return store.getComponent(ref, TransformComponent.getComponentType());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void putTransformSafely(Store<EntityStore> store, Ref<EntityStore> ref, TransformComponent transform, long nowMs) {
        if (store == null || ref == null || !ref.isValid() || transform == null) {
            return;
        }
        try {
            store.putComponent(ref, TransformComponent.getComponentType(), transform);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not update TFJ raptor command movement: " + describeThrowable(throwable));
        }
    }

    private static long markRaptorCareTick(RaptorRuntimeState state, long nowMs) {
        if (state == null) {
            return 0L;
        }
        long previous = state.lastCareTickMs <= 0L ? nowMs : state.lastCareTickMs;
        state.lastCareTickMs = nowMs;
        return Math.max(0L, nowMs - previous);
    }

    private static void drainRaptorFood(RaptorRuntimeState state, long elapsedMs) {
        if (state == null || elapsedMs <= 0L || state.roleInfo == null || !state.roleInfo.isGrowthStage()) {
            return;
        }
        double drain = ((double) elapsedMs / 1_000.0) * RAPTOR_FOOD_DRAIN_PER_SECOND;
        state.foodLevel = clamp(state.foodLevel - drain, 0.0, RAPTOR_FOOD_MAX);
    }

    private static void pauseRaptorGrowthForHunger(RaptorRuntimeState state, long elapsedMs) {
        if (state == null || elapsedMs <= 0L || state.roleInfo == null || !state.roleInfo.isGrowthStage()) {
            return;
        }
        state.startedAtMs += elapsedMs;
        state.completeAtMs += elapsedMs;
        if (state.nextTransitionAttemptMs > 0L) {
            state.nextTransitionAttemptMs += elapsedMs;
        } else {
            state.nextTransitionAttemptMs = state.completeAtMs;
        }
    }

    private static RaptorRuntimeState createReplacementRaptorState(RaptorRuntimeState previous, Ref<EntityStore> spawnedRef, NPCEntity spawnedNpc, String nextRole, RaptorSex sex, long nowMs) {
        RaptorRoleInfo nextInfo = RAPTOR_BY_ROLE.get(normalizeRoleKey(nextRole));
        if (previous == null || spawnedRef == null || nextInfo == null) {
            return null;
        }
        RaptorRuntimeState replacement = new RaptorRuntimeState(raptorKey(spawnedRef, spawnedNpc), previous.worldKey, spawnedRef, nextInfo, sex, nowMs);
        replacement.inheritCareFrom(previous, nowMs);
        return replacement;
    }

    private static void rememberReplacementRaptor(RaptorRuntimeState replacementState, RaptorSex inheritedSex, Store<EntityStore> store, Ref<EntityStore> spawnedRef, long nowMs) {
        if (replacementState == null) {
            return;
        }
        KNOWN_RAPTORS.put(replacementState.key, replacementState);
        RAPTOR_SEX_BY_KEY.put(replacementState.key, inheritedSex == null ? replacementState.sex : inheritedSex);
        forceRaptorAppearance(store, spawnedRef, replacementState.roleInfo.role, nowMs);
    }

    private static EntityStatMap getEntityStatsSafely(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        try {
            return store.getComponent(ref, EntityStatMap.getComponentType());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static EntityStatValue firstStat(EntityStatMap stats, String... names) {
        if (stats == null || names == null) {
            return null;
        }
        for (String name : names) {
            try {
                EntityStatValue value = stats.get(name);
                if (value != null) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static EntityStatValue getHealthStat(EntityStatMap stats) {
        return firstStat(stats, "Health", "health", "HP", "Hp", "Hitpoints", "HitPoints");
    }

    private static EntityStatValue getSpeedStat(EntityStatMap stats) {
        return firstStat(stats, "Speed", "MaxSpeed", "MovementSpeed", "WalkSpeed", "MoveSpeed");
    }

    private static EntityStatValue getDamageStat(EntityStatMap stats) {
        return firstStat(stats, "Damage", "AttackDamage", "MeleeDamage", "DamageDealtMultiplier");
    }

    private static void syncRaptorHealthFromStats(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRuntimeState state) {
        if (state == null) {
            return;
        }
        EntityStatValue health = getHealthStat(getEntityStatsSafely(store, raptorRef));
        if (health == null) {
            if (state.careMaxHealth <= 0.0) {
                state.careMaxHealth = raptorCareMaxHealth(state.roleInfo);
            }
            if (!state.careHealthInitialized || state.careHealth <= 0.0) {
                state.careHealth = state.careMaxHealth;
                state.careHealthInitialized = true;
            }
            return;
        }
        try {
            state.careMaxHealth = Math.max(1.0, health.getMax());
            if (!state.careHealthInitialized) {
                state.careHealth = clamp(health.get(), 0.0, state.careMaxHealth);
                state.careHealthInitialized = true;
            } else {
                state.careHealth = clamp(state.careHealth, 0.0, state.careMaxHealth);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void healRaptorCare(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRuntimeState state, double amount, long nowMs) {
        if (state == null || amount <= 0.0) {
            return;
        }
        EntityStatMap stats = getEntityStatsSafely(store, raptorRef);
        EntityStatValue health = getHealthStat(stats);
        double currentHealth = state.careHealthInitialized ? state.careHealth : Math.max(1.0, state.careMaxHealth);
        if (stats != null && health != null) {
            try {
                state.careMaxHealth = Math.max(1.0, health.getMax());
                float next = (float) clamp(currentHealth + amount, 0.0, state.careMaxHealth);
                stats.setStatValue(health.getIndex(), next);
                state.careHealth = clamp(next, 0.0, state.careMaxHealth);
                state.careHealthInitialized = true;
                return;
            } catch (Throwable throwable) {
                debugGlobal(nowMs, "Could not heal raptor stat value: " + describeThrowable(throwable));
            }
        }
        state.careHealth = clamp(currentHealth + amount, 0.0, Math.max(1.0, state.careMaxHealth));
        state.careHealthInitialized = true;
    }

    private static void applyRaptorUpgradeStats(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRuntimeState state, long nowMs) {
        if (state == null || state.roleInfo == null || !state.roleInfo.isBreedingStage()) {
            return;
        }
        EntityStatMap stats = getEntityStatsSafely(store, raptorRef);
        if (stats == null) {
            return;
        }
        applyStatValue(stats, getSpeedStat(stats), raptorEffectiveSpeed(state), nowMs, "speed");
        applyStatValue(stats, getDamageStat(stats), raptorEffectiveDamage(state), nowMs, "damage");
    }

    private static void applyStatValue(EntityStatMap stats, EntityStatValue stat, double value, long nowMs, String label) {
        if (stats == null || stat == null || !Double.isFinite(value)) {
            return;
        }
        try {
            stats.setStatValue(stat.getIndex(), (float) value);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not apply TFJ raptor " + label + " upgrade stat: " + describeThrowable(throwable));
        }
    }

    private static void damageRaptorCare(Store<EntityStore> store, Ref<EntityStore> raptorRef, NPCEntity npc, RaptorRuntimeState state, double amount, long nowMs) {
        if (state == null || amount <= 0.0 || state.dead) {
            return;
        }
        EntityStatMap stats = getEntityStatsSafely(store, raptorRef);
        EntityStatValue health = getHealthStat(stats);
        double currentHealth = state.careHealthInitialized ? state.careHealth : Math.max(1.0, state.careMaxHealth);
        if (stats != null && health != null) {
            try {
                state.careMaxHealth = Math.max(1.0, health.getMax());
                currentHealth = clamp(currentHealth, 0.0, state.careMaxHealth);
                float next = (float) Math.max(0.0, currentHealth - amount);
                stats.setStatValue(health.getIndex(), next);
                state.careHealth = next;
                state.careHealthInitialized = true;
            } catch (Throwable throwable) {
                debugGlobal(nowMs, "Could not damage raptor stat value: " + describeThrowable(throwable));
                state.careHealth = Math.max(0.0, currentHealth - amount);
                state.careHealthInitialized = true;
            }
        } else {
            state.careHealth = Math.max(0.0, currentHealth - amount);
            state.careHealthInitialized = true;
        }
        debug("TFJ raptor " + state.roleInfo.role + " is hungry; hp=" + formatRaptorHealth(state) + ".");
        if (state.careHealth <= 0.01) {
            removeRaptorForStarvation(store, raptorRef, npc, state, nowMs);
        }
    }

    private static void removeRaptorForStarvation(Store<EntityStore> store, Ref<EntityStore> raptorRef, NPCEntity npc, RaptorRuntimeState state, long nowMs) {
        if (state == null || state.dead) {
            return;
        }
        state.dead = true;
        try {
            NPCEntity liveNpc = npc == null ? getNpcSafely(store, raptorRef) : npc;
            if (liveNpc != null) {
                liveNpc.setToDespawn();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (store != null && raptorRef != null) {
                store.removeEntity(raptorRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            }
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not remove starving raptor " + state.roleInfo.role + ": " + describeThrowable(throwable));
        }
        KNOWN_RAPTORS.remove(state.key);
        RAPTOR_SEX_BY_KEY.remove(state.key);
        debug("TFJ raptor " + state.roleInfo.role + " died from hunger.");
    }

    private static double raptorCareMaxHealth(RaptorRoleInfo roleInfo) {
        if (roleInfo == null || roleInfo.stageLabel == null) {
            return 20.0;
        }
        String stage = roleInfo.stageLabel.toLowerCase(Locale.ROOT);
        double multiplier = roleInfo.info == null ? 1.0 : roleInfo.info.healthMultiplier;
        double baseHealth;
        if (stage.contains("hatchling")) {
            baseHealth = 12.0;
        } else if (stage.contains("youngling")) {
            baseHealth = 18.0;
        } else if (stage.contains("juvenile")) {
            baseHealth = 26.0;
        } else if (stage.contains("adolescent")) {
            baseHealth = 34.0;
        } else {
            baseHealth = 40.0;
        }
        return Math.max(1.0, baseHealth * multiplier);
    }

    private static int totalStoredMeat(RaptorRuntimeState state) {
        if (state == null || state.meatSlotCounts == null) {
            return 0;
        }
        int total = 0;
        for (int count : state.meatSlotCounts) {
            total += Math.max(0, count);
        }
        return total;
    }

    private static String consumeStoredMeat(RaptorRuntimeState state) {
        if (state == null) {
            return "";
        }
        for (int i = 0; i < RAPTOR_CARE_SLOT_COUNT; i++) {
            if (state.meatSlotCounts[i] <= 0) {
                continue;
            }
            String itemId = state.meatSlotItems[i];
            state.meatSlotCounts[i]--;
            if (state.meatSlotCounts[i] <= 0) {
                state.meatSlotItems[i] = "";
                state.meatSlotCounts[i] = 0;
            }
            return itemId == null ? "" : itemId;
        }
        return "";
    }

    private static String depositOneMeatFromPlayer(Player player, RaptorRuntimeState state, int slotIndex) {
        if (state == null || state.dead) {
            return "This raptor is no longer available.";
        }
        if (state.roleInfo != null && state.roleInfo.isBreedingStage()) {
            return "Adults no longer need meat.";
        }
        int slot = Math.max(0, Math.min(RAPTOR_CARE_SLOT_COUNT - 1, slotIndex));
        if (state.meatSlotCounts[slot] >= RAPTOR_CARE_SLOT_CAPACITY) {
            return "That meat slot is full.";
        }
        ItemContainer container = resolvePlayerInventoryContainer(player);
        if (container == null) {
            return "Inventory is not available.";
        }
        String preferred = normalizeItemId(state.meatSlotItems[slot]);
        MeatInventoryMatch match = preferred.isBlank() ? findFirstMeat(container) : findMeat(container, preferred);
        if (match == null) {
            return preferred.isBlank() ? "No meat found in your inventory." : "No more " + displayItemName(preferred) + " found.";
        }
        if (!preferred.isBlank() && !preferred.equals(normalizeItemId(match.itemId))) {
            return "That slot already contains " + displayItemName(preferred) + ".";
        }
        if (!removeItemFromInventory(container, match.itemId, 1)) {
            return "Could not move that meat.";
        }
        state.meatSlotItems[slot] = match.itemId;
        state.meatSlotCounts[slot] = Math.min(RAPTOR_CARE_SLOT_CAPACITY, state.meatSlotCounts[slot] + 1);
        state.starving = false;
        state.nextStarveDamageMs = 0L;
        if (state.foodLevel <= RAPTOR_AUTO_EAT_THRESHOLD) {
            state.nextAutoFeedMs = Math.max(state.nextAutoFeedMs, System.currentTimeMillis() + RAPTOR_AUTO_EAT_DELAY_MS);
        }
        return "Stored " + displayItemName(match.itemId) + " for " + raptorDisplayName(state) + ".";
    }

    private static ItemContainer resolvePlayerInventoryContainer(Player player) {
        Object inventory = invokeNoArg(player, "getInventory");
        if (inventory == null) {
            return null;
        }
        String[] methodNames = {
            "getCombinedBackpackStorageHotbarFirst",
            "getCombinedEverything",
            "getCombinedInventory",
            "getCombinedStorageHotbar",
            "getCombinedStorage"
        };
        for (String methodName : methodNames) {
            Object result = invokeNoArg(inventory, methodName);
            if (result instanceof ItemContainer container) {
                return container;
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static MeatInventoryMatch findFirstMeat(ItemContainer container) {
        if (container == null) {
            return null;
        }
        try {
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack itemStack = container.getItemStack(slot);
                if (isUsableMeatStack(itemStack)) {
                    return new MeatInventoryMatch(itemStack.getItemId(), itemStack.getQuantity());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static MeatInventoryMatch findMeat(ItemContainer container, String normalizedItemId) {
        if (container == null || normalizedItemId == null || normalizedItemId.isBlank()) {
            return null;
        }
        try {
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack itemStack = container.getItemStack(slot);
                if (isUsableMeatStack(itemStack) && normalizedItemId.equals(normalizeItemId(itemStack.getItemId()))) {
                    return new MeatInventoryMatch(itemStack.getItemId(), itemStack.getQuantity());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isUsableMeatStack(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        try {
            return !itemStack.isEmpty() && itemStack.getQuantity() > 0 && isMeatItemId(itemStack.getItemId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean removeItemFromInventory(ItemContainer container, String itemId, int quantity) {
        if (container == null || itemId == null || itemId.isBlank() || quantity <= 0) {
            return false;
        }
        try {
            ItemStack itemStack = new ItemStack(itemId, quantity);
            if (!container.canRemoveItemStack(itemStack, true, false)) {
                return false;
            }
            ItemStackTransaction transaction = container.removeItemStack(itemStack, true, false);
            return transaction != null && transaction.succeeded();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isMeatItemId(String itemId) {
        String normalized = normalizeItemId(itemId);
        if (normalized.isBlank()) {
            return false;
        }
        for (String token : MEAT_ITEM_TOKENS) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeItemId(String itemId) {
        return itemId == null ? "" : itemId.toLowerCase(Locale.ROOT).replace("hytale:", "").replace("icedfoxstudios:", "").replace("icedfoxstudios.theforgottenjungle:", "");
    }

    private static String displayItemName(String itemId) {
        String normalized = normalizeItemId(itemId);
        if (normalized.isBlank()) {
            return "meat";
        }
        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf(':'));
        String clean = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        clean = clean.replace('_', ' ').replace('-', ' ').trim();
        if (clean.isBlank()) {
            return "meat";
        }
        StringBuilder builder = new StringBuilder(clean.length());
        boolean uppercaseNext = true;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isWhitespace(c)) {
                builder.append(c);
                uppercaseNext = true;
            } else if (uppercaseNext) {
                builder.append(Character.toUpperCase(c));
                uppercaseNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String formatRaptorHealth(RaptorRuntimeState state) {
        if (state == null) {
            return "0 / 0 HP";
        }
        return Math.max(0, (int) Math.ceil(state.careHealth)) + " / " + Math.max(1, (int) Math.ceil(state.careMaxHealth)) + " HP";
    }

    private static double raptorHealthProgress(RaptorRuntimeState state) {
        if (state == null || state.careMaxHealth <= 0.0) {
            return 0.0;
        }
        return clamp(state.careHealth / state.careMaxHealth, 0.0, 1.0);
    }

    private static String formatRaptorFood(RaptorRuntimeState state) {
        if (state == null) {
            return "Food 0%";
        }
        int percent = (int) Math.round(raptorFoodProgress(state) * 100.0);
        return "Food " + percent + "%";
    }

    private static double raptorFoodProgress(RaptorRuntimeState state) {
        if (state == null || RAPTOR_FOOD_MAX <= 0.0) {
            return 0.0;
        }
        if (state.roleInfo != null && state.roleInfo.isBreedingStage()) {
            return 1.0;
        }
        return clamp(state.foodLevel / RAPTOR_FOOD_MAX, 0.0, 1.0);
    }

    private static String raptorFoodStatus(RaptorRuntimeState state) {
        if (state == null || state.roleInfo == null) {
            return "";
        }
        if (state.roleInfo.isBreedingStage()) {
            return "Adult raptor";
        }
        if (state.foodLevel <= 0.01) {
            return "Starving";
        }
        int stored = totalStoredMeat(state);
        return stored > 0 ? stored + " meat stored" : "No reserve meat";
    }

    private static double raptorGrowthProgress(RaptorRuntimeState state, long nowMs) {
        if (state == null || state.roleInfo == null || state.roleInfo.durationMs <= 0L) {
            return 0.0;
        }
        if (state.roleInfo.isBreedingStage()) {
            return 1.0;
        }
        return clamp((double) (nowMs - state.startedAtMs) / (double) state.roleInfo.durationMs, 0.0, 1.0);
    }

    private static void setAdultUpgradeTexts(UICommandBuilder commandBuilder, RaptorRuntimeState state) {
        if (commandBuilder == null) {
            return;
        }
        if (state == null) {
            commandBuilder.set("#TFJRaptorAdultDamageText.Text", "Damage Lv. --");
            commandBuilder.set("#TFJRaptorAdultSpeedText.Text", "Speed Lv. --");
            commandBuilder.set("#TFJRaptorAdultStaminaUpgradeText.Text", "Stamina Lv. --");
            return;
        }
        commandBuilder.set("#TFJRaptorAdultDamageText.Text", "Damage Lv. " + state.damageLevel + " | " + formatCompactDecimal(raptorEffectiveDamage(state)));
        commandBuilder.set("#TFJRaptorAdultSpeedText.Text", "Speed Lv. " + state.speedLevel + " | " + formatCompactDecimal(raptorEffectiveSpeed(state)));
        commandBuilder.set("#TFJRaptorAdultStaminaUpgradeText.Text", "Stamina Lv. " + state.staminaLevel + " | " + (int) Math.round(raptorMaxStamina(state)));
    }

    private static String formatRaptorLevel(RaptorRuntimeState state) {
        if (state == null) {
            return "Level --";
        }
        if (state.level >= RAPTOR_MAX_LEVEL) {
            return "Level " + state.level + " | Max";
        }
        return "Level " + state.level + " | " + state.xp + " / " + raptorXpToNext(state.level) + " XP";
    }

    private static double raptorXpProgress(RaptorRuntimeState state) {
        if (state == null || state.level >= RAPTOR_MAX_LEVEL) {
            return 1.0;
        }
        return clamp((double) state.xp / (double) Math.max(1, raptorXpToNext(state.level)), 0.0, 1.0);
    }

    private static String formatRaptorStamina(RaptorRuntimeState state) {
        if (state == null) {
            return "Stamina --";
        }
        return "Stamina " + Math.max(0, (int) Math.ceil(state.stamina)) + " / " + Math.max(1, (int) Math.ceil(raptorMaxStamina(state)));
    }

    private static double raptorStaminaProgress(RaptorRuntimeState state) {
        if (state == null) {
            return 0.0;
        }
        return clamp(state.stamina / Math.max(1.0, raptorMaxStamina(state)), 0.0, 1.0);
    }

    private static double raptorMaxStamina(RaptorRuntimeState state) {
        return 100.0 + (Math.max(0, state == null ? 0 : state.staminaLevel) * 18.0);
    }

    private static double raptorEffectiveDamage(RaptorRuntimeState state) {
        double species = state == null || state.roleInfo == null || state.roleInfo.info == null ? 1.0 : state.roleInfo.info.healthMultiplier * 0.35 + state.roleInfo.info.speedMultiplier * 0.65;
        return RAPTOR_ADULT_BASE_DAMAGE * species + (Math.max(0, state == null ? 0 : state.damageLevel) * 1.35);
    }

    private static double raptorEffectiveSpeed(RaptorRuntimeState state) {
        double species = state == null || state.roleInfo == null || state.roleInfo.info == null ? 1.0 : state.roleInfo.info.speedMultiplier;
        return 10.0 * species + (Math.max(0, state == null ? 0 : state.speedLevel) * 0.55);
    }

    private static int raptorXpToNext(int level) {
        int safeLevel = clampInt(level, 1, RAPTOR_MAX_LEVEL);
        return 80 + (safeLevel * 40);
    }

    private static void addRaptorXp(RaptorRuntimeState state, int amount) {
        if (state == null || amount <= 0 || state.level >= RAPTOR_MAX_LEVEL) {
            return;
        }
        state.xp = Math.max(0, state.xp + amount);
        while (state.level < RAPTOR_MAX_LEVEL && state.xp >= raptorXpToNext(state.level)) {
            state.xp -= raptorXpToNext(state.level);
            state.level++;
            state.unspentPoints++;
        }
        if (state.level >= RAPTOR_MAX_LEVEL) {
            state.xp = 0;
        }
    }

    private static String trainRaptor(RaptorRuntimeState state, long nowMs) {
        if (state == null) {
            return "Raptor unavailable.";
        }
        if (state.level >= RAPTOR_MAX_LEVEL) {
            return "Training complete. Max level reached.";
        }
        long remainingMs = Math.max(0L, state.nextTrainingAtMs - nowMs);
        if (remainingMs > 0L) {
            return "Training ready in " + formatCooldownLong(remainingMs) + ".";
        }
        int before = state.level;
        int needed = Math.max(1, raptorXpToNext(state.level) - state.xp);
        addRaptorXp(state, needed);
        state.nextTrainingAtMs = nowMs + RAPTOR_TRAINING_COOLDOWN_MS;
        return state.level > before ? "Training complete. Level up." : "Training complete.";
    }

    private static String raptorTrainButtonText(RaptorRuntimeState state, long nowMs) {
        if (state == null) {
            return "Train";
        }
        if (state.level >= RAPTOR_MAX_LEVEL) {
            return "Max";
        }
        long remainingMs = Math.max(0L, state.nextTrainingAtMs - nowMs);
        return remainingMs <= 0L ? "Train" : formatCooldownShort(remainingMs);
    }

    private static String upgradeRaptorStat(RaptorRuntimeState state, String stat) {
        if (state == null) {
            return "Raptor unavailable.";
        }
        if (state.unspentPoints <= 0) {
            return "No upgrade points available.";
        }
        String normalized = stat == null ? "" : stat.toLowerCase(Locale.ROOT);
        if ("damage".equals(normalized)) {
            if (state.damageLevel >= RAPTOR_MAX_UPGRADE_LEVEL) {
                return "Damage is already maxed.";
            }
            state.damageLevel++;
        } else if ("speed".equals(normalized)) {
            if (state.speedLevel >= RAPTOR_MAX_UPGRADE_LEVEL) {
                return "Speed is already maxed.";
            }
            state.speedLevel++;
        } else if ("stamina".equals(normalized)) {
            if (state.staminaLevel >= RAPTOR_MAX_UPGRADE_LEVEL) {
                return "Stamina is already maxed.";
            }
            state.staminaLevel++;
            state.stamina = clamp(state.stamina + 18.0, 0.0, raptorMaxStamina(state));
        } else {
            return "Unknown upgrade.";
        }
        state.unspentPoints--;
        return "Upgrade applied.";
    }

    private static void setRaptorOwner(RaptorRuntimeState state, Ref<EntityStore> playerEntityRef) {
        if (state == null || playerEntityRef == null) {
            return;
        }
        state.ownerRef = playerEntityRef;
        try {
            PlayerRef playerRef = playerEntityRef.getStore() == null ? null : playerEntityRef.getStore().getComponent(playerEntityRef, PlayerRef.getComponentType());
            state.ownerUuid = playerRef == null ? null : playerRef.getUuid();
        } catch (Throwable ignored) {
            state.ownerUuid = null;
        }
    }

    private static void rememberGuardPosition(Store<EntityStore> store, RaptorRuntimeState state) {
        if (state == null || state.ref == null) {
            return;
        }
        TransformComponent transform = getTransformSafely(store, state.ref);
        if (transform == null || transform.getPosition() == null) {
            return;
        }
        state.guardX = transform.getPosition().x();
        state.guardY = transform.getPosition().y();
        state.guardZ = transform.getPosition().z();
        state.hasGuardPosition = true;
    }

    private static String tryMountRaptor(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef, RaptorRuntimeState state) {
        if (playerEntityRef == null || store == null || player == null || playerRef == null || state == null || state.ref == null || !state.ref.isValid()) {
            return "Could not mount now.";
        }
        if (state.stamina < RAPTOR_MOUNT_STAMINA_COST) {
            return "Not enough stamina.";
        }
        try {
            NPCEntity npc = getNpcSafely(store, state.ref);
            Role role = npc == null ? null : npc.getRole();
            if (npc == null || role == null) {
                return "Mount role unavailable.";
            }
            Object mountType = getComponentTypeReflect("com.hypixel.hytale.builtin.mounts.NPCMountComponent");
            if (mountType == null) {
                return "Mount component unavailable.";
            }
            if (getComponentReflect(store, state.ref, mountType) != null) {
                return "Already mounted.";
            }
            NPCPlugin plugin = NPCPlugin.get();
            if (plugin == null) {
                return "NPC plugin unavailable.";
            }
            String roleName = resolveRoleName(npc);
            int originalRoleIndex = plugin.getIndex(roleName);
            int emptyRoleIndex = plugin.getIndex("Empty_Role");
            if (originalRoleIndex < 0 || emptyRoleIndex < 0) {
                debug("TFJ mount failed: originalRoleIndex=" + originalRoleIndex + ", emptyRoleIndex=" + emptyRoleIndex + ", role=" + roleName);
                return "Mount role index unavailable.";
            }
            Object mount = ensureComponentReflect(store, state.ref, mountType);
            if (mount == null) {
                return "Could not create mount.";
            }
            boolean pterodactylMount = isPterodactylState(state);
            invokeReflect(mount, "setOriginalRoleIndex", originalRoleIndex);
            invokeReflect(mount, "setOwnerPlayerRef", playerRef);
            if (pterodactylMount) {
                invokeReflect(mount, "setAnchor", 0.0f, 2.42f, -0.38f);
            } else {
                invokeReflect(mount, "setAnchor", 0.0f, 1.08f, 0.10f);
            }
            npc.playAnimation(state.ref, AnimationSlot.Status, null, store);
            if (!requestRoleChangeCompat(state.ref, role, emptyRoleIndex, false, store)) {
                return "Mount failed. Check TFJ logs.";
            }
            applyMountMovementConfig(playerEntityRef, playerRef, player, store, pterodactylMount ? "TFJ_Pterodactyl_Mount" : "Mount");
            if (pterodactylMount) {
                rememberActivePterodactylMount(playerRef, getNetworkId(store, state.ref));
                setPterodactylRiderFlightMovement(playerEntityRef, store, playerRef, true);
            }
            state.stamina = clamp(state.stamina - RAPTOR_MOUNT_STAMINA_COST, 0.0, raptorMaxStamina(state));
            state.lastMountedAtMs = System.currentTimeMillis();
            setRaptorOwner(state, playerEntityRef);
            addRaptorXp(state, 10);
            return "Mounted.";
        } catch (Throwable throwable) {
            debug("Could not mount TFJ adult raptor: " + describeThrowable(throwable));
            return "Mount failed. Check TFJ logs.";
        }
    }

    private static boolean isPterodactylState(RaptorRuntimeState state) {
        return state != null
            && state.roleInfo != null
            && state.roleInfo.info != null
            && "Pterodactyl".equals(state.roleInfo.info.suffix);
    }

    private static boolean isPterodactylInfo(EggInfo info) {
        return info != null && "Pterodactyl".equals(info.suffix);
    }

    private static boolean isTfjPterodactylMountComponent(Object npcMount) {
        int originalRoleIndex = asIntegerOrZero(invokeReflect(npcMount, "getOriginalRoleIndex"));
        return originalRoleIndex > 0
            && (originalRoleIndex == getTfjPterodactylRoleIndex()
                || originalRoleIndex == getTfjPterodactylMountRoleIndex());
    }

    private static int getTfjPterodactylRoleIndex() {
        int cached = tfjPterodactylRoleIndex;
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }
        int roleIndex = getNpcRoleIndex("TFJ_Pterodactyl");
        tfjPterodactylRoleIndex = roleIndex;
        return roleIndex;
    }

    private static int getTfjPterodactylMountRoleIndex() {
        int cached = tfjPterodactylMountRoleIndex;
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }
        int roleIndex = getNpcRoleIndex("TFJ_Pterodactyl_Mount");
        tfjPterodactylMountRoleIndex = roleIndex;
        return roleIndex;
    }

    private static int getNpcRoleIndex(String roleName) {
        try {
            NPCPlugin plugin = NPCPlugin.get();
            if (plugin == null || roleName == null || roleName.isBlank() || !plugin.hasRoleName(roleName)) {
                return -1;
            }
            return plugin.getIndex(roleName);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static void rememberActivePterodactylMount(PlayerRef riderRef, int mountNetworkId) {
        if (riderRef == null || riderRef.getUuid() == null || mountNetworkId == 0) {
            return;
        }
        UUID riderUuid = riderRef.getUuid();
        Integer previousMountId = PTERODACTYL_RIDER_ACTIVE_MOUNT_ID.put(riderUuid, mountNetworkId);
        long nowMs = System.currentTimeMillis();
        if (previousMountId == null || previousMountId != mountNetworkId || !PTERODACTYL_RIDER_ACTIVE_MOUNT_STARTED_MS.containsKey(riderUuid)) {
            PTERODACTYL_RIDER_ACTIVE_MOUNT_STARTED_MS.put(riderUuid, nowMs);
        }
        PTERODACTYL_RIDER_ACTIVE_MOUNT_SEEN_MS.put(riderUuid, nowMs);
    }

    private static void clearPterodactylMountState(int mountNetworkId) {
        if (mountNetworkId == 0) {
            return;
        }
        PTERODACTYL_FLIGHT_ACTIVE.remove(mountNetworkId);
        PTERODACTYL_FLIGHT_ANIMATION_ACTIVE.remove(mountNetworkId);
        PTERODACTYL_JUMP_HELD.remove(mountNetworkId);
        PTERODACTYL_JUMP_STARTED_MS.remove(mountNetworkId);
        PTERODACTYL_LAST_Y.remove(mountNetworkId);
        PTERODACTYL_LANDING_SINCE_MS.remove(mountNetworkId);
        PTERODACTYL_NEXT_ANIMATION_MS.remove(mountNetworkId);
        PTERODACTYL_LAST_DEBUG.remove(mountNetworkId);
    }

    private static void clearPterodactylRiderActiveMountState(UUID riderUuid) {
        if (riderUuid == null) {
            return;
        }
        PTERODACTYL_RIDER_ACTIVE_MOUNT_ID.remove(riderUuid);
        PTERODACTYL_RIDER_ACTIVE_MOUNT_SEEN_MS.remove(riderUuid);
        PTERODACTYL_RIDER_ACTIVE_MOUNT_STARTED_MS.remove(riderUuid);
        PTERODACTYL_RIDER_LAST_LOOK.remove(riderUuid);
        PTERODACTYL_RIDER_LAST_MOVEMENT.remove(riderUuid);
        PTERODACTYL_RIDER_LAST_INPUT_MS.remove(riderUuid);
    }

    private static PterodactylMoveInput recentPterodactylMoveInput(PlayerRef riderRef) {
        if (riderRef == null || riderRef.getUuid() == null) {
            return null;
        }
        long lastInputMs = PTERODACTYL_RIDER_LAST_INPUT_MS.getOrDefault(riderRef.getUuid(), 0L);
        if (lastInputMs == 0L || System.currentTimeMillis() - lastInputMs > 1000L) {
            return null;
        }
        return PTERODACTYL_RIDER_LAST_MOVEMENT.get(riderRef.getUuid());
    }

    private static PterodactylLookInput readPterodactylLookInput(Object inputUpdate) {
        Object direction = invokeReflect(inputUpdate, "direction");
        if (direction == null) {
            return null;
        }
        Double pitch = getNumberMemberOrNull(direction, "pitch");
        Double yaw = getNumberMemberOrNull(direction, "yaw");
        if (pitch == null || yaw == null) {
            return null;
        }
        double roll = getNumberMember(direction, "roll", 0.0);
        return new PterodactylLookInput(pitch, yaw, roll);
    }

    private static PterodactylMoveInput readPterodactylMoveInput(Object inputUpdate) {
        Object movementStates = invokeReflect(inputUpdate, "movementStates");
        return movementStates == null ? null : PterodactylMoveInput.fromStates(movementStates);
    }

    private static Object getMovementStates(Object movementStatesComponent) {
        return invokeReflect(movementStatesComponent, "getMovementStates");
    }

    private static void tickPterodactylFlightMotion(
        Ref<EntityStore> mountRef,
        Object componentAccessor,
        Object movementStatesComponent,
        TransformComponent mountTransform,
        TransformComponent riderTransform,
        HeadRotation riderHeadRotation,
        PterodactylLookInput capturedLook,
        PterodactylMoveInput capturedMovement,
        int mountNetworkId,
        float deltaTime
    ) {
        if (mountRef == null || componentAccessor == null || movementStatesComponent == null) {
            return;
        }
        Object states = getMovementStates(movementStatesComponent);
        if (states == null) {
            return;
        }
        PterodactylMoveInput input = capturedMovement == null ? PterodactylMoveInput.fromStates(states) : capturedMovement;
        boolean wantsUp = input.jumping || input.swimJumping;
        boolean wantsDown = input.crouching || input.forcedCrouching;
        boolean wasOnGround = getBooleanMember(states, "onGround");
        boolean jumpStarted = false;
        long nowMs = System.currentTimeMillis();
        if (mountNetworkId != 0) {
            if (wantsUp) {
                jumpStarted = PTERODACTYL_JUMP_HELD.add(mountNetworkId);
                PTERODACTYL_LANDING_SINCE_MS.remove(mountNetworkId);
                if (jumpStarted) {
                    PTERODACTYL_JUMP_STARTED_MS.put(mountNetworkId, nowMs);
                }
            } else {
                PTERODACTYL_JUMP_HELD.remove(mountNetworkId);
                PTERODACTYL_JUMP_STARTED_MS.remove(mountNetworkId);
            }
        }

        Vector3d forward = capturedLook == null ? null : capturedLook.toForward();
        if (forward == null) {
            forward = getHeadRotationForward(riderHeadRotation);
        }
        if (forward == null) {
            forward = getTransformForward(riderTransform == null ? mountTransform : riderTransform);
        }
        double forwardY = clamp(forward.y(), -0.85, 0.85);
        boolean lookingDownForLanding = forwardY < -0.55;
        boolean effectiveWantsDown = wantsDown || lookingDownForLanding;
        boolean nativeAirborne = getBooleanMember(states, "jumping")
            || getBooleanMember(states, "falling")
            || getBooleanMember(states, "fallingFar")
            || getBooleanMember(states, "gliding")
            || getBooleanMember(states, "flying")
            || !getBooleanMember(states, "onGround");
        if (mountNetworkId != 0 && (jumpStarted || (nativeAirborne && !effectiveWantsDown))) {
            PTERODACTYL_FLIGHT_ACTIVE.add(mountNetworkId);
        }
        boolean flightActive = mountNetworkId != 0 && PTERODACTYL_FLIGHT_ACTIVE.contains(mountNetworkId);
        double verticalSpeed = 0.0;
        if (flightActive) {
            setPterodactylFlightStates(states, true);
            invokeReflect(movementStatesComponent, "setMovementStates", states);

            if (effectiveWantsDown) {
                verticalSpeed = -6.0;
            } else if (wantsUp) {
                verticalSpeed = 6.5;
            } else {
                double deadzone = capturedLook == null ? 0.12 : 0.10;
                if (Math.abs(forwardY) > deadzone) {
                    verticalSpeed = clamp((forwardY - Math.copySign(deadzone, forwardY)) * 10.0, -5.0, 5.0);
                }
            }
        }
        verticalSpeed = clamp(verticalSpeed, -6.5, 7.0);

        Object velocity = getComponentReflect(componentAccessor, mountRef, getComponentTypeReflect("com.hypixel.hytale.server.core.modules.physics.component.Velocity"));
        double beforeY = getNumberMember(velocity, "y", getNumberMember(velocity, "getY", 0.0));
        if (velocity != null && flightActive) {
            invokeReflect(velocity, "setY", verticalSpeed);
            if (verticalSpeed > 0.1) {
                invokeReflect(velocity, "addForce", 0.0, 2.5, 0.0);
            } else if (verticalSpeed < -0.1) {
                invokeReflect(velocity, "addForce", 0.0, -2.5, 0.0);
            }
            invokeReflect(velocity, "setClient", getNumberMember(velocity, "x", getNumberMember(velocity, "getX", 0.0)), getNumberMember(velocity, "y", verticalSpeed), getNumberMember(velocity, "z", getNumberMember(velocity, "getZ", 0.0)));
        }

        if (mountTransform == null || mountTransform.getPosition() == null) {
            return;
        }
        double currentY = mountTransform.getPosition().y();
        Double previousY = mountNetworkId == 0 ? null : PTERODACTYL_LAST_Y.put(mountNetworkId, currentY);
        double yDelta = previousY == null ? 0.0 : currentY - previousY;
        boolean descentBlocked = verticalSpeed < -0.2 && Math.abs(yDelta) < 0.10;
        boolean landingCandidate = flightActive
            && !wantsUp
            && (wasOnGround || effectiveWantsDown)
            && (wasOnGround || (!getBooleanMember(states, "jumping") && !getBooleanMember(states, "falling") && !getBooleanMember(states, "fallingFar") && Math.abs(yDelta) < 0.08));
        if (mountNetworkId != 0 && landingCandidate) {
            long landingSince = PTERODACTYL_LANDING_SINCE_MS.computeIfAbsent(mountNetworkId, ignored -> nowMs);
            if (nowMs - landingSince >= 250L) {
                PTERODACTYL_FLIGHT_ACTIVE.remove(mountNetworkId);
                PTERODACTYL_FLIGHT_ANIMATION_ACTIVE.remove(mountNetworkId);
                PTERODACTYL_NEXT_ANIMATION_MS.remove(mountNetworkId);
                PTERODACTYL_JUMP_HELD.remove(mountNetworkId);
                PTERODACTYL_JUMP_STARTED_MS.remove(mountNetworkId);
                PTERODACTYL_LANDING_SINCE_MS.remove(mountNetworkId);
                setPterodactylFlightStates(states, false);
                invokeReflect(movementStatesComponent, "setMovementStates", states);
                if (velocity != null) {
                    invokeReflect(velocity, "setY", 0.0);
                    invokeReflect(velocity, "setClient", getNumberMember(velocity, "x", getNumberMember(velocity, "getX", 0.0)), 0.0, getNumberMember(velocity, "z", getNumberMember(velocity, "getZ", 0.0)));
                }
                PTERODACTYL_LAST_DEBUG.put(mountNetworkId, String.format(Locale.ROOT, "landed forwardY=%.3f y=%.2f yDelta=%.3f down=%s lookDown=%s descentBlocked=%s", forwardY, currentY, yDelta, wantsDown, lookingDownForLanding, descentBlocked));
                return;
            }
        } else if (mountNetworkId != 0) {
            PTERODACTYL_LANDING_SINCE_MS.remove(mountNetworkId);
        }

        boolean shouldAnimateFlight = getBooleanMember(states, "jumping")
            || getBooleanMember(states, "falling")
            || getBooleanMember(states, "fallingFar")
            || getBooleanMember(states, "gliding")
            || (getBooleanMember(states, "flying") && !getBooleanMember(states, "onGround"))
            || (flightActive && (!wasOnGround || wantsUp || effectiveWantsDown || Math.abs(verticalSpeed) > 0.15));
        if (mountNetworkId != 0) {
            if (shouldAnimateFlight) {
                PTERODACTYL_FLIGHT_ANIMATION_ACTIVE.add(mountNetworkId);
            } else {
                PTERODACTYL_FLIGHT_ANIMATION_ACTIVE.remove(mountNetworkId);
            }
        }
        if (!flightActive) {
            if (mountNetworkId != 0) {
                PTERODACTYL_LAST_DEBUG.put(mountNetworkId, String.format(Locale.ROOT, "grounded forwardY=%.3f jump=%s down=%s nativeAirborne=%s y=%.2f", forwardY, wantsUp, effectiveWantsDown, nativeAirborne, currentY));
            }
            return;
        }

        if (velocity == null && Math.abs(verticalSpeed) > 0.1) {
            double step = verticalSpeed * Math.min(Math.max(deltaTime, 0.0f), 0.05f);
            Vector3d position = mountTransform.getPosition();
            invokeReflect(mountTransform, "setPosition", new Vector3d(position.x(), position.y() + step, position.z()));
        }
        if (mountNetworkId != 0) {
            PTERODACTYL_LAST_DEBUG.put(mountNetworkId, String.format(Locale.ROOT, "targetY=%.2f beforeY=%.2f afterY=%.2f forwardY=%.3f jump=%s down=%s yDelta=%.3f y=%.2f", verticalSpeed, beforeY, velocity == null ? verticalSpeed : getNumberMember(velocity, "y", verticalSpeed), forwardY, wantsUp, effectiveWantsDown, yDelta, currentY));
        }
    }

    private static void setPterodactylFlightStates(Object states, boolean flying) {
        setBooleanMember(states, "flying", flying);
        setBooleanMember(states, "gliding", false);
        setBooleanMember(states, "falling", false);
        setBooleanMember(states, "fallingFar", false);
        setBooleanMember(states, "jumping", false);
        setBooleanMember(states, "swimJumping", false);
        setBooleanMember(states, "walking", false);
        setBooleanMember(states, "running", false);
        setBooleanMember(states, "sprinting", false);
        setBooleanMember(states, "onGround", !flying);
    }

    private static void playPterodactylFlightAnimation(Ref<EntityStore> mountRef, NPCEntity npc, Store<EntityStore> store, int mountNetworkId, Object movementStates) {
        if (mountRef == null || npc == null || store == null || mountNetworkId == 0) {
            return;
        }
        boolean flightAnimationActive = PTERODACTYL_FLIGHT_ANIMATION_ACTIVE.contains(mountNetworkId);
        boolean grounded = movementStates != null
            && getBooleanMember(movementStates, "onGround")
            && !getBooleanMember(movementStates, "flying")
            && !getBooleanMember(movementStates, "gliding")
            && !getBooleanMember(movementStates, "jumping")
            && !getBooleanMember(movementStates, "falling")
            && !getBooleanMember(movementStates, "fallingFar");
        long nowMs = System.currentTimeMillis();
        long nextMs = PTERODACTYL_NEXT_ANIMATION_MS.getOrDefault(mountNetworkId, 0L);
        if (nextMs > nowMs) {
            return;
        }
        PTERODACTYL_NEXT_ANIMATION_MS.put(mountNetworkId, nowMs + 550L);
        String animation = (flightAnimationActive || !grounded) ? "Fly" : "Idle";
        try {
            npc.playAnimation(mountRef, AnimationSlot.Movement, animation, store);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not play TFJ pterodactyl flight animation: " + describeThrowable(throwable));
        }
    }

    private static void setPterodactylRiderFlightMovement(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, PlayerRef playerRef, boolean enabled) {
        if (playerEntityRef == null || store == null || playerRef == null) {
            return;
        }
        Object movementManager = getComponentReflect(store, playerEntityRef, getComponentTypeReflect("com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager"));
        Object settings = invokeReflect(movementManager, "getSettings");
        if (settings == null) {
            return;
        }
        boolean changed = enabled ? enablePterodactylFlightSettings(settings) : disablePterodactylFlightSettings(settings);
        if (changed) {
            invokeReflect(movementManager, "update", invokeReflect(playerRef, "getPacketHandler"));
        }
    }

    private static boolean enablePterodactylFlightSettings(Object settings) {
        boolean changed = false;
        changed |= setBooleanMemberIfDifferent(settings, "canFly", true);
        changed |= setNumberMemberIfDifferent(settings, "mass", 0.35);
        changed |= setNumberMemberIfDifferent(settings, "dragCoefficient", 0.95);
        changed |= setNumberMemberIfDifferent(settings, "velocityResistance", 0.02);
        changed |= setNumberMemberIfDifferent(settings, "jumpForce", 7.5);
        changed |= setNumberMemberIfDifferent(settings, "swimJumpForce", 7.5);
        changed |= setNumberMemberIfDifferent(settings, "jumpBufferDuration", 0.15);
        changed |= setNumberMemberIfDifferent(settings, "jumpBufferMaxYVelocity", 0.4);
        changed |= setNumberMemberIfDifferent(settings, "horizontalFlySpeed", 16.0);
        changed |= setNumberMemberIfDifferent(settings, "verticalFlySpeed", 4.5);
        changed |= setNumberMemberIfDifferent(settings, "maxSpeedMultiplier", 14.0);
        changed |= setNumberMemberIfDifferent(settings, "airControlMinMultiplier", 1.25);
        changed |= setNumberMemberIfDifferent(settings, "airControlMaxMultiplier", 4.0);
        changed |= setNumberMemberIfDifferent(settings, "airSpeedMultiplier", 1.5);
        changed |= setNumberMemberIfDifferent(settings, "airDragMin", 1.0);
        changed |= setNumberMemberIfDifferent(settings, "airDragMax", 1.0);
        changed |= setNumberMemberIfDifferent(settings, "airFrictionMin", 0.0);
        changed |= setNumberMemberIfDifferent(settings, "airFrictionMax", 0.0);
        changed |= setNumberMemberIfDifferent(settings, "variableJumpFallForce", 4.0);
        changed |= setNumberMemberIfDifferent(settings, "fallJumpForce", 7.5);
        changed |= setNumberMemberIfDifferent(settings, "fallMomentumLoss", 0.1);
        changed |= setNumberMemberIfDifferent(settings, "wishDirectionGravityY", 0.0);
        changed |= setNumberMemberIfDifferent(settings, "wishDirectionWeightY", 1.0);
        return changed;
    }

    private static boolean disablePterodactylFlightSettings(Object settings) {
        return setBooleanMemberIfDifferent(settings, "canFly", false);
    }

    private static void resetPterodactylRiderMountState(Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
        if (playerEntityRef == null || store == null || !playerEntityRef.isValid()) {
            return;
        }
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        invokeReflect(player, "setMountEntityId", 0);
        Object playerInput = getComponentReflect(store, playerEntityRef, getComponentTypeReflect("com.hypixel.hytale.server.core.modules.entity.player.PlayerInput"));
        invokeReflect(playerInput, "setMountId", 0);
        Object movementManager = getComponentReflect(store, playerEntityRef, getComponentTypeReflect("com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager"));
        if (!invokeReflectSuccess(movementManager, "resetDefaultsAndUpdate", playerEntityRef, store)) {
            Object settings = invokeReflect(movementManager, "getSettings");
            if (disablePterodactylFlightSettings(settings)) {
                PlayerRef playerRef = getPlayerRefSafely(store, playerEntityRef);
                invokeReflect(movementManager, "update", invokeReflect(playerRef, "getPacketHandler"));
            }
        }
    }

    private static int getNetworkId(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        Object networkId = getComponentReflect(store, entityRef, getComponentTypeReflect("com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId"));
        return asIntegerOrZero(invokeReflect(networkId, "getId"));
    }

    private static PlayerRef asPlayerRef(Object value) {
        return value instanceof PlayerRef playerRef ? playerRef : null;
    }

    private static int asIntegerOrZero(Object value) {
        Integer integer = asInteger(value);
        return integer == null ? 0 : integer;
    }

    private static void logPterodactylFlightDebug(Store<EntityStore> store, String message, Object... args) {
        long currentSecond = System.currentTimeMillis() / 1000L;
        if (lastPterodactylFlightDebugSecond == currentSecond) {
            return;
        }
        lastPterodactylFlightDebugSecond = currentSecond;
        debug(String.format(Locale.ROOT, message, args));
    }

    private static boolean requestRoleChangeCompat(Ref<EntityStore> npcRef, Role role, int roleIndex, boolean preserveState, Store<EntityStore> store) {
        Method fallback = null;
        for (Method method : RoleChangeSystem.class.getMethods()) {
            if (!"requestRoleChange".equals(method.getName())) {
                continue;
            }
            int parameterCount = method.getParameterCount();
            if (parameterCount == 7) {
                try {
                    method.invoke(null, npcRef, role, roleIndex, preserveState, null, null, store);
                    return true;
                } catch (Exception exception) {
                    debug("TFJ adult mount long role-change failed: " + describeThrowable(exception));
                }
            } else if (parameterCount == 5) {
                fallback = method;
            }
        }
        if (fallback != null) {
            try {
                fallback.invoke(null, npcRef, role, roleIndex, preserveState, store);
                return true;
            } catch (Exception exception) {
                debug("TFJ adult mount short role-change failed: " + describeThrowable(exception));
            }
        } else {
            debug("TFJ adult mount role-change unavailable.");
        }
        return false;
    }

    private static void applyMountMovementConfig(Ref<EntityStore> playerEntityRef, PlayerRef playerRef, Player player, Store<EntityStore> store, String movementConfigId) {
        if (playerEntityRef == null || playerRef == null || player == null || store == null) {
            return;
        }
        try {
            Object physicsType = getComponentTypeReflect("com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues");
            Object movementManagerType = getComponentTypeReflect("com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager");
            Object physicsValues = physicsType == null ? null : getComponentReflect(store, playerEntityRef, physicsType);
            Object movementManager = movementManagerType == null ? null : getComponentReflect(store, playerEntityRef, movementManagerType);
            if (physicsValues == null || movementManager == null) {
                return;
            }
            Class<?> movementConfigClass = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.movement.MovementConfig");
            Object assetMap = movementConfigClass.getMethod("getAssetMap").invoke(null);
            Object movementConfig = assetMap == null ? null : assetMap.getClass().getMethod("getAsset", String.class).invoke(assetMap, movementConfigId == null || movementConfigId.isBlank() ? "Mount" : movementConfigId);
            if (movementConfig == null) {
                return;
            }
            Object packet = movementConfig.getClass().getMethod("toPacket").invoke(movementConfig);
            Object gameMode = invokeReflect(player, "getGameMode");
            invokeReflect(movementManager, "setDefaultSettings", packet, physicsValues, gameMode);
            invokeReflect(movementManager, "applyDefaultSettings");
            Object packetHandler = invokeReflect(playerRef, "getPacketHandler");
            invokeReflect(movementManager, "update", packetHandler);
        } catch (Throwable throwable) {
            debug("Could not apply TFJ mount movement config: " + describeThrowable(throwable));
        }
    }

    private static Object getComponentTypeReflect(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getMethod("getComponentType").invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getComponentReflect(Store<EntityStore> store, Ref<EntityStore> ref, Object componentType) {
        return getComponentReflect((Object) store, ref, componentType);
    }

    private static Object getComponentReflect(Object componentAccessor, Ref<EntityStore> ref, Object componentType) {
        if (componentAccessor == null || ref == null || componentType == null) {
            return null;
        }
        try {
            for (Method method : componentAccessor.getClass().getMethods()) {
                if (!"getComponent".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                try {
                    return method.invoke(componentAccessor, ref, componentType);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void removeComponentReflect(Object componentAccessor, Ref<EntityStore> ref, Object componentType) {
        if (componentAccessor == null || ref == null || componentType == null) {
            return;
        }
        try {
            for (Method method : componentAccessor.getClass().getMethods()) {
                if (!("tryRemoveComponent".equals(method.getName()) || "removeComponent".equals(method.getName())) || method.getParameterCount() != 2) {
                    continue;
                }
                try {
                    method.invoke(componentAccessor, ref, componentType);
                    return;
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object ensureComponentReflect(Store<EntityStore> store, Ref<EntityStore> ref, Object componentType) {
        if (store == null || ref == null || componentType == null) {
            return null;
        }
        try {
            for (Method method : store.getClass().getMethods()) {
                if (!"ensureAndGetComponent".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                try {
                    return method.invoke(store, ref, componentType);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object invokeReflect(Object target, String methodName, Object... args) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        int count = args == null ? 0 : args.length;
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!methodName.equals(method.getName()) || method.getParameterCount() != count) {
                    continue;
                }
                try {
                    return method.invoke(target, args == null ? new Object[0] : args);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean invokeReflectSuccess(Object target, String methodName, Object... args) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return false;
        }
        int count = args == null ? 0 : args.length;
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!methodName.equals(method.getName()) || method.getParameterCount() != count) {
                    continue;
                }
                try {
                    method.invoke(target, args == null ? new Object[0] : args);
                    return true;
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Throwable throwable) {
            debug("TFJ reflection failed: " + methodName + " on " + target.getClass().getName() + " -> " + describeThrowable(throwable));
        }
        return false;
    }

    private static PlayerRef getPlayerRefSafely(Store<EntityStore> store, Ref<EntityStore> playerEntityRef) {
        if (store == null || playerEntityRef == null) {
            return null;
        }
        try {
            return store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean getBooleanMember(Object target, String memberName) {
        if (target == null || memberName == null || memberName.isBlank()) {
            return false;
        }
        try {
            Field field = target.getClass().getField(memberName);
            Object value = field.get(target);
            return value instanceof Boolean booleanValue && booleanValue;
        } catch (Throwable ignored) {
        }
        Object value = invokeReflect(target, memberName);
        if (!(value instanceof Boolean)) {
            value = invokeReflect(target, "is" + capitalize(memberName));
        }
        if (!(value instanceof Boolean)) {
            value = invokeReflect(target, "get" + capitalize(memberName));
        }
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private static void setBooleanMember(Object target, String memberName, boolean value) {
        if (target == null || memberName == null || memberName.isBlank()) {
            return;
        }
        try {
            Field field = target.getClass().getField(memberName);
            field.setBoolean(target, value);
            return;
        } catch (Throwable ignored) {
        }
        invokeReflect(target, "set" + capitalize(memberName), value);
    }

    private static boolean setBooleanMemberIfDifferent(Object target, String memberName, boolean value) {
        boolean previous = getBooleanMember(target, memberName);
        if (previous == value) {
            return false;
        }
        setBooleanMember(target, memberName, value);
        return getBooleanMember(target, memberName) == value;
    }

    private static Double getNumberMemberOrNull(Object target, String memberName) {
        if (target == null || memberName == null || memberName.isBlank()) {
            return null;
        }
        try {
            Field field = target.getClass().getField(memberName);
            Object value = field.get(target);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Throwable ignored) {
        }
        Object value = memberName.startsWith("get") ? invokeReflect(target, memberName) : null;
        if (!(value instanceof Number)) {
            value = invokeReflect(target, memberName);
        }
        if (!(value instanceof Number)) {
            value = invokeReflect(target, "get" + capitalize(memberName));
        }
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static double getNumberMember(Object target, String memberName, double fallback) {
        Double value = getNumberMemberOrNull(target, memberName);
        return value == null ? fallback : value;
    }

    private static boolean setNumberMemberIfDifferent(Object target, String memberName, double value) {
        Double previous = getNumberMemberOrNull(target, memberName);
        if (previous != null && Math.abs(previous - value) < 0.0001) {
            return false;
        }
        boolean updated = false;
        try {
            Field field = target.getClass().getField(memberName);
            Class<?> type = field.getType();
            if (type == float.class || type == Float.class) {
                field.setFloat(target, (float) value);
            } else if (type == double.class || type == Double.class) {
                field.setDouble(target, value);
            } else if (type == int.class || type == Integer.class) {
                field.setInt(target, (int) Math.round(value));
            } else if (Number.class.isAssignableFrom(type)) {
                field.set(target, value);
            } else {
                return false;
            }
            updated = true;
        } catch (Throwable ignored) {
        }
        if (!updated) {
            Object result = invokeReflect(target, "set" + capitalize(memberName), value);
            updated = result != null || previous != null;
        }
        return updated;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean openRaptorCarePage(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef, Ref<EntityStore> raptorRef) {
        if (playerEntityRef == null || store == null || player == null || playerRef == null || raptorRef == null) {
            return false;
        }
        NPCEntity npc = getNpcSafely(store, raptorRef);
        RaptorRoleInfo roleInfo = getRaptorRoleInfo(npc);
        if (roleInfo == null) {
            return false;
        }
        try {
            if (player.getPageManager() == null) {
                return false;
            }
            long nowMs = System.currentTimeMillis();
            RaptorRuntimeState state = getOrCreateRaptorState(store, worldKey(getWorld(store)), raptorRef, npc, roleInfo, nowMs);
            syncRaptorHealthFromStats(store, raptorRef, state);
            if (roleInfo.isBreedingStage()) {
                player.getPageManager().openCustomPage(playerEntityRef, store, new RaptorAdultPage(playerRef, state.key));
            } else {
                player.getPageManager().openCustomPage(playerEntityRef, store, new RaptorCarePage(playerRef, state.key));
            }
            return true;
        } catch (Throwable throwable) {
            debug("Could not open TFJ raptor care page: " + describeThrowable(throwable));
            return false;
        }
    }

    private static boolean openRaptorGuidePage(Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Player player, PlayerRef playerRef) {
        if (playerEntityRef == null || store == null || player == null || playerRef == null) {
            return false;
        }
        try {
            if (player.getPageManager() == null) {
                return false;
            }
            player.getPageManager().openCustomPage(playerEntityRef, store, new RaptorGuidePage(playerRef));
            return true;
        } catch (Throwable throwable) {
            debug("Could not open TFJ raptor guide page: " + describeThrowable(throwable));
            return false;
        }
    }

    private static boolean handleRaptorCareInteraction(
        Ref<EntityStore> playerEntityRef,
        Player player,
        PlayerRef playerRef,
        Store<EntityStore> store,
        LookedRaptor lookedRaptor,
        long nowMs,
        boolean debugNow
    ) {
        if (playerEntityRef == null || player == null || playerRef == null || store == null || lookedRaptor == null || lookedRaptor.ref == null) {
            return false;
        }
        UUID playerUuid = playerRef.getUuid();
        clearRaptorCarePrompt(playerUuid, playerEntityRef, store, lookedRaptor.ref, nowMs);

        NPCEntity npc = getNpcSafely(store, lookedRaptor.ref);
        if (!setRaptorCareInteractable(npc, lookedRaptor.ref, playerEntityRef, store, true, nowMs, debugNow)) {
            return false;
        }
        if (!consumeRaptorCareInteraction(npc, playerEntityRef, nowMs, debugNow)) {
            return false;
        }

        String openKey = String.valueOf(playerUuid) + "|" + String.valueOf(lookedRaptor.ref);
        Long previous = RAPTOR_CARE_LAST_OPEN_MS.put(openKey, nowMs);
        if (previous != null && nowMs - previous < RAPTOR_CARE_OPEN_COOLDOWN_MS) {
            return true;
        }
        boolean opened = openRaptorCarePage(playerEntityRef, store, player, playerRef, lookedRaptor.ref);
        if (debugNow) {
            debug("Raptor care interaction consumed for " + lookedRaptor.state.roleInfo.role + "; opened=" + opened + ".");
        }
        return opened;
    }

    private static void clearRaptorCarePrompt(UUID playerUuid, Ref<EntityStore> playerEntityRef, Store<EntityStore> store, Ref<EntityStore> keepRef, long nowMs) {
        if (playerUuid == null || playerEntityRef == null) {
            return;
        }
        Ref<EntityStore> previousRef = RAPTOR_CARE_PROMPT_BY_PLAYER.get(playerUuid);
        if (previousRef != null && !sameEntityRef(previousRef, keepRef)) {
            NPCEntity previousNpc = getNpcSafely(store, previousRef);
            setRaptorCareInteractable(previousNpc, previousRef, playerEntityRef, store, false, nowMs, false);
        }
        if (keepRef == null) {
            RAPTOR_CARE_PROMPT_BY_PLAYER.remove(playerUuid);
        } else {
            RAPTOR_CARE_PROMPT_BY_PLAYER.put(playerUuid, keepRef);
        }
    }

    private static boolean sameEntityRef(Ref<EntityStore> left, Ref<EntityStore> right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return String.valueOf(left).equals(String.valueOf(right));
    }

    private static Object getStateSupportSafely(Role role, long nowMs, boolean debugNow) {
        if (role == null) {
            return null;
        }
        try {
            return role.getClass().getMethod("getStateSupport").invoke(role);
        } catch (Throwable throwable) {
            if (debugNow) {
                debugGlobal(nowMs, "Could not resolve NPC StateSupport: " + describeThrowable(throwable));
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Ref<EntityStore> getInteractionIterationTargetSafely(Object stateSupport, long nowMs, boolean debugNow) {
        if (stateSupport == null) {
            return null;
        }
        try {
            Object target = stateSupport.getClass().getMethod("getInteractionIterationTarget").invoke(stateSupport);
            return target instanceof Ref<?> ? (Ref<EntityStore>) target : null;
        } catch (Throwable throwable) {
            if (debugNow) {
                debugGlobal(nowMs, "Could not resolve NPC interaction target: " + describeThrowable(throwable));
            }
            return null;
        }
    }

    private static boolean setRaptorCareInteractable(
        NPCEntity npc,
        Ref<EntityStore> raptorRef,
        Ref<EntityStore> playerEntityRef,
        Store<EntityStore> store,
        boolean interactable,
        long nowMs,
        boolean debugNow
    ) {
        if (npc == null || raptorRef == null || playerEntityRef == null || store == null) {
            return false;
        }
        Role role = npc.getRole();
        Object stateSupport = getStateSupportSafely(role, nowMs, debugNow);
        try {
            if (stateSupport == null) {
                return false;
            }
            Method setInteractable = stateSupport.getClass().getMethod(
                "setInteractable",
                Ref.class,
                Ref.class,
                boolean.class,
                String.class,
                boolean.class,
                Store.class
            );
            setInteractable.invoke(
                stateSupport,
                raptorRef,
                playerEntityRef,
                interactable,
                interactable ? RAPTOR_CARE_INTERACTION_HINT : null,
                interactable,
                store
            );
            return true;
        } catch (Throwable throwable) {
            if (debugNow || interactable) {
                debugGlobal(nowMs, "Could not update TFJ raptor care interactable prompt: " + describeThrowable(throwable));
            }
            return false;
        }
    }

    private static boolean consumeRaptorCareInteraction(NPCEntity npc, Ref<EntityStore> playerEntityRef, long nowMs, boolean debugNow) {
        if (npc == null || playerEntityRef == null) {
            return false;
        }
        Role role = npc.getRole();
        Object stateSupport = getStateSupportSafely(role, nowMs, debugNow);
        try {
            if (stateSupport == null) {
                return false;
            }
            Method consumeInteraction = stateSupport.getClass().getMethod("consumeInteraction", Ref.class);
            return Boolean.TRUE.equals(consumeInteraction.invoke(stateSupport, playerEntityRef));
        } catch (Throwable throwable) {
            if (debugNow) {
                debugGlobal(nowMs, "Could not consume TFJ raptor care interaction: " + describeThrowable(throwable));
            }
            return false;
        }
    }



    private static int fastForwardRaptorsInWorld(String currentWorldKey, boolean includeGrowth, boolean includeBreeding, long nowMs, long remainingMs) {
        long safeRemaining = Math.max(1_000L, remainingMs);
        int affected = 0;
        Iterator<Map.Entry<String, RaptorRuntimeState>> iterator = KNOWN_RAPTORS.entrySet().iterator();
        while (iterator.hasNext()) {
            RaptorRuntimeState state = iterator.next().getValue();
            if (state == null || !safeEquals(currentWorldKey, state.worldKey)) {
                continue;
            }
            if (nowMs - state.lastSeenMs > STALE_RAPTOR_REMOVE_MS) {
                RAPTOR_SEX_BY_KEY.remove(state.key);
                iterator.remove();
                continue;
            }
            boolean eligible = (includeGrowth && state.roleInfo.isGrowthStage()) || (includeBreeding && state.roleInfo.isBreedingStage());
            if (!eligible) {
                continue;
            }
            state.startedAtMs = Math.max(0L, nowMs - Math.max(0L, state.roleInfo.durationMs - safeRemaining));
            state.completeAtMs = nowMs + safeRemaining;
            state.nextTransitionAttemptMs = state.completeAtMs;
            state.lastCareTickMs = nowMs;
            if (state.roleInfo.isGrowthStage()) {
                state.foodLevel = RAPTOR_FOOD_MAX;
                state.starving = false;
                state.nextStarveDamageMs = 0L;
                state.nextAutoFeedMs = 0L;
            }
            state.transitionPending = false;
            affected++;
        }
        debug("Fast-forwarded " + affected + " TFJ raptor lifecycle state(s) in world " + currentWorldKey + " to " + (safeRemaining / 1000L) + "s remaining.");
        return affected;
    }

    private static boolean advanceRaptorRole(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRoleInfo roleInfo, RaptorRuntimeState state, long nowMs) {
        if (store == null || raptorRef == null || roleInfo == null || state == null || roleInfo.nextRole == null || roleInfo.nextRole.isBlank()) {
            return false;
        }
        NPCEntity npc = getNpcSafely(store, raptorRef);
        RaptorRoleInfo currentRoleInfo = getRaptorRoleInfo(npc);
        if (npc == null || currentRoleInfo != roleInfo) {
            debugGlobal(nowMs, "Skipped TFJ raptor role advance; current role changed from " + roleInfo.role + " to " + (currentRoleInfo == null ? resolveRoleName(npc) : currentRoleInfo.role) + ".");
            return false;
        }
        TransformComponent transform;
        try {
            transform = store.getComponent(raptorRef, TransformComponent.getComponentType());
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not read transform before replacing " + roleInfo.role + ": " + describeThrowable(throwable));
            return false;
        }
        if (transform == null || transform.getPosition() == null) {
            debugGlobal(nowMs, "Missing transform before replacing " + roleInfo.role + ".");
            return false;
        }
        NPCPlugin plugin;
        try {
            plugin = NPCPlugin.get();
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "NPCPlugin.get failed while replacing " + roleInfo.role + ": " + describeThrowable(throwable));
            return false;
        }
        if (plugin == null) {
            debugGlobal(nowMs, "NPCPlugin is not ready; keeping " + roleInfo.role + " at 00:00 for retry.");
            return false;
        }
        int targetRoleIndex;
        try {
            targetRoleIndex = plugin.getIndex(roleInfo.nextRole);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not read next raptor role index for " + roleInfo.nextRole + ": " + describeThrowable(throwable));
            return false;
        }
        if (targetRoleIndex < 0) {
            debugGlobal(nowMs, "Missing next raptor role " + roleInfo.nextRole + " while replacing " + roleInfo.role + "; targetIndex=" + targetRoleIndex + ".");
            return false;
        }
        Vector3d spawnPosition = new Vector3d(transform.getPosition());
        Rotation3f rotation = transform.getRotation() == null ? new Rotation3f() : transform.getRotation();
        RaptorSex inheritedSex = state.sex;
        Object spawned;
        try {
            spawned = plugin.spawnEntity(store, targetRoleIndex, spawnPosition, rotation, null, null);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "spawnEntity replacement failed for " + roleInfo.role + " -> " + roleInfo.nextRole + " at " + formatVector(spawnPosition) + ": " + describeThrowable(throwable));
            return false;
        }
        if (spawned == null) {
            debugGlobal(nowMs, "spawnEntity replacement returned null for " + roleInfo.role + " -> " + roleInfo.nextRole + ".");
            return false;
        }
        Ref<EntityStore> spawnedRef = extractSpawnedEntityRef(spawned);
        RaptorRuntimeState replacementState = null;
        if (spawnedRef != null) {
            NPCEntity spawnedNpc = getNpcSafely(store, spawnedRef);
            replacementState = createReplacementRaptorState(state, spawnedRef, spawnedNpc, roleInfo.nextRole, inheritedSex, nowMs);
        }
        try {
            store.removeEntity(raptorRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            KNOWN_RAPTORS.remove(state.key);
            RAPTOR_SEX_BY_KEY.remove(state.key);
            rememberReplacementRaptor(replacementState, inheritedSex, store, spawnedRef, nowMs);
            debug("Replaced TFJ raptor " + roleInfo.role + " -> " + roleInfo.nextRole + " at " + formatVector(spawnPosition) + ".");
            return true;
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Spawned replacement but could not remove old " + roleInfo.role + ": " + describeThrowable(throwable));
            try {
                npc.setToDespawn();
            } catch (Throwable ignored) {
            }
            KNOWN_RAPTORS.remove(state.key);
            RAPTOR_SEX_BY_KEY.remove(state.key);
            return true;
        }
    }

    private static boolean forceRaptorAppearance(Store<EntityStore> store, Ref<EntityStore> raptorRef, String appearance, long nowMs) {
        if (store == null || raptorRef == null || !raptorRef.isValid() || appearance == null || appearance.isBlank()) {
            return false;
        }
        boolean appearanceChanged = false;
        try {
            appearanceChanged = NPCEntity.setAppearance(raptorRef, appearance, store);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not force TFJ raptor appearance " + appearance + ": " + describeThrowable(throwable));
        }
        RaptorRoleInfo visualInfo = RAPTOR_BY_ROLE.get(normalizeRoleKey(appearance));
        boolean modelChanged = forceRaptorModel(store, raptorRef, visualInfo, nowMs);
        debug("Forced TFJ raptor visuals appearance=" + appearance + ", appearanceChanged=" + appearanceChanged + ", modelChanged=" + modelChanged + ".");
        return appearanceChanged || modelChanged;
    }

    private static boolean forceRaptorModel(Store<EntityStore> store, Ref<EntityStore> raptorRef, RaptorRoleInfo roleInfo, long nowMs) {
        if (store == null || raptorRef == null || !raptorRef.isValid() || roleInfo == null) {
            return false;
        }
        try {
            ModelAsset modelAsset = ModelAsset.getAssetMap() == null ? null : ModelAsset.getAssetMap().getAsset(roleInfo.role);
            if (modelAsset == null) {
                debugGlobal(nowMs, "Could not resolve TFJ raptor ModelAsset for " + roleInfo.role + ".");
                return false;
            }
            ModelComponent currentComponent = store.getComponent(raptorRef, ModelComponent.getComponentType());
            Model currentModel = currentComponent == null ? null : currentComponent.getModel();
            Map<String, String> attachments = currentModel == null ? null : currentModel.getRandomAttachmentIds();
            Model scaled = attachments == null || attachments.isEmpty()
                ? Model.createScaledModel(modelAsset, (float) roleInfo.visualScale)
                : Model.createScaledModel(modelAsset, (float) roleInfo.visualScale, attachments);
            if (scaled == null) {
                debugGlobal(nowMs, "Model.createScaledModel returned null for " + roleInfo.role + " scale=" + roleInfo.visualScale + ".");
                return false;
            }
            store.putComponent(raptorRef, ModelComponent.getComponentType(), new ModelComponent(scaled));
            NPCEntity npc = getNpcSafely(store, raptorRef);
            if (npc != null && npc.getRole() != null) {
                npc.getRole().updateMotionControllers(raptorRef, scaled, scaled.getBoundingBox(), store);
            }
            return true;
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not force TFJ raptor model for " + roleInfo.role + " scale=" + roleInfo.visualScale + ": " + describeThrowable(throwable));
            return false;
        }
    }

    private static RaptorRoleInfo getRaptorRoleInfo(NPCEntity npc) {
        String role = resolveRoleName(npc);
        if (role == null || role.isBlank()) {
            return null;
        }
        return RAPTOR_BY_ROLE.get(normalizeRoleKey(role));
    }

    private static String resolveRoleName(NPCEntity npc) {
        if (npc == null) {
            return "";
        }
        try {
            String roleName = npc.getRoleName();
            if (roleName != null && !roleName.isBlank()) {
                return roleName;
            }
        } catch (Throwable ignored) {
        }
        try {
            NPCPlugin plugin = NPCPlugin.get();
            if (plugin != null && npc.getRoleIndex() >= 0) {
                String roleName = plugin.getName(npc.getRoleIndex());
                return roleName == null ? "" : roleName;
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static String raptorKey(Ref<EntityStore> ref, NPCEntity npc) {
        if (npc != null) {
            try {
                UUID uuid = npc.getUuid();
                if (uuid != null) {
                    return uuid.toString();
                }
            } catch (Throwable ignored) {
            }
        }
        return ref == null ? "unknown" : ref.toString();
    }

    private static String raptorDisplayName(RaptorRuntimeState raptor) {
        if (raptor == null || raptor.roleInfo == null || raptor.roleInfo.info == null) {
            return "";
        }
        if (raptor.roleInfo.isBreedingStage() || "Adult".equalsIgnoreCase(raptor.roleInfo.stageLabel)) {
            return raptor.roleInfo.info.displayName;
        }
        return raptor.roleInfo.stageLabel + " " + raptor.roleInfo.info.displayName;
    }

    private static String mutationLabel(EggInfo info) {
        if (info == null || info.suffix == null || info.suffix.isBlank()) {
            return "Mutation: --";
        }
        return "Mutation: " + info.suffix;
    }

    private static String raptorBreedingToggleText(RaptorRuntimeState raptor) {
        if (raptor == null || raptor.roleInfo == null) {
            return "Breeding: --";
        }
        if (!raptor.roleInfo.isBreedingStage()) {
            return "Breeding: Adult only";
        }
        return raptor.breedingEnabled ? "Breeding: On" : "Breeding: Off";
    }

    private static String raptorHudStatus(RaptorRuntimeState raptor, long remainingMs) {
        String status = raptorStatus(raptor, remainingMs);
        if (raptor == null || raptor.sex == null) {
            return status;
        }
        return raptor.sex.label + " - " + status;
    }

    private static String raptorStatus(RaptorRuntimeState raptor, long remainingMs) {
        if (raptor == null || raptor.roleInfo == null) {
            return "";
        }
        if (raptor.roleInfo.isBreedingStage() && !raptor.breedingEnabled) {
            return "Breeding off";
        }
        if (raptor.needsAdultMate && raptor.roleInfo.isBreedingStage()) {
            return "Needs enabled mate";
        }
        if (raptor.starving && raptor.roleInfo.isGrowthStage()) {
            return "Hungry";
        }
        if (raptor.roleInfo.isBreedingStage() && remainingMs <= 0L) {
            return raptor.sex == RaptorSex.FEMALE ? "Ready to lay" : "Breeding ready";
        }
        if (raptor.transitionPending && raptor.roleInfo.isBreedingStage()) {
            return "Laying egg";
        }
        if (raptor.transitionPending && raptor.roleInfo.canAdvance()) {
            return "Changing";
        }
        return raptor.roleInfo.statusLabel;
    }

    private static RaptorSex getOrCreateRaptorSex(String key) {
        if (key == null || key.isBlank()) {
            return RaptorSex.FEMALE;
        }
        RaptorSex existing = RAPTOR_SEX_BY_KEY.get(key);
        if (existing != null) {
            return existing;
        }
        RaptorSex created = ((key.hashCode() & 1) == 0) ? RaptorSex.FEMALE : RaptorSex.MALE;
        RAPTOR_SEX_BY_KEY.put(key, created);
        return created;
    }

    private static boolean tryLayRaptorEgg(Store<EntityStore> store, World world, Ref<EntityStore> raptorRef, RaptorRuntimeState state, long nowMs) {
        if (store == null || world == null || raptorRef == null || state == null || state.roleInfo == null || state.roleInfo.info == null) {
            return false;
        }
        if (state.sex != RaptorSex.FEMALE) {
            state.needsAdultMate = false;
            return false;
        }
        if (!state.breedingEnabled) {
            state.needsAdultMate = false;
            return false;
        }
        TransformComponent transform;
        try {
            transform = store.getComponent(raptorRef, TransformComponent.getComponentType());
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not read transform for raptor egg laying " + state.roleInfo.role + ": " + describeThrowable(throwable));
            return false;
        }
        if (transform == null || transform.getPosition() == null) {
            debugGlobal(nowMs, "Missing transform for raptor egg laying " + state.roleInfo.role + ".");
            return false;
        }
        RaptorRuntimeState mate = findNearbyAdultMaleMate(store, state, transform.getPosition(), nowMs);
        if (mate == null) {
            state.needsAdultMate = true;
            debug("Female " + state.roleInfo.role + " is ready but has no enabled adult male mate within " + BREEDING_MATE_RADIUS_BLOCKS + " blocks.");
            return false;
        }
        state.needsAdultMate = false;
        EggInfo offspringInfo = chooseRaptorOffspringInfo(state, mate, nowMs);
        BlockType eggType = resolveCustomBlockType(offspringInfo.eggBlockName);
        if (eggType == null || eggType.isUnknown()) {
            resolveEggBlockIds();
            eggType = resolveCustomBlockType(offspringInfo.eggBlockName);
        }
        if (eggType == null || eggType.isUnknown()) {
            debugGlobal(nowMs, "Could not resolve egg block type for laying " + offspringInfo.eggBlockName + ".");
            return false;
        }
        NPCEntity npc = getNpcSafely(store, raptorRef);
        playEggLayingAnimation(raptorRef, npc, store, nowMs);
        Vector3d position = transform.getPosition();
        int baseX = (int) Math.floor(position.x());
        int baseY = (int) Math.floor(position.y());
        int baseZ = (int) Math.floor(position.z());
        for (int yOffset : EGG_LAYING_Y_OFFSETS) {
            for (int[] offset : EGG_LAYING_OFFSETS) {
                int x = baseX + offset[0];
                int y = baseY + yOffset;
                int z = baseZ + offset[1];
                if (!isAirBlock(world, x, y, z)) {
                    continue;
                }
                if (trySetBlock(world, x, y, z, eggType)) {
                    getOrCreateEgg(world, x, y, z, offspringInfo, nowMs);
                    resetRaptorCooldown(state, nowMs);
                    resetRaptorCooldown(mate, nowMs);
                    debug("Female " + state.roleInfo.role + " bred with " + mate.roleInfo.role + " and laid " + offspringInfo.displayName + " egg (" + mutationLabel(offspringInfo) + ") at " + x + " " + y + " " + z + ".");
                    return true;
                }
            }
        }
        debugGlobal(nowMs, "Female " + state.roleInfo.role + " was ready to lay, but no empty block was available near " + formatVector(position) + ".");
        return false;
    }

    private static void playEggLayingAnimation(Ref<EntityStore> raptorRef, NPCEntity npc, Store<EntityStore> store, long nowMs) {
        if (raptorRef == null || npc == null || store == null) {
            return;
        }
        try {
            npc.playAnimation(raptorRef, AnimationSlot.Status, "Laydown", store);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not play raptor laying status animation: " + describeThrowable(throwable));
        }
        try {
            npc.playAnimation(raptorRef, AnimationSlot.Action, "Eat", store);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not play raptor laying action animation: " + describeThrowable(throwable));
        }
    }

    private static void resetRaptorCooldown(RaptorRuntimeState state, long nowMs) {
        if (state == null || state.roleInfo == null) {
            return;
        }
        state.startedAtMs = nowMs;
        state.completeAtMs = nowMs + state.roleInfo.durationMs;
        state.nextTransitionAttemptMs = state.completeAtMs;
        state.transitionPending = false;
        state.needsAdultMate = false;
    }

    private static RaptorRuntimeState findNearbyAdultMaleMate(Store<EntityStore> store, RaptorRuntimeState female, Vector3d position, long nowMs) {
        if (store == null || female == null || position == null) {
            return null;
        }
        double maxDistanceSq = BREEDING_MATE_RADIUS_BLOCKS * BREEDING_MATE_RADIUS_BLOCKS;
        double nearestDistanceSq = Double.MAX_VALUE;
        RaptorRuntimeState nearestMate = null;
        for (RaptorRuntimeState candidate : KNOWN_RAPTORS.values()) {
            if (candidate == null || candidate == female || candidate.sex != RaptorSex.MALE) {
                continue;
            }
            if (!candidate.breedingEnabled) {
                continue;
            }
            if (nowMs < candidate.completeAtMs) {
                continue;
            }
            if (candidate.roleInfo == null || candidate.roleInfo.info == null || !candidate.roleInfo.isBreedingStage() || !safeEquals(candidate.worldKey, female.worldKey)) {
                continue;
            }
            if (isPterodactylInfo(candidate.roleInfo.info) != isPterodactylInfo(female.roleInfo == null ? null : female.roleInfo.info)) {
                continue;
            }
            if (nowMs - candidate.lastSeenMs > STALE_RAPTOR_REMOVE_MS || candidate.ref == null || !candidate.ref.isValid()) {
                continue;
            }
            TransformComponent mateTransform;
            try {
                mateTransform = store.getComponent(candidate.ref, TransformComponent.getComponentType());
            } catch (Throwable ignored) {
                continue;
            }
            if (mateTransform == null || mateTransform.getPosition() == null) {
                continue;
            }
            Vector3d matePosition = mateTransform.getPosition();
            double dx = matePosition.x() - position.x();
            double dy = matePosition.y() - position.y();
            double dz = matePosition.z() - position.z();
            double distanceSq = (dx * dx) + (dy * dy) + (dz * dz);
            if (distanceSq <= maxDistanceSq && distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestMate = candidate;
            }
        }
        return nearestMate;
    }

    private static EggInfo chooseRaptorOffspringInfo(RaptorRuntimeState female, RaptorRuntimeState male, long nowMs) {
        EggInfo mother = female == null || female.roleInfo == null ? null : female.roleInfo.info;
        EggInfo father = male == null || male.roleInfo == null ? null : male.roleInfo.info;
        if (mother == null && father == null) {
            return EGG_INFOS.isEmpty() ? null : EGG_INFOS.get(0);
        }
        if (mother == null) {
            return father;
        }
        if (father == null) {
            return mother;
        }
        if (isPterodactylInfo(mother) || isPterodactylInfo(father)) {
            debugGlobal(nowMs, "TFJ breeding roll mother=" + mother.suffix + ", father=" + father.suffix + ", result=" + mother.suffix + " (family locked).");
            return mother;
        }

        List<BreedingOutcome> outcomes = new ArrayList<>();
        if (safeEquals(mother.suffix, father.suffix)) {
            List<BreedingOutcome> mutations = new ArrayList<>();
            int mutationWeight = addSameColorMutations(mutations, mother.suffix);
            addOutcome(outcomes, mother, Math.max(1, 100 - mutationWeight));
            outcomes.addAll(mutations);
        } else {
            List<BreedingOutcome> mutations = new ArrayList<>();
            int mutationWeight = addCrossColorMutations(mutations, mother.suffix, father.suffix);
            int parentWeight = Math.max(1, (100 - mutationWeight) / 2);
            addOutcome(outcomes, mother, parentWeight);
            addOutcome(outcomes, father, parentWeight);
            outcomes.addAll(mutations);
        }

        EggInfo selected = selectWeightedOutcome(outcomes);
        if (selected == null) {
            selected = ThreadLocalRandom.current().nextBoolean() ? mother : father;
        }
        debugGlobal(nowMs, "TFJ raptor breeding roll mother=" + mother.suffix + ", father=" + father.suffix + ", result=" + selected.suffix + ".");
        return selected;
    }

    private static int addSameColorMutations(List<BreedingOutcome> outcomes, String suffix) {
        return addMutationOptions(outcomes, sameColorMutationOptions(suffix));
    }

    private static int addCrossColorMutations(List<BreedingOutcome> outcomes, String first, String second) {
        return addMutationOptions(outcomes, crossColorMutationOptions(first, second));
    }

    private static int addMutationOptions(List<BreedingOutcome> outcomes, List<MutationOption> options) {
        if (outcomes == null || options == null || options.isEmpty()) {
            return 0;
        }
        int weight = 0;
        for (MutationOption option : options) {
            if (option == null) {
                continue;
            }
            weight += addOutcome(outcomes, option.suffix, option.weight);
        }
        return weight;
    }

    private static List<MutationOption> sameColorMutationOptions(String suffix) {
        String color = normalizeSuffix(suffix);
        if ("red".equals(color)) {
            return mutationOptions(option("Crimson", 6), option("Gold", 2));
        }
        if ("blue".equals(color)) {
            return mutationOptions(option("Azure", 6), option("Black", 2));
        }
        if ("yellow".equals(color)) {
            return mutationOptions(option("Amber", 5), option("Gold", 2));
        }
        if ("green".equals(color)) {
            return mutationOptions(option("Jade", 5), option("Emerald", 2));
        }
        if ("white".equals(color)) {
            return mutationOptions(option("Ivory", 6), option("Gold", 2));
        }
        if ("cyan".equals(color)) {
            return mutationOptions(option("Azure", 5), option("Emerald", 3));
        }
        if ("black".equals(color)) {
            return mutationOptions(option("Azure", 7), option("Crimson", 3), option("Violet", 2));
        }
        if ("gold".equals(color)) {
            return mutationOptions(option("Ivory", 5), option("Amber", 4), option("Crimson", 3));
        }
        if ("violet".equals(color)) {
            return mutationOptions(option("Crimson", 5), option("Azure", 4), option("Rose", 3));
        }
        if ("emerald".equals(color)) {
            return mutationOptions(option("Jade", 6), option("Ivory", 4), option("Gold", 2));
        }
        if ("amber".equals(color)) {
            return mutationOptions(option("Gold", 5), option("Crimson", 3), option("Rose", 2));
        }
        if ("rose".equals(color)) {
            return mutationOptions(option("Crimson", 5), option("Violet", 3), option("Ivory", 2));
        }
        if ("jade".equals(color)) {
            return mutationOptions(option("Emerald", 6), option("Ivory", 3), option("Gold", 2));
        }
        if ("ivory".equals(color)) {
            return mutationOptions(option("Gold", 5), option("Jade", 3), option("Emerald", 2));
        }
        if ("azure".equals(color)) {
            return mutationOptions(option("Black", 6), option("Violet", 4), option("Crimson", 2));
        }
        if ("crimson".equals(color)) {
            return mutationOptions(option("Gold", 5), option("Black", 4), option("Violet", 3), option("Azure", 2));
        }
        return List.of();
    }

    private static List<MutationOption> crossColorMutationOptions(String first, String second) {
        if (pairIs(first, second, "Red", "Blue")) {
            return mutationOptions(option("Violet", 12), option("Black", 4));
        }
        if (pairIs(first, second, "Red", "Yellow")) {
            return mutationOptions(option("Amber", 12), option("Gold", 4));
        }
        if (pairIs(first, second, "Red", "White")) {
            return mutationOptions(option("Rose", 10), option("Crimson", 2));
        }
        if (pairIs(first, second, "Red", "Green")) {
            return mutationOptions(option("Amber", 7), option("Jade", 5));
        }
        if (pairIs(first, second, "Red", "Cyan")) {
            return mutationOptions(option("Crimson", 4), option("Azure", 4), option("Violet", 4));
        }
        if (pairIs(first, second, "Blue", "Cyan")) {
            return mutationOptions(option("Azure", 12), option("Emerald", 4));
        }
        if (pairIs(first, second, "Blue", "Green")) {
            return mutationOptions(option("Emerald", 8), option("Jade", 4));
        }
        if (pairIs(first, second, "Blue", "White")) {
            return mutationOptions(option("Azure", 7), option("Ivory", 5));
        }
        if (pairIs(first, second, "Yellow", "Green")) {
            return mutationOptions(option("Amber", 7), option("Jade", 5));
        }
        if (pairIs(first, second, "Yellow", "White")) {
            return mutationOptions(option("Ivory", 8), option("Gold", 4));
        }
        if (pairIs(first, second, "Yellow", "Cyan")) {
            return mutationOptions(option("Emerald", 6), option("Gold", 6));
        }
        if (pairIs(first, second, "Green", "Cyan")) {
            return mutationOptions(option("Jade", 10), option("Emerald", 6));
        }
        if (pairIs(first, second, "Green", "White")) {
            return mutationOptions(option("Jade", 8), option("Ivory", 4));
        }
        if (pairIs(first, second, "White", "Cyan")) {
            return mutationOptions(option("Gold", 5), option("Azure", 6), option("Ivory", 5));
        }

        List<MutationOption> lineOptions = lineReinforcementMutationOptions(first, second);
        if (!lineOptions.isEmpty()) {
            return lineOptions;
        }

        List<MutationOption> advancedOptions = advancedMutationOptions(first, second);
        if (!advancedOptions.isEmpty()) {
            return advancedOptions;
        }

        if (pairHas(first, second, "Black")) {
            return mutationOptions(option("Black", 8), option("Crimson", 3));
        }
        if (pairHas(first, second, "Gold")) {
            return mutationOptions(option("Gold", 7), option("Ivory", 3));
        }
        return List.of();
    }

    private static List<MutationOption> lineReinforcementMutationOptions(String first, String second) {
        if (pairIs(first, second, "Red", "Crimson")) {
            return mutationOptions(option("Crimson", 9), option("Rose", 4));
        }
        if (pairIs(first, second, "Red", "Rose")) {
            return mutationOptions(option("Rose", 8), option("Crimson", 3));
        }
        if (pairIs(first, second, "Red", "Violet")) {
            return mutationOptions(option("Violet", 6), option("Crimson", 4));
        }
        if (pairIs(first, second, "Blue", "Azure")) {
            return mutationOptions(option("Azure", 9), option("Black", 3));
        }
        if (pairIs(first, second, "Blue", "Violet")) {
            return mutationOptions(option("Violet", 6), option("Azure", 4));
        }
        if (pairIs(first, second, "Cyan", "Azure")) {
            return mutationOptions(option("Azure", 9), option("Emerald", 3));
        }
        if (pairIs(first, second, "Cyan", "Emerald")) {
            return mutationOptions(option("Emerald", 6), option("Azure", 4));
        }
        if (pairIs(first, second, "Green", "Emerald")) {
            return mutationOptions(option("Emerald", 8), option("Jade", 4));
        }
        if (pairIs(first, second, "Green", "Jade")) {
            return mutationOptions(option("Jade", 8), option("Emerald", 3));
        }
        if (pairIs(first, second, "Yellow", "Amber")) {
            return mutationOptions(option("Amber", 9), option("Gold", 3));
        }
        if (pairIs(first, second, "Yellow", "Gold")) {
            return mutationOptions(option("Gold", 6), option("Amber", 4));
        }
        if (pairIs(first, second, "White", "Ivory")) {
            return mutationOptions(option("Ivory", 9), option("Gold", 3));
        }
        if (pairIs(first, second, "White", "Gold")) {
            return mutationOptions(option("Gold", 6), option("Ivory", 4));
        }
        if (pairIs(first, second, "White", "Rose")) {
            return mutationOptions(option("Rose", 5), option("Ivory", 3));
        }
        return List.of();
    }

    private static List<MutationOption> advancedMutationOptions(String first, String second) {
        if (pairIs(first, second, "Black", "Gold")) {
            return mutationOptions(option("Crimson", 6), option("Gold", 5), option("Black", 4), option("Ivory", 2));
        }
        if (pairIs(first, second, "Black", "Violet")) {
            return mutationOptions(option("Azure", 7), option("Crimson", 5), option("Black", 4));
        }
        if (pairIs(first, second, "Black", "Emerald")) {
            return mutationOptions(option("Jade", 6), option("Black", 5), option("Azure", 3));
        }
        if (pairIs(first, second, "Black", "Amber")) {
            return mutationOptions(option("Crimson", 5), option("Black", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Black", "Rose")) {
            return mutationOptions(option("Crimson", 7), option("Violet", 5), option("Black", 3));
        }
        if (pairIs(first, second, "Black", "Jade")) {
            return mutationOptions(option("Black", 5), option("Emerald", 5), option("Azure", 3));
        }
        if (pairIs(first, second, "Black", "Ivory")) {
            return mutationOptions(option("Black", 5), option("Ivory", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Black", "Azure")) {
            return mutationOptions(option("Black", 8), option("Azure", 6), option("Crimson", 3));
        }
        if (pairIs(first, second, "Black", "Crimson")) {
            return mutationOptions(option("Crimson", 8), option("Black", 7), option("Gold", 3));
        }
        if (pairIs(first, second, "Gold", "Violet")) {
            return mutationOptions(option("Gold", 5), option("Crimson", 5), option("Rose", 4));
        }
        if (pairIs(first, second, "Gold", "Emerald")) {
            return mutationOptions(option("Gold", 5), option("Emerald", 5), option("Jade", 4));
        }
        if (pairIs(first, second, "Gold", "Amber")) {
            return mutationOptions(option("Gold", 8), option("Amber", 5), option("Crimson", 3));
        }
        if (pairIs(first, second, "Gold", "Rose")) {
            return mutationOptions(option("Rose", 6), option("Gold", 5), option("Ivory", 3));
        }
        if (pairIs(first, second, "Gold", "Jade")) {
            return mutationOptions(option("Jade", 6), option("Gold", 5), option("Emerald", 3));
        }
        if (pairIs(first, second, "Gold", "Ivory")) {
            return mutationOptions(option("Gold", 8), option("Ivory", 6), option("Jade", 3));
        }
        if (pairIs(first, second, "Gold", "Azure")) {
            return mutationOptions(option("Azure", 5), option("Gold", 5), option("Ivory", 3));
        }
        if (pairIs(first, second, "Gold", "Crimson")) {
            return mutationOptions(option("Crimson", 7), option("Gold", 7), option("Ivory", 3));
        }
        if (pairIs(first, second, "Violet", "Emerald")) {
            return mutationOptions(option("Jade", 5), option("Azure", 4), option("Violet", 4));
        }
        if (pairIs(first, second, "Violet", "Amber")) {
            return mutationOptions(option("Rose", 6), option("Amber", 4), option("Crimson", 3));
        }
        if (pairIs(first, second, "Violet", "Rose")) {
            return mutationOptions(option("Rose", 7), option("Violet", 5), option("Crimson", 3));
        }
        if (pairIs(first, second, "Violet", "Jade")) {
            return mutationOptions(option("Jade", 5), option("Violet", 4), option("Azure", 3));
        }
        if (pairIs(first, second, "Violet", "Ivory")) {
            return mutationOptions(option("Rose", 5), option("Ivory", 5), option("Gold", 2));
        }
        if (pairIs(first, second, "Violet", "Azure")) {
            return mutationOptions(option("Azure", 8), option("Violet", 5), option("Black", 3));
        }
        if (pairIs(first, second, "Violet", "Crimson")) {
            return mutationOptions(option("Crimson", 8), option("Violet", 5), option("Rose", 3));
        }
        if (pairIs(first, second, "Emerald", "Amber")) {
            return mutationOptions(option("Jade", 10), option("Gold", 5), option("Ivory", 3));
        }
        if (pairIs(first, second, "Emerald", "Rose")) {
            return mutationOptions(option("Jade", 6), option("Rose", 5), option("Emerald", 3));
        }
        if (pairIs(first, second, "Emerald", "Jade")) {
            return mutationOptions(option("Jade", 8), option("Emerald", 6), option("Ivory", 3));
        }
        if (pairIs(first, second, "Emerald", "Ivory")) {
            return mutationOptions(option("Ivory", 7), option("Emerald", 6), option("Jade", 3));
        }
        if (pairIs(first, second, "Emerald", "Azure")) {
            return mutationOptions(option("Jade", 5), option("Azure", 5), option("Emerald", 3));
        }
        if (pairIs(first, second, "Emerald", "Crimson")) {
            return mutationOptions(option("Jade", 5), option("Crimson", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Amber", "Rose")) {
            return mutationOptions(option("Rose", 6), option("Amber", 5), option("Crimson", 3));
        }
        if (pairIs(first, second, "Amber", "Jade")) {
            return mutationOptions(option("Jade", 7), option("Amber", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Amber", "Ivory")) {
            return mutationOptions(option("Ivory", 6), option("Amber", 5), option("Gold", 4));
        }
        if (pairIs(first, second, "Amber", "Azure")) {
            return mutationOptions(option("Azure", 5), option("Amber", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Amber", "Crimson")) {
            return mutationOptions(option("Crimson", 7), option("Amber", 5), option("Gold", 4));
        }
        if (pairIs(first, second, "Rose", "Jade")) {
            return mutationOptions(option("Rose", 5), option("Jade", 5), option("Ivory", 3));
        }
        if (pairIs(first, second, "Rose", "Ivory")) {
            return mutationOptions(option("Ivory", 6), option("Rose", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Rose", "Azure")) {
            return mutationOptions(option("Azure", 6), option("Rose", 5), option("Violet", 3));
        }
        if (pairIs(first, second, "Rose", "Crimson")) {
            return mutationOptions(option("Crimson", 9), option("Rose", 5), option("Violet", 3));
        }
        if (pairIs(first, second, "Jade", "Ivory")) {
            return mutationOptions(option("Ivory", 6), option("Jade", 6), option("Emerald", 3));
        }
        if (pairIs(first, second, "Jade", "Azure")) {
            return mutationOptions(option("Azure", 5), option("Jade", 5), option("Emerald", 3));
        }
        if (pairIs(first, second, "Jade", "Crimson")) {
            return mutationOptions(option("Crimson", 6), option("Jade", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Ivory", "Azure")) {
            return mutationOptions(option("Azure", 5), option("Ivory", 5), option("Gold", 3));
        }
        if (pairIs(first, second, "Ivory", "Crimson")) {
            return mutationOptions(option("Crimson", 6), option("Ivory", 5), option("Gold", 4));
        }
        if (pairIs(first, second, "Azure", "Crimson")) {
            return mutationOptions(option("Crimson", 7), option("Azure", 6), option("Black", 4));
        }
        return List.of();
    }

    private static MutationOption option(String suffix, int weight) {
        return new MutationOption(suffix, weight);
    }

    private static List<MutationOption> mutationOptions(MutationOption... options) {
        if (options == null || options.length == 0) {
            return List.of();
        }
        List<MutationOption> result = new ArrayList<>();
        for (MutationOption option : options) {
            if (option != null && option.weight > 0) {
                result.add(option);
            }
        }
        return result;
    }

    private static boolean pairIs(String first, String second, String left, String right) {
        String a = normalizeSuffix(first);
        String b = normalizeSuffix(second);
        String l = normalizeSuffix(left);
        String r = normalizeSuffix(right);
        return (a.equals(l) && b.equals(r)) || (a.equals(r) && b.equals(l));
    }

    private static boolean pairHas(String first, String second, String color) {
        String normalized = normalizeSuffix(color);
        return normalizeSuffix(first).equals(normalized) || normalizeSuffix(second).equals(normalized);
    }

    private static int addOutcome(List<BreedingOutcome> outcomes, String suffix, int weight) {
        return addOutcome(outcomes, getEggInfoBySuffix(suffix), weight);
    }

    private static int addOutcome(List<BreedingOutcome> outcomes, EggInfo info, int weight) {
        if (outcomes == null || info == null || weight <= 0) {
            return 0;
        }
        outcomes.add(new BreedingOutcome(info, weight));
        return weight;
    }

    private static EggInfo selectWeightedOutcome(List<BreedingOutcome> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (BreedingOutcome outcome : outcomes) {
            if (outcome != null) {
                totalWeight += Math.max(0, outcome.weight);
            }
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cursor = 0;
        for (BreedingOutcome outcome : outcomes) {
            if (outcome == null || outcome.weight <= 0) {
                continue;
            }
            cursor += outcome.weight;
            if (roll < cursor) {
                return outcome.info;
            }
        }
        return outcomes.get(outcomes.size() - 1).info;
    }

    private static boolean isAirBlock(World world, int x, int y, int z) {
        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return false;
        }
        return getBlockSafely(chunk, x, y, z) == BlockType.EMPTY_ID;
    }

    @SuppressWarnings("unchecked")
    private static Ref<EntityStore> extractSpawnedEntityRef(Object spawned) {
        if (spawned instanceof Ref<?>) {
            return (Ref<EntityStore>) spawned;
        }
        if (spawned == null) {
            return null;
        }
        String[] methods = {"left", "first", "key", "right", "second", "value"};
        for (String methodName : methods) {
            try {
                Object candidate = spawned.getClass().getMethod(methodName).invoke(spawned);
                if (candidate instanceof Ref<?>) {
                    return (Ref<EntityStore>) candidate;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static void registerRaptorRoleInfo(EggInfo info) {
        RAPTOR_BY_ROLE.put(normalizeRoleKey(info.hatchlingRole), new RaptorRoleInfo(info, info.hatchlingRole, "Hatchling", "Growing", info.younglingRole, HATCHLING_GROWTH_MS, false, 0.44));
        RAPTOR_BY_ROLE.put(normalizeRoleKey(info.younglingRole), new RaptorRoleInfo(info, info.younglingRole, "Youngling", "Growing", info.juvenileRole, YOUNGLING_GROWTH_MS, false, 0.58));
        RAPTOR_BY_ROLE.put(normalizeRoleKey(info.juvenileRole), new RaptorRoleInfo(info, info.juvenileRole, "Juvenile", "Maturing", info.subadultRole, JUVENILE_GROWTH_MS, false, 0.74));
        RAPTOR_BY_ROLE.put(normalizeRoleKey(info.subadultRole), new RaptorRoleInfo(info, info.subadultRole, "Adolescent", "Maturing", info.adultRole, SUBADULT_GROWTH_MS, false, 0.88));
        RAPTOR_BY_ROLE.put(normalizeRoleKey(info.adultRole), new RaptorRoleInfo(info, info.adultRole, "Adult", "Breeding cooldown", "", ADULT_BREEDING_COOLDOWN_MS, true, 1.05));
    }

    private static String normalizeRoleKey(String role) {
        return role == null ? "" : role.toLowerCase(Locale.ROOT).replace("icedfoxstudios.theforgottenjungle:", "").replace("icedfoxstudios:", "");
    }

    private static void scanNearbyEggs(World world, TransformComponent transform, long nowMs) {
        if (world == null || transform == null || transform.getPosition() == null) {
            return;
        }
        Vector3d position = transform.getPosition();
        int centerX = (int) Math.floor(position.x());
        int centerY = (int) Math.floor(position.y());
        int centerZ = (int) Math.floor(position.z());
        for (int dx = -NEARBY_SCAN_RADIUS; dx <= NEARBY_SCAN_RADIUS; dx++) {
            for (int dz = -NEARBY_SCAN_RADIUS; dz <= NEARBY_SCAN_RADIUS; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;
                    EggInfo info = getEggInfoAt(world, x, y, z);
                    if (info != null) {
                        getOrCreateEgg(world, x, y, z, info, nowMs);
                    }
                }
            }
        }
    }

    private static LookedEgg findLookedEgg(
        World world,
        int index,
        ArchetypeChunk<EntityStore> archetypeChunk,
        CommandBuffer<EntityStore> commandBuffer,
        long nowMs,
        boolean debugNow
    ) {
        if (world == null || archetypeChunk == null || commandBuffer == null) {
            if (debugNow) {
                debug("Cannot run look-at check: world/archetypeChunk/commandBuffer is null.");
            }
            return null;
        }

        Vector3i targetBlockPos;
        try {
            targetBlockPos = TargetUtil.getTargetBlock(archetypeChunk.getReferenceTo(index), LOOK_DISTANCE, commandBuffer);
        } catch (Throwable throwable) {
            if (debugNow) {
                debug("TargetUtil.getTargetBlock failed: " + throwable.getClass().getName() + ": " + throwable.getMessage());
            }
            return null;
        }
        if (targetBlockPos == null) {
            if (debugNow) {
                debug("TargetUtil returned no target block within " + LOOK_DISTANCE + " blocks.");
            }
            return null;
        }
        if (debugNow) {
            debug("Target block=" + formatPos(targetBlockPos) + " " + describeBlockAt(world, getX(targetBlockPos), getY(targetBlockPos), getZ(targetBlockPos)));
        }

        Vector3i rootPos = resolveBaseBlock(world, targetBlockPos);
        if (rootPos == null) {
            if (debugNow) {
                debug("Could not resolve base block for target " + formatPos(targetBlockPos) + ".");
            }
            return null;
        }
        EggInfo info = getEggInfoAt(world, getX(rootPos), getY(rootPos), getZ(rootPos));
        if (info == null) {
            if (debugNow) {
                debug("Target root block is not a registered TFJ egg: root=" + formatPos(rootPos) + " " + describeBlockAt(world, getX(rootPos), getY(rootPos), getZ(rootPos)));
            }
            return null;
        }
        getOrCreateEgg(world, getX(rootPos), getY(rootPos), getZ(rootPos), info, nowMs);
        if (debugNow) {
            debug("Looking at registered TFJ egg " + info.displayName + " at " + formatPos(rootPos) + ".");
        }
        return new LookedEgg(getX(rootPos), getY(rootPos), getZ(rootPos), info);
    }

    private static void processKnownEggs(Store<EntityStore> store, World world, CommandBuffer<EntityStore> commandBuffer, long nowMs) {
        String currentWorldKey = worldKey(world);
        Iterator<Map.Entry<String, EggRuntimeState>> iterator = KNOWN_EGGS.entrySet().iterator();
        while (iterator.hasNext()) {
            EggRuntimeState egg = iterator.next().getValue();
            if (!currentWorldKey.equals(egg.worldKey)) {
                continue;
            }
            if (egg.hatched) {
                iterator.remove();
                continue;
            }

            EggInfo currentInfo = getEggInfoAt(world, egg.x, egg.y, egg.z);
            if (currentInfo != null) {
                egg.lastSeenMs = nowMs;
            } else if (nowMs - egg.lastSeenMs > STALE_EGG_REMOVE_MS) {
                iterator.remove();
                continue;
            }

            if (nowMs >= egg.hatchAtMs && nowMs >= egg.nextHatchAttemptMs && !egg.hatchPending) {
                egg.hatchPending = true;
                if (commandBuffer != null) {
                    commandBuffer.run(commandStore -> {
                        long hatchNowMs = System.currentTimeMillis();
                        Store<EntityStore> hatchStore = commandStore == null ? store : commandStore;
                        boolean hatched = hatchEgg(hatchStore, world, egg, hatchNowMs);
                        egg.hatchPending = false;
                        if (hatched) {
                            egg.hatched = true;
                        } else {
                            egg.nextHatchAttemptMs = hatchNowMs + HATCH_RETRY_INTERVAL_MS;
                        }
                    });
                } else if (hatchEgg(store, world, egg, nowMs)) {
                    egg.hatchPending = false;
                    egg.hatched = true;
                    iterator.remove();
                } else {
                    egg.hatchPending = false;
                    egg.nextHatchAttemptMs = nowMs + HATCH_RETRY_INTERVAL_MS;
                }
            }
        }
    }

    private static int fastForwardEggsInWorld(World world, long nowMs, long remainingMs) {
        if (world == null) {
            return 0;
        }
        String currentWorldKey = worldKey(world);
        long safeRemaining = Math.max(1_000L, remainingMs);
        int affected = 0;
        for (EggRuntimeState egg : KNOWN_EGGS.values()) {
            if (egg == null || !currentWorldKey.equals(egg.worldKey)) {
                continue;
            }
            EggInfo currentInfo = getEggInfoAt(world, egg.x, egg.y, egg.z);
            if (currentInfo == null || currentInfo != egg.info) {
                continue;
            }
            egg.startedAtMs = Math.max(0L, nowMs - Math.max(0L, INCUBATION_MS - safeRemaining));
            egg.hatchAtMs = nowMs + safeRemaining;
            egg.nextHatchAttemptMs = egg.hatchAtMs;
            egg.lastSeenMs = nowMs;
            affected++;
        }
        debug("Fast-forwarded " + affected + " TFJ raptor egg(s) in world " + currentWorldKey + " to " + (safeRemaining / 1000L) + "s remaining.");
        return affected;
    }

    private static EggRuntimeState getOrCreateEgg(World world, int x, int y, int z, EggInfo info, long nowMs) {
        String key = eggKey(world, x, y, z);
        EggRuntimeState egg = KNOWN_EGGS.get(key);
        if (egg == null || egg.info != info) {
            egg = new EggRuntimeState(worldKey(world), x, y, z, info, nowMs);
            KNOWN_EGGS.put(key, egg);
        }
        egg.lastSeenMs = nowMs;
        return egg;
    }

    private static boolean hatchEgg(Store<EntityStore> store, World world, EggRuntimeState egg, long nowMs) {
        if (store == null || world == null || egg == null) {
            return false;
        }
        EggInfo currentInfo = getEggInfoAt(world, egg.x, egg.y, egg.z);
        if (currentInfo == null || currentInfo != egg.info) {
            return false;
        }
        NPCPlugin npcPlugin;
        try {
            npcPlugin = NPCPlugin.get();
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "NPCPlugin.get failed while hatching " + egg.info.displayName + ": " + describeThrowable(throwable));
            return false;
        }
        if (npcPlugin == null) {
            debugGlobal(nowMs, "NPCPlugin is not ready; keeping " + egg.info.displayName + " egg at 00:00 for retry.");
            return false;
        }
        debug("Hatch attempt for " + egg.info.displayName + " egg at " + egg.x + " " + egg.y + " " + egg.z + " role=" + egg.info.hatchlingRole + ".");
        int roleIndex = -1;
        try {
            roleIndex = npcPlugin.getIndex(egg.info.hatchlingRole);
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "Could not read hatchling role index for " + egg.info.hatchlingRole + ": " + throwable.getClass().getSimpleName());
        }
        boolean roleKnown = roleIndex >= 0;
        if (!roleKnown) {
            try {
                roleKnown = npcPlugin.hasRoleName(egg.info.hatchlingRole);
            } catch (Throwable throwable) {
                debugGlobal(nowMs, "Could not resolve hatchling role " + egg.info.hatchlingRole + ": " + throwable.getClass().getSimpleName());
            }
        }
        debug("Hatch role resolution role=" + egg.info.hatchlingRole + ", index=" + roleIndex + ", known=" + roleKnown + ".");
        if (roleIndex < 0) {
            debugGlobal(nowMs, "Missing hatchling role index " + egg.info.hatchlingRole + " for " + egg.info.displayName + " egg; known=" + roleKnown + ".");
            return false;
        }

        Store<EntityStore> entityStore = store;

        for (double yOffset : HATCHLING_SPAWN_Y_OFFSETS) {
            for (double[] offset : HATCHLING_SPAWN_OFFSETS) {
                Vector3d spawnPosition = new Vector3d(egg.x + 0.5 + offset[0], egg.y + yOffset, egg.z + 0.5 + offset[1]);
                Object spawned = trySpawnHatchling(npcPlugin, entityStore, egg.info.hatchlingRole, roleIndex, spawnPosition, nowMs);
                if (spawned != null) {
                    debug("Spawned hatchling " + egg.info.hatchlingRole + " at " + formatVector(spawnPosition) + "; clearing egg block.");
                    boolean cleared = false;
                    try {
                        cleared = clearEggBlock(world, egg.x, egg.y, egg.z);
                    } catch (Throwable throwable) {
                        debugGlobal(nowMs, "Spawned " + egg.info.hatchlingRole + " but clearing egg block failed safely at " + egg.x + " " + egg.y + " " + egg.z + ": " + describeThrowable(throwable));
                    }
                    debug("Clear egg block result at " + egg.x + " " + egg.y + " " + egg.z + ": " + cleared + ".");
                    if (!cleared) {
                        debugGlobal(nowMs, "Spawned " + egg.info.hatchlingRole + " but could not clear egg block at " + egg.x + " " + egg.y + " " + egg.z + ".");
                    }
                    return true;
                }
            }
        }
        debugGlobal(nowMs, "Could not spawn hatchling " + egg.info.hatchlingRole + " near egg at " + egg.x + " " + egg.y + " " + egg.z + "; retrying without resetting timer.");
        return false;
    }

    private static Object trySpawnHatchling(NPCPlugin npcPlugin, Store<EntityStore> store, String role, int roleIndex, Vector3d spawnPosition, long nowMs) {
        try {
            debug("Trying spawnEntity role=" + role + ", index=" + roleIndex + ", pos=" + formatVector(spawnPosition) + ".");
            Object spawned = npcPlugin.spawnEntity(store, roleIndex, spawnPosition, new Rotation3f(), null, null);
            if (spawned != null) {
                debug("spawnEntity success role=" + role + ", index=" + roleIndex + ", result=" + spawned.getClass().getName() + ".");
                return spawned;
            }
            debugGlobal(nowMs, "spawnEntity returned null for hatchling role " + role + ", index=" + roleIndex + ".");
        } catch (Throwable throwable) {
            debugGlobal(nowMs, "spawnEntity failed for hatchling role " + role + ", index=" + roleIndex + ", pos=" + formatVector(spawnPosition) + ": " + describeThrowable(throwable));
        }
        return null;
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", vector.x(), vector.y(), vector.z());
    }

    private static String describeThrowable(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        while (throwable instanceof java.lang.reflect.InvocationTargetException) {
            Throwable target = ((java.lang.reflect.InvocationTargetException) throwable).getTargetException();
            if (target == null || target == throwable) {
                break;
            }
            throwable = target;
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }

    private static boolean clearEggBlock(World world, int x, int y, int z) {
        return trySetBlock(world, x, y, z, BlockType.EMPTY);
    }

    private static EggInfo getEggInfo(int blockId) {
        if (blockId == BlockType.UNKNOWN_ID || blockId == BlockType.EMPTY_ID) {
            return null;
        }
        return EGG_BY_BLOCK_ID.get(blockId);
    }

    private static EggInfo getEggInfo(BlockType blockType) {
        if (blockType == null || blockType.isUnknown()) {
            return null;
        }
        EggInfo mapped = EGG_BY_BLOCK_KEY.get(normalizeBlockKey(blockType.getId()));
        if (mapped != null) {
            return mapped;
        }
        String normalizedId = normalizeBlockKey(blockType.getId());
        for (EggInfo info : EGG_INFOS) {
            if (normalizedId.contains(normalizeBlockKey(info.eggBlockName))) {
                return info;
            }
        }
        return null;
    }

    private static EggInfo getEggInfoAt(World world, int x, int y, int z) {
        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return null;
        }
        EggInfo info = getEggInfo(getBlockSafely(chunk, x, y, z));
        if (info != null) {
            return info;
        }
        try {
            return getEggInfo(chunk.getBlockType(x, y, z));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void resolveEggBlockIds() {
        EGG_BY_BLOCK_ID.clear();
        EGG_BY_BLOCK_KEY.clear();
        List<String> unresolved = new ArrayList<>();
        for (EggInfo info : EGG_INFOS) {
            String blockName = info.eggBlockName;
            BlockType baseType = resolveCustomBlockType(blockName);
            int blockId = registerEggBlockType(info, baseType);
            info.blockId = blockId;
            if (blockId == BlockType.UNKNOWN_ID || blockId == BlockType.EMPTY_ID || blockId == Integer.MIN_VALUE) {
                unresolved.add(blockName + "=" + blockId);
            }
        }
        if (!unresolved.isEmpty() && shouldDebugGlobal(System.currentTimeMillis())) {
            System.out.println("[TFJ] Raptor egg numeric block IDs deferred; BlockType name fallback active: " + String.join(", ", unresolved));
        }
    }

    private static int registerEggBlockType(EggInfo info, BlockType blockType) {
        if (info == null || blockType == null || blockType.isUnknown()) {
            return BlockType.UNKNOWN_ID;
        }
        String id = blockType.getId();
        EGG_BY_BLOCK_KEY.put(normalizeBlockKey(id), info);
        try {
            int index = BlockType.getAssetMap().getIndex(id);
            if (index != Integer.MIN_VALUE && index != BlockType.EMPTY_ID && index != BlockType.UNKNOWN_ID) {
                EGG_BY_BLOCK_ID.put(index, info);
            }
            return index;
        } catch (Throwable ignored) {
            return BlockType.UNKNOWN_ID;
        }
    }

    private static BlockType resolveCustomBlockType(String blockName) {
        String[] candidates = {
            blockName,
            "IcedFoxStudios:" + blockName,
            "IcedFoxStudios.TheForgottenJungle:" + blockName
        };
        for (String candidate : candidates) {
            try {
                BlockType blockType = BlockType.fromString(candidate);
                if (blockType != null && !blockType.isUnknown()) {
                    return blockType;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String normalizeBlockKey(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replace("icedfoxstudios.theforgottenjungle:", "").replace("icedfoxstudios:", "");
    }

    private static WorldChunk getChunkSafely(World world, int x, int z) {
        if (world == null) {
            return null;
        }
        try {
            return world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Vector3i resolveBaseBlock(World world, Vector3i pos) {
        if (world == null || pos == null) {
            return null;
        }
        int x = getX(pos);
        int y = getY(pos);
        int z = getZ(pos);
        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return null;
        }
        try {
            int filler = chunk.getFiller(x, y, z);
            return filler == 0 ? new Vector3i(x, y, z) : new Vector3i(x - FillerBlockUtil.unpackX(filler), y - FillerBlockUtil.unpackY(filler), z - FillerBlockUtil.unpackZ(filler));
        } catch (Throwable ignored) {
            return new Vector3i(x, y, z);
        }
    }

    private static boolean shouldDebugPlayer(UUID playerUuid, long nowMs) {
        if (playerUuid == null) {
            return shouldDebugGlobal(nowMs);
        }
        long nextDebug = NEXT_DEBUG_BY_PLAYER.getOrDefault(playerUuid, 0L);
        if (nowMs < nextDebug) {
            return false;
        }
        NEXT_DEBUG_BY_PLAYER.put(playerUuid, nowMs + DEBUG_INTERVAL_MS);
        return true;
    }

    private static boolean shouldDebugGlobal(long nowMs) {
        if (nowMs < nextGlobalDebugMs) {
            return false;
        }
        nextGlobalDebugMs = nowMs + DEBUG_INTERVAL_MS;
        return true;
    }

    private static void debugGlobal(long nowMs, String message) {
        if (shouldDebugGlobal(nowMs)) {
            debug(message);
        }
    }

    private static void debug(String message) {
        System.out.println("[TFJ][Incubation] " + message);
    }

    private static String playerLabel(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return "unknown";
        }
        return playerRef.getUuid().toString();
    }

    private static int getX(Vector3i pos) {
        return pos.x();
    }

    private static int getY(Vector3i pos) {
        return pos.y();
    }

    private static int getZ(Vector3i pos) {
        return pos.z();
    }

    private static String formatPos(Vector3i pos) {
        if (pos == null) {
            return "null";
        }
        return getX(pos) + "," + getY(pos) + "," + getZ(pos);
    }

    private static String describeBlockAt(World world, int x, int y, int z) {
        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return "chunk=unloaded";
        }
        int rawBlock = getBlockSafely(chunk, x, y, z);
        String typeId = "unknown";
        try {
            BlockType type = chunk.getBlockType(x, y, z);
            if (type != null) {
                typeId = type.getId();
            }
        } catch (Throwable ignored) {
        }
        EggInfo rawInfo = getEggInfo(rawBlock);
        EggInfo typeInfo = null;
        try {
            typeInfo = getEggInfo(chunk.getBlockType(x, y, z));
        } catch (Throwable ignored) {
        }
        return "raw=" + rawBlock + ", type=" + typeId + ", rawEgg=" + eggInfoLabel(rawInfo) + ", typeEgg=" + eggInfoLabel(typeInfo);
    }

    private static String eggInfoLabel(EggInfo info) {
        return info == null ? "none" : info.suffix;
    }

    private static int getBlockSafely(WorldChunk chunk, int x, int y, int z) {
        if (chunk == null) {
            return BlockType.EMPTY_ID;
        }
        int localX = ChunkUtil.localCoordinate(x);
        int localY = ChunkUtil.localCoordinate(y);
        int localZ = ChunkUtil.localCoordinate(z);
        try {
            return chunk.getBlock(localX, y, localZ);
        } catch (Throwable ignored) {
        }
        try {
            return chunk.getBlock(localX, localY, localZ);
        } catch (Throwable ignored) {
            return BlockType.EMPTY_ID;
        }
    }

    private static boolean trySetBlock(World world, int x, int y, int z, BlockType blockType) {
        if (world == null || blockType == null || blockType.isUnknown()) {
            return false;
        }
        WorldChunk chunk = getChunkSafely(world, x, z);
        if (chunk == null) {
            return false;
        }
        int blockIndex;
        try {
            blockIndex = BlockType.getAssetMap().getIndex(blockType.getId());
        } catch (Throwable ignored) {
            return false;
        }
        if (blockIndex == Integer.MIN_VALUE) {
            return false;
        }
        int localX = ChunkUtil.localCoordinate(x);
        int localY = ChunkUtil.localCoordinate(y);
        int localZ = ChunkUtil.localCoordinate(z);
        int rotation = 0;
        try {
            rotation = chunk.getRotationIndex(localX, localY, localZ);
        } catch (Throwable ignored) {
        }
        try {
            if (chunk.setBlock(localX, y, localZ, blockIndex, blockType, rotation, 0, SET_BLOCK_UPDATE_REASON)) {
                chunk.markNeedsSaving();
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (chunk.setBlock(localX, localY, localZ, blockIndex, blockType, rotation, 0, SET_BLOCK_UPDATE_REASON)) {
                chunk.markNeedsSaving();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Vector3d getHeadRotationForward(HeadRotation headRotation) {
        if (headRotation == null) {
            return null;
        }
        try {
            Vector3d forward = headRotation.getDirection();
            if (forward == null || !forward.isFinite() || isNearZero(forward, 0.0001)) {
                return null;
            }
            return new Vector3d(forward).normalize();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Vector3d getTransformForward(TransformComponent transform) {
        if (transform == null || transform.getRotation() == null) {
            return new Vector3d(0.0, 0.0, -1.0);
        }
        float rawPitch = transform.getRotation().pitch();
        float rawYaw = transform.getRotation().yaw();
        double pitch = Math.abs(rawPitch) > Math.PI * 2.0 ? Math.toRadians(rawPitch) : rawPitch;
        double yaw = Math.abs(rawYaw) > Math.PI * 2.0 ? Math.toRadians(rawYaw) : rawYaw;
        Vector3d forward = new Vector3d(0.0, 0.0, -1.0);
        forward.rotateX(pitch);
        forward.rotateY(yaw);
        if (!forward.isFinite() || isNearZero(forward, 0.0001)) {
            return new Vector3d(0.0, 0.0, -1.0);
        }
        return forward.normalize();
    }

    private static boolean isNearZero(Vector3d vector, double epsilon) {
        double lengthSquared = vector.x() * vector.x() + vector.y() * vector.y() + vector.z() * vector.z();
        return lengthSquared <= epsilon * epsilon;
    }

    private static String eggKey(World world, int x, int y, int z) {
        return worldKey(world) + ":" + x + ":" + y + ":" + z;
    }

    private static String worldKey(World world) {
        return world == null || world.getName() == null ? "unknown" : world.getName();
    }

    private static final class PterodactylLookInput {
        private final double pitch;
        private final double yaw;
        @SuppressWarnings("unused")
        private final double roll;

        private PterodactylLookInput(double pitch, double yaw, double roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }

        private Vector3d toForward() {
            try {
                double normalizedPitch = Math.abs(pitch) > Math.PI * 2.0 ? Math.toRadians(pitch) : pitch;
                double normalizedYaw = Math.abs(yaw) > Math.PI * 2.0 ? Math.toRadians(yaw) : yaw;
                Vector3d forward = new Vector3d(0.0, 0.0, -1.0);
                forward.rotateX(normalizedPitch);
                forward.rotateY(normalizedYaw);
                if (!forward.isFinite() || isNearZero(forward, 0.0001)) {
                    return null;
                }
                return forward.normalize();
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static final class PterodactylMoveInput {
        private final boolean jumping;
        private final boolean swimJumping;
        private final boolean crouching;
        private final boolean forcedCrouching;

        private PterodactylMoveInput(boolean jumping, boolean swimJumping, boolean crouching, boolean forcedCrouching) {
            this.jumping = jumping;
            this.swimJumping = swimJumping;
            this.crouching = crouching;
            this.forcedCrouching = forcedCrouching;
        }

        private static PterodactylMoveInput fromStates(Object states) {
            return new PterodactylMoveInput(
                getBooleanMember(states, "jumping"),
                getBooleanMember(states, "swimJumping"),
                getBooleanMember(states, "crouching"),
                getBooleanMember(states, "forcedCrouching")
            );
        }
    }

    private static final class EggInfo {
        private final String suffix;
        private final String displayName;
        private final String eggBlockName;
        private final String roleBaseName;
        private final String hatchlingRole;
        private final boolean naturalSpawn;
        private final double healthMultiplier;
        private final double speedMultiplier;
        private final String traitLabel;
        private final String younglingRole;
        private final String juvenileRole;
        private final String subadultRole;
        private final String adultRole;
        private int blockId = BlockType.UNKNOWN_ID;

        private EggInfo(String suffix, String displayName, String hatchlingRole, boolean naturalSpawn, double healthMultiplier, double speedMultiplier, String traitLabel) {
            this(suffix, displayName, "TFJ_Raptor_Egg_" + suffix, "TFJ_Raptor_" + suffix, hatchlingRole, naturalSpawn, healthMultiplier, speedMultiplier, traitLabel);
        }

        private EggInfo(String suffix, String displayName, String eggBlockName, String roleBaseName, String hatchlingRole, boolean naturalSpawn, double healthMultiplier, double speedMultiplier, String traitLabel) {
            this.suffix = suffix;
            this.displayName = displayName;
            this.eggBlockName = eggBlockName == null || eggBlockName.isBlank() ? "TFJ_Raptor_Egg_" + suffix : eggBlockName;
            this.roleBaseName = roleBaseName == null || roleBaseName.isBlank() ? "TFJ_Raptor_" + suffix : roleBaseName;
            this.hatchlingRole = hatchlingRole;
            this.naturalSpawn = naturalSpawn;
            this.healthMultiplier = clamp(healthMultiplier, 0.25, 4.0);
            this.speedMultiplier = clamp(speedMultiplier, 0.25, 4.0);
            this.traitLabel = traitLabel == null ? "" : traitLabel;
            this.younglingRole = this.roleBaseName + "_Youngling";
            this.juvenileRole = this.roleBaseName + "_Juvenile";
            this.subadultRole = this.roleBaseName + (this.roleBaseName.startsWith("TFJ_Raptor_") ? "_Subadult" : "_Adolescent");
            this.adultRole = this.roleBaseName;
        }
    }

    private static final class BreedingOutcome {
        private final EggInfo info;
        private final int weight;

        private BreedingOutcome(EggInfo info, int weight) {
            this.info = info;
            this.weight = weight;
        }
    }

    private static final class MutationOption {
        private final String suffix;
        private final int weight;

        private MutationOption(String suffix, int weight) {
            this.suffix = suffix == null ? "" : suffix;
            this.weight = Math.max(0, weight);
        }
    }

    private static final class RaptorGuidePageData {
        private final String title;
        private final String subtitle;
        private final List<RaptorGuideRow> rows;

        private RaptorGuidePageData(String title, String subtitle, List<RaptorGuideRow> rows) {
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.rows = rows == null ? new ArrayList<>() : rows;
        }
    }

    private static final class RaptorGuideRow {
        private final String name;
        private final String detail;
        private final String chance;

        private RaptorGuideRow(String name, String detail, String chance) {
            this.name = name == null ? "" : name;
            this.detail = detail == null ? "" : detail;
            this.chance = chance == null ? "" : chance;
        }
    }

    private static final class EggRuntimeState {
        private final String worldKey;
        private final int x;
        private final int y;
        private final int z;
        private final EggInfo info;
        private long startedAtMs;
        private long hatchAtMs;
        private long nextHatchAttemptMs;
        private long lastSeenMs;
        private boolean hatchPending;
        private boolean hatched;

        private EggRuntimeState(String worldKey, int x, int y, int z, EggInfo info, long nowMs) {
            this.worldKey = worldKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.info = info;
            this.startedAtMs = nowMs;
            this.hatchAtMs = nowMs + INCUBATION_MS;
            this.nextHatchAttemptMs = this.hatchAtMs;
            this.lastSeenMs = nowMs;
        }
    }

    private static final class RaptorRoleInfo {
        private final EggInfo info;
        private final String role;
        private final String stageLabel;
        private final String statusLabel;
        private final String nextRole;
        private final long durationMs;
        private final boolean breedingStage;
        private final double visualScale;

        private RaptorRoleInfo(EggInfo info, String role, String stageLabel, String statusLabel, String nextRole, long durationMs, boolean breedingStage, double visualScale) {
            this.info = info;
            this.role = role;
            this.stageLabel = stageLabel;
            this.statusLabel = statusLabel;
            this.nextRole = nextRole;
            this.durationMs = Math.max(1_000L, durationMs);
            this.breedingStage = breedingStage;
            this.visualScale = clamp(visualScale, 0.1, 4.0);
        }

        private boolean canAdvance() {
            return this.nextRole != null && !this.nextRole.isBlank();
        }

        private boolean isGrowthStage() {
            return canAdvance();
        }

        private boolean isBreedingStage() {
            return this.breedingStage;
        }
    }

    private enum RaptorSex {
        FEMALE("Female"),
        MALE("Male");

        private final String label;

        RaptorSex(String label) {
            this.label = label;
        }
    }

    private enum RaptorCommandMode {
        IDLE("Idle"),
        FOLLOW("Follow"),
        GUARD("Guard");

        private final String label;

        RaptorCommandMode(String label) {
            this.label = label;
        }
    }

    private static final class RaptorRuntimeState {
        private final String key;
        private final String worldKey;
        private Ref<EntityStore> ref;
        private final RaptorRoleInfo roleInfo;
        private final RaptorSex sex;
        private final String[] meatSlotItems = new String[RAPTOR_CARE_SLOT_COUNT];
        private final int[] meatSlotCounts = new int[RAPTOR_CARE_SLOT_COUNT];
        private long startedAtMs;
        private long completeAtMs;
        private long lastSeenMs;
        private long nextTransitionAttemptMs;
        private long nextStarveDamageMs;
        private long nextAutoFeedMs;
        private long lastCareTickMs;
        private double foodLevel;
        private double careHealth;
        private double careMaxHealth;
        private boolean transitionPending;
        private boolean needsAdultMate;
        private boolean starving;
        private boolean dead;
        private boolean careHealthInitialized;
        private boolean breedingEnabled;
        private int level;
        private int xp;
        private int unspentPoints;
        private int damageLevel;
        private int speedLevel;
        private int staminaLevel;
        private double stamina;
        private RaptorCommandMode commandMode = RaptorCommandMode.IDLE;
        private UUID ownerUuid;
        private Ref<EntityStore> ownerRef;
        private boolean hasGuardPosition;
        private double guardX;
        private double guardY;
        private double guardZ;
        private long nextAdultXpMs;
        private long lastAdultTickMs;
        private long lastMountedAtMs;
        private long nextCommandTargetRefreshMs;
        private long nextTrainingAtMs;

        private RaptorRuntimeState(String key, String worldKey, Ref<EntityStore> ref, RaptorRoleInfo roleInfo, RaptorSex sex, long nowMs) {
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
            this.careMaxHealth = raptorCareMaxHealth(roleInfo);
            this.careHealth = this.careMaxHealth;
            this.breedingEnabled = false;
            this.level = 1;
            this.xp = 0;
            this.unspentPoints = 0;
            this.damageLevel = 0;
            this.speedLevel = 0;
            this.staminaLevel = 0;
            this.stamina = raptorMaxStamina(this);
            this.nextAdultXpMs = nowMs + RAPTOR_ACTIVITY_XP_MS;
            this.lastAdultTickMs = nowMs;
            this.nextCommandTargetRefreshMs = nowMs;
            this.nextTrainingAtMs = 0L;
        }

        private void inheritCareFrom(RaptorRuntimeState previous, long nowMs) {
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
            this.stamina = clamp(previous.stamina, 0.0, raptorMaxStamina(this));
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

    private static final class LookedRaptor {
        private final Ref<EntityStore> ref;
        private final RaptorRuntimeState state;

        private LookedRaptor(Ref<EntityStore> ref, RaptorRuntimeState state) {
            this.ref = ref;
            this.state = state;
        }
    }

    private static final class LookedEgg {
        private final int x;
        private final int y;
        private final int z;
        private final EggInfo info;

        private LookedEgg(int x, int y, int z, EggInfo info) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.info = info;
        }
    }
}
