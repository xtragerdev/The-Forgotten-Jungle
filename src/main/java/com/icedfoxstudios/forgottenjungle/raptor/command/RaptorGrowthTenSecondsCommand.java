package com.icedfoxstudios.forgottenjungle.raptor.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;

public final class RaptorGrowthTenSecondsCommand extends CommandBase {
    public RaptorGrowthTenSecondsCommand() {
        super("tfjgrow10", "Set nearby TFJ hatchling/juvenile growth timers to the final 10 seconds.");
        addAliases("tfjraptorgrow10", "tfjgrowth10");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ForgottenJungleRuntime.queueFastForwardRequest(ctx, false, true, false, "TFJ: pedido recibido. Mira o acercate a un raptor joven; su crecimiento bajara a 10 segundos.");
    }
}
