package beamline.miners.simpleconformance.utils;

import java.awt.Color;

public class GuiUtils {

	public static Color fromWeightToColor(Double value) {
		value = (value > 1) ? 1.0 : (value < 0) ? 0.0 : value;
		value *= 100;
		int R = (int) ((255d * value) / 100d);
		int G = (int) ((255d * (100d - value)) / 100d);
		int B = 0;
		return new Color(R, G, B);
	}

	public static String getHTMLColorString(Color color) {
		String red = Integer.toHexString(color.getRed());
		String green = Integer.toHexString(color.getGreen());
		String blue = Integer.toHexString(color.getBlue());

		return "#" + 
			(red.length() == 1 ? "0" + red : red) +
			(green.length() == 1 ? "0" + green : green) +
			(blue.length() == 1 ? "0" + blue : blue);
	}

}