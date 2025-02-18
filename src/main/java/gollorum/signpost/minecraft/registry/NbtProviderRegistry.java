package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootNbtProvider;
import net.minecraft.core.Registry;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class NbtProviderRegistry {

    private static final DeferredRegister<LootNbtProviderType> Register =
        DeferredRegister.create(Registry.LOOT_NBT_PROVIDER_REGISTRY, Signpost.MOD_ID);

    public static final RegistryObject<LootNbtProviderType> RegisteredWaystone =
        Register.register("waystone", RegisteredWaystoneLootNbtProvider::createProviderType);

    public static void register(IEventBus bus){
        Register.register(bus);
    }

}
