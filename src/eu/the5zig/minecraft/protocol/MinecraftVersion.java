package eu.the5zig.minecraft.protocol;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static eu.the5zig.minecraft.protocol.Logger.debug;
import static eu.the5zig.minecraft.protocol.Logger.info;

public class MinecraftVersion {

	private final File file;
	private final String version;

	private List<Packet> packets;
	private int protocolVersion;

	public MinecraftVersion(File file) {
		this.file = file;
		this.version = file.getName().substring(0, file.getName().lastIndexOf("."));
	}

	public File getFile() {
		return file;
	}

	public String getVersion() {
		return version;
	}

	public List<Packet> getPackets() {
		return packets;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void extractPackets() {
		debug("Extracting Packets from " + file.getName());
		PacketFindTask packetFindTask = new PacketFindTask(this);
		packetFindTask.start();
		this.packets = packetFindTask.getPackets();
		this.protocolVersion = packetFindTask.getProtocolVersion();
		info("Found " + packets.size() + " packets for protocol " + protocolVersion + " in Minecraft " + version + "!");
	}

	public void compare(MinecraftVersion original) throws IOException {
		info("=======================================");
		info("Comparing " + original.getVersion() + " to " + getVersion());
		info("=======================================");


		// check Protocol Version
		if (original.getProtocolVersion() != getProtocolVersion()) {
			info(original.getVersion() + " Protocol (" + original.getProtocolVersion() + ") does not match " + getVersion() + " (" + getProtocolVersion() + ")");
		} else {
			info("Protocol Version does match (" + getProtocolVersion() + ")");
		}

		LinkedHashMap<ConnectionState, LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>>> originalConnectionStates = new LinkedHashMap<>();
		for (Packet originalPacket : original.getPackets()) {
			LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>> packetDirections = originalConnectionStates.computeIfAbsent(originalPacket.getConnectionState(),
					connectionState -> new LinkedHashMap<>());
			LinkedHashMap<Integer, String> packets = packetDirections.computeIfAbsent(originalPacket.getPacketDirection(), packetDirection -> new LinkedHashMap<>());
			packets.put(originalPacket.getPacketId(), originalPacket.getClassName());
		}
		LinkedHashMap<ConnectionState, LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>>> compareConnectionStates = new LinkedHashMap<>();
		for (Packet comparePacket : getPackets()) {
			LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>> packetDirections = compareConnectionStates.computeIfAbsent(comparePacket.getConnectionState(),
					connectionState -> new LinkedHashMap<>());
			LinkedHashMap<Integer, String> packets = packetDirections.computeIfAbsent(comparePacket.getPacketDirection(), packetDirection -> new LinkedHashMap<>());
			packets.put(comparePacket.getPacketId(), comparePacket.getClassName());
		}

		for (Map.Entry<ConnectionState, LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>>> originalConnectionStateEntry : originalConnectionStates.entrySet()) {
			ConnectionState originalConnectionState = originalConnectionStateEntry.getKey();
			LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>> originalPacketDirections = originalConnectionStateEntry.getValue();
			LinkedHashMap<PacketDirection, LinkedHashMap<Integer, String>> comparePacketDirections = compareConnectionStates.get(originalConnectionState);
			if (comparePacketDirections == null) {
				info("Connection State " + originalConnectionState.toString() + " is not present in " + getVersion());
				continue;
			}
			for (Map.Entry<PacketDirection, LinkedHashMap<Integer, String>> originalPacketDirectionEntry : originalPacketDirections.entrySet()) {
				PacketDirection originalPacketDirection = originalPacketDirectionEntry.getKey();
				LinkedHashMap<Integer, String> originalPackets = originalPacketDirectionEntry.getValue();
				LinkedHashMap<Integer, String> comparePackets = comparePacketDirections.get(originalPacketDirection);
				if (comparePackets == null) {
					info("Packet Direction " + originalPacketDirection.toString() + " is not present in " + getVersion());
					continue;
				}
				for (Map.Entry<Integer, String> originalPacketEntry : originalPackets.entrySet()) {
					Integer originalPacketId = originalPacketEntry.getKey();
					String originalPacketName = originalPacketEntry.getValue();
					String comparePacketName = comparePackets.get(originalPacketId);
					if (comparePacketName == null) {
						info("Packet " + originalPacketName + " is not present in " + getVersion());
						continue;
					}

					if (!originalPacketName.equals(comparePacketName)) {
						Logger.info("Packet with id " + originalPacketId + " has changed from " + originalPacketName + " to " + comparePacketName);
					}

					if (originalPacketName.indexOf(".") != originalPacketName.indexOf(".class") || comparePacketName.indexOf(".") != comparePacketName.indexOf(".class")) { // inner class
						continue;
					}
					File originalPacket = new File(Main.DECOMPILED_FILES_PARENT + File.separator + original.getVersion(), originalPacketName.substring(0, originalPacketName.lastIndexOf(
							"" + ".class")) + ".java");
					File comparePacket = new File(Main.DECOMPILED_FILES_PARENT + File.separator + getVersion(), comparePacketName.substring(0, comparePacketName.lastIndexOf("" + ".class")) +
							".java");

					List<String> originalLines = toString(originalPacket);
					List<String> compareLines = toString(comparePacket);
					for (int i = 0; i < originalLines.size(); i++) {
						String originalLine = originalLines.get(i);
						for (int j = 0; j < compareLines.size(); j++) {
							String compareLine = compareLines.get(j);
							for (Packet o_packet : original.getPackets()) {
								String o_packet_name = o_packet.getClassName().substring(0, o_packet.getClassName().lastIndexOf(".class"));
								if (originalLine.contains(o_packet_name) && (originalLine.charAt(originalLine.indexOf(o_packet_name) + o_packet_name.length()) == ' ' ||
										(originalLine.indexOf(o_packet_name) > 0 && originalLine.charAt(originalLine.indexOf(o_packet_name) - 1) == ' ' && originalLine.charAt(
												originalLine.indexOf(o_packet_name) + o_packet_name.length()) == '('))) {
									for (Packet c_packet : getPackets()) {
										if (c_packet.equals(o_packet)) {
											compareLine = compareLine.replace(c_packet.getClassName().substring(0, c_packet.getClassName().lastIndexOf(".class")), o_packet_name);
										}
									}
								}
							}
							if (i == j && !originalLine.equals(compareLine)) {
								int levenshteinDistance = StringUtils.getLevenshteinDistance(originalLine, compareLine);
								// ignore changed minecraft names
								if (levenshteinDistance > 3) {
									Logger.info("Line " + (i + 1) + " in " + original.getVersion() + "#" + originalPacketName + " is different to " + getVersion() + "#" + comparePacketName +
											" (Packet id " + originalPacketId + ")");
								}
							}
						}
					}
				}
			}
		}
	}

	private static List<String> toString(File file) throws IOException {
		List<String> result = new ArrayList<String>();
		InputStream inputStream = null;
		BufferedReader reader = null;
		try {
			inputStream = new FileInputStream(file);
			reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} finally {
			if (reader != null)
				reader.close();
			if (inputStream != null)
				inputStream.close();
		}
		return result;
	}
}
