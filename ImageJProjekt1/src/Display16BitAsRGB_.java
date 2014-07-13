import java.util.Vector;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.ImagePlus;


// a filter that displays a 16 bit grayscale image as 8 bit RGB image
public class Display16BitAsRGB_ implements PlugInFilter{

	
	// --- public attributes for the view/controller ---
	
	// min and max pixel value occurring in input image 
	public int min_value = 0, max_value = 255*255;
	
	// current window center + width
	public int window_center, window_width;
	
	// display options
	public boolean showInverted = false, showHiClipping = false, showLoClipping = false;
	
	// --- end of public properties ---
	
	// a dialog provides the combined view + controller
	private Display16BitAsRGBDialog dialog;

	// input and output image
	private ImageProcessor inputProcessor;
	private ImagePlus outputImage;

	// this method is called to check which types of images this plugin accepts 
	@Override
	public int setup(String arg, ImagePlus imp) {
		// this plugin only accepts 16 bit grayscale images (i.e. DICOM images)
		return DOES_16+NO_CHANGES;
	}

	// this method is called when the plugin is applied through the ImageJ UI 
	@Override
	public void run(ImageProcessor ip) {
		
		// analyze the input image, find min_value and max_value
		inputProcessor = ip;
		findMinMaxPixelValue();
		
		// initially use a very soft all-enclosing window
		calculateAutoWindow();
		window_width*=2;
		
		// create and show the dialg window 
		dialog = new Display16BitAsRGBDialog(this);
		dialog.setVisible(true);
		
		// create a new output image. make it RGB so we can draw colored markers into it 
		outputImage = NewImage.createRGBImage("Output Image",ip.getWidth(), ip.getHeight(), 1, 0);			
		calculateOutputImage();
		
		// show it
		outputImage.show();
		outputImage.updateAndDraw();
			
	}

	// given min and max, automatically determine a default display window 
	public void calculateAutoWindow() {
		int newCenter = (int)((max_value-min_value)/2);
		int newWidth = max_value-min_value;
		window_width = newWidth;
		window_center = newCenter;
	}
	

	// calculate the pixels of the output image from those of the input image 
	private void calculateOutputImage() {

		// colors to be used for low/hi clipping
		final int color_low  = 0x0000ff;
		final int color_high = 0xff0000;			
		
		short[] inPixels = (short[]) inputProcessor.getPixels();
		int[] outPixels = (int[]) outputImage.getProcessor().getPixels();

		// determine borders of window
		int low = (window_center-(window_width/2));
		int high = (window_center+(window_width/2));

		for (int i=0; i<inputProcessor.getWidth(); i++) {
			for (int j = 0; j < inputProcessor.getHeight(); j++) {
				int index = j * inputProcessor.getWidth()+i;

				// just the current pixel color; int because the window width can be much wider than the "short" type
				int zwischenPixel = (int)(inPixels[index] & 0xffff);
				
				// check if the current pixel is within the window
				if (zwischenPixel>low && zwischenPixel<high) {
					
					// apply the window: subtract lower value, divide by width, multiply by graytones
					float windowedPixel = (zwischenPixel-low)*255/window_width;
					
					// apply to RGB
					int red = (int)windowedPixel;
					int green = (int)windowedPixel;
					int blue = (int)windowedPixel;
					outPixels[index] = (int)((red & 0xff)<<16)+((green & 0xff)<<8) + (blue & 0xff); 
				}
				
				// clippings
				else if (inPixels[index]<low) {
					if (showLoClipping) {
						outPixels[index] = color_low;
					}
					else {
						outPixels[index] = 0x000000;
					}
				}
				else if (inPixels[index]>high) {
					if (showHiClipping) {
						outPixels[index] = color_high;
					}
					else {
						outPixels[index] = 0xffffff;
					}
				}
			}
		}
		if (showInverted) {
			for (int i=0; i<inputProcessor.getWidth(); i++) {
				for (int j = 0; j < inputProcessor.getHeight(); j++) {
					int index = j * inputProcessor.getWidth()+i;
					int red = 255-(int)(outPixels[index] & 0xff0000)>>16;
					int green = 255-(int)(outPixels[index] & 0x00ff00)>>8;
					int blue = 255-(int)(outPixels[index] & 0x0000ff);
					outPixels[index] = (int)((red & 0xff)<<16)+((green & 0xff)<<8) + (blue & 0xff); 
				}
			}
		}

		
	}

	// go through pixels of the input image and find min/max etc.
	private void findMinMaxPixelValue() {
		
		short[] pixels = (short[]) inputProcessor.getPixels();
		int maxValue = 0;
		int minValue = 65535;
		for (int i=0; i<inputProcessor.getWidth(); i++) {
			for (int j = 0; j < inputProcessor.getHeight(); j++) {
				int index = j * inputProcessor.getWidth()+i;
				int zwischenPixel = pixels[index] & 0xffff;
				if (zwischenPixel>maxValue) {
					maxValue = zwischenPixel;
				}
				if (zwischenPixel<minValue) {
					minValue = zwischenPixel;
				}
			}
		}
		max_value = maxValue;
		min_value = minValue;
	}

	// this is called from the controller if anything has changed
	public void update() {
		if(outputImage != null) {
			calculateOutputImage();
			outputImage.show();
			outputImage.updateAndDraw();
		}		
	}

}
