package com.fluidanimations.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uma animacao no formato "Bedrock Animation" (o .animation.json que o Blockbench exporta).
 * Suporta rotation / position / scale por osso, com interpolacao linear entre keyframes.
 * Valores com expressoes Molang (strings que nao sao numeros) viram 0.
 */
public final class CustomAnimation {
	public final String name;
	public final boolean loop;
	public final float length;
	public final Map<String, Bone> bones;

	private CustomAnimation(String name, boolean loop, float length, Map<String, Bone> bones) {
		this.name = name;
		this.loop = loop;
		this.length = length;
		this.bones = bones;
	}

	public record Bone(Channel rotation, Channel position, Channel scale) {}

	public static final class Channel {
		private final float[] times;
		private final float[][] values;

		Channel(float[] times, float[][] values) {
			this.times = times;
			this.values = values;
		}

		float lastTime() {
			return times.length == 0 ? 0f : times[times.length - 1];
		}

		/** Escreve em out[0..2] o valor no tempo t (segundos). */
		public void sample(float t, float[] out) {
			int n = times.length;
			if (n == 1 || t <= times[0]) {
				System.arraycopy(values[0], 0, out, 0, 3);
				return;
			}
			if (t >= times[n - 1]) {
				System.arraycopy(values[n - 1], 0, out, 0, 3);
				return;
			}
			int i = 1;
			while (i < n - 1 && times[i] < t) i++;
			float t0 = times[i - 1], t1 = times[i];
			float f = t1 > t0 ? (t - t0) / (t1 - t0) : 0f;
			for (int k = 0; k < 3; k++) {
				out[k] = values[i - 1][k] + (values[i][k] - values[i - 1][k]) * f;
			}
		}
	}

	// ------------------------------------------------------------------ parsing

	public static CustomAnimation parse(String name, JsonObject json) {
		boolean loop = false;
		if (json.has("loop")) {
			JsonElement l = json.get("loop");
			loop = l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean() && l.getAsBoolean();
		}

		Map<String, Bone> bones = new HashMap<>();
		float maxTime = 0f;
		if (json.has("bones") && json.get("bones").isJsonObject()) {
			for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("bones").entrySet()) {
				if (!e.getValue().isJsonObject()) continue;
				JsonObject b = e.getValue().getAsJsonObject();
				Channel rot = parseChannel(b.get("rotation"), 0f);
				Channel pos = parseChannel(b.get("position"), 0f);
				Channel scl = parseChannel(b.get("scale"), 1f);
				for (Channel c : new Channel[]{rot, pos, scl}) {
					if (c != null) maxTime = Math.max(maxTime, c.lastTime());
				}
				bones.put(e.getKey(), new Bone(rot, pos, scl));
			}
		}

		float length = maxTime;
		if (json.has("animation_length") && json.get("animation_length").isJsonPrimitive()) {
			length = json.get("animation_length").getAsFloat();
		}
		if (length <= 0f) length = 1f;
		return new CustomAnimation(name, loop, length, bones);
	}

	private static Channel parseChannel(JsonElement el, float fallback) {
		if (el == null || el.isJsonNull()) return null;

		// Valor constante: [x,y,z] ou numero unico
		if (el.isJsonArray() || el.isJsonPrimitive()) {
			return new Channel(new float[]{0f}, new float[][]{vec(el, fallback)});
		}

		// Keyframes: { "0.0": [..], "0.5": { "post": [..] } }
		List<float[]> entries = new ArrayList<>();
		for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
			float time;
			try {
				time = Float.parseFloat(e.getKey());
			} catch (NumberFormatException ex) {
				continue;
			}
			JsonElement v = e.getValue();
			if (v.isJsonObject()) {
				JsonObject o = v.getAsJsonObject();
				v = o.has("post") ? o.get("post") : o.get("pre");
			}
			float[] value = vec(v, fallback);
			entries.add(new float[]{time, value[0], value[1], value[2]});
		}
		if (entries.isEmpty()) return null;
		entries.sort((a, b) -> Float.compare(a[0], b[0]));

		float[] times = new float[entries.size()];
		float[][] values = new float[entries.size()][3];
		for (int i = 0; i < entries.size(); i++) {
			float[] en = entries.get(i);
			times[i] = en[0];
			values[i] = new float[]{en[1], en[2], en[3]};
		}
		return new Channel(times, values);
	}

	private static float[] vec(JsonElement el, float fallback) {
		float[] out = {fallback, fallback, fallback};
		if (el == null) return out;
		if (el.isJsonPrimitive()) {
			float v = num(el.getAsJsonPrimitive(), fallback);
			return new float[]{v, v, v};
		}
		if (el.isJsonArray()) {
			JsonArray a = el.getAsJsonArray();
			for (int i = 0; i < 3 && i < a.size(); i++) {
				JsonElement item = a.get(i);
				out[i] = item.isJsonPrimitive() ? num(item.getAsJsonPrimitive(), fallback) : fallback;
			}
		}
		return out;
	}

	private static float num(JsonPrimitive p, float fallback) {
		if (p.isNumber()) return p.getAsFloat();
		try {
			return Float.parseFloat(p.getAsString());
		} catch (NumberFormatException e) {
			return fallback; // expressao Molang: ignorada
		}
	}
}
