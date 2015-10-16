package eu.the5zig.minecraft.protocol.utils;

import eu.the5zig.minecraft.protocol.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConsoleReaderThread extends Thread {

	private final InputStream inputStream;
	private final BufferedReader reader;

	public ConsoleReaderThread(InputStream inputStream) {
		super("Console Reader Thread");
		this.inputStream = inputStream;
		this.reader = new BufferedReader(new InputStreamReader(inputStream));
		setDaemon(true);
		start();
	}

	@Override
	public void run() {
		try {
			while (inputStream.available() >= 0) {
				String inputLine = reader.readLine();
				if (inputLine != null)
					Logger.debug(inputLine);
			}
		} catch (Throwable e) {
			Logger.error("An Exception occured while reading from input stream!", e);
		} finally {
			Utils.closeQuietly(reader);
			Utils.closeQuietly(inputStream);
		}
	}
}
