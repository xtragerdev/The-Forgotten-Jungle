package com.icedfoxstudios.forgottenjungle.raptor.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;

public final class RaptorBreedingTenSecondsCommand extends CommandBase {
    public RaptorBreedingTenSecondsCommand() {
        super("tfjbreed10", "Set nearby adult TFJ raptor breeding cooldowns to the final 10 seconds.");
        addAliases("tfjraptorbreed10", "tfjcooldown10");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ForgottenJungleRuntime.queueFastForwardRequest(ctx, false, false, true, "TFJ: pedido recibido. Mira o acercate a un raptor adulto; su cooldown bajara a 10 segundos.");
    }
}
