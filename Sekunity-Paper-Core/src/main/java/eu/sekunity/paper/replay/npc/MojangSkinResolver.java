package eu.sekunity.paper.replay.npc;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
/**
 * © Copyright 17.01.2026 - 16:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class MojangSkinResolver {

	private MojangSkinResolver() {}

	public record Skin(String value, String signature) {}

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	public static void resolveAsync(JavaPlugin plugin, UUID uuid, Consumer<Skin> cb) {
		CompletableFuture.runAsync(() -> {
			try {
				Skin skin = resolve(uuid);
				if (skin != null) {
					cb.accept(skin);
				} else {
					plugin.getLogger().warning("[Replay] Skin resolve returned null for " + uuid);
				}
			} catch (Exception ex) {
				plugin.getLogger().warning("[Replay] Skin resolve failed for " + uuid + ": " + ex.getMessage());
			}
		});
	}

	public static Skin resolve(UUID uuid) throws Exception {
		// sessionserver expects uuid without dashes
		String u = uuid.toString().replace("-", "");
		URI uri = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + u + "?unsigned=false");

		HttpRequest req = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(8))
				.GET()
				.build();

		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (res.statusCode() != 200) return null;

		String body = res.body();

		// super-minimal JSON extraction (keine deps):
		// "name":"...","properties":[{"name":"textures","value":"...","signature":"..."}]
		int pIdx = body.indexOf("\"properties\"");
		if (pIdx < 0) return null;

		int texturesIdx = body.indexOf("\"name\":\"textures\"", pIdx);
		if (texturesIdx < 0) return null;

		String value = extractJsonString(body, "\"value\":\"", texturesIdx);
		String sig = extractJsonString(body, "\"signature\":\"", texturesIdx);

		if (value == null || sig == null) return null;
		return new Skin(value, sig);
	}

	private static String extractJsonString(String body, String key, int from) {
		int i = body.indexOf(key, from);
		if (i < 0) return null;
		i += key.length();
		int end = body.indexOf('"', i);
		if (end < 0) return null;
		return body.substring(i, end);
	}
}
