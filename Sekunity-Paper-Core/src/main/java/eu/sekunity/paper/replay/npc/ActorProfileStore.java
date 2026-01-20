package eu.sekunity.paper.replay.npc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/**
 * © Copyright 17.01.2026 - 15:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ActorProfileStore {

	private ActorProfileStore() {}

	public static void write(Path file, Collection<ActorProfile> profiles) throws IOException {
		Files.createDirectories(file.getParent());
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
			out.writeInt(0x534B4150); // 'SKAP'
			out.writeShort(1);

			out.writeInt(profiles.size());
			for (ActorProfile p : profiles) {
				writeUuid(out, p.uuid());
				writeString(out, p.name());
				writeString(out, p.prefix());

				ActorSkin s = p.skin();
				writeString(out, s != null ? s.value() : null);
				writeString(out, s != null ? s.signature() : null);
			}
		}
	}

	public static Map<UUID, ActorProfile> read(Path file) throws IOException {
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
			int magic = in.readInt();
			if (magic != 0x534B4150) throw new IOException("Invalid actors magic");
			short ver = in.readShort();
			if (ver != 1) throw new IOException("Unsupported actors version: " + ver);

			int count = in.readInt();
			Map<UUID, ActorProfile> map = new HashMap<>();
			for (int i = 0; i < count; i++) {
				UUID u = readUuid(in);
				String name = readString(in);
				String prefix = readString(in);
				String v = readString(in);
				String sig = readString(in);

				ActorSkin skin = (v != null && sig != null) ? new ActorSkin(v, sig) : null;
				map.put(u, new ActorProfile(u, name, prefix, skin));
			}
			return map;
		}
	}

	private static void writeString(DataOutputStream out, String s) throws IOException {
		if (s == null) { out.writeInt(-1); return; }
		byte[] b = s.getBytes(StandardCharsets.UTF_8);
		out.writeInt(b.length);
		out.write(b);
	}

	private static String readString(DataInputStream in) throws IOException {
		int len = in.readInt();
		if (len == -1) return null;
		byte[] b = in.readNBytes(len);
		return new String(b, StandardCharsets.UTF_8);
	}

	private static void writeUuid(DataOutputStream out, UUID u) throws IOException {
		out.writeLong(u.getMostSignificantBits());
		out.writeLong(u.getLeastSignificantBits());
	}

	private static UUID readUuid(DataInputStream in) throws IOException {
		long most = in.readLong();
		long least = in.readLong();
		return new UUID(most, least);
	}
}
