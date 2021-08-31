package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RecipeRegistry {

	private static final DeferredRegister<RecipeSerializer<?>> Register =
		DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Signpost.MOD_ID);

	public static final RegistryObject<CutWaystoneRecipe.Serializer> CutWaystoneSerializer =
		Register.register(
			CutWaystoneRecipe.RegistryName,
			() -> CutWaystoneRecipe.Serializer.INSTANCE
		);

	public static void register(IEventBus bus){
		Register.register(bus);
	}

}
