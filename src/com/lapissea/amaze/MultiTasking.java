package com.lapissea.amaze;

import java.util.ArrayList;

public class MultiTasking{
	
	private static class TaskedThread extends Thread{
		
		Runnable task;
		
		public TaskedThread(int id){
			super("TaskExecutor-"+id);
		}
		
		@Override
		public void run(){
			while(true){
				if(hasWork()){
					task.run();
					task=null;
					ask();
				}else ask();
			}
		}
		
		private void ask(){
			if(askOverlord(this)) try{
				synchronized(this){
					wait();
				}
			}catch(InterruptedException e){
				throw new RuntimeException(e);
			}
		}
		
		boolean hasWork(){
			return task!=null;
		}
		
	}
	
	public static boolean							USE_MULTITHREADING	=true;
	public static final int							THREAD_CAP			=Runtime.getRuntime().availableProcessors();
	private static final ArrayList<TaskedThread>	THREADS				=new ArrayList<>();
	private static final ArrayList<TaskedThread>	FREE_THREADS		=new ArrayList<>();
	private static final ArrayList<Runnable>		QUEUE				=new ArrayList<>();
	private static Runnable							FINISH;
	static{
		for(int i=0;i<THREAD_CAP;i++){
			TaskedThread thread=new TaskedThread(i);
			thread.start();
			FREE_THREADS.add(thread);
			THREADS.add(thread);
		}
	}
	
	public synchronized static void run(Runnable task){
		if(!USE_MULTITHREADING) task.run();
		else if(!FREE_THREADS.isEmpty()){
			TaskedThread thread=FREE_THREADS.remove(0);
			thread.task=task;
			synchronized(thread){
				thread.notifyAll();
			}
		}else QUEUE.add(task);
	}
	
	public static void onFinish(Runnable finish){
		FINISH=finish;
	}
	
	public static void kill(){
		QUEUE.clear();
	}
	
	public static boolean hasWork(){
		if(!USE_MULTITHREADING){
			if(QUEUE.isEmpty())return false;
			QUEUE.stream().forEach(Runnable::run);
			QUEUE.clear();
			return false;
		}
		return !QUEUE.isEmpty()||THREADS.stream().anyMatch(TaskedThread::hasWork);
	}
	
	public static int pendingWork(){
		return QUEUE.size();
	}
	
	private synchronized static boolean askOverlord(TaskedThread taskedThread){
		if(QUEUE.isEmpty()){
			FREE_THREADS.add(taskedThread);
			if(FINISH!=null&&!hasWork()){
				FINISH.run();
				FINISH=null;
			}
			return true;
		}
		//System.out.println(QUEUE.size());
		taskedThread.task=QUEUE.remove(0);
		return false;
	}
	
}
