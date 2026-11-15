package com.fluidanimations.anim;

/**
 * Resultado da animacao calculada para UMA entidade em UM frame. Fica guardado dentro do
 * render state (ver LivingEntityRenderStateMixin) para ser lido depois pelos hooks de render/modelo.
 */
public final class ProceduralPose {
	/** false = nao aplicar nada (morto, dormindo, nadando, desligado...). */
	public boolean valid;

	// Transformacao do corpo inteiro (aplicada no PoseStack)
	public float dy;            // deslocamento vertical em blocos
	public float pitch;         // graus, positivo = inclina para frente
	public float roll;          // graus, inclinacao lateral
	public float sx = 1f, sy = 1f, sz = 1f;
	public float height = 1f;   // altura da entidade (pivo no meio do corpo)

	// Dados para os modelos (membros)
	public float intensity = 1f;
	public float walkPhase;     // fase do passo (radianos, cresce com a distancia andada)
	public float walkAmount;    // 0 = parado, 1 = andando normal, >1 = correndo
	public float timeSeconds;   // tempo da entidade em segundos
	public float phaseOffset;   // deslocamento aleatorio por entidade (evita rebanho sincronizado)
	public boolean hurting;

	// Animacao customizada escolhida para este frame (pode ser null)
	public CustomAnimation clip;
	public float clipTime;

	public void reset() {
		valid = false;
		dy = pitch = roll = 0f;
		sx = sy = sz = 1f;
		height = 1f;
		intensity = 1f;
		walkPhase = walkAmount = timeSeconds = phaseOffset = 0f;
		hurting = false;
		clip = null;
		clipTime = 0f;
	}
}
