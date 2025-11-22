package visualgoose.nldbutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import visualgoose.nldbutils.ModComponents;
import visualgoose.nldbutils.component.TPARequestsComponent;

public class TPACommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registryAccess,
                                Commands.CommandSelection environment) {
        dispatcher.register(
                Commands.literal("tpa")
                        .then(Commands.argument("destination", new ServerPlayerArgumentType())
                                .executes((context) -> sendTPARequest(context, false)))
        );
        dispatcher.register(
                Commands.literal("tpahere")
                        .then(Commands.argument("teleporter", new ServerPlayerArgumentType())
                                .executes((context -> sendTPARequest(context, true))))
        );
        dispatcher.register(
                Commands.literal("tpaaccept")
                        .then(Commands.literal("*")
                                .executes(context -> acceptTPARequest(context, true)))
                        .then(Commands.argument("requestor", new ServerPlayerArgumentType())
                                .executes(context -> acceptTPARequest(context, false)))
        );
        dispatcher.register(
                Commands.literal("tpacancel")
                        .then(Commands.literal("*")
                                .executes(context -> cancelTPARequest(context, true)))
                        .then(Commands.argument("other", new ServerPlayerArgumentType())
                                .executes(context -> cancelTPARequest(context, false)))
        );
        dispatcher.register(
                Commands.literal("tpalist")
                        .executes(TPACommand::tpaRequestList)
        );
    }

    private static int sendTPARequest(CommandContext<CommandSourceStack> context, boolean tpaHere) throws CommandSyntaxException {
        if(!context.getSource().isPlayer()) {
            context.getSource().sendFailure(Component.literal("Command must be used by a player"));
            return -1;
        }
        TPARequestsComponent tpaRequestsComponent =
                ModComponents.TPA_REQUESTS.getNullable(context.getSource().getServer().getLevel(Level.OVERWORLD));
        if(tpaRequestsComponent == null) {
            context.getSource().sendFailure(Component.literal("Failed to execute command (TPARequestsComponent is null)"));
            return -1;
        }
        if(tpaHere)
            return tpaRequestsComponent.sendRequest(ServerPlayerArgumentType.getPlayerUUID(context, "teleporter", true),
                    context.getSource().getPlayer().getUUID(), context.getSource().getPlayer());
        else
            return tpaRequestsComponent.sendRequest(context.getSource().getPlayer().getUUID(),
                    ServerPlayerArgumentType.getPlayerUUID(context, "destination", true), context.getSource().getPlayer());
    }

    private static int cancelTPARequest(CommandContext<CommandSourceStack> context, boolean all) throws CommandSyntaxException {
        if(!context.getSource().isPlayer())
        {
            context.getSource().sendFailure(Component.literal("This command must be used by a player"));
            return -1;
        }
        TPARequestsComponent component = ModComponents.TPA_REQUESTS.getNullable(context.getSource().getServer().getLevel(Level.OVERWORLD));
        if(component == null)
            return -1;
        if(all)
            return component.cancelRequest(context.getSource().getPlayer(), null);
        else
            return component.cancelRequest(context.getSource().getPlayer(), ServerPlayerArgumentType.getPlayerUUID(context, "other", true));
    }

    private static int tpaRequestList(CommandContext<CommandSourceStack> context) {
        if(!context.getSource().isPlayer())
        {
            context.getSource().sendFailure(Component.literal("This command must be used by a player"));
            return -1;
        }
        TPARequestsComponent component = ModComponents.TPA_REQUESTS.getNullable(context.getSource().getServer().getLevel(Level.OVERWORLD));
        if(component == null)
            return -1;
        component.listRequests(context.getSource().getPlayer());
        return 0;
    }

    private static int acceptTPARequest(CommandContext<CommandSourceStack> context, boolean all) throws CommandSyntaxException {
        if(!context.getSource().isPlayer())
        {
            context.getSource().sendFailure(Component.literal("This command must be used by a player"));
            return -1;
        }
        TPARequestsComponent component = ModComponents.TPA_REQUESTS.getNullable(context.getSource().getServer().getLevel(Level.OVERWORLD));
        if(component == null)
            return -1;
        if(all)
            return component.acceptRequest(context.getSource().getPlayer(), null);
        else
            return component.acceptRequest(context.getSource().getPlayer(), ServerPlayerArgumentType.getPlayerUUID(context, "requestor", true));
    }
}
