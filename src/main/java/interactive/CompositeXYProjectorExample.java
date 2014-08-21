/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2014 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
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

package interactive;

import io.scif.img.IO;
import io.scif.img.SCIFIOImgPlus;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.projector.composite.CompositeXYProjector;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class CompositeXYProjectorExample {

	public static void main(final String[] args) throws Exception {
		final JFileChooser fileChooser = new JFileChooser();
		final int rval = fileChooser.showOpenDialog(null);
		if (rval != JFileChooser.APPROVE_OPTION) return; // canceled
		final File file = fileChooser.getSelectedFile();
		if (file == null || !file.exists()) {
			System.out.println("Invalid file: " + file);
			return;
		}
		final String path = file.getAbsolutePath();

		System.out.println("Loading image...");
		final SCIFIOImgPlus<?> img = IO.openImgs(path).get(0);
		display((SCIFIOImgPlus) img);
	}

	public static <T extends RealType<T>> void display(final ImgPlus<T> img) {
		// width and height of the raw data
		// NB: Assumes first two dimensions are [X, Y].
		final long width = img.dimension(0);
		final long height = img.dimension(1);

		// number of channels to composite together
		final int cIndex = img.dimensionIndex(Axes.CHANNEL);
		final long channels = img.dimension(cIndex);
		System.out.println("Data is " + width + " x " + height + " x " + channels);

		// width and height of the BufferedImage to paint
		final int scaledWidth = (int) Math.min(width, 2000);
		final int scaledHeight = (int) Math.min(height, 2000);

		// create the composite converters
		final ArrayList<Converter<T, ARGBType>> converters = new ArrayList<>();
		for (int c=0; c<channels; c++) {
			final ColorTable colorTable;
			switch (c) {
				case 0: colorTable = ColorTables.RED; break;
				case 1: colorTable = ColorTables.GREEN; break;
				case 2: colorTable = ColorTables.BLUE; break;
				case 3: colorTable = ColorTables.CYAN; break;
				case 4: colorTable = ColorTables.MAGENTA; break;
				case 5: colorTable = ColorTables.YELLOW; break;
				default: colorTable = ColorTables.GRAYS; break;
			}
			// NB: Autoscales each channel. This may not be what you want!
			final double min = 0; //img.getChannelMinimum(c);
			final double max = 255; //img.getChannelMaximum(c);
			converters.add(new RealLUTConverter<T>(min, max, colorTable));
		}

		final ARGBScreenImage screenImage =
			new ARGBScreenImage(scaledWidth, scaledHeight);
		final CompositeXYProjector<T> proj =
			new CompositeXYProjector<T>(img, screenImage, converters, cIndex);
		proj.setComposite(true);

		// project the image
		System.out.println("Mapping data to screen image...");
		proj.map();

		// finally, here is the BufferedImage
		final BufferedImage bi = screenImage.image();

		// show it!
		System.out.println("Displaying screen image...");
		final JFrame frame = new JFrame(img.getName());
		final ImageIcon imageIcon = new ImageIcon(bi, img.getName());
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new JLabel(imageIcon), BorderLayout.CENTER);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}


	/**
	 * Built-in lookup tables, adapted from ImageJ.
	 * 
	 * @see "https://github.com/imagej/imagej-common/blob/imagej-common-0.8.2/src/main/java/net/imagej/display/ColorTables.java"
	 */
	public static class ColorTables {

		public static final ColorTable8 RED = primary(4); // 100
		public static final ColorTable8 GREEN = primary(2); // 010
		public static final ColorTable8 BLUE = primary(1); // 001
		public static final ColorTable8 CYAN = primary(3); // 011
		public static final ColorTable8 MAGENTA = primary(5); // 101
		public static final ColorTable8 YELLOW = primary(6); // 110
		public static final ColorTable8 GRAYS = primary(7); // 111

		private static ColorTable8 primary(final int color) {
			final byte[] r = new byte[256], g = new byte[256], b = new byte[256];
			for (int i = 0; i < 256; i++) {
				if ((color & 4) != 0) r[i] = (byte) i;
				if ((color & 2) != 0) g[i] = (byte) i;
				if ((color & 1) != 0) b[i] = (byte) i;
			}
			return new ColorTable8(r, g, b);
		}

	}

}
