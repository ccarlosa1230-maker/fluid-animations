package com.fluidanimations.anim;

import com.fluidanimations.config.AnimConfig;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Anima os OSSOS (ModelPart) de modelos vanilla. Chamado no fim de setupAnim() dos modelos
 * humanoides (jogador, zumbi, esqueleto...) e quadrupedes (porco, vaca, ovelha...).
 */
public final class ModelAnimator {
	public enum Kind { HUMANOID, QUADRUPED }

	private static final float[] TMP = new float[3];

	private ModelAnimator() {}

	public static void apply(EntityModel<?> model, LivingEntityRenderState state, Kind kind) {
		if (!(state instanceof AnimHolder holder)) return;
		ProceduralPose p = holder.fluidanim$pose();
		if (!p.valid) return;

		ModelPart root = model.root();
		AnimConfig cfg = AnimConfig.INSTANCE;

		switch (kind) {
			case HUMANOID -> {
				if (cfg.bodyTwist) humanoidTwist(root, p);
			}
			case QUADRUPED -> {
				if (cfg.lookAround) lookAround(root, p);
			}
		}

		if (cfg.customAnimations && p.clip != null) {
			applyClip(root, p.clip, p.clipTime);
		}
	}

	/** Tronco gira levemente a cada passo, cabeca compensa, bracos acompanham o tronco. */
	private static void humanoidTwist(ModelPart root, ProceduralPose p) {
		float amount = Math.min(1.2f, p.walkAmount);
		if (amount <= 0.01f) return;
		float twist = Mth.sin(p.walkPhase) * 0.09f * amount * p.intensity;

		if (root.hasChild("body")) root.getChild("body").yRot += twist;
		if (root.hasChild("head")) root.getChild("head").yRot -= twist * 0.6f;
		if (root.hasChild("right_arm")) root.getChild("right_arm").yRot += twist;
		if (root.hasChild("left_arm")) root.getChild("left_arm").yRot += twist;
	}

	/** Animais parados movem a cabeca devagar, como se olhassem em volta. */
	private static void lookAround(ModelPart root, ProceduralPose p) {
		if (!root.hasChild("head")) return;
		float idle = Mth.clamp(1f - p.walkAmount * 3f, 0f, 1f) * (p.hurting ? 0f : 1f);
		if (idle <= 0f) return;

		float t = p.timeSeconds;
		float yaw = (Mth.sin(t * 0.8f + p.phaseOffset) + Mth.sin(t * 0.31f + p.phaseOffset * 2f)) * 0.5f;
		float pitch = Mth.sin(t * 0.6f + p.phaseOffset + 1.3f);

		ModelPart head = root.getChild("head");
		head.yRot += yaw * 0.18f * idle * p.intensity;
		head.xRot += pitch * 0.05f * idle * p.intensity;
	}

	/**
	 * Aplica uma animacao do Blockbench. Os ossos sao procurados pelo nome entre os filhos diretos
	 * do modelo (ex.: head, body, right_arm, left_leg, right_hind_leg...).
	 *
	 * Convencao de eixos (ajuste os sinais aqui se algo ficar espelhado):
	 *   rotacao: x e y invertidos, z normal   |   posicao: x e y invertidos, z normal (em pixels)
	 */
	private static void applyClip(ModelPart root, CustomAnimation clip, float time) {
		for (var entry : clip.bones.entrySet()) {
			if (!root.hasChild(entry.getKey())) continue;
			ModelPart part = root.getChild(entry.getKey());
			CustomAnimation.Bone bone = entry.getValue();

			if (bone.rotation() != null) {
				bone.rotation().sample(time, TMP);
				part.xRot -= TMP[0] * Mth.DEG_TO_RAD;
				part.yRot -= TMP[1] * Mth.DEG_TO_RAD;
				part.zRot += TMP[2] * Mth.DEG_TO_RAD;
			}
			if (bone.position() != null) {
				bone.position().sample(time, TMP);
				part.x -= TMP[0];
				part.y -= TMP[1];
				part.z += TMP[2];
			}
			if (bone.scale() != null) {
				bone.scale().sample(time, TMP);
				part.xScale *= TMP[0];
				part.yScale *= TMP[1];
				part.zScale *= TMP[2];
			}
		}
	}
}
