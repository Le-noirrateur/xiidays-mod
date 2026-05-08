package com.mceteams.xiidays.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class CommandRegistry {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        XiidaysCommand.register(dispatcher);
    }
}
