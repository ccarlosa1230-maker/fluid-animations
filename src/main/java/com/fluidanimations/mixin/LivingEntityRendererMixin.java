package com.fluidanimations.mixin;

import com.fluidanimations.anim.ProceduralAnimator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cobre jogador, animais, monstros, aldeoes... (tudo que usa LivingEntityRenderer).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

	/** 1) Calcula a pose uma vez por frame, logo depois do vanilla preencher o render state. */
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"),
			require = 0)
	private void fluidanim$extract(T entity, S state, float partialTick, CallbackInfo ci) {
		ProceduralAnimator.extract(entity, state, partialTick);
	}

	/** 2) Aplica no PoseStack depois das rotacoes do vanilla (corpo virado, deitado, etc). */
	@Inject(
			method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
			at = @At("TAIL"),
			require = 0)
	private void fluidanim$setupRotations(S state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
		ProceduralAnimator.apply(state, poseStack);
	}
}
