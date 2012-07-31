package syndeticlogic.catena.type;

import syndeticlogic.catena.codec.Codec;
import syndeticlogic.catena.type.Type;

public class IntegerValue extends Value {
	private int decoded;

	public IntegerValue(int data) {
	    super(null, 0, 0);
	    decoded = data;
	}
	
	public IntegerValue(byte[] data, int offset) {
	    super(data, offset, Type.INTEGER.length());
        assert data.length - offset >= Type.INTEGER.length();
	    this.decoded = Codec.getCodec().decodeInteger(data, offset);
	}
    
	@Override
	public Object objectize() {
	    return new Integer(decoded);
	}

	@Override
    public Type type() {
        return Type.INTEGER;
    }

	@Override
    public int compareTo(byte[] rawBytes, int offset, int length) {
	    assert rawBytes.length - offset >= length && length == Type.INTEGER.length();
        int value = Codec.getCodec().decodeInteger(rawBytes, offset);
        if(decoded > value)
            return 1;
        else if(decoded == value)
            return 0;
        else
            return -1;
    }
	
	@Override
	public void reset(byte[] data, int offset, int length) {
        assert data.length - offset >= Type.INTEGER.length();
	    super.reset(data, offset, length);
	    this.decoded = Codec.getCodec().decodeInteger(data, offset);
	}
}