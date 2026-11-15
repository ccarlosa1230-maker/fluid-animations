package com.fluidanimations.mixin;

import com.fluidanimations.anim.ModelAnimator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jogador, zumbi, esqueleto, piglin... */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

	@Inject(
			method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
			at = @At("TAIL"),
			require = 0)
	private void fluidanim$setupAnim(HumanoidRenderState state, CallbackInfo ci) {
		ModelAnimator.apply((EntityModel<?>) (Object) this, state, ModelAnimator.Kind.HUMANOID);
	}
}
