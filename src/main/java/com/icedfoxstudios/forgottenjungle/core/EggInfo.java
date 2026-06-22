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


final class EggInfo {
    final String suffix;
    final String displayName;
    final String eggBlockName;
    final String roleBaseName;
    final String hatchlingRole;
    final boolean naturalSpawn;
    final double healthMultiplier;
    final double speedMultiplier;
    final String traitLabel;
    final String younglingRole;
    final String juvenileRole;
    final String subadultRole;
    final String adultRole;
    int blockId = BlockType.UNKNOWN_ID;

    EggInfo(String suffix, String displayName, String hatchlingRole, boolean naturalSpawn, double healthMultiplier, double speedMultiplier, String traitLabel) {
        this(suffix, displayName, "TFJ_Raptor_Egg_" + suffix, "TFJ_Raptor_" + suffix, hatchlingRole, naturalSpawn, healthMultiplier, speedMultiplier, traitLabel);
    }

    EggInfo(String suffix, String displayName, String eggBlockName, String roleBaseName, String hatchlingRole, boolean naturalSpawn, double healthMultiplier, double speedMultiplier, String traitLabel) {
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
