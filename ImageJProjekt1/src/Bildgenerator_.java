import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/*
 * Bildgenerator_ is an ImageJ plugin to generate images algorithmically 
 */
public class Bildgenerator_ implements PlugIn, ActionListener {

	// choices to be displayed in the combo box
	private String[] choices = { "Schwarz-Weiss-Verlauf", "Rot-Grün-Verlauf", "Rot-Grün-Gelb-Verlauf", 
			"Japanische Flagge (nur Breite)", "geglättete japanische Flagge (nur Breite)", "Schachbrett (nur Breite)", "..." };

	// array of already generated images
	private Vector<ImagePlus> images;

	// dialog UI elements
	private JDialog dialog;
	private JSlider slider_width, slider_height;
	private JComboBox selection;
	private JButton action_button;

	// initialization of the plugin
	@Override
	public void run(String arg) {

		// start with empty list of generated images
		images = new Vector<ImagePlus>();

		// create and show control dialog
		dialog = makeDialog();
		dialog.setVisible(true);

	}

	// action listener for the action button
	@Override
	public void actionPerformed(ActionEvent ev) {

		// read out the parameters of the image to be generated
		int width = slider_width.getValue();
		int height = slider_height.getValue();
		int choice = selection.getSelectedIndex();
		ImagePlus img;

		// call the corresponding method, depending on the user's choice
		switch (choice) {
		case 0:
			img = makeGradientGreyscaleImage(width, height);
			break;
		case 1:
			img = makeGradientRedGreenImage(width, height);
			break;
		case 2:
			img = makeGradientRedGreenYellowImage(width, height);
			break;
		case 3:
			// correct japanese flag is 3/2, so width will be used, height
			// (smaller) will be calculated
			double correctHeight = (double) width / 3 * 2;
			img = makeJapaneseFlag(width, (int) correctHeight);
			break;
		case 4:
			double correctHeight2 = (double) width / 3 * 2;
			// make flag with 0.8/radius smoothing clearance
			img = makeJapaneseSmoothFlag(width, (int) correctHeight2, 0.8);
			break;
		case 5:
			// Chessboard is square, so only width is used;
			img = makeChessboardImage (width, width);
			break;
		default:
			IJ.error("Sorry, operation not implemented (yet).");
			return;
		}

		// show the newly generated image
		img.show();
		img.updateAndDraw();

		// remember generated image in an array
		images.add(img);
	}

	// make an 8 bit grayscale image with a horizontal gradient
	public ImagePlus makeGradientGreyscaleImage(int width, int height) {

		// 8 bit grayscale image
		ImagePlus img = NewImage.createByteImage("Schwarz-Weiss-Verlauf",
				width, height, 1, NewImage.FILL_BLACK);
		ImageProcessor proc = img.getProcessor();
		byte[] pixels = (byte[]) proc.getPixels();

		// loop over all columns
		for (int i = 0; i < width; i++) {

			// calculate interpolated value between black and white
			double interpol = i / (double) (width - 1);
			byte value = (byte) (0 * (1 - interpol) + 255 * interpol);

			// set all pixels in this column
			for (int j = 0; j < height; j++) {

				// pixel position
				int index = j * width + i;

				// write new pixel
				pixels[index] = value;
			}
		}
		return img;
	}

	public ImagePlus makeGradientRedGreenImage(int width, int height) {
		ImagePlus img = NewImage.createRGBImage("Rot-Grün-Verlauf", width,
				height, 1, NewImage.FILL_BLACK);
		ImageProcessor proc = img.getProcessor();
		int[] pixels = (int[]) proc.getPixels();

		// loop over all columns
		for (int i = 0; i < width; i++) {

			// calculate interpolated gradient value
			double interpol = i / (double) (width - 1);
			int value = (int) (0 * (1 - interpol) + 255 * interpol);

			// set all pixels in this column
			for (int j = 0; j < height; j++) {

				// pixel position
				int index = j * width + i;

				// write new RGB pixel
				int red = (int) (pixels[index] & 0xff0000) >> 16;
				int green = (int) (pixels[index] & 0x00ff00) >> 8;
				int blue = (int) (pixels[index] & 0x0000ff);
				red = 255 - value;
				green = value;
				blue = 0;
				pixels[index] = ((red & 0xff) << 16) + ((green & 0xff) << 8)
						+ (blue & 0xff);
			}
		}
		return img;
	}

	public ImagePlus makeGradientRedGreenYellowImage(int width, int height) {
		ImagePlus img = NewImage.createRGBImage("Rot-Grün-Gelb-Verlauf", width,
				height, 1, NewImage.FILL_BLACK);
		ImageProcessor proc = img.getProcessor();
		int[] pixels = (int[]) proc.getPixels();

		// loop over all columns
		for (int i = 0; i < width; i++) {

			// calculate first interpolated gradient value
			double interpolX = i / (double) (width - 1);
			

			// set all pixels in this column
			for (int j = 0; j < height; j++) {

				// second interpolated gradient value
				double interpolY = j / (double) (height - 1);

				// pixel position
				int index = j * width + i;

				// write new RGB pixel
				int red = (int) (pixels[index] & 0xff0000) >> 16;
				int green = (int) (pixels[index] & 0x00ff00) >> 8;
				int blue = (int) (pixels[index] & 0x0000ff);
				
				// calculate the average from both values
				double red1 = (1-interpolX)*255;
				double green1 = interpolX*255;
				double green2 = interpolY*255;
				red = (int) ((red1+255)/2);
				green = (int) ((green1+green2)/2);
				blue = 0;
				pixels[index] = ((red & 0xff) << 16) + ((green & 0xff) << 8)
						+ (blue & 0xff);
			}
		}
		return img;
	}
	
	public ImagePlus makeJapaneseFlag(int width, int height) {
		ImagePlus img = NewImage.createRGBImage("Japanische Flagge", width,
				height, 1, NewImage.FILL_WHITE);
		ImageProcessor proc = img.getProcessor();
		int[] pixels = (int[]) proc.getPixels();

		// loop over all columns
		for (int i = 0; i < width; i++) {

			// set all pixels in this column
			for (int j = 0; j < height; j++) {

				// pixel position
				int index = j * width + i;

				// compare pixel position with center + radius of "sun",
				// distance via pythagoras
				double centerx = 0.5 * (double) width;
				double centery = 0.5 * (double) height;
				double radius = 0.3 * (double) height;
				int centeri = (int) centerx;
				int centerj = (int) centery;
				double a = centeri - i;
				double b = centerj - j;
				double distance = Math.sqrt(a * a + b * b);
				// if (i > (centeri - radius) && i < (centeri + radius)
				// && j > (centerj - radius) && j < (centerj + radius)) {
				if (distance <= radius) {
					// write new RGB pixel
					int red = (int) (pixels[index] & 0xff0000) >> 16;
					int green = (int) (pixels[index] & 0x00ff00) >> 8;
					int blue = (int) (pixels[index] & 0x0000ff);
					red = 255;
					green = 0;
					blue = 0;
					pixels[index] = ((red & 0xff) << 16)
							+ ((green & 0xff) << 8) + (blue & 0xff);
				}
			}
		}
		return img;
	}

	public ImagePlus makeJapaneseSmoothFlag(int width, int height,
			double smoothingStart) {
		ImagePlus img = NewImage.createRGBImage("Japanische Flagge", width,
				height, 1, NewImage.FILL_WHITE);
		ImageProcessor proc = img.getProcessor();
		int[] pixels = (int[]) proc.getPixels();

		// loop over all columns
		for (int i = 0; i < width; i++) {

			// set all pixels in this column
			for (int j = 0; j < height; j++) {

				// pixel position
				int index = j * width + i;

				// compare pixel position with center + radius of "sun",
				// distance via pythagoras
				double centerx = 0.5 * (double) width;
				double centery = 0.5 * (double) height;
				double radius = 0.3 * (double) height;
				int centeri = (int) centerx;
				int centerj = (int) centery;
				double a = centeri - i;
				double b = centerj - j;
				double distance = Math.sqrt(a * a + b * b);
				if (distance <= radius) {
					// set distance for start of smoothing
					double relativeDistance = distance / radius;
					int red = (int) (pixels[index] & 0xff0000) >> 16;
					int green = (int) (pixels[index] & 0x00ff00) >> 8;
					int blue = (int) (pixels[index] & 0x0000ff);
					red = 255;
					// fade-out to white when smoothingStart is crossed
					if (relativeDistance > smoothingStart) {
						double value = (relativeDistance - smoothingStart)
								/ (1 - smoothingStart) * 255;
						green = (int) value;
						blue = (int) value;
					} else {
						green = 0;
						blue = 0;
					}
					pixels[index] = ((red & 0xff) << 16)
							+ ((green & 0xff) << 8) + (blue & 0xff);
				}
			}
		}
		return img;
	}

	public ImagePlus makeChessboardImage(int width, int height) {

		// 8 bit grayscale image
		ImagePlus img = NewImage.createByteImage("Schachbrettmuster",
				width, height, 1, NewImage.FILL_BLACK);
		ImageProcessor proc = img.getProcessor();
		byte[] pixels = (byte[]) proc.getPixels();

		// loop over all columns
		for (int i = 0; i < width; i++) {

			// set all pixels in this column
			for (int j = 0; j < height; j++) {
				// calculate the "field number" within the respective row or column
				double relFieldX = i/(double)width*8;
				double relFieldY = j/(double)height*8;
				// pixel position
				int index = j * width + i;
				// fill the field with white if row number is even and column number is odd or vice versa
				if (((int)relFieldX % 2 == 0 && (int)relFieldY % 2 == 1) || ((int)relFieldX % 2 == 1) && ((int)relFieldY % 2 == 0)) {
					int value = 255;
					pixels[index] = (byte) value;
				}
			}
		}
		return img;
	}

	
	
	// generate dialog with user interface to control this plugin
	private JDialog makeDialog() {

		// dialog containing UI elements to control plugin
		JDialog dialog = new JDialog(IJ.getInstance(), "Bildgenerator");
		dialog.setSize(640, 200);
		Container panel = dialog.getContentPane();

		// sliders to define image width + height
		slider_width = new JSlider(JSlider.HORIZONTAL, 0, 1024, 640);
		slider_width.setMajorTickSpacing(128);
		slider_width.setPaintTicks(true);
		slider_width.setPaintLabels(true);

		slider_height = new JSlider(JSlider.HORIZONTAL, 0, 768, 480);
		slider_height.setMajorTickSpacing(128);
		slider_height.setPaintTicks(true);
		slider_height.setPaintLabels(true);

		// combo box and action button
		selection = new JComboBox(choices);
		action_button = new JButton("Bild generieren");
		action_button.addActionListener(this);

		// layout: multiple horizontal boxes nexted in one vertical box
		BoxLayout rows = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(rows);

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.add(new JLabel("Bildbreite (pix):"));
		row.add(slider_width);
		panel.add(row);

		row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.add(new JLabel("Bildh�he (pix):"));
		row.add(slider_height);
		panel.add(row);

		row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.add(selection);
		row.add(action_button);
		row.add(Box.createHorizontalGlue());
		panel.add(row);

		// move all components up, keep free space at bottom
		panel.add(Box.createVerticalGlue());

		return dialog;
	}

}
