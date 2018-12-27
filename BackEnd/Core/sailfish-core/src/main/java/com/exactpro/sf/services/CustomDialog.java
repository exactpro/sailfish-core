/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.services;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.exactpro.sf.scriptrunner.ScriptRunException;

public class CustomDialog extends JDialog implements ActionListener
{
	/**
	 *
	 */
	private static final long serialVersionUID = 7896545202344376148L;
	public JTextField messageField = new JTextField("");
	public JLabel noticeField = new JLabel("OK - continue test script, Cancel - skip warning, Stop - stop test script.");
	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");
	private final JButton stopButton = new JButton("Stop");

	public byte doIt = 0;

	public GridBagConstraints getRC(int gridx, int gridy)
	{
		GridBagConstraints c1 = new GridBagConstraints();
		c1.gridx = gridx;
		c1.gridy = gridy;
		c1.fill = GridBagConstraints.NONE;
		c1.anchor = GridBagConstraints.NONE;
		c1.weightx = 1.0;
		c1.weighty= 1.0;
		c1.insets = new Insets( 1, 1, 1, 1 );
		return c1;
	}

	public CustomDialog()
	{
		this(null);
	}

	public CustomDialog(String description)
	{
		super((JDialog) null, "Warning");
		setModal( true );
		setAlwaysOnTop(true);
		setSize( 600, 150 );
		setResizable(true);
		setLocationRelativeTo(null);

		setLayout( new GridLayout(3, 1) );
		GridBagConstraints c1 = getRC(1, 1);

		messageField.setEditable(false);
		messageField.setBackground(null);
		messageField.setBorder(null);
		messageField.setCursor(new Cursor(Cursor.TEXT_CURSOR));
		messageField.setFont(noticeField.getFont());
		messageField.setText(description != null || "".equals(description) ? description :
			"<html>An unspecified description has been detected.<br/>Would you like to continue the test script?</html>");
		messageField.setVisible(true);

		add( messageField, c1 );

		GridBagConstraints c11 = getRC(1, 2);

		add( noticeField, c11 );

		JPanel bPanel = new JPanel(new FlowLayout());
		bPanel.add(okButton);
		bPanel.add(cancelButton);
		bPanel.add(stopButton);

		GridBagConstraints c5 = getRC(1, 3);
		add(bPanel, c5);

		getRootPane().setDefaultButton( okButton );
		okButton.addActionListener( this );
		cancelButton.addActionListener( this );
		stopButton.addActionListener( this );

		addWindowListener(new WindowAdapter( ) {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					Escape();
				} catch (ScriptRunException e1) {
					throw new ScriptRunException( "User interrupted." );
				}
			}
		});
	}

	public void Escape() throws ScriptRunException
	{
		doIt = 0;
		setVisible(false);
	}
	public void Accept()
	{
		doIt = 1;
		setVisible(false);
	}

	public void Stop() throws ScriptRunException
	{
		doIt = 2;
		setVisible(false);
	}


	@Override
	public void actionPerformed(ActionEvent action)
	{
		if(action.getSource().equals(okButton))
		{
			Accept();
		}
		else if (action.getSource().equals(cancelButton))
		{
			try {
				Escape();
			}
			catch (ScriptRunException e)
			{
				throw new ScriptRunException( "User interrupted." );
			}
		} else if (action.getSource().equals(stopButton))
		{
			Stop();
		}
	}
}