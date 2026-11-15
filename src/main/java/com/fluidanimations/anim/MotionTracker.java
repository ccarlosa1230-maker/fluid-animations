package com.fluidanimations.anim;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Guarda, por entidade, o historico necessario para suavizar movimentos entre frames:
 * velocidade de caminhada filtrada, distancia andada, inclinacao ao virar e mola de pouso.
 * So e usado na thread de render.
 */
final class MotionTracker {
	static final class Track {
		boolean init;
		long lastNanos;
		long lastSeenNanos;
		float lastBodyYaw;
		boolean wasOnGround = true;
		float lastVy;

		float walk;       // 0..~1.5, suavizado
		float dist;       // blocos andados (acumulado)
		float turnRoll;   // graus
		float squash;     // mola de achatamento (positivo = achatado)
		float squashVel;
	}

	private static final Map<Integer, Track> TRACKS = new HashMap<>();
	private static long lastCleanup;

	private MotionTracker() {}

	static Track update(LivingEntity e) {
		long now = System.nanoTime();
		Track t = TRACKS.computeIfAbsent(e.getId(), id -> new Track());
		t.lastSeenNanos = now;

		float dt = t.init ? (now - t.lastNanos) / 1_000_000_000f : 0f;
		dt = Mth.clamp(dt, 0f, 0.1f);
		t.lastNanos = now;

		float dx = (float) (e.getX() - e.xo);
		float dz = (float) (e.getZ() - e.zo);
		float vy = (float) (e.getY() - e.yo);
		float hSpeed = (float) Math.sqrt(dx * dx + dz * dz); // blocos por tick

		// Velocidade de caminhada normalizada (0.2 blocos/tick ~ andar normal de jogador)
		float walkTarget = Mth.clamp(hSpeed / 0.2f, 0f, 1.6f);
		t.walk += (walkTarget - t.walk) * Math.min(1f, dt * 10f);
		t.dist += hSpeed * 20f * dt;

		// Inclinacao ao virar
		if (!t.init) t.lastBodyYaw = e.yBodyRot;
		if (dt > 1e-4f) {
			float yawVel = Mth.wrapDegrees(e.yBodyRot - t.lastBodyYaw) / dt; // graus/s
			float target = Mth.clamp(-yawVel * 0.012f, -7f, 7f) * Math.min(1f, t.walk * 1.5f);
			t.turnRoll += (target - t.turnRoll) * Math.min(1f, dt * 8f);
		}
		t.lastBodyYaw = e.yBodyRot;

		// Mola de pouso: ao tocar o chao, empurra a mola proporcional a velocidade da queda
		boolean onGround = e.onGround();
		if (onGround && !t.wasOnGround && t.lastVy < -0.1f) {
			t.squashVel += Math.min(1.6f, -t.lastVy * 5f);
		}
		t.wasOnGround = onGround;
		t.lastVy = vy;

		if (dt > 0f) {
			final float k = 180f, c = 14f;
			t.squashVel += (-k * t.squash - c * t.squashVel) * dt;
			t.squash += t.squashVel * dt;
			t.squash = Mth.clamp(t.squash, -0.5f, 1.2f);
		}

		t.init = true;
		cleanup(now);
		return t;
	}

	static float verticalSpeed(Track t) {
		return t.lastVy;
	}

	private static void cleanup(long now) {
		if (now - lastCleanup < 5_000_000_000L) return;
		lastCleanup = now;
		Iterator<Track> it = TRACKS.values().iterator();
		while (it.hasNext()) {
			if (now - it.next().lastSeenNanos > 5_000_000_000L) it.remove();
		}
	}
}
