package visualgoose.nldbutils.command;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ServerPlayerArgumentType implements ArgumentType<String> {

    private static final SimpleCommandExceptionType NO_PLAYER_FOUND = new SimpleCommandExceptionType(Component.literal("No player was found"));

    public ServerPlayerArgumentType() {
    }

    @Override
    public String parse(StringReader stringReader) {
        int argBeginning = stringReader.getCursor();
        if(!stringReader.canRead())
            stringReader.skip();
        while(stringReader.canRead())
            stringReader.skip();
        return stringReader.getString().substring(argBeginning, stringReader.getCursor());
    }

    public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> commandContext, String string) throws CommandSyntaxException {
        List<ServerPlayer> players = commandContext.getSource().getLevel().getPlayers(serverPlayer -> true);
        ServerPlayer playerFound = null;
        String name = commandContext.getArgument(string, String.class);
        for(ServerPlayer player : players) {
            if(player.getName().getString().equals(name)) {
                playerFound = player;
                break;
            }
        }
        if(playerFound == null)
            throw NO_PLAYER_FOUND.create();
        return playerFound;
    }

    public static UUID getPlayerUUID(CommandContext<CommandSourceStack> commandContext, String string, boolean allowOffline) throws CommandSyntaxException {
        ServerPlayer player = null;
        try {
            player = getPlayer(commandContext, string);
        } catch (CommandSyntaxException e) {
            if(!allowOffline)
                throw e;
        }
        if(player != null) {
            return player.getUUID();
        }
        UUID playerUUID = null;
        Optional<NameAndId> nameAndId = commandContext.getSource().getServer().services().nameToIdCache().get(
                commandContext.getArgument(string, String.class));
        if(nameAndId.isPresent())
            playerUUID = nameAndId.get().id();
        if(playerUUID == null)
            throw NO_PLAYER_FOUND.create();
        return playerUUID;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof FabricClientCommandSource clientCommandSource) {
            return SharedSuggestionProvider.suggest(Collections2.transform(clientCommandSource.getClient().getConnection().getOnlinePlayers(), (playerInfo) -> playerInfo.getProfile().name()), builder);
        }
        if (context.getSource() instanceof CommandSourceStack source) {
            return SharedSuggestionProvider.suggest(Lists.transform(source.getServer().getPlayerList().getPlayers(), (serverPlayer) -> serverPlayer.getName().getString()), builder);
        }
        return builder.buildFuture();
    }
}
