package visualgoose.nldbutils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserNameToIdResolver;

import java.util.Optional;
import java.util.UUID;

public class TPARequest {
    public UUID from;
    public UUID to;
    public UUID requestor;
    public long expiresIn;

    public TPARequest(UUID from, UUID to, UUID requestor, long expiresIn) {
        this.from = from;
        this.to = to;
        this.requestor = requestor;
        this.expiresIn = expiresIn;
    }

    public boolean isFromRequestor() {
        return from == requestor;
    }

    public boolean isToRequestor() {
        return to == requestor;
    }

    public UUID getAcceptor() {
        if(from == requestor)
            return to;
        return from;
    }

    public String getFromName(MinecraftServer server) {
        ServerPlayer fromSP = server.getPlayerList().getPlayer(from);

        if(fromSP != null)
            return fromSP.getName().getString();

        UserNameToIdResolver resolver = server.services().nameToIdCache();
        Optional<NameAndId> nameAndIdOptional = resolver.get(from);
        return nameAndIdOptional.map(NameAndId::name).orElse("<unknown>");
    }

    public String getToName(MinecraftServer server) {
        ServerPlayer toSP = server.getPlayerList().getPlayer(to);

        if(toSP != null)
            return toSP.getName().getString();

        UserNameToIdResolver resolver = server.services().nameToIdCache();
        Optional<NameAndId> nameAndIdOptional = resolver.get(to);
        return nameAndIdOptional.map(NameAndId::name).orElse("<unknown>");
    }

    public String getRequestorName(MinecraftServer server) {
        ServerPlayer requestorSP = server.getPlayerList().getPlayer(requestor);

        if(requestorSP != null)
            return requestorSP.getName().getString();

        UserNameToIdResolver resolver = server.services().nameToIdCache();
        Optional<NameAndId> nameAndIdOptional = resolver.get(requestor);
        return nameAndIdOptional.map(NameAndId::name).orElse("<unknown>");
    }

    public String getAcceptorName(MinecraftServer server) {
        UUID acceptor = getAcceptor();
        ServerPlayer acceptorSP = server.getPlayerList().getPlayer(acceptor);

        if(acceptorSP != null)
            return acceptorSP.getName().getString();

        UserNameToIdResolver resolver = server.services().nameToIdCache();
        Optional<NameAndId> nameAndIdOptional = resolver.get(acceptor);
        return nameAndIdOptional.map(NameAndId::name).orElse("<unknown>");
    }
}
