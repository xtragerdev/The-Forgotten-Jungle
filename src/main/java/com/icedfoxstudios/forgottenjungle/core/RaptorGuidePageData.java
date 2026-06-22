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


final class RaptorGuidePageData {
    final String title;
    final String subtitle;
    final List<RaptorGuideRow> rows;

    RaptorGuidePageData(String title, String subtitle, List<RaptorGuideRow> rows) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.rows = rows == null ? new ArrayList<>() : rows;
    }
}
