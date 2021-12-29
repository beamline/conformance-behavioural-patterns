package beamline.miners.simpleconformance.utils;

public class Quadruple<A, B, C, D> extends Triple<A, B, C> {

	protected final D fourth;

	public Quadruple(A first, B second, C third, D fourth) {
		super(first, second, third);
		this.fourth = fourth;
	}

	public D getFourth() {
		return fourth;
	}

	private static boolean equals(Object x, Object y) {
		return ((x == null) && (y == null)) || ((x != null) && x.equals(y));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
		return (other instanceof Quadruple) &&
				equals(first, ((Quadruple<A, B, C, D>) other).first) &&
				equals(second, ((Quadruple<A, B, C, D>) other).second) &&
				equals(third, ((Quadruple<A, B, C, D>) other).third) &&
				equals(fourth, ((Quadruple<A, B, C, D>) other).fourth);
	}

	@Override
	public int hashCode() {
		int toRet = super.hashCode();
		if (third != null) {
			toRet = toRet * 31 + fourth.hashCode();
		}
		return toRet;
	}

	@Override
	public String toString() {
		return "(" + first + "," + second + "," + third + "," + fourth + ")";
	}
}