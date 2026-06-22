package com.icedfoxstudios.forgottenjungle.raptor.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;

public final class RaptorAllTenSecondsCommand extends CommandBase {
    public RaptorAllTenSecondsCommand() {
        super("tfjraptor10", "Set nearby TFJ raptor breeding lifecycle timers to the final 10 seconds.");
        addAliases("tfjall10", "tfjbreedall10");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ForgottenJungleRuntime.queueFastForwardRequest(ctx, true, true, true, "TFJ: pedido recibido. Huevos, crecimiento y cooldowns cercanos bajaran a 10 segundos.");
    }
}
