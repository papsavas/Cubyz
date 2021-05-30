package io.cubyz.gpack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.cubyz.Constants;

public class MessagePack {
	
	enum StreamType {
	    OBJECT_TYPE,
	    NULL,
	    PRIMITIVE
	  }
	
	enum PrimitiveType {
		PRIMITIVE_BOOLEAN,
		PRIMITIVE_STRING,
		PRIMITIVE_NUMBER
	}
	
	enum NumberType {
		INTEGER_POSITIVE,
		INTEGER_NEGATIVE,
		FLOAT,
		DOUBLE
	}

	public static byte[] encode(JsonElement json) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		StreamType JsonElementType = null;
		
		if(json.isJsonObject()) {
			JsonElementType = StreamType.OBJECT_TYPE;
		}
		
		if (json.isJsonNull()) {
			JsonElementType = StreamType.NULL;
		}
		
		if (json.isJsonPrimitive()) {
			JsonElementType = StreamType.PRIMITIVE;
		}
			
		switch(JsonElementType) {
			case OBJECT_TYPE:
				WriteObject(json,out);
				break;
			case NULL:
				WriteNull(out);
				break;
			case PRIMITIVE:
				WritePrimitive(json,out);
				break;
				
		}	
		out.close();
		return baos.toByteArray();
	}
	
	public static void WriteObject(JsonElement json,DataOutputStream out) throws IOException {
		JsonObject obj = json.getAsJsonObject();
		int size = obj.size();
		if (size <= 0xF) {
			out.write(0b10000000 | size);
		} else if (size <= 0xFFFF) {
			if (size <= 0xF) {
				out.write(0b10000000 | size);
			}
			out.write(0xDE);
			out.writeShort((short) size);
		} else {
			out.write(0xDF);
			out.writeInt(size);
		}
		
		for (Entry<String, JsonElement> entry : obj.entrySet()) {
			out.write(encode(new JsonPrimitive(entry.getKey())));
			out.write(encode(entry.getValue()));
		}
	}
	
	public static void WriteNull(DataOutputStream out) throws IOException{
		out.write(0xC0);
	}
	
	
	public static void WritePrimitive(JsonElement json,DataOutputStream out) throws IOException {
		JsonPrimitive prim = json.getAsJsonPrimitive();
		PrimitiveType PrimType = null;
		
		if (prim.isBoolean()) {
			PrimType = PrimitiveType.PRIMITIVE_BOOLEAN;
		}
		if (prim.isString()) {
			PrimType = PrimitiveType.PRIMITIVE_STRING;
		}
		if (prim.isNumber()) {
			PrimType = PrimitiveType.PRIMITIVE_NUMBER;
		}
		
		switch(PrimType) {
		case PRIMITIVE_BOOLEAN:
			WritePrimitiveBoolean(prim,out);
			break;
		case PRIMITIVE_STRING:
			WritePrimitiveString(prim,out);
			break;
		case PRIMITIVE_NUMBER:
			WritePrimitiveNumber(prim,out);
			break;
		}
	}
	
	public static void WritePrimitiveBoolean(JsonPrimitive prim,DataOutputStream out)throws IOException {
		out.write(prim.getAsBoolean() ? 0xc3 : 0xc2);
	}
	
	public static void WritePrimitiveString(JsonPrimitive prim,DataOutputStream out)throws IOException {
		String str = prim.getAsString();
		int len = str.length();
		if (len < 32) { // fixstr
			out.write(0b10100000 | len);
			out.write(str.getBytes(Constants.CHARSET));
		} else if (len < 0xFF) { // str 8
			out.write(0xD9);
			out.write(len);
			out.write(str.getBytes(Constants.CHARSET));
		} else if (len < 0xFFFF) { // str 16
			out.write(0xDA);
			out.writeShort(len);
			out.write(str.getBytes(Constants.CHARSET));
		} else if (len < 0xFFFFFFFF) { // str 32
			out.write(0xDB);
			out.writeInt(len);
			out.write(str.getBytes(Constants.CHARSET));
		}
	}
	
	public static void WriteDouble(double doubleNum,DataOutputStream out) throws IOException {
		out.write(0xCB);
		out.writeDouble(doubleNum);
	}
	
	public static void WriteFloat(float singleNum,DataOutputStream out) throws IOException {
		out.write(0xCA);
		out.writeFloat(singleNum);
	}
	
	public static void WritePositive(long num,DataOutputStream out) throws IOException {
		if (num <= 0x7F) { // 7-bit positive
			out.write((int) num);
		} else if (num <= 0xFF) { // 8-bit unsigned
			out.write(0xCC);
			out.write((int) num);
		} else if (num <= 0xFFFF) { // 16-bit unsigned
			out.write(0xCD);
			out.writeShort((short) num);
		} else if (num <= 0xFFFFFFFFL) { // 32-bit unsigned
			out.write(0xCE);
			out.writeInt((int) num);
		} else { // 64-bit unsigned (actually 63-bit due to Java's long being signed)
			out.write(0xCF);
			out.writeLong(num);
		}
	}
	
	public static void WriteNegative(long num,DataOutputStream out) throws IOException {
		if (num >= -0x1F) { // 5-bit negative
			int b = 0b11100000 | Math.abs((int) num);
			out.write(b);
		} else if (num >= Byte.MIN_VALUE) { // 8-bit signed
			out.write(0xD0);
			out.write((int) num);
		} else if (num >= Short.MIN_VALUE) { // 16-bit signed
			out.write(0xD1);
			out.writeShort((short) num);
		} else if (num >= Integer.MIN_VALUE) { // 32-bit signed
			out.write(0xD2);
			out.writeInt((int) num);
		} else { // 64-bit signed
			out.write(0xD3);
			out.writeLong(num);
		}
	}
	
	public static void WritePrimitiveNumber(JsonPrimitive prim,DataOutputStream out)throws IOException {
		long num = prim.getAsLong();
		double doubleNum = prim.getAsDouble();
		float singleNum = prim.getAsFloat();
		NumberType NumType = null;
		if (num != doubleNum) {
			if (singleNum != doubleNum) {
				NumType = NumberType.DOUBLE;
			}else {
				NumType = NumberType.FLOAT;
			}			
		}else {
			if (num >= 0) {
				NumType = NumberType.INTEGER_POSITIVE;
			}else {
				NumType = NumberType.INTEGER_NEGATIVE;
			}		
		}
		
		switch(NumType) {
		case DOUBLE:
			WriteDouble(doubleNum,out);
			break;
		case FLOAT:
			WriteFloat(singleNum,out);
			break;
		case INTEGER_POSITIVE:
			WritePositive(num,out);
			break;
		case INTEGER_NEGATIVE:
			WriteNegative(num,out);
			break;
		}	
	}	
}
