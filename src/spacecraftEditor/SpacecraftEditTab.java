package spacecraftEditor;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import common.Spacecraft;

public class SpacecraftEditTab extends JPanel {

	Spacecraft sat;
	JTabbedPane tabbedPane;
	
	public SpacecraftEditTab(Spacecraft s) {
		sat = s;
		
		setLayout(new BorderLayout(0, 0));
		add(tabbedPane, BorderLayout.CENTER);

	}
}
