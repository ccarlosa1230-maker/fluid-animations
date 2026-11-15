package com.fluidanimations;

import com.fluidanimations.anim.CustomAnimationManager;
import com.fluidanimations.config.AnimConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FluidAnimationsClient implements ClientModInitializer {
	public static final String MOD_ID = "fluidanimations";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		AnimConfig.load();

		// Recarrega as animacoes customizadas junto com os resource packs (F3+T tambem funciona)
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
				Identifier.fromNamespaceAndPath(MOD_ID, "custom_animations"),
				new CustomAnimationManager());

		LOGGER.info("Fluid Animations carregado");
	}
}
