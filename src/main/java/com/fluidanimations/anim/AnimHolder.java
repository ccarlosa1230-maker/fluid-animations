package com.fluidanimations.anim;

/** Interface injetada (via mixin) em LivingEntityRenderState para carregar a pose junto do estado. */
public interface AnimHolder {
	ProceduralPose fluidanim$pose();
}
