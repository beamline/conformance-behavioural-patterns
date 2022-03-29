/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package beamline.miners.simpleconformance.utils;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * A quadruple consisting of four elements
 * 
 * @author Uri Loya
 * @param <L>  the left element type
 * @param <ML> the middle left element type
 * @param <MR> the middle right element type
 * @param <R>  the right element type
 */
public abstract class Quadruple<L, ML, MR, R> implements Comparable<Quadruple<L, ML, MR, R>>, Serializable {
	private static final class QuadrupleAdapter<L, ML, MR, R> extends Quadruple<L, ML, MR, R> {

		private static final long serialVersionUID = 1L;

		@Override
		public L getLeft() {
			return null;
		}

		@Override
		public ML getMiddleLeft() {
			return null;
		}

		@Override
		public MR getMiddleRight() {
			return null;
		}

		@Override
		public R getRight() {
			return null;
		}

	}

	/** Serialization version */
	private static final long serialVersionUID = 1L;

	/**
	 * An empty array.
	 * <p>
	 * Consider using {@link #emptyArray()} to avoid generics warnings.
	 * </p>
	 */
	public static final Quadruple<?, ?, ?, ?>[] EMPTY_ARRAY = new Quadruple.QuadrupleAdapter[0];

	/**
	 * Returns the empty array singleton that can be assigned without compiler
	 * warning.
	 *
	 * @param <L>  the left element type
	 * @param <ML> the middle left element type
	 * @param <MR> the middle right element type
	 * @param <R>  the right element type
	 * @return the empty array singleton that can be assigned without compiler
	 *         warning.
	 */
	@SuppressWarnings("unchecked")
	public static <L, ML, MR, R> Quadruple<L, ML, MR, R>[] emptyArray() {
		return (Quadruple<L, ML, MR, R>[]) EMPTY_ARRAY;
	}

	/**
	 * <p>
	 * Obtains an immutable Quadruple of three objects inferring the generic types.
	 * </p>
	 *
	 * <p>
	 * This factory allows the Quadruple to be created using inference to obtain the
	 * generic types.
	 * </p>
	 *
	 * @param <L>         the left element type
	 * @param <ML>        the middle left element type
	 * @param <MR>        the middle right element type
	 * @param <R>         the right element type
	 * @param left        the left element, may be null
	 * @param middleLeft  the middle left element, may be null
	 * @param middleRight the middle right element, may be null
	 * @param right       the right element, may be null
	 * @return a Quadruple formed from the three parameters, not null
	 */
	public static <L, ML, MR, R> Quadruple<L, ML, MR, R> of(final L left, final ML middleLeft, final MR middleRight,
			final R right) {
		return new ImmutableQuadruple<>(left, middleLeft, middleRight, right);
	}

	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Compares the Quadruple based on the left element, followed by the middle
	 * element, finally the right element. The types must be {@code Comparable}.
	 * </p>
	 *
	 * @param other the other Quadruple, not null
	 * @return negative if this is less, zero if equal, positive if greater
	 */
	@Override
	public int compareTo(final Quadruple<L, ML, MR, R> other) {
		return new CompareToBuilder().append(getLeft(), other.getLeft()).append(getMiddleLeft(), other.getMiddleLeft())
				.append(getMiddleRight(), other.getMiddleRight()).append(getRight(), other.getRight()).toComparison();
	}

	/**
	 * <p>
	 * Compares this Quadruple to another based on the three elements.
	 * </p>
	 *
	 * @param obj the object to compare to, null returns false
	 * @return true if the elements of the Quadruple are equal
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Quadruple<?, ?, ?, ?>) {
			final Quadruple<?, ?, ?, ?> other = (Quadruple<?, ?, ?, ?>) obj;
			return Objects.equals(getLeft(), other.getLeft()) && Objects.equals(getMiddleLeft(), other.getMiddleLeft())
					&& Objects.equals(getMiddleRight(), other.getMiddleRight())
					&& Objects.equals(getRight(), other.getRight());
		}
		return false;
	}

	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Gets the left element from this Quadruple.
	 * </p>
	 *
	 * @return the left element, may be null
	 */
	public abstract L getLeft();

	/**
	 * <p>
	 * Gets the middle left element from this Quadruple.
	 * </p>
	 *
	 * @return the middle left element, may be null
	 */
	public abstract ML getMiddleLeft();

	/**
	 * <p>
	 * Gets the middle right element from this Quadruple.
	 * </p>
	 *
	 * @return the middle right element, may be null
	 */
	public abstract MR getMiddleRight();

	/**
	 * <p>
	 * Gets the right element from this Quadruple.
	 * </p>
	 *
	 * @return the right element, may be null
	 */
	public abstract R getRight();

	/**
	 * <p>
	 * Returns a suitable hash code.
	 * </p>
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return (getLeft() == null ? 0 : getLeft().hashCode())
				^ (getMiddleLeft() == null ? 0 : getMiddleLeft().hashCode())
				^ (getMiddleRight() == null ? 0 : getMiddleRight().hashCode())
				^ (getRight() == null ? 0 : getRight().hashCode());
	}

	/**
	 * <p>
	 * Returns a String representation of this Quadruple using the format
	 * {@code ($left,$middleLeft,$middleRight,$right)}.
	 * </p>
	 *
	 * @return a string describing this object, not null
	 */
	@Override
	public String toString() {
		return "(" + getLeft() + "," + getMiddleLeft() + "," + getMiddleRight() + "," + getRight() + ")";
	}

	/**
	 * <p>
	 * Formats the receiver using the given format.
	 * </p>
	 *
	 * <p>
	 * This uses {@link java.util.Formattable} to perform the formatting. Three
	 * variables may be used to embed the left and right elements. Use {@code %1$s}
	 * for the left element, {@code %2$s} for the middle left, {@code %3$s} for the
	 * middle right and {@code %4$s} for the right element. The default format used
	 * by {@code toString()} is {@code (%1$s,%2$s,%3$s,%4$s)}.
	 * </p>
	 *
	 * @param format the format string, optionally containing {@code %1$s},
	 *               {@code %2$s}, {@code %3$s} and {@code %4$s}, not null
	 * @return the formatted string, not null
	 */
	public String toString(final String format) {
		return String.format(format, getLeft(), getMiddleLeft(), getMiddleRight(), getRight());
	}

}