package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import common.Config;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
@SuppressWarnings("serial")
public class FilterPanel extends JPanel  implements ChangeListener, ActionListener {
	
	public static final int MATCHED = 2;
	public static final int RAISED_COSINE = 0;
	public static final int WINDOWED_SINC = 1;
	public static final int MAX_FILTERS = 3;
	
	String[] filterName = { 
			"Raised Cosine", 
			"Windowed Sinc",
			"Matched"
			};
	/*
	Filter filters[] = {
			new RaisedCosineFilter(),
			new WindowedSincFilter()
	};
	*/
	
	public static final int WS_LEN_MIN = 480;
	public static final int WS_LEN_MAX = 4800;
	public static final int RC_LEN_MIN = 64;
	public static final int RC_LEN_MAX = 512;
	
	public static final int FREQ_MIN = 0;
	public static final int FREQ_MAX = 400;
	public static final int FREQ_STEP = 25;
	
	JRadioButton[] filterRadioButtons;
	JSlider rcSlider;
	JSlider wsSlider;
	JSlider freqSlider;
	JLabel lFreq;
	JLabel lLength;
	
	private String L_FREQ = "Cutoff frequency (Hz)";
	private String L_LENGTH = "Filter Length (samples)";
	
	FilterPanel() {
		//filter
		this.setBorder(new TitledBorder(null, "Data Under Voice (DUV) Filter", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		initializeGui();
	}
	
	/*
	public Filter getFilter(double sampleRate, double freq, int len) {
		filters[Config.useFilterNumber].init(sampleRate, freq, len);
		return filters[Config.useFilterNumber];
	}
	*/
	
	public void initializeGui() {

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		ButtonGroup group = new ButtonGroup();
		filterRadioButtons = new JRadioButton[MAX_FILTERS];

		for (int i=0; i< MAX_FILTERS; i++) {
			filterRadioButtons[i] = addRadioButton(filterName[i]);
			group.add(filterRadioButtons[i]);
		}
	
		JLabel filler0 = new JLabel("");
		filler0.setMinimumSize(new Dimension(14,14));
		filler0.setMaximumSize(new Dimension(14,14));
		add(filler0);
		lFreq = new JLabel(L_FREQ);
		lFreq.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(lFreq);
		
		freqSlider = createSlider(FREQ_MIN, FREQ_MAX, (int)Config.filterFrequency);
		freqSlider.setMinorTickSpacing(FREQ_STEP);
		
		add(freqSlider);
		
		//JPanel panel1 = new JPanel();
		//add(panel1);
		JLabel filler1 = new JLabel("");
		filler1.setMinimumSize(new Dimension(14,14));
		filler1.setMaximumSize(new Dimension(14,14));
		add(filler1);
		lLength = new JLabel(L_LENGTH);
		lLength.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(lLength);
		
		rcSlider = createSlider(RC_LEN_MIN, RC_LEN_MAX, RC_LEN_MIN);
		wsSlider = createSlider(WS_LEN_MIN, WS_LEN_MAX, WS_LEN_MIN*2);
		//updateSlider();
		add(rcSlider);
		add(wsSlider);
		
		JLabel filler2 = new JLabel("");
		filler2.setMinimumSize(new Dimension(14,44));
		filler2.setMaximumSize(new Dimension(14,44));
		add(filler2);
		/*
		JPanel panel1 = new JPanel();
		add(panel1);
//		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.setLayout(new FlowLayout());
		JLabel lfreq = new JLabel("Cuttoff Freq");
		panel1.add(lfreq);
		JTextField txtFreq = new JTextField();
		panel1.add(txtFreq);
		JPanel panel2 = new JPanel();
		add(panel2);
		//panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.setLayout(new FlowLayout());
		JLabel lLength = new JLabel("Length (bits)");
		panel2.add(lLength);
		JTextField txtLength = new JTextField();
		panel2.add(txtLength);
		*/
		
		setFilter();
	}
	
	private JSlider createSlider(int min, int max, int tick) {
		JSlider slideFilterLength = new JSlider(JSlider.HORIZONTAL, min, max, tick); 
		slideFilterLength.addChangeListener(this);
		//Turn on labels at major tick marks.
		slideFilterLength.setMajorTickSpacing(tick);
		//slideFilterLength.setMinorTickSpacing(64);
		slideFilterLength.setPaintTicks(true);
		slideFilterLength.setPaintLabels(true);
		//slideFilterLength.createStandardLabels(tick);
		slideFilterLength.setSnapToTicks(true);
		
		return slideFilterLength;
	}
	
	public static void checkFilterParams() {
		if (Config.useFilterNumber == RAISED_COSINE) {
			if (Config.filterLength > RC_LEN_MAX) {
				Config.filterLength = RC_LEN_MAX;
			}
		
		} else if (Config.useFilterNumber == WINDOWED_SINC){
			if (Config.filterLength < WS_LEN_MIN) {
				Config.filterLength = WS_LEN_MIN;
			}
		
		} 
		
	}
	
	private void updateSlider() {
		checkFilterParams();
		if (Config.useFilterNumber == RAISED_COSINE) {
			rcSlider.setVisible(true);
			wsSlider.setVisible(false);
			rcSlider.setValue(Config.filterLength);
			freqSlider.setVisible(true);
		
		} else if (Config.useFilterNumber == WINDOWED_SINC){
			rcSlider.setVisible(false);
			wsSlider.setVisible(true);
			wsSlider.setValue(Config.filterLength);
			freqSlider.setVisible(true);
		
		} else {
			rcSlider.setVisible(false);
			wsSlider.setVisible(false);
			freqSlider.setVisible(false);
			
		}
		lFreq.setText(L_FREQ+" " + Config.filterFrequency);
		lLength.setText(L_LENGTH+" "+Config.filterLength);
		//Log.println("Filter Len Set to: " + Config.filterLength);
		//Config.save();

	}
	
	
	private void setFilter() {
		updateSlider();
		filterRadioButtons[Config.useFilterNumber].setSelected(true);
		//Config.filter = filters[Config.useFilterNumber];
		//Config.save();
	}
	
	private JRadioButton addRadioButton(String name) {
		JRadioButton radioButton = new JRadioButton(name);
		radioButton.setEnabled(true);
		radioButton.addActionListener(this);
		add(radioButton);
		return radioButton;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for (int i=0; i< MAX_FILTERS; i++) {
			if (e.getSource() == filterRadioButtons[i]) {
				Config.useFilterNumber = i;

				setFilter();
				Config.save();
			}
		}
	
	}

	/**
	 * Listen for changes from the slider
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider)e.getSource();
	    if (!source.getValueIsAdjusting()) {
	    	
	    	if (source == freqSlider) {
	    		int freq = (int)source.getValue();
	    		Config.filterFrequency = freq;
	    		Config.save();
	    	} else {
	    		int len = (int)source.getValue();
	    		Config.filterLength = len;
	    		Config.save();
	    	}
	    }
	}

	
}
