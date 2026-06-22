package com.icedfoxstudios.forgottenjungle.raptor.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;

public final class RaptorEggTenSecondsCommand extends CommandBase {
    public RaptorEggTenSecondsCommand() {
        super("tfjegg10", "Set nearby TFJ raptor eggs to the final 10 seconds.");
        addAliases("tfjeggtest");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ForgottenJungleRuntime.queueFastForwardRequest(ctx, true, false, false, "TFJ: pedido recibido. Ponte cerca del huevo; se pondra a 10 segundos en el siguiente tick.");
    }
}
