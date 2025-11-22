package visualgoose.nldbutils;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import visualgoose.nldbutils.command.ServerPlayerArgumentType;
import visualgoose.nldbutils.command.TPACommand;

public class NLDBUtils implements ModInitializer {
	public static final String MOD_ID = "nldbutils";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ArgumentTypeRegistry.registerArgumentType(id("player"), ServerPlayerArgumentType.class, SingletonArgumentInfo.contextFree(ServerPlayerArgumentType::new));
        CommandRegistrationCallback.EVENT.register(TPACommand::register);
	}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}