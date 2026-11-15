package com.fluidanimations.anim;

import com.fluidanimations.FluidAnimationsClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega, de qualquer resource pack ou mod:
 *   assets/<namespace>/fluidanimations/*.animation.json   (exportados do Blockbench)
 *   assets/<namespace>/fluidanimations/bindings.json      (liga entidade -> animacao)
 *
 * bindings.json:
 * {
 *   "minecraft:cow": { "idle": "animation.cow.idle", "walk": "animation.cow.walk" },
 *   "minecraft:player": { "attack": "animation.player.punch" }
 * }
 * Estados reconhecidos: idle, walk, sneak, attack, hurt.
 */
public class CustomAnimationManager implements ResourceManagerReloadListener {
	private static final String DIR = "fluidanimations";

	private static volatile Map<String, CustomAnimation> animations = Map.of();
	private static volatile Map<String, Map<String, String>> bindings = Map.of();

	/** Retorna a animacao ligada a (tipo de entidade, estado), ou null. */
	public static CustomAnimation find(String entityTypeId, String state) {
		Map<String, String> perEntity = bindings.get(entityTypeId);
		if (perEntity == null) return null;
		String animName = perEntity.get(state);
		return animName == null ? null : animations.get(animName);
	}

	public static boolean hasBindings(String entityTypeId) {
		return bindings.containsKey(entityTypeId);
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Map<String, CustomAnimation> anims = new HashMap<>();
		Map<String, Map<String, String>> binds = new HashMap<>();

		// 1) Animacoes
		Map<Identifier, Resource> files = manager.listResources(DIR, id -> id.getPath().endsWith(".animation.json"));
		for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
			try (BufferedReader reader = entry.getValue().openAsReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				if (!root.has("animations")) continue;
				for (Map.Entry<String, JsonElement> a : root.getAsJsonObject("animations").entrySet()) {
					if (a.getValue().isJsonObject()) {
						anims.put(a.getKey(), CustomAnimation.parse(a.getKey(), a.getValue().getAsJsonObject()));
					}
				}
			} catch (Exception e) {
				FluidAnimationsClient.LOGGER.warn("Falha ao ler animacao {}", entry.getKey(), e);
			}
		}

		// 2) Bindings (um por namespace; os de cima sobrescrevem os de baixo)
		for (String namespace : manager.getNamespaces()) {
			Optional<Resource> res = manager.getResource(Identifier.fromNamespaceAndPath(namespace, DIR + "/bindings.json"));
			if (res.isEmpty()) continue;
			try (BufferedReader reader = res.get().openAsReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				for (Map.Entry<String, JsonElement> e : root.entrySet()) {
					if (!e.getValue().isJsonObject()) continue;
					Map<String, String> states = binds.computeIfAbsent(e.getKey(), k -> new HashMap<>());
					for (Map.Entry<String, JsonElement> s : e.getValue().getAsJsonObject().entrySet()) {
						states.put(s.getKey(), s.getValue().getAsString());
					}
				}
			} catch (Exception ex) {
				FluidAnimationsClient.LOGGER.warn("Falha ao ler bindings de {}", namespace, ex);
			}
		}

		animations = anims;
		bindings = binds;
		FluidAnimationsClient.LOGGER.info("Animacoes customizadas: {} animacoes, {} entidades ligadas", anims.size(), binds.size());
	}
}
