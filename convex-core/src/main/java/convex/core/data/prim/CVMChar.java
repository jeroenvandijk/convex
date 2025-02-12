package convex.core.data.prim;

import java.nio.ByteBuffer;

import convex.core.Constants;
import convex.core.data.AString;
import convex.core.data.Blob;
import convex.core.data.BlobBuilder;
import convex.core.data.Strings;
import convex.core.data.Tag;
import convex.core.data.type.AType;
import convex.core.data.type.Types;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.lang.reader.ReaderUtils;

/**
 * Class for CVM Character values.
 * 
 * Characters are Unicode code point, and can be used to construct Strings on the CVM.
 * Limited to range 0 .. 0x10ffff as per Unicode standard
 */
public final class CVMChar extends APrimitive {
	public static int MAX_VALUE=0x10ffff; // 21 bits max Unicode value
	
	private static final CVMChar[] cache=new CVMChar[128];
	
	static {
		for (int i=0; i<128; i++) {
			cache[i]=new CVMChar(i);
		}
	}

	public static final CVMChar A = CVMChar.create('a');
	
	private final int value;
	
	private CVMChar(int value) {
		this.value=value;
	}
	
	@Override
	public AType getType() {
		return Types.CHARACTER;
	}

	// Gets a CVM Char for the given Unicode code point, or null if not value
	public static CVMChar create(long value) {
		if (value<0) return null; // invalid negative number
		if (value<128) return cache[(int)value];
		if (value>MAX_VALUE) return null;
		return new CVMChar((int)value);
	}
	
	@Override
	public long longValue() {
		return 0xffffffffl&value;
	}
	
	@Override
	public int estimatedEncodingSize() {
		return 1+3;
	}

	@Override
	public void validateCell() throws InvalidDataException {
		// Nothing to check. Always valid
	}
	
	/**
	 * Gets the length in bytes needed to express the character in an Encoding
	 * @param c Code point value
	 * @return Number of bytes needed for code point
	 */
	private static int encodedCharLength(int c) {
		if ((c&0xffff0000)==0) {
			return ((c&0x0000ff00)==0)?1:2;
		} else {
			return ((c&0xff000000)==0)?3:4;
		}
	}
	
	/** 
	 * Gets the UTF=8 length in bytes for this CVMChar
	 * @param c Code point value
	 * @return UTF lenth or -1 if not a valid unicode value
	 */
	public static int utfLength(int c) {
		if (c<0) return -1;
		if (c<=0x7f) return 1;
		if (c<=0x7ff) return 2;
		if (c<=0xffff) return 3;
		if (c<=MAX_VALUE) return 4;
		return -1;
	}
	
	public static CVMChar read(int len,ByteBuffer bb) throws BadFormatException {
		int value=0xff000000;
		for (int i=0; i<len;i++) {
			if (value==0) throw new BadFormatException("Leading zero in CVMChar encoding");
			byte b=bb.get();
			value=(value<<8)+(b&0xFF);
		}
		CVMChar result=create(value);
		if (result==null) throw new BadFormatException("CVMChar out of Unicode range");
		return result;
	}

	@Override
	public int encode(byte[] bs, int pos) {
		int len=encodedCharLength(value);
		bs[pos++]=(byte)(Tag.CHAR+(len-1));
		return encodeRaw(len,bs,pos);
	}

	public int encodeRaw(int len,byte[] bs, int pos) {
		for (int i=0; i<len; i++) {
			bs[pos+i]=(byte)((value>>((len-(i+1))*8))&0xff);
		}
		return pos+len;
	}
	
	@Override
	public int encodeRaw(byte[] bs, int pos) {
		throw new UnsupportedOperationException("Encoding requires a length in bytes");
	}

	@Override
	public boolean print(BlobBuilder bb, long limit) {
		// Prints like EDN.
		// Characters are preceded by a backslash: \c, \newline, \return, \space and
		// \tab yield
		// the corresponding characters.
		// Unicode characters are represented as in Java.
		// Backslash cannot be followed by whitespace.
		//
		switch(value) {
			case '\n': bb.append("\\newline"); break;
			case '\r': bb.append("\\return"); break;
			case ' ':  bb.append("\\space"); break;
			case '\t': bb.append("\\tab"); break;
			default:  {
				bb.append('\\');
				if (Character.isBmpCodePoint(value)) {
					bb.append((char)value);
				} else {
					bb.append(toString());
				}
			}
		}
		return bb.check(limit);
	}

	/**
	 * Returns the Java String representation of this CVMChar.
	 * 
	 * Different from {@link #print() print()} which returns a readable representation.
	 *
	 * For instance, on CVMChar \a, this methods returns "a" while {@link #print() print()} returns "\a".
	 */
	@Override
	public String toString() {
		if (Character.isValidCodePoint(value)) {
			return Character.toString(value);
		} else {
			return Constants.BAD_CHARACTER_STRING;
		}
	}

	@Override
	public double doubleValue() {
		return (double)value;
	}
	
	/**
	 * Parses a Character from a String
	 * @param s String to parse
	 * @return CVMChar instance, or null if not valid
	 */
	public static CVMChar parse(String s) {
		int n=s.length();
		
		if (n<2) return null;
		
		if (n==2) {
			return CVMChar.create(s.charAt(1));
		}
		
		if (s.charAt(1)=='u') {
			if (n==6) {
				char c = (char) Long.parseLong(s.substring(2),16);
				return CVMChar.create(c);
			}
		}
		
		s=s.substring(1);
		return ReaderUtils.specialCharacter(s);
	}
	
	@Override
	public byte getTag() {
		return (byte) (Tag.CHAR+(encodedCharLength(value)-1));
	}

	/**
	 *  Gets the Java char value of this CVM Character. 
	 *  
	 *  Not all Unicode code points fit in a JVM char, a "bad character" value is used as replacement if this is not possible.
	 */
	public char charValue() {
		if (Character.isBmpCodePoint(value)) {
			return (char)value;
		} else {
			return Constants.BAD_CHARACTER;
		}
	}

	/**
	 * Converts this Character to a Blob with its UTF-8 byte representation
	 * @return byte[] array containing UTF-8 bytes
	 */
	public byte[] toUTFBytes() {
		int n=utfLength(value);
		if (n<=0) throw new Error("Shouldn't happen: CVMChar out of range: "+value);
		byte[] bs=new byte[n];
		if (value<128) {
			bs[0]=(byte)value;
			return bs;
		}
		bs[0]=(byte)((0xff00>>(n))|(value>>((n-1)*6)));
		for (int i=1; i<n; i++) {
			bs[i]=(byte)(0x80|(0x3f&(value>>((n-1-i)*6))));
		}
		return bs;
	}
	
	/**
	 * Gets the Blob representation of this Character in UTF-8
	 * @return 1-4 Bytes Blob containing UTF-8 representation of this Character
	 */
	public Blob toUTFBlob() {
		return Blob.wrap(toUTFBytes());
	}

	@Override
	public AString toCVMString(long limit) {
		if (limit<=0) return null;
		return Strings.create(toUTFBlob());
	}


}
