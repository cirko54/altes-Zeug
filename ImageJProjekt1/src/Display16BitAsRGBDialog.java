import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import ij.*;

// dialog that acts as a view + controller for a specific ImageJ plugin
public class Display16BitAsRGBDialog extends JDialog implements ChangeListener {

	private static final long serialVersionUID = 1555993295227439414L;

	// the model for this controller 
	final Display16BitAsRGB_ model;
	
	// dialog UI elements
	private JLabel min_label, max_label; 
	private JSlider window_center_slider, window_width_slider;
	private JTextField window_center_txt, window_width_txt;
	private JCheckBox chk_inverted, chk_clipLo, chk_clipHi;

	// constructor puts together the elements of the dialog
	public Display16BitAsRGBDialog(final Display16BitAsRGB_ model) {
		
		// init dialog window 
		super(IJ.getInstance(), "Live-Fensterung");
		
		// remember the model
		this.model = model;
	
		// dialog containing UI elements to control plugin 
		setSize(400,200); 
		Container panel = getContentPane();
		
		// labels to show histogram statistics
		min_label = new JLabel(""+model.min_value);
		max_label = new JLabel(""+model.max_value);
		
		// slider with text to define the center of the pixel value image
		window_center_slider = new JSlider(JSlider.HORIZONTAL, model.min_value, model.max_value, model.min_value + (model.max_value-model.min_value)/2);
		window_center_slider.setMajorTickSpacing((model.max_value - model.min_value)  / 10);
		window_center_slider.setPaintTicks(true);
		window_center_slider.setPaintLabels(false);
		window_center_slider.addChangeListener(this);
		window_center_txt = new JTextField(5);

		// slider with text to define the width of the pixel value image
		window_width_slider = new JSlider(JSlider.HORIZONTAL, 0, (model.max_value-model.min_value)*2, model.max_value-model.min_value);
		window_width_slider.setMajorTickSpacing((model.max_value-model.min_value)*2/10);
		window_width_slider.setPaintTicks(true);
		window_width_slider.setPaintLabels(false);
		window_width_txt = new JTextField(5);
		
		// checkboxes to control some options
		chk_inverted = new JCheckBox("inverted", model.showInverted);
		chk_clipLo = new JCheckBox("show low clipping", model.showLoClipping);
		chk_clipHi = new JCheckBox("show hi clipping", model.showHiClipping);	
		chk_inverted.addChangeListener(this);
		chk_clipLo.addChangeListener(this);
		chk_clipHi.addChangeListener(this);
		
		// layout: multiple horizontal boxes nexted in one vertical box
		BoxLayout rows = new BoxLayout(panel,BoxLayout.Y_AXIS);
		panel.setLayout(rows);

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.add(new JLabel("Fenstermitte:"));
		addSliderWithTextField(row, window_center_slider,  window_center_txt, this);
		panel.add(row);
		
		row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.add(new JLabel("Fensterbreite:"));
		addSliderWithTextField(row, window_width_slider,  window_width_txt, this);
		panel.add(row);
						
		row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.add(new JLabel("Min pixel value: "));
		row.add(min_label);
		row.add(new JLabel("   Max pixel value: "));
		row.add(max_label);
		row.add(Box.createHorizontalGlue());
		panel.add(row);

		row = new JPanel();
		row.add(chk_inverted);
		row.add(chk_clipLo);
		row.add(chk_clipHi);
		row.add(Box.createHorizontalGlue());
		panel.add(row);		

		row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		JButton autoWindow = new JButton("Auto-Window"); 
		row.add(autoWindow);
		autoWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				model.calculateAutoWindow();
				model.update();
				updateViewController();
			}
		});
		row.add(autoWindow);
		row.add(Box.createHorizontalGlue());
		panel.add(row);

		// move all components up, keep free space at bottom
		panel.add(Box.createVerticalGlue());
		
		pack();		
	}

	// add the provided slider, add a text field, and sync the two. also 
	// make sure that the text field is connected to the change listener
	private void addSliderWithTextField(JPanel panel, final JSlider slider, final JTextField text, ChangeListener listener) {
		// add slider and text field to this panel
		panel.add(slider);
		panel.add(text);
		
		// set text field content to that of the slider
		text.setText(""+slider.getValue());

		// make sure the listener is registered
		slider.addChangeListener(listener);
		
		// update text if slider was changed
		slider.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				text.setText(""+slider.getValue());
			}
		});
		
		// update slider if text was changed
		// http://stackoverflow.com/questions/1548606/java-link-jslider-and-jtextfield-for-float-value
	    text.addKeyListener(new KeyAdapter(){
	            @Override
	            public void keyReleased(KeyEvent ke) {
	                String typed = text.getText();
	                if(!typed.matches("\\d+") ) {
	                    text.setText(""+slider.getValue());
	                }
	                int value = Integer.parseInt(typed);
	                if(value>slider.getMaximum())
	                	value = slider.getMaximum();
	                if(value<slider.getMinimum())
	                	value = slider.getMinimum();
	                slider.setValue(value);
	            }
	        });	
	}


	// update values in UI components according 
	public void updateViewController() {

		min_label.setText(""+model.min_value);
		max_label.setText(""+model.max_value);
		
		chk_inverted.setSelected(model.showInverted);
		chk_clipLo.setSelected(model.showLoClipping);
		chk_clipHi.setSelected(model.showHiClipping);

		window_center_slider.setValue(model.window_center);
		window_center_slider.setMinimum(model.min_value);
		window_center_slider.setMaximum(model.max_value);
		window_center_slider.setMajorTickSpacing((model.max_value-model.min_value)/10);

		window_width_slider.setValue(model.window_width);
		window_width_slider.setMinimum(0);
		window_width_slider.setMaximum((model.max_value-model.min_value)*2);
		window_width_slider.setMajorTickSpacing((model.max_value-model.min_value)*2/10);		

	}

	// update values in model according to the UI 
	public void updateModel() {

		model.showInverted   = chk_inverted.isSelected();
		model.showLoClipping = chk_clipLo.isSelected();
		model.showHiClipping = chk_clipHi.isSelected();

		model.window_center = window_center_slider.getValue();
		model.window_width  = window_width_slider.getValue();
		
		model.update();

	}

	// whenever one of the UI elements changes...
	@Override
	public void stateChanged(ChangeEvent e) {
		updateModel();		
	}
}

