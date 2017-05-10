package com.lapissea.amaze;

public class Main{
	
	public static void main(String[] args){
		new Gui();
		Timer.start();
	}

	private static final Runnable SLOW_MO=()->UtilM.sleep(1),MO=()->{};
	private static Runnable MO_SATE=MO;

	public static void setSlowMotion(boolean flag){
		MO_SATE=flag?SLOW_MO:MO;
	}
	public static boolean isSlowMo(){
		return MO_SATE==SLOW_MO;
	}
	
	public static void slowMo(){
		MO_SATE.run();
	}
	
	
}
