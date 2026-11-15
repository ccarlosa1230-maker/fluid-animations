package com.fluidanimations.mixin;

import com.fluidanimations.anim.AnimHolder;
import com.fluidanimations.anim.ProceduralPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Faz todo LivingEntityRenderState carregar uma ProceduralPose. */
@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements AnimHolder {
	@Unique
	private final ProceduralPose fluidanim$pose = new ProceduralPose();

	@Override
	public ProceduralPose fluidanim$pose() {
		return fluidanim$pose;
	}
}
