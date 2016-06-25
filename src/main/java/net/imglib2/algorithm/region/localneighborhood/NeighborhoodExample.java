/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.algorithm.region.localneighborhood;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class NeighborhoodExample
{
	public static void main( final String[] args )
	{
		final ArrayImg< IntType, IntArray > img = ArrayImgs.ints(
				new int[] {
						0, 0, 0, 0, 0,
						0, 1, 0, 0, 0,
						0, 0, 0, 1, 0,
						0, 1, 0, 0, 0,
						0, 1, 0, 0, 0
				},
				5, 5 );
		findLocalMaxima( Views.interval( Views.extendBorder( img ), img ) );
	}

	public static < T extends Type< T > & Comparable< T > > void findLocalMaxima( final RandomAccessibleInterval< T > img )
	{
		// Create a neighborhood Shape, in this case a rectangle.
		// The parameters are span and skipCenter: span = 1 says that this will
		// be a 3x3x...x3 rectangle shape (where 3 == 2 * span + 1). skipCenter
		// = true says that the center pixel of the 3x3x...x3 shape is skipped
		// when iterating the shape.
		final RectangleShape shape = new RectangleShape( 1, true );

		// Create a RandomAccess of img (This will be used to access the center
		// pixel of a neighborhood.)
		final RandomAccess< T > center = img.randomAccess();

		// Use the shape to create an Iterable<Neighborhood<T>>.
		// This is a IterableInterval whose elements of Neighborhood<T> are all
		// the 3x3x...x3 neighborhoods of img.
		// The Neighborhood<T> themselves are IterableIntervals over the pixels
		// of type T in the 3x3x...x3 neighborhood. The Neighborhood<T> are also
		// Localizable, and localize() provides the coordinates which the
		// neighborhood is currently centered on.
		//
		// Note: By "all the 3x3x...x3 neighborhoods of img" we mean the set of
		// 3x3x...x3 neighborhoods centered on every pixel of img.
		// This means that out-of-bounds values will be accessed. The 3x3
		// neighborhood centered on pixel (0,0) contains pixels
		// {(-1,-1)...(1,1)}
		final Iterable< Neighborhood< T > > neighborhoods = shape.neighborhoods( img );

		// Iterate over all neighborhoods.
		for ( final Neighborhood< T > neighborhood : neighborhoods )
		{
			// Position the center RandomAccess to the origin of the current
			// neighborhood and get() the centerValue.
			center.setPosition( neighborhood );
			final T centerValue = center.get();

			// Loop over pixels of the neighborhood and check whether the
			// centerValue is strictly greater than all of them. Note that
			// because we specified skipCenter = true for the RectangleShape the
			// center pixel itself is not included in the neighborhood values.
			boolean isMaximum = true;
			for ( final T value : neighborhood )
			{
				if ( value.compareTo( centerValue ) >= 0 )
				{
					isMaximum = false;
					break;
				}
			}

			// If this is a maximum print it's coordinates.
			if ( isMaximum )
				System.out.println( "maximum found at " + Util.printCoordinates( center ) );
		}
	}
}
