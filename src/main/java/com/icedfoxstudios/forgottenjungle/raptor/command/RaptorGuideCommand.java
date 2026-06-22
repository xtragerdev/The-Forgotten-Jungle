package com.icedfoxstudios.forgottenjungle.raptor.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.icedfoxstudios.forgottenjungle.core.ForgottenJungleRuntime;

public final class RaptorGuideCommand extends CommandBase {
    public RaptorGuideCommand() {
        super("tfjraptorguide", "Open the TFJ raptor breeding and mutation guide.");
        addAliases("tfjguide", "tfjbreeding", "tfjmutations");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        ForgottenJungleRuntime.queueGuideOpenRequest(ctx);
    }
}
