package com.fluidanimations.mixin;

import com.fluidanimations.config.AnimConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Objetos: itens soltos no chao. O vanilla usa ageInTicks para girar e flutuar o item;
 * escalar esse tempo deixa o movimento mais lento e suave.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
			at = @At("TAIL"),
			require = 0)
	private void fluidanim$items(T entity, S state, float partialTick, CallbackInfo ci) {
		AnimConfig cfg = AnimConfig.INSTANCE;
		if (cfg.enabled && cfg.droppedItems && entity instanceof ItemEntity) {
			state.ageInTicks *= cfg.droppedItemSpeed;
		}
	}
}
