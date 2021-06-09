package io.cubyz.ndt;

import io.cubyz.Logger;

/**
 * NDT (Named Data Tag)
 * @author zenith391
 */
@Deprecated
public class NDTTag {

	protected byte type;
	protected byte[] content;
	protected int expectedLength;
	
	public byte getType() {
		return this.type;
	}
	
	public boolean validate() {
		return this.content.length == this.expectedLength;
	}
	
	public void setBytes(byte[] bytes) {
		this.content = bytes;
	}
	
	public void setByte(int index, byte b) {
		this.content[index] = b;
	}
	
	public byte[] getData() {
		return this.content;
	}
	
	public static NDTTag fromBytes(byte[] bytes) {
		byte[] tagBytes = NDTContainer.subArray(1, bytes.length, bytes);
		if (bytes[0] == NDTConstants.TYPE_INT) {
			NDTInteger ndt = new NDTInteger();
			ndt.setBytes(tagBytes);
			return ndt;
		}
		if (bytes[0] == NDTConstants.TYPE_LONG) {
			NDTLong ndt = new NDTLong();
			ndt.setBytes(tagBytes);
			return ndt;
		}
		if (bytes[0] == NDTConstants.TYPE_FLOAT) {
			NDTFloat ndt = new NDTFloat();
			ndt.setBytes(tagBytes);
			return ndt;
		}
		if (bytes[0] == NDTConstants.TYPE_STRING) {
			NDTString ndt = new NDTString();
			ndt.setBytes(tagBytes);
			return ndt;
		}
		if (bytes[0] == NDTConstants.TYPE_CONTAINER) {
			return new NDTContainer(tagBytes);
		}
		Logger.warning("Unknown NDT tag type: " + bytes[0]);
		return null;
	}
	
}
