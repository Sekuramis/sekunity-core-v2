package eu.sekunity.paper.service.nick;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.sekunity.api.service.SkinProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * © Copyright 12.01.2026 - 19:09 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class MojangSkinImporter
{
	private final HttpClient http;
	private final Executor async;

	public MojangSkinImporter(Executor async)
	{
		this.http = HttpClient.newHttpClient();
		this.async = async;
	}

	public CompletableFuture<SkinProfile> fetch(UUID uuid)
	{
		String plain = uuid.toString().replace("-", "");
		return fetchProfileFromSessionServer(uuid, plain);
	}

	private CompletableFuture<SkinProfile> fetchProfileFromSessionServer(UUID uuid, String uuidNoDashes)
	{
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoDashes + "?unsigned=false"))
				.GET()
				.build();

		return http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApplyAsync(res -> {
			if (res.statusCode() != 200)
				throw new IllegalStateException("sessionserver lookup failed: http " + res.statusCode() + " body=" + res.body());

			JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();

			String name = root.has("name") && !root.get("name").isJsonNull() ? root.get("name").getAsString() : "";
			if (name.isBlank())
				throw new IllegalStateException("missing name in sessionserver response");

			JsonArray props = root.getAsJsonArray("properties");
			if (props == null || props.isEmpty())
				throw new IllegalStateException("missing properties in sessionserver response");

			String value = null;
			String signature = null;

			for (JsonElement el : props)
			{
				JsonObject o = el.getAsJsonObject();
				if (!o.has("name")) continue;

				String propName = o.get("name").getAsString();
				if (!"textures".equals(propName)) continue;

				value = o.has("value") && !o.get("value").isJsonNull() ? o.get("value").getAsString() : null;
				signature = o.has("signature") && !o.get("signature").isJsonNull() ? o.get("signature").getAsString() : null;
				break;
			}

			if (value == null || value.isBlank())
				throw new IllegalStateException("textures value missing");

			if (signature == null || signature.isBlank())
				throw new IllegalStateException("textures signature missing (need unsigned=false)");

			return new SkinProfile(uuid, name, value, signature);
		}, async);
	}
}
