package com.fluidanimations.mixin;

import com.fluidanimations.anim.ModelAnimator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Porco, vaca, ovelha e outros animais de 4 patas que usam QuadrupedModel. */
@Mixin(QuadrupedModel.class)
public abstract class QuadrupedModelMixin {

	@Inject(
			method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)V",
			at = @At("TAIL"),
			require = 0)
	private void fluidanim$setupAnim(LivingEntityRenderState state, CallbackInfo ci) {
		ModelAnimator.apply((EntityModel<?>) (Object) this, state, ModelAnimator.Kind.QUADRUPED);
	}
}
