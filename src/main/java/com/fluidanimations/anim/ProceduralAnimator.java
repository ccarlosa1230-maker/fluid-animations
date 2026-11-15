package com.fluidanimations.anim;

import com.fluidanimations.config.AnimConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Calcula a pose procedural de cada entidade viva (jogador, animais, monstros, etc.).
 *  - extract(): chamado no fim de LivingEntityRenderer#extractRenderState (calcula e guarda no state)
 *  - apply():   chamado no fim de LivingEntityRenderer#setupRotations (aplica no PoseStack)
 */
public final class ProceduralAnimator {
	private static final Map<EntityType<?>, String> TYPE_IDS = new IdentityHashMap<>();

	private ProceduralAnimator() {}

	public static void extract(LivingEntity e, LivingEntityRenderState state, float partialTick) {
		if (!(state instanceof AnimHolder holder)) return;
		ProceduralPose p = holder.fluidanim$pose();
		p.reset();

		AnimConfig cfg = AnimConfig.INSTANCE;
		if (!cfg.enabled || cfg.intensity <= 0f) return;
		if (e.isDeadOrDying() || e.isSleeping() || e.isPassenger()) return;
		Pose pose = e.getPose();
		if (pose != Pose.STANDING && pose != Pose.CROUCHING) return;

		MotionTracker.Track tr = MotionTracker.update(e);

		float categoryScale = e instanceof Player ? 0.6f
				: e instanceof Enemy ? 0.8f
				: e instanceof Animal ? 1.0f : 0.9f;
		if (e.isBaby()) categoryScale *= 1.3f;
		float k = cfg.intensity * categoryScale;

		p.valid = true;
		p.intensity = k;
		p.height = e.getBbHeight();
		p.timeSeconds = (e.tickCount + partialTick) / 20f;
		p.phaseOffset = ((e.getId() * 0.618034f) % 1f) * Mth.TWO_PI;
		p.walkAmount = tr.walk;
		p.walkPhase = tr.dist * 4.8f;
		p.hurting = e.hurtTime > 0;

		boolean ground = e.onGround();
		float hurtF = e.hurtDuration > 0 ? e.hurtTime / (float) e.hurtDuration : 0f;

		// Respirar: pulsa a escala vertical, mais fraco quando andando
		if (cfg.breathing) {
			float b = Mth.sin(p.timeSeconds * Mth.TWO_PI / 3.4f + p.phaseOffset);
			float calm = 1f - Math.min(1f, tr.walk);
			p.sy *= 1f + b * 0.012f * k * (0.4f + 0.6f * calm);
			float xz = 1f - b * 0.006f * k * (0.4f + 0.6f * calm);
			p.sx *= xz;
			p.sz *= xz;
		}

		// Subir e descer a cada passo
		if (cfg.walkBob && ground) {
			p.dy += Math.abs(Mth.sin(p.walkPhase)) * 0.025f * Math.min(1f, tr.walk) * k;
		}

		// Inclinar para frente conforme a velocidade
		if (cfg.leanOnMove && ground) {
			p.pitch += Math.min(1.4f, tr.walk) * 3.5f * k;
		}

		// Inclinar para o lado ao virar
		if (cfg.turnRoll) {
			p.roll += tr.turnRoll * k;
		}

		// Achatar ao pousar e esticar ao subir
		if (cfg.landSquash) {
			float s = tr.squash;
			p.sy *= 1f - s * 0.20f * k;
			float wide = 1f + s * 0.09f * k;
			p.sx *= wide;
			p.sz *= wide;

			float up = Mth.clamp(MotionTracker.verticalSpeed(tr), 0f, 0.5f);
			if (!ground && up > 0f) {
				p.sy *= 1f + up * 0.14f * k;
				float thin = 1f - up * 0.06f * k;
				p.sx *= thin;
				p.sz *= thin;
			}
		}

		// Recuar ao levar dano
		if (cfg.hurtFlinch && hurtF > 0f) {
			float w = Mth.sin(hurtF * Mth.PI);
			p.pitch -= w * 6f * k;
			p.roll += w * 3f * k;
		}

		// Animacao customizada (Blockbench)
		if (cfg.customAnimations) {
			selectClip(e, p, partialTick, hurtF, pose == Pose.CROUCHING);
		}
	}

	private static void selectClip(LivingEntity e, ProceduralPose p, float partialTick, float hurtF, boolean sneaking) {
		String typeId = TYPE_IDS.computeIfAbsent(e.getType(), t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString());
		if (!CustomAnimationManager.hasBindings(typeId)) return;

		CustomAnimation clip = null;
		float time = 0f;

		if (hurtF > 0f && (clip = CustomAnimationManager.find(typeId, "hurt")) != null) {
			time = (1f - hurtF) * clip.length;
		} else if (e.isUsingItem() && (clip = CustomAnimationManager.find(typeId, "use")) != null) {
			time = p.timeSeconds;
		} else if (sneaking && (clip = CustomAnimationManager.find(typeId, "sneak")) != null) {
			time = p.timeSeconds;
		} else if (p.walkAmount > 0.1f && (clip = CustomAnimationManager.find(typeId, "walk")) != null) {
			// velocidade da animacao acompanha a velocidade do movimento
			time = p.walkPhase / Mth.TWO_PI * clip.length;
		} else {
			clip = CustomAnimationManager.find(typeId, "idle");
			time = p.timeSeconds + p.phaseOffset;
		}
		if (clip == null) return;

		if (clip.loop) {
			time = time % clip.length;
			if (time < 0f) time += clip.length;
		} else {
			time = Math.min(time, clip.length);
		}
		p.clip = clip;
		p.clipTime = time;
	}

	/** Aplica a pose no PoseStack. Origem = pes da entidade, eixo Y para cima (antes do flip do modelo). */
	public static void apply(LivingEntityRenderState state, PoseStack poseStack) {
		if (!(state instanceof AnimHolder holder)) return;
		ProceduralPose p = holder.fluidanim$pose();
		if (!p.valid) return;

		poseStack.translate(0f, p.dy, 0f);

		float c = p.height * 0.5f; // rotaciona em volta do meio do corpo
		poseStack.translate(0f, c, 0f);
		if (p.pitch != 0f || p.roll != 0f) {
			poseStack.mulPose(new Matrix4f().rotateX(p.pitch * Mth.DEG_TO_RAD).rotateZ(p.roll * Mth.DEG_TO_RAD));
		}
		poseStack.translate(0f, -c, 0f);

		poseStack.scale(p.sx, p.sy, p.sz); // escala a partir dos pes: pousar achata sem afundar no chao
	}
}
