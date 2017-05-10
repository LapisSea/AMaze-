package com.lapissea.amaze;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;

import com.lapissea.amaze.Pathfinder.Node;
import com.lapissea.amaze.Timer.UpdateableImpl;

import net.coobird.thumbnailator.Thumbnailator;

@SuppressWarnings("serial")
public class Gui extends JFrame{
	
	public static Color BKG_COL=Color.WHITE;
	
	public UpdateableImpl	ticker		=Timer.newImpl(this::tick);
	BufferedImage			img,smallerImg;
	JPanel					canvas;
	Pathfinder				pathfinder	=new Pathfinder(this);
	private Vec2i			imgSize		=new Vec2i(),imgPos=new Vec2i();
	private float			lastZoom	=-1,zoom=1;
	Node					close;
	
	public Gui(){
		super("aMaze");
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e){}
		
		setSize(474, 336);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
		loadImageGui();
		setVisible(true);
		createBufferStrategy(2);
	}
	
	private void tick(){
		if(img!=null){
			SwingUtilities.invokeLater(()->canvas.repaint());
		}
	}
	
	private void openImage(){
		FileDialog fd=new FileDialog(this);
		fd.setVisible(true);
		if(fd.getFile()==null) return;
		openImage(new File(fd.getDirectory(), fd.getFile()));
	}
	
	private void openImage(File file){
		
		try{
			BufferedImage img=Objects.requireNonNull(ImageIO.read(file));
			pathfinder.imageProcessThr.kill();
			this.img=img;
			runGui();
			
			new Thread(()->{
				pathfinder.clear();
				boolean[][] data=new boolean[img.getWidth()][img.getHeight()];
				
				for(int x=0;x<img.getWidth();x++){
					Main.slowMo();
					for(int y=0;y<img.getHeight();y++){
						Color px=new Color(img.getRGB(x, y));
						boolean col=(px.getRed()+px.getGreen()+px.getBlue())/(3F*256)>0.5F;
						img.setRGB(x, y, (col?Color.WHITE:Color.BLACK).hashCode());
						data[x][y]=col;
					}
				}
				
				pathfinder.process(data);
			}).start();
			
		}catch(Exception e2){
			JOptionPane.showMessageDialog(this, "Selected file is not an image!", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void draw(Graphics2D g, int w, int h){
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		int imgW=img.getWidth(),imgH=img.getHeight();
		zoom=Math.min(w/(float)imgW, h/(float)imgH)*0.99F;
		int imgActW=(int)(imgW*zoom),imgActH=(int)(imgH*zoom);
		imgSize.set(imgActW, imgActH);
		imgPos.set((w-imgActW)/2, (h-imgActH)/2);
		AffineTransform trans=g.getTransform();
		g.translate(imgPos.getX(), imgPos.getY());
		if(lastZoom!=zoom)ticker.markDirty();
		
		if(zoom<1){
			if(lastZoom!=zoom) smallerImg=Thumbnailator.createThumbnail(img, imgActW, imgActH);
			g.drawImage(smallerImg, 0, 0, null);
		}else g.drawImage(img, 0, 0, imgActW, imgActH, null);
		
		g.translate(zoom/2, zoom/2);
		try{
			int itsTimeToStop=100000,lel=pathfinder.nodes.size(),increment=Math.max((int)(1/zoom), 1),lil=(int)(lel*(1/zoom));
			
			if(lil>itsTimeToStop) increment=lil/itsTimeToStop;
			increment=2;
			List<Node> top=new ArrayList<>();
			for(int j=0;j<lel;j+=increment){
				if(j>=lel) break;
				
				Node n=pathfinder.nodes.get(j);
				if(n==null) continue;
				if(n.used&&!n.bad) top.add(n);
				else drawNode(n, g);
			}
			for(Node n:top)
				drawNode(n, g);
			
			g.setColor(Color.GREEN);
			g.setStroke(new BasicStroke(3));

			for(int i=1;i<pathfinder.path.size();i++){
				Node n1=pathfinder.path.get(i-1),n2=pathfinder.path.get(i);
				if(n1==null||n2==null) continue;
				
				int x2=(int)(n2.pos.getX()*zoom);
				int y2=(int)(n2.pos.getY()*zoom);
				int x1=(int)(n1.pos.getX()*zoom);
				int y1=(int)(n1.pos.getY()*zoom);
				
				g.drawLine(x1, y1, x2, y2);
				
			}
			g.setColor(Color.BLUE);
			for(int i=1;i<pathfinder.debugPairs.size();i+=2){
				Node n1=pathfinder.debugPairs.get(i-1),n2=pathfinder.debugPairs.get(i);
				if(n1==null||n2==null) continue;
				
				int x2=(int)(n2.pos.getX()*zoom);
				int y2=(int)(n2.pos.getY()*zoom);
				int x1=(int)(n1.pos.getX()*zoom);
				int y1=(int)(n1.pos.getY()*zoom);
				
				g.drawLine(x1, y1, x2, y2);
				
			}
			
		}catch(Exception e){}
		if(close!=null){
			
			int x=(int)(close.pos.getX()*zoom);
			int y=(int)(close.pos.getY()*zoom);
			int size=14;
			g.fillRect(x-size/2, y-size/2, size, size);
		}
		//System.out.println(zoom);
		lastZoom=zoom;
		
		int work=MultiTasking.pendingWork();
		if(work>0){
			g.setTransform(trans);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
			g.setColor(Color.DARK_GRAY);
			g.drawString("Work: "+work, 20, 20);
		}
	}
	
	private void drawNode(Node n, Graphics2D g){
		
		int size=Math.max(1, (int)Math.sqrt(zoom)*n.debugMateCount);
		int x=(int)(n.pos.getX()*zoom);
		int y=(int)(n.pos.getY()*zoom);
		
		if(!n.special){
			g.setColor(n.bad?new Color(150, 100, 100):n.used?Color.GREEN:Color.LIGHT_GRAY);
			g.fillRect(x-size/2, y-size/2, size, size);
		}
		
		if(n.special){
			g.setColor(Color.GREEN.darker());
			size=(int)Math.min(size*3, Math.max(1, zoom))*(n.used?3:1);
			g.fillRect(x-size/2, y-size/2, size, size);
		}
	}
	
	private void runGui(){
		getContentPane().removeAll();
		JPanel mainPanel=(JPanel)getContentPane();
		mainPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel=new JPanel();
		panel.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		panel.setBackground(Color.WHITE);
		FlowLayout flowLayout=(FlowLayout)panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		mainPanel.add(panel, BorderLayout.NORTH);
		
		JButton btnAnotherMaze=new JButton("Another maze");
		panel.add(btnAnotherMaze);
		btnAnotherMaze.addActionListener(e->openImage());
		
		JCheckBox chckbxUseMultithreading=new JCheckBox("Use multi-threading");
		chckbxUseMultithreading.setBackground(Color.WHITE);
		panel.add(chckbxUseMultithreading);
		chckbxUseMultithreading.addActionListener(e->MultiTasking.USE_MULTITHREADING=chckbxUseMultithreading.isSelected());
		chckbxUseMultithreading.setSelected(MultiTasking.USE_MULTITHREADING);
		
		JCheckBox chckbxSlowMotionMode=new JCheckBox("Slow motion mode");
		chckbxSlowMotionMode.setBackground(Color.WHITE);
		panel.add(chckbxSlowMotionMode);
		chckbxSlowMotionMode.addActionListener(e->Main.setSlowMotion(chckbxSlowMotionMode.isSelected()));
		chckbxSlowMotionMode.setSelected(Main.isSlowMo());
		
		JPanel panel_1=new JPanel();
		panel_1.setBackground(Color.WHITE);
		mainPanel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		
		canvas=new JPanel(){
			
			@Override
			protected void paintComponent(Graphics g){
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
				if(img!=null){
					draw((Graphics2D)g, this.getWidth(), this.getHeight());
				}
			}
		};
		panel_1.add(canvas);
		MouseAdapter m=new MouseAdapter(){
			
			@Override
			public void mouseClicked(MouseEvent e){
				int x=e.getX()-imgPos.getX();
				int y=e.getY()-imgPos.getY();
				if(x<0||y<0||x>imgSize.getX()||y>imgSize.getY()) return;
				x/=zoom;
				y/=zoom;
				if(SwingUtilities.isLeftMouseButton(e)) pathfinder.setStart(x, y);
				if(SwingUtilities.isRightMouseButton(e)) pathfinder.setEnd(x, y);
			}
			
			@Override
			public void mouseMoved(MouseEvent e){
				int x=e.getX()-imgPos.getX();
				int y=e.getY()-imgPos.getY();
				ticker.markDirty();
				if(x<0||y<0||x>imgSize.getX()||y>imgSize.getY()){
					close=null;
					return;
				}
				x/=zoom;
				y/=zoom;
				close=pathfinder.findClosest(x, y);
			}
		};
		
		canvas.addMouseMotionListener(m);
		canvas.addMouseListener(m);
		canvas.addMouseWheelListener(m);
		
		ticker.markDirty();
		
		SwingUtilities.invokeLater(()->SwingUtilities.updateComponentTreeUI(getContentPane()));
		
	}
	
	private void loadImageGui(){
		JPanel mainPanel=new JPanel();
		mainPanel.setBackground(BKG_COL);
		
		JPanel wrapperPanel=new JPanel(new GridBagLayout());
		wrapperPanel.setBackground(BKG_COL);
		GridBagConstraints gbc_mainPanel=new GridBagConstraints();
		gbc_mainPanel.insets=new Insets(0, 0, 5, 0);
		gbc_mainPanel.gridx=0;
		gbc_mainPanel.gridy=0;
		wrapperPanel.add(mainPanel, gbc_mainPanel);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JLabel note=new JLabel("Select an image (dark = wall, light = path)");
		note.setAlignmentX(Component.CENTER_ALIGNMENT);
		mainPanel.add(note);
		
		Component verticalStrut=Box.createVerticalStrut(5);
		mainPanel.add(verticalStrut);
		
		JButton loadButton=new JButton("Load maze");
		loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		mainPanel.add(loadButton);
		loadButton.addActionListener(e->openImage());
		
		getContentPane().removeAll();
		getContentPane().add(wrapperPanel);
	}
}
