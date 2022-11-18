package beamline.miners.behavioalconformance.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class represents a direct following relationship between two activities.
 * 
 * @author Andrea Burattin
 */
public class DirectFollowingRelation implements Serializable {

	private static final long serialVersionUID = 2253666384775480618L;
	private String left = null;
	private String right = null;

	/**
	 * Construct a direct following relation between the two activities
	 * 
	 * @param first
	 * @param second
	 */
	public DirectFollowingRelation(String left, String right) {
		this.left = left;
		this.right = right;
	}
	
	public String getLeft() {
		return left;
	}
	
	public String getRight() {
		return right;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(left).append(right).toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DirectFollowingRelation) {
			DirectFollowingRelation obj2 = (DirectFollowingRelation) obj;
			return obj2.left.equals(left) && obj2.right.equals(right);
		}
		return false;
	}

	@Override
	public String toString() {
		return left + " > " + right;
	}
}
