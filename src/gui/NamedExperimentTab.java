package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import common.Config;
import common.Log;
import common.FoxSpacecraft;

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
public class NamedExperimentTab extends ExperimentTab implements ItemListener, Runnable, MouseListener {

	private static final String DECODED = "Payloads Decoded: ";
	public final int DEFAULT_DIVIDER_LOCATION = 350;

	JLabel lblName;
	private String NAME;
	JLabel lblFramesDecoded;

	//JCheckBox showRawBytes;
	ExperimentLayoutTableModel expTableModel;
	RagPacketTableModel expPacketTableModel; // translated values put in layout2

	JPanel healthPanel;
	JPanel topHalfPackets;
	JPanel bottomHalfPackets;

	boolean displayTelem = true;

	BitArrayLayout layout; // raw values
	BitArrayLayout layout2; // translated format

	public NamedExperimentTab(FoxSpacecraft sat, String displayName, BitArrayLayout displayLayout, BitArrayLayout displayLayout2, int displayType)  {
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " " + displayName;

		//int j = 0;
		this.layout = displayLayout;
		this.layout2 = displayLayout2;

		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, layout.name, "splitPaneHeight");

		lblName = new JLabel(NAME);
		lblName.setMaximumSize(new Dimension(1600, 20));
		lblName.setMinimumSize(new Dimension(1600, 20));
		lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
		topPanel.add(lblName);

		lblFramesDecoded = new JLabel(DECODED);
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblFramesDecoded.setBorder(new EmptyBorder(5, 2, 5, 5) );
		topPanel.add(lblFramesDecoded);

		healthPanel = new JPanel();

		healthPanel.setLayout(new BoxLayout(healthPanel, BoxLayout.Y_AXIS));
		healthPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		healthPanel.setBackground(Color.DARK_GRAY);

		topHalfPackets = new JPanel(); 
		topHalfPackets.setBackground(Color.DARK_GRAY);
		bottomHalfPackets = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars5.png");
		bottomHalfPackets.setBackground(Color.DARK_GRAY);
		healthPanel.add(topHalfPackets);
		healthPanel.add(bottomHalfPackets);

		initDisplayHalves(healthPanel);

		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));

		BitArrayLayout none = null;
		if (layout2 == null ) {
			Log.errorDialog("MISSING LAYOUTS", "The spacecraft file for satellite " + fox.user_display_name + " is missing the layout definition for "
					+ NAME + "\n  Remove this satellite or fix the layout file");
			System.exit(1);
		} else 
			try {
				analyzeModules(layout2, none, none, displayType);
			} catch (LayoutLoadException e) {
				Log.errorDialog("FATAL - Load Aborted", e.getMessage());
				e.printStackTrace(Log.getWriter());
				System.exit(1);
			}
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				healthPanel, centerPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		if (splitPaneHeight != 0) 
			splitPane.setDividerLocation(splitPaneHeight);
		else
			splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);

		SplitPaneUI spui = splitPane.getUI();
		if (spui instanceof BasicSplitPaneUI) {
			// Setting a mouse listener directly on split pane does not work, because no events are being received.
			((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					splitPaneHeight = splitPane.getDividerLocation();
					//Log.println("SplitPane: " + splitPaneHeight);
					Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, layout.name, "splitPaneHeight", splitPaneHeight);
				}
			});
		}
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);

		showRawBytes = new JCheckBox("Show Raw Bytes", Config.displayRawRadData);
		bottomPanel.add(showRawBytes );
		showRawBytes.addItemListener(this);


		addBottomFilter();

		expTableModel = new ExperimentLayoutTableModel(layout);
		expPacketTableModel = new RagPacketTableModel();
		addTables(expTableModel,expPacketTableModel);

		addPacketModules();
		topHalfPackets.setVisible(false);
		bottomHalfPackets.setVisible(false);

		// initial populate
		parseRadiationFrames();
	}

	int total;
	protected void displayFramesDecoded(int u) {
		total = u;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				lblFramesDecoded.setText(DECODED + total);
				lblFramesDecoded.invalidate();
				topPanel.validate();
			}
		});
	
	}

	private void addPacketModules() {

	}


	protected void addTables(AbstractTableModel expTableModel, AbstractTableModel ragPacketTableModel) {
		super.addTables(expTableModel, ragPacketTableModel);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );

		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		
		for (int i=0; i<table.getColumnCount()-2; i++) {
			column = table.getColumnModel().getColumn(i+2);
			column.setPreferredWidth(25);
		}

		column = packetTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = packetTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);

		column = packetTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(80);

		column = packetTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(70);

		column = packetTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(600);
	}

	protected void parseRadiationFrames() {
		if (!Config.payloadStore.initialized()) return;
		String[][] data = null;
					
		if (Config.displayRawRadData) {
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, layout.name);	
			if (data != null && data.length > 0)
				parseRawBytes(data,expTableModel);
		} else {
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, layout2.name);	
			if (data != null && data.length > 0) {
				parseTelemetry(data);
			}
		}

		if (showRawBytes.isSelected()) {
			packetScrollPane.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			packetScrollPane.setVisible(true);
			scrollPane.setVisible(false);
		}

		displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, layout.name));
		MainWindow.frame.repaint();
	}


	protected void parseTelemetry(String data[][]) {
		int len = data.length;
		long[][] keyPacketData = null;
		String[][] packetData = null;
		keyPacketData = new long[len][1];
		packetData = new String[len][5];
		for (int i=0; i < len; i++) { 
			keyPacketData[len-i-1][0] = Long.parseLong(data[i][0]);
			packetData[len-i-1][0] = String.format("%08x", Long.parseLong(data[i][0]));
			packetData[len-i-1][1] = data[i][1];
			packetData[len-i-1][2] = data[i][2];
			packetData[len-i-1][3] = data[i][3];
			packetData[len-i-1][4] = data[i][4];
		}

		if (packetData.length > 0) {

			expPacketTableModel.setData(keyPacketData, packetData);
		}

	}

	public void updateTab(FramePart rad, boolean refreshTable) {
		if (!Config.payloadStore.initialized()) return;
		if (rad != null) {
			for (DisplayModule mod : topModules) {
				if (mod != null)
					mod.updateRtValues(rad);
			}
			if (bottomModules != null)
			for (DisplayModule mod : bottomModules) {
				if (mod != null)
					mod.updateRtValues(rad);
			}
		}
	}

	@Override
	public void run() {
		Thread.currentThread().setName("UwTab");
		running = true;
		done = false;
		int currentFrames = 0;
		boolean justStarted = true;
		while(running) {

			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HealthTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			if (foxId != 0 && Config.payloadStore.initialized()) {
				int frames = Config.payloadStore.getNumberOfFrames(foxId, layout.name);
				if (frames != currentFrames) {
					currentFrames = frames;
					updateTab(Config.payloadStore.getLatest(foxId, layout.name), true);
					displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, layout.name));
					Config.payloadStore.setUpdated(foxId, layout.name, false);
					MainWindow.setTotalDecodes();
					parseRadiationFrames(); //which also repaints the window
					if (justStarted) {
						openGraphs();
						justStarted = false;
					}
					MainWindow.frame.repaint();
				}
				// If either of these are toggled then redisplay the results
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					parseRadiationFrames();
					updateTab(Config.payloadStore.getLatest(foxId, layout.name), true);
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatest(foxId, layout.name), true);
				}
			}
		}
		done = true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		super.itemStateChanged(e);
		Object source = e.getItemSelectable();

		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawValues = false;
			} else {
				Config.displayRawValues = true;
			}
			Config.save();
			updateTab(Config.payloadStore.getLatest(foxId, layout.name), true);

		}
	}

	@Override
	public void parseFrames() {
		parseRadiationFrames();

	}

	protected void displayRow(JTable table, int fromRow, int row) {
		if (Config.displayRawRadData) {
			long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
			long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
			//Log.println("RESET: " + reset_l);
			//Log.println("UPTIME: " + uptime);
			int reset = (int)reset_l;
			updateTab(Config.payloadStore.getFramePart(foxId, reset, uptime, layout.name, false), false);
		} else {
			updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
		}
		if (fromRow == NO_ROW_SELECTED)
			fromRow = row;
		if (fromRow <= row)
			table.setRowSelectionInterval(fromRow, row);
		else
			table.setRowSelectionInterval(row, fromRow);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}