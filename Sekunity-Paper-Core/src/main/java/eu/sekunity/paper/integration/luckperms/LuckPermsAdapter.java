package eu.sekunity.paper.integration.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * © Copyright 11.01.2026 - 17:16 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public interface LuckPermsAdapter
{
	CompletableFuture<String> prefix(UUID uuid);
	CompletableFuture<String> suffix(UUID uuid);
	CompletableFuture<String> primaryGroup(UUID uuid);
	int weight(UUID uuid);
}
