package beamline.miners.simpleconformance.utils;

public class Triple<A, B, C> extends Pair<A, B> {

	protected final C third;

	public Triple(A first, B second, C third) {
		super(first, second);
		this.third = third;
	}

	public C getThird() {
		return third;
	}

	private static boolean equals(Object x, Object y) {
		return ((x == null) && (y == null)) || ((x != null) && x.equals(y));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
		return (other instanceof Triple) &&
				equals(first, ((Triple<A, B, C>) other).first) &&
				equals(second, ((Triple<A, B, C>) other).second) &&
				equals(third, ((Triple<A, B, C>) other).third);
	}

	@Override
	public int hashCode() {
		int toRet = super.hashCode();
		if (third != null) {
			toRet = toRet * 31 + third.hashCode();
		}
		return toRet;
	}

	@Override
	public String toString() {
		return "(" + first + "," + second + "," + third + ")";
	}
}