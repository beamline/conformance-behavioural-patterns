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

/**
 * An immutable quadruple consisting of four {@code Object} elements
 * 
 * @author Uri Loya
 * @param <L>  the left element type
 * @param <ML> the middle left element type
 * @param <MR> the middle right element type
 * @param <R>  the right element type
 */
public final class ImmutableQuadruple<L, ML, MR, R> extends Quadruple<L, ML, MR, R> {
	/**
	 * An empty array.
	 * <p>
	 * Consider using {@link #emptyArray()} to avoid generics warnings.
	 * </p>
	 */
	public static final ImmutableQuadruple<?, ?, ?, ?>[] EMPTY_ARRAY = new ImmutableQuadruple[0];

	/**
	 * An immutable quadruple of nulls.
	 */
	// This is not defined with generics to avoid warnings in call sites.
	@SuppressWarnings("rawtypes")
	private static final ImmutableQuadruple NULL = of(null, null, null, null);

	/** Serialization version */
	private static final long serialVersionUID = 1L;

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
	public static <L, ML, MR, R> ImmutableQuadruple<L, ML, MR, R>[] emptyArray() {
		return (ImmutableQuadruple<L, ML, MR, R>[]) EMPTY_ARRAY;
	}

	/**
	 * Returns an immutable quadruple of nulls.
	 *
	 * @param <L>  the left element of this quadruple. Value is {@code null}.
	 * @param <ML> the middle left element of this quadruple. Value is {@code null}.
	 * @param <MR> the middle right element of this quadruple. Value is
	 *             {@code null}.
	 * @param <R>  the right element of this quadruple. Value is {@code null}.
	 * @return an immutable quadruple of nulls.
	 */
	@SuppressWarnings("unchecked")
	public static <L, ML, MR, R> ImmutableQuadruple<L, ML, MR, R> nullQuadruple() {
		return NULL;
	}

	/**
	 * <p>
	 * Obtains an immutable quadruple of four objects inferring the generic types.
	 * </p>
	 *
	 * <p>
	 * This factory allows the quadruple to be created using inference to obtain the
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
	 * @return a quadruple formed from the four parameters, not null
	 */
	public static <L, ML, MR, R> ImmutableQuadruple<L, ML, MR, R> of(final L left, final ML middleLeft,
			final MR middleRight, final R right) {
		return new ImmutableQuadruple<>(left, middleLeft, middleRight, right);
	}

	/** Left object */
	public final L left;

	/** Middle left object */
	public final ML middleLeft;

	/** Middle right object */
	public final MR middleRight;

	/** Right object */
	public final R right;

	/**
	 * Create a new quadruple instance.
	 *
	 * @param left        the left value, may be null
	 * @param middleLeft  the middle left value, may be null
	 * @param middleRight the middle right value, may be null
	 * @param right       the right value, may be null
	 */
	public ImmutableQuadruple(final L left, final ML middleLeft, final MR middleRight, final R right) {
		super();
		this.left = left;
		this.middleLeft = middleLeft;
		this.middleRight = middleRight;
		this.right = right;
	}

	// -----------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public L getLeft() {
		return left;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ML getMiddleLeft() {
		return middleLeft;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MR getMiddleRight() {
		return middleRight;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public R getRight() {
		return right;
	}
}
