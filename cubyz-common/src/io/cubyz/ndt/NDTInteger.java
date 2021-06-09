package io.cubyz.ndt;

import io.cubyz.math.Bits;

@Deprecated
public class NDTInteger extends NDTTag {
	public NDTInteger()
	{
		this.expectedLength = 4;
		this.type = NDTConstants.TYPE_INT;
	}
	
	public int getValue() {
		return Bits.getInt(this.content, 0);
	}
	
	public void setValue(int i) {
		this.content = new byte[4];
		Bits.putInt(this.content, 0, i);
	}
	
	public String toString() {
		return "NDTInteger[value=" + getValue() + "]";
	}
	
}
