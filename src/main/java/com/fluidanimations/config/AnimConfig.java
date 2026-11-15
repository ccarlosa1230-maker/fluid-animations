package com.fluidanimations.config;

import com.fluidanimations.FluidAnimationsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuracao salva em config/fluidanimations.json. Edite o arquivo e reinicie o jogo.
 */
public final class AnimConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fluidanimations.json");

	public static AnimConfig INSTANCE = new AnimConfig();

	/** Liga/desliga o mod inteiro. */
	public boolean enabled = true;
	/** Multiplicador geral da intensidade (0 = nada, 1 = padrao, 2 = exagerado). */
	public float intensity = 1.0f;

	public boolean breathing = true;       // respirar (escala do corpo)
	public boolean walkBob = true;         // sobe e desce ao andar
	public boolean leanOnMove = true;      // inclina para frente ao andar
	public boolean turnRoll = true;        // inclina para o lado ao virar
	public boolean landSquash = true;      // achata ao pousar / estica ao pular
	public boolean hurtFlinch = true;      // recua ao tomar dano
	public boolean bodyTwist = true;       // torcao do tronco ao andar (humanoides)
	public boolean lookAround = true;      // animais olham ao redor parados
	public boolean customAnimations = true; // animacoes do Blockbench (ver README)

	public boolean droppedItems = true;    // itens no chao
	/** 1.0 = vanilla. Menor que 1 deixa o giro/flutuar dos itens no chao mais lento e suave. */
	public float droppedItemSpeed = 0.6f;

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				AnimConfig loaded = GSON.fromJson(Files.readString(FILE), AnimConfig.class);
				if (loaded != null) INSTANCE = loaded;
			}
			save(); // regrava para incluir opcoes novas
		} catch (IOException | RuntimeException e) {
			FluidAnimationsClient.LOGGER.warn("Nao foi possivel ler a config, usando padroes", e);
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			FluidAnimationsClient.LOGGER.warn("Nao foi possivel salvar a config", e);
		}
	}

	private AnimConfig() {}
}
