package eu.the5zig.minecraft.protocol;

import eu.the5zig.minecraft.protocol.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PacketFindTask {

	private final File minecraftJarFile;
	private final File extractDestination;
	private final File decompileDestination;

	private int protocolVersion;
	private String protocolClassName;
	private String content;
	private List<Packet> packets = new ArrayList<>();

	public PacketFindTask(MinecraftVersion minecraftVersion) {
		this.minecraftJarFile = minecraftVersion.getFile();
		this.extractDestination = new File(Main.EXTRACTED_FILES_DIR, minecraftVersion.getVersion());
		extractDestination.mkdirs();
		this.decompileDestination = new File(Main.DECOMPILED_FILES_PARENT, minecraftVersion.getVersion());
		decompileDestination.mkdirs();
	}

	public synchronized void start() {
		try {
			findProtocolVersion();
			findProtocolClass();
			extractProtocolClass();
			decompileProtocolClass();
			fixProtocolClassImports();
			findPackets();
			extractPackets();
			decompilePackets();
			cleanUp();
			Logger.debug("Done.");
		} catch (Throwable throwable) {
			Logger.error("Could not extract Minecraft Packets!", throwable);
		}
	}

	public List<Packet> getPackets() {
		return packets;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	private void findProtocolVersion() throws IOException, InterruptedException {
		Logger.debug("Searching for Protocol Version...");
		String _GuiConnectingClass = getClassByStringConstant("Couldn't connect to server");
		Utils.extractClasses(minecraftJarFile, extractDestination, _GuiConnectingClass);
		Utils.decompile(decompileDestination, new File(extractDestination, _GuiConnectingClass));

		InputStream is = null;
		BufferedReader reader = null;
		String line;

		try {
			is = new FileInputStream(new File(decompileDestination, _GuiConnectingClass.substring(0, _GuiConnectingClass.lastIndexOf(".class")) + ".java"));
			reader = new BufferedReader(new InputStreamReader(is));

			int inetAddressLine = -1;
			while ((line = reader.readLine()) != null) {
				if (inetAddressLine != -1)
					inetAddressLine++;

				if (inetAddressLine == 3) {
					protocolVersion = Integer.valueOf(line.split("\\(|,")[3]);
					Logger.debug("Protocol Version: " + protocolVersion);
					return;
				}

				if (line.contains("InetAddress.getByName"))
					inetAddressLine = 0;
			}
		} finally {
			Utils.closeQuietly(reader);
			Utils.closeQuietly(is);
		}
		throw new RuntimeException("Could not find protocol Version!");
	}

	private void findProtocolClass() throws IOException {
		this.protocolClassName = getClassByStringConstant("is already known to ID");
	}

	private String getClassByStringConstant(String string) throws IOException {
		ZipInputStream zin = null;
		String line;
		String className = null;
		try {
			zin = new ZipInputStream(new FileInputStream(minecraftJarFile));
			for (ZipEntry e; (e = zin.getNextEntry()) != null; ) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
				while ((line = reader.readLine()) != null) {
					if (line.contains(string)) {
						className = e.getName();
						Logger.debug("Found Class (%s)", className);
						break;
					}
				}
			}
		} finally {
			Utils.closeQuietly(zin);
		}
		if (className == null)
			throw new RuntimeException("Could not find Class!");

		return className;
	}

	private void extractProtocolClass() throws IOException {
		Utils.extractClasses(minecraftJarFile, extractDestination, protocolClassName, protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + "$1.class",
				protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + "$2.class", protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + "$3.class",
				protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + "$4.class");
	}

	private void decompileProtocolClass() throws IOException, InterruptedException {
		Utils.decompile(decompileDestination, new File(extractDestination, protocolClassName), new File(extractDestination, protocolClassName.substring(0, protocolClassName.lastIndexOf(
				".class")) + "$1.class"), new File(extractDestination, protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + "$2.class"), new File(extractDestination,
				protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + "$3.class"), new File(extractDestination, protocolClassName.substring(0,
				protocolClassName.lastIndexOf(".class")) + "$4.class"));
	}

	private void fixProtocolClassImports() throws IOException {
		Logger.debug("Fixing Protocol Class Imports");
		File protocolFile = new File(decompileDestination, protocolClassName.substring(0, protocolClassName.lastIndexOf(".class")) + ".java");

		InputStream is = null;
		BufferedReader reader = null;
		String line;

		try {
			is = new FileInputStream(protocolFile);
			reader = new BufferedReader(new InputStreamReader(is));

			String previousLine = "";
			List<String> imports = new ArrayList<>();
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append(System.lineSeparator());

				if (line.startsWith("import ")) {
					previousLine += line;
					if (line.endsWith(";")) {
						String importClass = previousLine.replace("import ", "").replace(";", "");
						imports.add(importClass);

						previousLine = "";
					}
				}
			}
			content = stringBuilder.toString();
			imports.stream().filter(anImport -> anImport.contains(".")).forEach(anImport -> {
				String subClass = anImport.substring(anImport.lastIndexOf(".") + 1) + ".class";
				content = content.replace(", " + subClass, ", " + anImport + ".class");
			});
		} finally {
			Utils.closeQuietly(reader);
			Utils.closeQuietly(is);
		}
	}

	private void findPackets() throws IOException {
		ConnectionState currentConnectionState = ConnectionState.HANDSHAKE;
		int clientboundCount = 0, serverboundCount = 0;
		for (String line : content.split("\n")) {
			if (line.startsWith("         this.a")) {
				PacketDirection packetDirection = line.split("\\.|,")[2].equals("b") ? PacketDirection.CLIENTBOUND : PacketDirection.SERVERBOUND;
				if (packetDirection == PacketDirection.CLIENTBOUND) {
					Packet e = new Packet(currentConnectionState, packetDirection, clientboundCount++, line.split(", |\\)")[1]);
					packets.add(e);
					Logger.debug("Adding Packet " + e);
				} else {
					Packet e = new Packet(currentConnectionState, packetDirection, serverboundCount++, line.split(", |\\)")[1]);
					packets.add(e);
					Logger.debug("Adding Packet " + e);
				}
			} else if (line.contains("},") || line.contains("};")) {
				if (currentConnectionState.ordinal() < ConnectionState.values().length - 1)
					currentConnectionState = ConnectionState.values()[currentConnectionState.ordinal() + 1];
				clientboundCount = serverboundCount = 0;
			}
		}
	}

	private void extractPackets() throws IOException {
		List<String> packetNames = new ArrayList<>();
		packets.forEach(packet -> packetNames.add(packet.getClassName()));

		Utils.extractClasses(minecraftJarFile, extractDestination, packetNames.toArray(new String[packetNames.size()]));
	}

	private void decompilePackets() throws IOException, InterruptedException {
		List<File> packetFiles = new ArrayList<>();
		packets.forEach(packet -> packetFiles.add(new File(extractDestination, packet.getClassName())));

		Utils.decompile(decompileDestination, packetFiles.toArray(new File[packetFiles.size()]));
	}

	private void cleanUp() {

	}

}
