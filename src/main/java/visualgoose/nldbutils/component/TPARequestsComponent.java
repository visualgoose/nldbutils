package visualgoose.nldbutils.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;
import visualgoose.nldbutils.TPARequest;

import java.util.*;

public class TPARequestsComponent implements org.ladysnake.cca.api.v3.component.Component, ServerTickingComponent {
    public static final Codec<TPARequest> TPA_REQUEST_CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("from").forGetter(tpaRequest -> tpaRequest.from),
            UUIDUtil.CODEC.fieldOf("to").forGetter(tpaRequest -> tpaRequest.to),
            UUIDUtil.CODEC.fieldOf("requestor").forGetter(tpaRequest -> tpaRequest.requestor),
            Codec.LONG.fieldOf("expiresIn").forGetter(tpaRequest -> tpaRequest.expiresIn)
    ).apply(i, TPARequest::new));
    public static final Codec<List<TPARequest>> TPA_REQUEST_LIST_CODEC = TPA_REQUEST_CODEC.listOf();

    Level world;
    boolean overworld;
    LinkedList<TPARequest> tpaRequests;

    public TPARequestsComponent(Level world) {
        this.world = world;
        this.overworld = world.dimension().equals(Level.OVERWORLD);
        if(overworld) {
            tpaRequests = new LinkedList<>();
        }
    }

    public int sendRequest(UUID from, UUID to, ServerPlayer requestor) {
        if(from == to) {
            requestor.sendSystemMessage(Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                    .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                    .append(Component.literal("You can't send a TPA request to yourself").setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false))));
            return 0;
        }
        ServerPlayer autoAcceptRequestor = null;
        boolean exists = false;
        for(int i = 0; i < tpaRequests.size(); i++) {
            if(tpaRequests.get(i).from == from && tpaRequests.get(i).to == to) {
                if(tpaRequests.get(i).requestor != requestor.getUUID())
                    autoAcceptRequestor = world.getServer().getPlayerList().getPlayer(tpaRequests.get(i).requestor);
                exists = true;
                break;
            }
        }
        if(autoAcceptRequestor != null) {
            requestor.sendSystemMessage(Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                    .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                    .append(Component.literal("Automatically accepting tpa request from ")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                    .append(Component.literal(autoAcceptRequestor.getName().getString())
                                    .setStyle(Style.EMPTY.withColor(0xffe200))));
            return acceptRequest(requestor, autoAcceptRequestor.getUUID());
        }
        if(exists) {
            requestor.sendSystemMessage(Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                            .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                            .append(Component.literal("You already sent a similar tpa request")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false))));
            return 0;
        }
        TPARequest tpaRequest = new TPARequest(from, to, requestor.getUUID(), 600);
        String fromName = tpaRequest.getFromName(world.getServer());
        String toName = tpaRequest.getToName(world.getServer());
        requestor.sendSystemMessage(Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal("You've sent a TPA request:\n     ")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                .append(Component.literal(fromName)
                        .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                .append(Component.literal(toName)
                        .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                .append(Component.literal(" "))
                .append(Component.literal("(Cancel)")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Cancel this TPA request")
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true))))
                                .withClickEvent(new ClickEvent.RunCommand("/tpacancel " + (tpaRequest.isFromRequestor() ? toName : fromName)))
                        )));

        ServerPlayer acceptor = world.getServer().getPlayerList().getPlayer(tpaRequest.getAcceptor());
        if(acceptor != null) {
            Component toOtherComponent = Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                    .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                    .append(Component.literal("A TPA request has been sent to you:\n     ")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                    .append(Component.literal(fromName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                    .append(Component.literal(toName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(Component.literal(" "))
                    .append(Component.literal("(Accept)")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(true)
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Accept this TPA request")
                                            .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(true))))
                                    .withClickEvent(new ClickEvent.RunCommand("/tpaaccept " + (tpaRequest.isFromRequestor() ? fromName : toName)))
                            ));
            acceptor.sendSystemMessage(toOtherComponent);
        }
        tpaRequests.add(tpaRequest);
        return 1;
    }

    //if requestor is null all requests are canceled
    public int cancelRequest(ServerPlayer requestor, UUID other) {
        if(other != null) {
            for(TPARequest tpaRequest : tpaRequests) {
                if(tpaRequest.requestor != requestor.getUUID() || tpaRequest.getAcceptor() != other)
                    continue;

                ServerPlayer from = world.getServer().getPlayerList().getPlayer(tpaRequest.from);
                ServerPlayer to = world.getServer().getPlayerList().getPlayer(tpaRequest.to);

                String fromName = tpaRequest.getFromName(world.getServer());
                String toName = tpaRequest.getToName(world.getServer());

                Component message = Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                        .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                        .append(Component.literal("A TPA request has been canceled:\n     ")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                        .append(Component.literal(fromName)
                                .setStyle(Style.EMPTY.withColor(from == requestor ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                        .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                        .append(Component.literal(toName)
                                .setStyle(Style.EMPTY.withColor(to == requestor ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)));
                if(from != null) {
                    from.sendSystemMessage(message);
                }
                if(to != null) {
                    to.sendSystemMessage(message);
                }
                tpaRequests.remove(tpaRequest);
                return 1;
            }
        }

        int canceledRequestCount = 0;
        MutableComponent requestorCanceledList = Component.empty();
        for(TPARequest tpaRequest : tpaRequests) {
            if(tpaRequest.requestor != requestor.getUUID())
                continue;

            String fromName = tpaRequest.getFromName(world.getServer());
            String toName = tpaRequest.getToName(world.getServer());
            
            ServerPlayer acceptor = world.getServer().getPlayerList().getPlayer(tpaRequest.getAcceptor());
            if(acceptor != null)
            {
                Component acceptorMessage = Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                        .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                        .append(Component.literal("A TPA request has been canceled:\n     ")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                        .append(Component.literal(fromName)
                                .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ?
                                        0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                        .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                        .append(Component.literal(toName)
                                .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ?
                                        0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)));
                acceptor.sendSystemMessage(acceptorMessage);
            }

            requestorCanceledList.append("\n  -  ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false))
                    .append(Component.literal(fromName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                    .append(Component.literal(toName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)));

            tpaRequests.remove(tpaRequest);
            canceledRequestCount++;
        }
        if(canceledRequestCount == 0) {
            requestor.sendSystemMessage(
                    Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                    .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                    .append(Component.literal("You don't have any outgoing TPA requests")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false))));
            return 0;
        }

        requestor.sendSystemMessage(
                Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal("You have canceled " + canceledRequestCount + " TPA requests:")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                .append(requestorCanceledList));
        return canceledRequestCount;
    }

    public void listRequests(ServerPlayer requestor) {
        ArrayList<TPARequest> requests = new ArrayList<>();
        for(TPARequest tpaRequest : tpaRequests) {
            if(tpaRequest.from == requestor.getUUID() || tpaRequest.to == requestor.getUUID())
                requests.add(tpaRequest);
        }
        if(requests.isEmpty()) {
            requestor.sendSystemMessage(
                    Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                            .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                            .append(Component.literal("You don't have any TPA requests")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false))));
            return;
        }
        MutableComponent component = Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal("You have the following ingoing and outgoing TPA requests:")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)));
        for(TPARequest tpaRequest : requests) {
            String fromName = tpaRequest.getFromName(world.getServer());
            String toName = tpaRequest.getToName(world.getServer());

            component.append(Component.literal("\n  -  ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false))
                    .append(Component.literal(fromName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                    .append(Component.literal(toName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(" "));
            if(requestor.getUUID() == tpaRequest.requestor) {
                component.append(Component.literal("(Cancel)")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Cancel this TPA request")
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true))))
                                .withClickEvent(new ClickEvent.RunCommand("/tpacancel " + tpaRequest.getAcceptorName(world.getServer())))));
            } else {
                component.append(Component.literal("(Accept)")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(true)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Accept this TPA request")
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(true))))
                                .withClickEvent(new ClickEvent.RunCommand("/tpaaccept " + tpaRequest.getRequestorName(world.getServer())))));
            }
        }
        requestor.sendSystemMessage(component);
    }

    //if requestor is null all requests are accepted
    public int acceptRequest(ServerPlayer acceptor, UUID requestor) {
        ArrayList<TPARequest> requests = new ArrayList<>();
        if(requestor != null) {
            for(TPARequest tpaRequest : tpaRequests) {
                if(tpaRequest.requestor == requestor)
                {
                    if(tpaRequest.from == acceptor.getUUID() || tpaRequest.to == acceptor.getUUID())
                    {
                        requests.add(tpaRequest);
                        break;
                    }
                }
            }
        } else {
            for(TPARequest tpaRequest : tpaRequests) {
                if(tpaRequest.requestor != acceptor.getUUID() &&
                        (tpaRequest.from == acceptor.getUUID() || tpaRequest.to == acceptor.getUUID()))
                    requests.add(tpaRequest);
            }
        }
        MutableComponent component = Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                .append(Component.literal("You have accepted " + requests.size() + " TPA requests:")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)));
        for(TPARequest tpaRequest : requests) {
            ServerPlayer from = world.getServer().getPlayerList().getPlayer(tpaRequest.from);
            ServerPlayer to = world.getServer().getPlayerList().getPlayer(tpaRequest.to);
            ServerPlayer requestorSP = world.getServer().getPlayerList().getPlayer(tpaRequest.requestor);

            String fromName = tpaRequest.getFromName(world.getServer());
            String toName = tpaRequest.getToName(world.getServer());

            if(from == null || to == null) {
                component.append(Component.literal("\n  -  ")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal(tpaRequest.getRequestorName(world.getServer()))
                                .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ? 0xffe200 : ChatFormatting.DARK_GRAY.getColor())
                                        .withBold(false)))
                        .append(Component.literal(" is offline, deleting request")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(false)));

                tpaRequests.remove(tpaRequest);
                continue;
            }
            component.append(Component.literal("\n  -  ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false))
                    .append(Component.literal(fromName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                    .append(Component.literal(toName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false))));
            requestorSP.sendSystemMessage(Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                    .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                    .append(Component.literal("Your TPA request has been accepted:\n     ")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                    .append(Component.literal(fromName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                    .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                    .append(Component.literal(toName)
                            .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ?
                                    0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false))));
            from.teleport(new TeleportTransition(to.level(), to.position(), Vec3.ZERO, to.getYRot(), to.getXRot(), entity -> {}));
            tpaRequests.remove(tpaRequest);
        }
        acceptor.sendSystemMessage(component);
        return 1;
    }

    @Override
    public void readData(ValueInput valueInput) {
        if(!overworld)
            return;
        tpaRequests.clear();
        Optional<List<TPARequest>> tpaRequestsRead = valueInput.read("tpa_requests", TPA_REQUEST_LIST_CODEC);
        tpaRequestsRead.ifPresent(requests -> tpaRequests.addAll(requests));
    }

    @Override
    public void writeData(ValueOutput valueOutput) {
        if(!overworld)
            return;
        valueOutput.store("tpa_requests", TPA_REQUEST_LIST_CODEC, tpaRequests);
    }

    @Override
    public void serverTick() {
        if(!overworld)
            return;
        tpaRequests.forEach(tpaRequest -> {
            tpaRequest.expiresIn--;
            if(tpaRequest.expiresIn == 0) {
                ServerPlayer from = world.getServer().getPlayerList().getPlayer(tpaRequest.from);
                ServerPlayer to = world.getServer().getPlayerList().getPlayer(tpaRequest.to);

                String requestorName = tpaRequest.getRequestorName(world.getServer());
                String fromName = tpaRequest.getFromName(world.getServer());
                String toName = tpaRequest.getToName(world.getServer());
                Component message = Component.literal("TPA ").setStyle(Style.EMPTY.withColor(0x065253).withBold(true))
                        .append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
                        .append(Component.literal("A TPA request has expired:\n     ")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                        .append(Component.literal(fromName)
                                .setStyle(Style.EMPTY.withColor(tpaRequest.isFromRequestor() ?
                                        0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)))
                        .append(Component.literal(" -> ").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(false)))
                        .append(Component.literal(toName)
                                .setStyle(Style.EMPTY.withColor(tpaRequest.isToRequestor() ?
                                        0xffe200 : ChatFormatting.DARK_GRAY.getColor()).withBold(false)));

                if(from != null) {
                    from.sendSystemMessage(message);
                }
                if(to != null) {
                    to.sendSystemMessage(message);
                }

                tpaRequests.remove(tpaRequest);
            }
        });
    }
}
