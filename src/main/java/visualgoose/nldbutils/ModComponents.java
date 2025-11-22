package visualgoose.nldbutils;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
import visualgoose.nldbutils.component.TPARequestsComponent;

public class ModComponents implements WorldComponentInitializer {
    public static final ComponentKey<TPARequestsComponent> TPA_REQUESTS =
            ComponentRegistry.getOrCreate(NLDBUtils.id("tpa_requests"), TPARequestsComponent.class);

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry worldComponentFactoryRegistry) {
        worldComponentFactoryRegistry.register(TPA_REQUESTS, TPARequestsComponent::new);
    }
}
