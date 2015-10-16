package eu.the5zig.minecraft.protocol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

	public static final String MINECRAFT_FILES_DIR = "files";
	public static final String EXTRACTED_FILES_DIR = "tmp";
	public static final String DECOMPILED_FILES_PARENT = "decomp";

	public static void main(String[] args) throws IOException, InterruptedException {
		File dir = new File(MINECRAFT_FILES_DIR);
		File original = new File(dir, "original.jar");
		if (!dir.exists() || !original.exists())
			throw new FileNotFoundException();

		File[] files = dir.listFiles();
		if (files == null)
			throw new FileNotFoundException();

		MinecraftVersion originalVersion = new MinecraftVersion(original);
		originalVersion.extractPackets();
		for (File file : files) {
			if (file.isDirectory() || !file.getName().endsWith(".jar") || file.getName().equals("original.jar"))
				continue;
			MinecraftVersion minecraftVersion = new MinecraftVersion(file);
			minecraftVersion.extractPackets();
			minecraftVersion.compare(originalVersion);
		}

	}

}
