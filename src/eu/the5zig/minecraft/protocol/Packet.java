package eu.the5zig.minecraft.protocol;

public class Packet {

	private final ConnectionState connectionState;
	private final PacketDirection packetDirection;
	private final int packetId;
	private final String className;

	public Packet(ConnectionState connectionState, PacketDirection packetDirection, int packetId, String className) {
		this.connectionState = connectionState;
		this.packetDirection = packetDirection;
		this.packetId = packetId;
		this.className = className;
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	public PacketDirection getPacketDirection() {
		return packetDirection;
	}

	public int getPacketId() {
		return packetId;
	}

	public String getClassName() {
		return className;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Packet packet = (Packet) o;

		if (packetId != packet.packetId)
			return false;
		if (connectionState != packet.connectionState)
			return false;
		return packetDirection == packet.packetDirection;

	}

	@Override
	public int hashCode() {
		int result = connectionState != null ? connectionState.hashCode() : 0;
		result = 31 * result + (packetDirection != null ? packetDirection.hashCode() : 0);
		result = 31 * result + packetId;
		return result;
	}

	@Override
	public String toString() {
		return "Packet{" +
				"connectionState=" + connectionState +
				", packetDirection=" + packetDirection +
				", packetId=" + packetId +
				", className='" + className + '\'' +
				'}';
	}
}
