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
		INTEGER_POSITIVE,
		INTEGER_NEGATIVE,
		FLOAT,
		DOUBLE
	}


	public static void WritePrimitive(JsonElement json,DataOutputStream out) throws IOException {
		JsonPrimitive prim = json.getAsJsonPrimitive();
		PrimitiveType PrimType = null;
		float singleNum = 0;
		long num = 0;
		double doubleNum = 0;
		MessagePack mpack = new MessagePack();
		PrimitiveFactory primitiveFactory = mpack.new PrimitiveFactory();
		
		PrimitiveInterface prim1;
		if (prim.isBoolean()) {
			PrimType = PrimitiveType.PRIMITIVE_BOOLEAN;
			prim1 = primitiveFactory.getPrimitiveType(PrimType);
			prim1.WriteIt(prim,out,doubleNum,singleNum,num);
		}
		if (prim.isString()) {
			PrimType = PrimitiveType.PRIMITIVE_STRING;
			prim1 = primitiveFactory.getPrimitiveType(PrimType);
			prim1.WriteIt(prim,out,doubleNum,singleNum,num);
		}
		if (prim.isNumber()) {
			num = prim.getAsLong();
			doubleNum = prim.getAsDouble();
			singleNum = prim.getAsFloat();
			if (num != doubleNum) {
				if (singleNum != doubleNum) {
					PrimType = PrimitiveType.DOUBLE;
					prim1 = primitiveFactory.getPrimitiveType(PrimType);
					prim1.WriteIt(prim,out,doubleNum,singleNum,num);
				}else {
					PrimType = PrimitiveType.FLOAT;
					prim1 = primitiveFactory.getPrimitiveType(PrimType);
					prim1.WriteIt(prim,out,doubleNum,singleNum,num);
				}			
			}else {
				if (num >= 0) {
					PrimType = PrimitiveType.INTEGER_POSITIVE;
					prim1 = primitiveFactory.getPrimitiveType(PrimType);
					prim1.WriteIt(prim,out,doubleNum,singleNum,num);
				}else {
					PrimType = PrimitiveType.INTEGER_NEGATIVE;
					prim1 = primitiveFactory.getPrimitiveType(PrimType);
					prim1.WriteIt(prim,out,doubleNum,singleNum,num);
				}		
			}
		}
	}
	
	public class PrimitiveFactory {
		
		   //use getPrimitiveType method to get type of Primitive
		   public PrimitiveInterface getPrimitiveType(PrimitiveType primType){
		      if(primType == null){
		         return null;
		      }		
		      if(primType.equals(PrimitiveType.PRIMITIVE_BOOLEAN)){
		         return new BooleanPrimitive();	         
		      } else if(primType.equals(PrimitiveType.PRIMITIVE_STRING)){
		         return new StringPrimimitve();        
		      } else if(primType.equals(PrimitiveType.DOUBLE)){
		         return new DoublePrimimitve();
		      } else if(primType.equals(PrimitiveType.FLOAT)){
			     return new FloatPrimimitve();      
			  } else if(primType.equals(PrimitiveType.INTEGER_POSITIVE)){
			     return new NegativeIntegerPrimimitve();
			  } else if(primType.equals(PrimitiveType.INTEGER_NEGATIVE)){
				 return new PositiveIntegerPrimimitve();
			  }
		      
		      return null;
		   }
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
	
	public interface PrimitiveInterface{
		void WriteIt(JsonPrimitive prim, DataOutputStream out,double doubleNum,float singleNum,long num) throws IOException;
	}
	
	public class BooleanPrimitive implements PrimitiveInterface {
		@Override
		public void WriteIt(JsonPrimitive prim,DataOutputStream out,double doubleNum,float singleNum,long num)throws IOException  {
			   out.write(prim.getAsBoolean() ? 0xc3 : 0xc2);
		}
	}
	
	public class StringPrimimitve implements PrimitiveInterface {
		@Override
		public void WriteIt(JsonPrimitive prim,DataOutputStream out,double doubleNum,float singleNum,long num)throws IOException  {
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
	}
	
	public class DoublePrimimitve implements PrimitiveInterface {
		@Override
		public void WriteIt(JsonPrimitive prim,DataOutputStream out,double doubleNum,float singleNum,long num)throws IOException  {
			out.write(0xCB);
			out.writeDouble(doubleNum);
		}
	}
	
	public class FloatPrimimitve implements PrimitiveInterface {
		@Override
		public void WriteIt(JsonPrimitive prim,DataOutputStream out,double doubleNum,float singleNum,long num)throws IOException  {
			out.write(0xCA);
			out.writeFloat(singleNum);
		}
	}
	
	public class NegativeIntegerPrimimitve implements PrimitiveInterface {
		@Override
		public void WriteIt(JsonPrimitive prim,DataOutputStream out,double doubleNum,float singleNum,long num)throws IOException  {
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
	}
	
	public class PositiveIntegerPrimimitve implements PrimitiveInterface {
		@Override
		public void WriteIt(JsonPrimitive prim,DataOutputStream out,double doubleNum,float singleNum,long num)throws IOException  {
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
	}
	
}
