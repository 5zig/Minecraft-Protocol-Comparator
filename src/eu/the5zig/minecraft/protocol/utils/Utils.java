package eu.the5zig.minecraft.protocol.utils;

import eu.the5zig.minecraft.protocol.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static eu.the5zig.minecraft.protocol.Logger.debug;
import static eu.the5zig.minecraft.protocol.Logger.info;

public class Utils {

	private Utils() {
	}

	public static void decompile(File destination, File... classFiles) throws IOException, InterruptedException {
		if (classFiles.length == 0)
			throw new IllegalArgumentException("Class Files count may not be zero!");

		List<String> args = new ArrayList<>();
		args.add("java");
		args.add("-jar");
		args.add("fernflower.jar");

		int classCount = 0;
		for (File classFile : classFiles) {
			if (classFile.getName().endsWith(".class")) {
				File destFile = new File(destination, classFile.getName().substring(0, classFile.getName().lastIndexOf(".class")) + ".java");
				if (destFile.exists()) {
					Logger.debug("Skipping decompilation of " + classFile.getName() + " since it has been decompiled!");
					continue;
				}
				args.add(classFile.getAbsolutePath());
				classCount++;
			}
		}
		if (classCount == 0) {
			Logger.info("Aborting decompilation progress: No classes need to be decompiled!");
			return;
		}
		args.add(destination.getAbsolutePath());

		info("Decompiling " + classCount + " files to " + destination.getAbsolutePath() + " using fernflower");
		debug("Launch Args: " + args.toString());

		Process process = new ProcessBuilder(args).start();
		ConsoleReaderThread inputReader = new ConsoleReaderThread(process.getInputStream());
		ConsoleReaderThread errorReader = new ConsoleReaderThread(process.getErrorStream());

		int exitCode = process.waitFor();
		info("Decompilation ended with exit code " + exitCode + "!");
		inputReader.interrupt();
		errorReader.interrupt();
	}

	public static void extractClasses(File file, File dest, String... classNames) throws IOException {
		ZipFile zipFile = new ZipFile(file);
		for (String className : classNames) {
			if (className.indexOf(".") != className.indexOf(".class")) { // inner class
				className = className.substring(0, className.indexOf(".")) + "$" + className.substring(className.indexOf(".") + 1);
			}
			ZipEntry entry = zipFile.getEntry(className);
			if (entry == null) {
				Logger.error("Could not find class " + className + " in " + file);
				continue;
			}

			InputStream is = null;
			OutputStream out = null;
			try {
				is = zipFile.getInputStream(entry);
				File fileDest = new File(dest, entry.getName());
				if (fileDest.exists()) {
					Logger.debug("Skipping " + entry.getName() + " since it already exists!");
					continue;
				}
				out = new FileOutputStream(fileDest);

				byte[] buf = new byte[8192];
				int count;
				while ((count = is.read(buf)) > 0) {
					out.write(buf, 0, count);
				}

				Logger.debug("Extracted class " + className);
			} finally {
				closeQuietly(is);
				closeQuietly(out);
			}
		}

	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable == null)
			return;
		try {
			closeable.close();
		} catch (IOException ignored) {
		}
	}
}
