package convex.core.data;

import convex.core.Constants;

public abstract class AObject {
	/**
	 * We cache the Blob for the binary encoding of this Cell
	 */
	protected Blob encoding;

	/**
	 * Prints this Object to a readable String Representation
	 * 
	 * @param sb StringBuilder to append to
	 * @param limit Limit of printing in string bytes
	 * @return True if fully printed, false otherwise
	 */
	public abstract boolean print(BlobBuilder sb, long limit);
	
	/**
	 * Prints this Object as a CVM String value
	 * @return String representation
	 */
	public final AString print() {
		return print(Constants.PRINT_LIMIT);
	}
	
	/**
	 * Prints this Object as a CVM String value
	 * @param limit Limit of bytes to print
	 * @return String representation
	 */
	public final AString print(long limit) {
		BlobBuilder bb = new BlobBuilder();
		print(bb,limit);
		AString s=bb.getCVMString();
		if (!bb.check(limit)) {
			s=s.append("<<Print limit exceeded>>");
		}
		return s;
	}
	
	/**
	 * Gets the encoded byte representation of this cell.
	 * 
	 * @return A blob representing this cell in encoded form
	 */
	public Blob getEncoding() {
		if (encoding==null) encoding=createEncoding();
		return encoding;
	}
	
	/**
	 * Creates a Blob object representing this object. Should be called only after
	 * the cached encoding has been checked.
	 * 
	 * @return Blob Encoding of Object
	 */
	protected abstract Blob createEncoding();
	
	/**
	 * Attach the given encoding Blob to this object, if no encoding is currently cached
	 * 
	 * Warning: Blob must be the correct canonical representation of this Cell,
	 * otherwise bad things may happen (incorrect hashcode, etc.)
	 * 
	 * @param data Encoding of Value. Must be a correct canonical encoding.
	 */
	public final void attachEncoding(Blob data) {
		this.encoding=data;
	}

}
