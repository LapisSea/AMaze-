package com.lapissea.amaze;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import net.coobird.thumbnailator.Thumbnailator;
import sun.awt.shell.ShellFolder;

public class UtilM{
	
	public static void sleep(long ms){
		try{
			Thread.sleep(ms);
		}catch(InterruptedException e){}
	}
	
	public static void sleep(long ms, int nano){
		try{
			Thread.sleep(ms, nano);
		}catch(InterruptedException e){}
	}
	
	public static BufferedImage toBufferedImage(Icon img){
		BufferedImage bimage=new BufferedImage(img.getIconWidth(), img.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D bGr=bimage.createGraphics();
		img.paintIcon(null, bGr, 0, 0);
		bGr.dispose();
		return bimage;
	}
	
	public static BufferedImage toBufferedImage(Image img){
		if(img instanceof BufferedImage) return (BufferedImage)img;
		
		BufferedImage bimage=new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D bGr=bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();
		
		return bimage;
	}
	
	public static Color blend(Color c1, Color c2, float ratio){
		if(ratio>1f) ratio=1f;
		else if(ratio<0f) ratio=0f;
		float iRatio=1.0f-ratio;
		
		int i1=c1.getRGB();
		int i2=c2.getRGB();
		
		int a1=(i1>>24&0xff);
		int r1=((i1&0xff0000)>>16);
		int g1=((i1&0xff00)>>8);
		int b1=(i1&0xff);
		
		int a2=(i2>>24&0xff);
		int r2=((i2&0xff0000)>>16);
		int g2=((i2&0xff00)>>8);
		int b2=(i2&0xff);
		
		int a=(int)((a1*iRatio)+(a2*ratio));
		int r=(int)((r1*iRatio)+(r2*ratio));
		int g=(int)((g1*iRatio)+(g2*ratio));
		int b=(int)((b1*iRatio)+(b2*ratio));
		
		return new Color(a<<24|r<<16|g<<8|b);
	}
	
	public static BufferedImage screenshotComponent(Component c){
		if(c.getWidth()>0&&c.getHeight()>0){
			BufferedImage img=new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g=img.getGraphics();
			c.paint(g);
			g.dispose();
			return img;
		}
		return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	}
	
	private static final Character[] ILEGALS={'\\','/',':','*','?','"','<','>','|'};
	
	public static boolean isCharBadForPath(char ch){
		return Arrays.stream(ILEGALS).anyMatch(c->c.charValue()==ch);
	}
	
	public static String nameToSafePath(String s){
		int len=s.length();
		StringBuilder sb=new StringBuilder(len);
		for(int i=0;i<len;i++){
			char ch=s.charAt(i);
			if(isCharBadForPath(ch)){
				sb.append('%');
				if(ch<0x10){
					sb.append('0');
				}
				sb.append(Integer.toHexString(ch));
			}else{
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	public static BufferedImage getFileImage(File file, int size){
		BufferedImage img;
		try{
			img=ImageIO.read(file);
			float zoom=Math.min(1, ((float)size)/Math.max(img.getWidth(), img.getHeight()));
			img=Thumbnailator.createThumbnail(img, (int)(img.getWidth()*zoom), (int)(img.getHeight()*zoom));
		}catch(Exception e){
			try{
				img=Thumbnailator.createThumbnail(UtilM.toBufferedImage(ShellFolder.getShellFolder(file).getIcon(true)), size, size);
			}catch(FileNotFoundException e1){
				return null;
			}
		}
		return img;
	}
}
