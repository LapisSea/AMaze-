package com.lapissea.amaze;

public class Vec2i{
	
	private int x,y;
	
	public Vec2i(){
		this(0,0);
	}
	public Vec2i(int x, int y){
		set(x, y);
	}
	
	public void set(int x, int y){
		this.x=x;
		this.y=y;
	}
	
	public int getX(){
		return x;
	}
	
	public void setX(int x){
		this.x=x;
	}
	
	public int getY(){
		return y;
	}
	
	public void setY(int y){
		this.y=y;
	}
	public float dist(Vec2i pos){
		int x1=getX()-pos.getX();
		int y1=getY()-pos.getY();
		return (float)Math.sqrt(x1*x1+y1*y1);
	}
	public float atan2(Vec2i pos){
		int x1=pos.getX()-getX();
		int y1=pos.getY()-getY();
		
		
		return (float)Math.atan2(x1,y1);
	}
	
	@Override
	public String toString(){
		return "Vec2i{x="+getX()+", y="+getY()+"}";
	}
	
}
