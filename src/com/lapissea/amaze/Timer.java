package com.lapissea.amaze;

import java.util.ArrayList;
import java.util.List;

public class Timer{
	
	public interface Updateable{
		
		void update();
		
		void setDirty(boolean dirty);
		
		default void markDirty(){
			register();
			setDirty(true);
		}
		
		boolean isDirty();

		boolean isRegistered();
		void register();
		void unregister();
		
	}
	
	public static class UpdateableImpl implements Updateable{
		
		private final Runnable	hook;
		private boolean			dirty		=true;
		private boolean			registered	=false;
		
		public UpdateableImpl(Runnable hook){
			this.hook=hook;
		}
		
		@Override
		public void update(){
			hook.run();
		}
		
		@Override
		public void setDirty(boolean dirty){
			this.dirty=dirty;
		}
		
		@Override
		public boolean isDirty(){
			return dirty;
		}
		
		@Override
		public String toString(){
			return hashCode()+"";
		}
		
		@Override
		public boolean isRegistered(){
			return registered;
		}

		@Override
		public void register(){
			if(registered)return;
			registered=true;
			add(this);
		}

		@Override
		public void unregister(){
			if(!registered)return;
			registered=false;
			remove(this);
		}
	}
	
	private static final List<Updateable>	LISTENERS	=new ArrayList<>(),TO_ADD=new ArrayList<>(),TO_REMOVE=new ArrayList<>();
	private static boolean					RUNNING;
	
	public static void start(){
		new Thread(Timer::run).start();
	}
	
	private static void run(){
		
		int ups=60;
		
		double nextUpdate=0,fequ=1000_000_000D/ups;
		
		while(true){
			long tim=System.nanoTime();
			
			if(tim>=nextUpdate){
				nextUpdate=tim+fequ;
				update();
			}
			if(tim<nextUpdate-1) UtilM.sleep(0, 500000);
		}
	}
	
	private static void update(){
		
		if(TO_ADD.size()>0){
			LISTENERS.addAll(TO_ADD);
			TO_ADD.clear();
		}
		
		RUNNING=true;
		LISTENERS.stream().filter(Updateable::isDirty).forEach(u->{
			u.setDirty(false);
			u.update();
		});
		RUNNING=false;
		
		if(TO_REMOVE.size()>0){
			LISTENERS.removeAll(TO_REMOVE);
			TO_REMOVE.clear();
		}
	}
	
	public static UpdateableImpl newImpl(Runnable hook){
		return add(new UpdateableImpl(hook));
	}
	
	private static <T extends Updateable> T add(T u){
		if(!LISTENERS.contains(u)){
			u.markDirty();
			(RUNNING?TO_ADD:LISTENERS).add(u);
		}
		return u;
	}
	
	private static <T extends Updateable> T remove(T u){
		(RUNNING?TO_REMOVE:LISTENERS).remove(u);
		return u;
	}
}
