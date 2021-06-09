package io.cubyz.ndt;

import io.cubyz.math.Bits;

@Deprecated
public class NDTFloat extends NDTTag {

	public NDTFloat()
	{
		this.expectedLength = 4;
		this.type = NDTConstants.TYPE_FLOAT;
	}
	
	public float getValue() {
		return Bits.getFloat(this.content, 0);
	}
	
	public void setValue(float i) {
		this.content = new byte[4];
		Bits.putFloat(this.content, 0, i);
	}
	
	public String toString() {
		return "NDTFloat[value=" + getValue() + "]";
	}
	
}
