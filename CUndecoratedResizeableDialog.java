package com.acm.main.gui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
 
/**
 * This JDialog subclass supports resizing and undecorated mode.
 * There is also support for disalbing the resizing on any given side.
 */
public class CUndecoratedResizeableDialog extends JDialog implements MouseListener, MouseMotionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static final protected int	NW_SIDE		= 1;
	static final protected int	N_SIDE		= 2;
	static final protected int	NE_SIDE		= 3;
	static final protected int	L_SIDE		= 4;
	static final protected int	R_SIDE		= 5;
	static final protected int	SW_SIDE		= 6;
	static final protected int	S_SIDE		= 7;
	static final protected int	SE_SIDE		= 8;
	
	private JPanel	resizePanel		= null;
	private JPanel	contentPanel	= null;
	private JLabel	left			= null;
	private JLabel	right			= null;
	private JLabel	top				= null;
	private JLabel	bottom			= null;
	private JLabel	topleft			= null;
	private JLabel	topright		= null;
	private JLabel	bottomleft		= null;
	private JLabel	bottomright		= null;
 
	private Rectangle startSize = null;
	
	private HashSet<Integer> hsDisabledSides = new HashSet<Integer>();
	
	final int minWidth;
	final int minHeight;
	
	public CUndecoratedResizeableDialog(int minWidth, int minHeight)
	{
		super();
		initialize();
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}
	
	private void initialize()
	{
		resizePanel = new JPanel(new BorderLayout());
		contentPanel = new JPanel(new BorderLayout());
		setUndecorated(true);
		
		left = new JLabel();
		right = new JLabel();
		top = new JLabel();
		bottom = new JLabel();
		topleft = new JLabel();
		topright = new JLabel();
		bottomleft = new JLabel();
		bottomright = new JLabel();
		
		left.setPreferredSize(new Dimension(2, 0));
		left.setMinimumSize(new Dimension(2, 0));
		
		right.setPreferredSize(new Dimension(2, 0));
		right.setMinimumSize(new Dimension(2, 0));
		top.setPreferredSize(new Dimension(0, 2));
		top.setMinimumSize(new Dimension(0, 2));
		bottom.setPreferredSize(new Dimension(0, 2));
		bottom.setMinimumSize(new Dimension(0, 2));
		
		left.addMouseListener(this);
		right.addMouseListener(this);
		top.addMouseListener(this);
		bottom.addMouseListener(this);
 
		left.addMouseMotionListener(this);
		right.addMouseMotionListener(this);
		top.addMouseMotionListener(this);
		bottom.addMouseMotionListener(this);
		
		left.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
		right.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
		top.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
		bottom.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
 
		topleft.setPreferredSize(new Dimension(3, 3));
		topleft.setMinimumSize(new Dimension(3, 3));
		topright.setPreferredSize(new Dimension(3, 3));
		topright.setMinimumSize(new Dimension(3, 3));
		bottomleft.setPreferredSize(new Dimension(3, 3));
		bottomleft.setMinimumSize(new Dimension(3, 3));
		bottomright.setPreferredSize(new Dimension(3, 3));
		bottomright.setMinimumSize(new Dimension(3, 3));
		
		topleft.addMouseListener(this);
		topright.addMouseListener(this);
		bottomleft.addMouseListener(this);
		bottomright.addMouseListener(this);
 
		topleft.addMouseMotionListener(this);
		topright.addMouseMotionListener(this);
		bottomleft.addMouseMotionListener(this);
		bottomright.addMouseMotionListener(this);
		
		topleft.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
		topright.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
		bottomleft.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
		bottomright.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
		
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBackground(Color.BLACK);
		northPanel.add(topleft, BorderLayout.WEST);
		northPanel.add(top, BorderLayout.CENTER);
		northPanel.add(topright, BorderLayout.EAST);
 
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setBackground(Color.BLACK);
		southPanel.add(bottomleft, BorderLayout.WEST);
		southPanel.add(bottom, BorderLayout.CENTER);
		southPanel.add(bottomright, BorderLayout.EAST);
		
		resizePanel.add(left, BorderLayout.WEST);
		resizePanel.add(right, BorderLayout.EAST);
		resizePanel.add(northPanel, BorderLayout.NORTH);
		resizePanel.add(southPanel, BorderLayout.SOUTH);
		resizePanel.add(contentPanel, BorderLayout.CENTER);
		
		resizePanel.setBackground(Color.BLACK);
		
		this.setContentPane(resizePanel);
	}
	
	@Override
	public Container getContentPane()
	{
		return contentPanel;
	}
 
	public void mousePressed(MouseEvent e) 
	{
		startSize = this.getBounds();
	}
 
	public void mouseDragged(MouseEvent e)
	{
		if (minWidth >= getWidth() || minHeight >= getHeight()){
			if (minWidth >= getWidth()){
				startSize.width = minWidth + 1;
			}
			if (minHeight >= getHeight()){
				startSize.height = minHeight + 1;
			}
			this.setBounds(startSize);
			return;
		}
		if (startSize == null)
			return;
		if (e.getSource() == topleft)
		{
			if (hsDisabledSides.contains(NW_SIDE))
				return;
			startSize.y += e.getY();
			startSize.height -= e.getY();
			startSize.x += e.getX();
			startSize.width -= e.getX();
			this.setBounds(startSize);
		}
		else
		if (e.getSource() == top)
		{
			if (hsDisabledSides.contains(N_SIDE))
				return;
			startSize.y += e.getY();
			startSize.height -= e.getY();
			this.setBounds(startSize);
		}
		else
		if (e.getSource() == topright)
		{
			if (hsDisabledSides.contains(NE_SIDE))
				return;
			startSize.y += e.getY();
			startSize.height -= e.getY();
			startSize.width += e.getX();
			this.setBounds(startSize);
			
		}
		else
		if (e.getSource() == left)
		{
			if (hsDisabledSides.contains(L_SIDE))
				return;
			startSize.x += e.getX();
			startSize.width -= e.getX();
			this.setBounds(startSize);
		}
		else
		if (e.getSource() == right)
		{
			if (hsDisabledSides.contains(R_SIDE))
				return;
			startSize.width += e.getX();
			this.setBounds(startSize);
		}
		else
		if (e.getSource() == bottomleft)
		{
			if (hsDisabledSides.contains(SW_SIDE))
				return;
			startSize.height += e.getY();
			startSize.x += e.getX();
			startSize.width -= e.getX();
			this.setBounds(startSize);
		}
		else
		if (e.getSource() == bottom)
		{
			if (hsDisabledSides.contains(S_SIDE))
				return;
			startSize.height += e.getY();
			this.setBounds(startSize);
		}
		else
		if (e.getSource() == bottomright)
		{
			if (hsDisabledSides.contains(SE_SIDE))
				return;
			startSize.height += e.getY();
			startSize.width += e.getX();
			this.setBounds(startSize);
		}
		if (minWidth >= getWidth() || minHeight >= getHeight()){
			if (minWidth >= getWidth()){
				startSize.width = minWidth + 1;
			}
			if (minHeight >= getHeight()){
				startSize.height = minHeight + 1;
			}
			this.setBounds(startSize);
			return;
		}
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {} 
	public void mouseEntered(MouseEvent e) {} 
	public void mouseExited(MouseEvent e) {}
 
	protected void disableResizeSide(int side)
	{
		hsDisabledSides.add(side);
		if (side == NW_SIDE)
		{
			topleft.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == N_SIDE)
		{
			top.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == NE_SIDE)
		{
			topright.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == L_SIDE)
		{
			left.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == R_SIDE)
		{
			right.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == SW_SIDE)
		{
			bottomleft.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == S_SIDE)
		{
			bottom.setCursor(Cursor.getDefaultCursor());
		}
		else
		if (side == SE_SIDE)
		{
			bottomright.setCursor(Cursor.getDefaultCursor());
		}
	}
}
