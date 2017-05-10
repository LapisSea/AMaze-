package com.lapissea.amaze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class Pathfinder{
	
	public class KillableThread extends Thread{
		
		protected boolean shouldKill=false;
		
		public KillableThread(Runnable target, String name){
			super(target, name);
		}
		
		public void kill(){
			shouldKill=true;
			synchronized(this){
				try{
					join();
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
		
		protected boolean endIfAsked(){
			synchronized(this){
				notifyAll();
			}
			if(shouldKill) MultiTasking.kill();
			
			return shouldKill;
		}
	}
	
	public static class Node{
		
		public final Vec2i	pos;
		public List<Node>	connectedNodes	=new ArrayList<>();
		public int			debugMateCount;
		public boolean		used,bad;
		public boolean		special;
		
		public Node(int x, int y){
			this(new Vec2i(x, y));
		}
		
		public Node(Vec2i pos){
			this.pos=pos;
			for(int i=0;i<Side.values().length;i++)
				connectedNodes.add(null);
		}
		
		public Node conNode(Side s){
			return connectedNodes.get(s.id);
		}
		
		void addConNode(Node n){
			if(!connectedNodes.contains(n)) connectedNodes.add(n);
		}
	}
	
	public static enum Side{
		UP(0, 0, 1, 1, Math.PI),DOWN(1, 0, -1, 0, 0),LEFT(2, -1, 0, 3, -Math.PI/2),RIGHT(3, 1, 0, 2, Math.PI/2);
		
		public final int	id,x,y;
		public final float	angle;
		private final int	oppositeId;
		
		private Side(int id, int x, int y, int oppositeId, double angle){
			this.id=id;
			this.x=x;
			this.y=y;
			this.oppositeId=oppositeId;
			this.angle=(float)angle;
		}
		
		public Side opposite(){
			return Side.values()[oppositeId];
		}
	}
	
	public Stack<Node>		path					=new Stack<>();
	public List<Node>		nodes					=new ArrayList<>(),debugPairs=new ArrayList<>();
	public List<Node>		nodesToRemove			=new ArrayList<>();
	public Node[][]			nodeMap;
	public Node				start,end,curentPoint;
	public KillableThread	imageProcessThr			=new KillableThread(()->{}, "");
	public KillableThread	pathfindingProcessThr	=new KillableThread(()->{}, "");
	Gui						gui;
	
	private boolean[][] maze;
	
	public Pathfinder(Gui gui){
		this.gui=gui;
	}
	
	private void markDirty(){
		gui.ticker.markDirty();
	}
	
	private boolean get(int x, int y, Side s){
		return get(x+s.x, y+s.y);
	}
	
	private boolean get(int x, int y){
		if(x<0||y<0||x>=maze.length||y>=maze[x].length) return false;
		return maze[x][y];
	}
	
	private void addNode(int x, int y){
		markDirty();
		synchronized(nodes){
			nodes.add(nodeMap[x][y]=new Node(x, y));
		}
	}
	
	private void processLine(int x){
		for(int y=0;y<maze[x].length;y++){
			boolean b=maze[x][y];
			if(!b) continue;
			
			//should it be a node?
			boolean up=get(x, y, Side.UP);
			boolean down=get(x, y, Side.DOWN);
			boolean left=get(x, y, Side.LEFT);
			boolean right=get(x, y, Side.RIGHT);
			
			if((up!=down)||(left!=right)){
				addNode(x, y);
			}else if(x%5==0&&y%5==0) addNode(x, y);
			else{
				//addNode(x, y);
				int count=0;
				if(up) count++;
				if(down) count++;
				if(left) count++;
				if(right) count++;
				if(count>2){
					if(count==4){
						if(get(x+1, y+1)&&get(x-1, y+1)&&get(x+1, y-1)&&get(x-1, y-1)) continue;
					}
					addNode(x, y);
				}
			}
		}
	}
	
	private void processNode(Node n){
		n.debugMateCount=0;
		Node[] sides=new Node[4];
		int nx=n.pos.getX(),ny=n.pos.getY();
		for(Side s:Side.values()){
			//if(n.conNode(s)!=null) continue;
			
			int checkX=nx;
			int checkY=ny;
			
			Node closest=null;
			try{
				do{
					checkX+=s.x;
					checkY+=s.y;
					closest=nodeMap[checkX][checkY];
				}while(closest==null);
			}catch(IndexOutOfBoundsException e){}
			
			if(closest!=null){
				boolean wall=false;
				int clox=closest.pos.getX();
				int cloy=closest.pos.getY();
				
				for(int x=nx>clox?clox:nx, x1=(nx<clox?clox:nx)+1;x<x1;x++){
					for(int y=ny>cloy?cloy:ny, y1=(ny<cloy?cloy:ny)+1;y<y1;y++){
						
						if(!maze[x][y]){
							wall=true;
							break;
						}
						
					}
				}
				
				if(wall) continue;
				
				sides[s.id]=closest;
				n.debugMateCount++;
				
				closest.connectedNodes.set(s.opposite().id, n);
				n.connectedNodes.set(s.id, closest);
			}
			
		}
		
		markDirty();
	}
	
	private void waitUntilFinish(){
		MultiTasking.onFinish(()->{
			synchronized(this){
				notifyAll();
			}
		});
		synchronized(this){
			try{
				wait();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	
	public void clear(){
		path.clear();
		nodes.clear();
		if(maze!=null) nodeMap=new Node[maze.length][maze[0].length];
		MultiTasking.kill();
		start=end=null;
		markDirty();
	}
	
	public void process(boolean[][] maze){
		this.maze=maze;
		clear();
		
		long timeStart=System.currentTimeMillis();
		
		imageProcessThr=new KillableThread(()->{
			float threadDiv=MultiTasking.THREAD_CAP*0.9F;
			
			if(imageProcessThr.endIfAsked()) return;
			
			System.out.println("Analysing image");
			
			int step1=(int)(maze.length/threadDiv);
			for(int x=0;x<maze.length;x+=step1){
				int x1=x;
				MultiTasking.run(()->{
					for(int x2=x1, x3=Math.min(maze.length, x2+step1);x2<x3;x2++){
						if(imageProcessThr.endIfAsked()) return;
						processLine(x2);
						Main.slowMo();
					}
				});
			}
			if(imageProcessThr.endIfAsked()) return;
			if(MultiTasking.hasWork()){
				System.out.println("Waiting for analysation finish");
				waitUntilFinish();
				UtilM.sleep(200);
			}
			if(imageProcessThr.endIfAsked()) return;
			System.out.println("Processing nodes");
			
			int step2=(int)(nodes.size()/threadDiv);
			
			for(int i=0;i<nodes.size();i+=step2){
				int i1=i;
				MultiTasking.run(()->{
					for(int j=i1, k=Math.min(nodes.size(), i1+step2);j<k;j++){
						if(imageProcessThr.endIfAsked()) return;
						processNode(nodes.get(j));
						Main.slowMo();
					}
				});
			}
			if(imageProcessThr.endIfAsked()) return;
			if(MultiTasking.hasWork()){
				System.out.println("Waiting for processing to finish");
				waitUntilFinish();
				UtilM.sleep(200);
			}
			//nodeMap=null;
			if(imageProcessThr.endIfAsked()) return;
			
			System.out.println("Done in: "+(System.currentTimeMillis()-timeStart)+(MultiTasking.USE_MULTITHREADING?"":" not")+" using multithreading");
			System.out.println("Node count="+nodes.size());
			
		}, "ImageProcessing");
		imageProcessThr.start();
	}
	
	public void setStart(int x, int y){
		if(start!=null) start.special=false;
		start=findClosest(x, y);
		if(start!=null){
			processNode(start);
			start.special=true;
		}
		pathfind();
	}
	
	public void setEnd(int x, int y){
		if(end!=null) end.special=false;
		end=findClosest(x, y);
		if(end!=null){
			processNode(end);
			end.special=true;
		}
		pathfind();
	}
	
	public void pathfind(){
		pathfindingProcessThr.kill();
		
		nodes.stream().forEach(p->p.used=p.bad=false);
		path.clear();
		debugPairs.clear();
		markDirty();
		//nodes.stream().filter(p->p.dirty).forEach(this::processNode);
		if(start==null||end==null) return;
		pathfindingProcessThr=new KillableThread(()->{
			pathfindRun();
			
			int l=path.size()-1;
			if(l==0) return;
			
			for(int i=0;i<path.size();i++){
				Node n=path.get(i);
				n.used=true;
				n.bad=false;
				if(i==0) n.addConNode(path.get(1));
				else if(i==l) n.addConNode(path.get(l-1));
				else{
					n.addConNode(path.get(i+1));
					n.addConNode(path.get(i-1));
				}
			}
			curentPoint=null;
			
		}, "Pathfinding");
		pathfindingProcessThr.start();
	}
	
	private void pathfindRun(){
		curentPoint=start;
		System.out.println("Nodes to end floding...");
		while(curentPoint!=end){
			if(pathfindingProcessThr.shouldKill) break;
			if(!runPathfind()) break;
		}
		path.add(curentPoint);
		curentPoint.used=true;
		System.out.println("End reached!");
		
		System.out.println("Optimizing path step 1...");
		
		shortenPath1();
		if(pathfindingProcessThr.shouldKill) return;
		System.out.println("Step 1 optimization done");
		
		System.out.println("Optimizing path step 2...");
		
		shortenPath2();
		if(pathfindingProcessThr.shouldKill) return;
		
		System.out.println("Step 2 optimization done");
		
		System.out.println("Done!");
	}
	
	private void shortenPath1(){
		boolean change;
		int lastChange=0;
		
		do{
			change=false;
			if(pathfindingProcessThr.shouldKill) break;
			
			for(int i=Math.max(0, lastChange), j1=path.size()-2;i<j1;i++){
				
				Node nn=path.get(i);
				int i1=i+1;
				
				int last=i1;
				for(int j=i1;j<path.size()-1;j++){
					if(rayTrace(nn, path.get(j))) last=j;
				}
				
				if(last>i1){
					int ammount=last-i;
					for(int x=0;x<20;x++)
						Main.slowMo();
					Node n=null;
					for(int k=0;k<ammount-1;k++){
						n=path.remove(i1);
						n.bad=true;
					}
					Node toAdd=path.get(i1);
					nn.addConNode(toAdd);
					change=true;
					lastChange=i-1;
					markDirty();
					break;
				}
			}
		}while(change);
		markDirty();
	}
	
	private void shortenPath2(){
		boolean change;
		do{
			change=false;
			
			for(int i=1;i<path.size()-1;i++){
				if(pathfindingProcessThr.shouldKill) return;
				
				Node n1=path.get(i-1),n2=path.get(i),n3=path.get(i+1),start=n2,end=n2;
				int x1=n1.pos.getX(),y1=n1.pos.getY(),x2=n2.pos.getX(),y2=n2.pos.getY(),x3=n3.pos.getX(),y3=n3.pos.getY();
				
				float d1=n1.pos.dist(n2.pos),d2=n3.pos.dist(n2.pos),perc=0,step=1/Math.max(d1, d2);
				if(step==1) continue;
				
				markDirty();
				
				float dist1=d1+d2;
				debugPairs.clear();
				debugPairs.add(n1);
				debugPairs.add(n3);
				for(int j=0;j<20;j++)
					Main.slowMo();
				
				while(true){
					perc+=step;
					if(perc>=1) break;
					
					int startX=(int)(x1+(x2-x1)*(1-perc)),startY=(int)(y1+(y2-y1)*(1-perc)),endX=(int)(x2+(x3-x2)*perc),endY=(int)(y2+(y3-y2)*perc);
					Node startNode=findClosest(startX, startY);
					Node endNode=findClosest(endX, endY);
					if(startNode==start&&endNode==end) continue;
					float dist2=n1.pos.dist(startNode.pos)+startNode.pos.dist(endNode.pos)+endNode.pos.dist(n3.pos);
					
					if(dist1<=dist2) continue;
					
					debugPairs.clear();
					debugPairs.add(n1);
					debugPairs.add(n3);
					debugPairs.add(startNode);
					debugPairs.add(endNode);
					markDirty();
					
					if(!rayTrace(startNode, endNode)) break;
					start=startNode;
					end=endNode;
				}
				
				if(start==n2||end==n2) continue;
				path.remove(i);
				n2.bad=true;
				change=true;
				if(end!=n3){
					path.add(i, end);
					end.used=true;
					end.bad=false;
				}
				if(start!=n1){
					path.add(i, start);
					start.used=true;
					start.bad=false;
				}
				markDirty();
				i--;
			}
			debugPairs.clear();
			markDirty();
			shortenPath1();
		}while(change);
		debugPairs.clear();
		
	}
	
	public Node findClosest(int x, int y){
		return findClosest(new Vec2i(x, y));
	}
	
//	private Node findClosest(Vec2i pos){
//		return nodes.stream().min((a, b)->Float.compare(pos.dist(a.pos), pos.dist(b.pos))).orElse(null);
//	}
	
	private Node findClosest(Vec2i pos){
		Node exact=getNodeAt(pos.getX(), pos.getY());
		if(exact!=null)return exact;
		
		List<Node> found=new ArrayList<>();
		Consumer<Node> add=n->{
			if(n!=null)found.add(n);
		};
		
		int rad=1;
		while(found.isEmpty()&&rad<nodeMap.length&&rad<nodeMap[0].length){
			scanRing(add, pos, rad);
			rad++;
		}
		
		if(found.isEmpty())return null;
		return found.stream().min((a, b)->Float.compare(pos.dist(a.pos), pos.dist(b.pos))).get();
	}
	
	private void scanRing(Consumer<Node> add,Vec2i pos,int rad){
		
		for(int x=-rad;x<=rad;x++){
			add.accept(getNodeAt(x+pos.getX(), pos.getY()+rad));
			add.accept(getNodeAt(x+pos.getX(), pos.getY()-rad));
		}
		rad--;
		for(int y=-rad;y<=rad;y++){
			add.accept(getNodeAt(pos.getX()+rad, pos.getY()+y));
			add.accept(getNodeAt(pos.getX()-rad, pos.getY()+y));
		}
		
	}
	private Node getNodeAt(int x,int y){
		if(x<0||y<0||x>=nodeMap.length||y>=nodeMap[0].length)return null;
		return nodeMap[x][y];
	}
	
	private boolean rayTrace(Node n1, Node n2){
		float x1=n1.pos.getX(),y1=n1.pos.getY(),x2=n2.pos.getX(),y2=n2.pos.getY();
		
		float iter=n1.pos.dist(n2.pos)*10,angle=(float)Math.atan2(x2-x1, y2-y1);
		Side s=Arrays.stream(Side.values()).min((a, b)->Float.compare(Math.abs(a.angle-angle), Math.abs(b.angle-angle))).get();
		int lastX=-1,lastY=-1;
		
		for(int i=0, j=(int)iter;i<j;i++){
			float prog=i/iter;
			int x3=(int)(x1+(x2-x1)*prog);
			int y3=(int)(y1+(y2-y1)*prog);
			
			if(lastX==x3&&lastY==y3) continue;
			lastX=x3;
			lastY=y3;
			
			if(!get(x3, y3)) return false;
			if(i<j+1&&!get(x3+s.x, y3+s.y)) return false;
		}
		
		return true;
	}
	
	private boolean runPathfind(){
		path.push(curentPoint);
		curentPoint.used=true;
		
		Node sid=curentPoint.connectedNodes.stream().filter(s->s!=null&&!s.used&&!s.bad).min((a, b)->Float.compare(a.pos.dist(end.pos), b.pos.dist(end.pos))).orElse(null);
		
		if(sid!=null){
			curentPoint=sid;
			Main.slowMo();
		}else{
			path.pop().bad=true;
			path.pop().bad=true;
			if(path.isEmpty()){
				path.forEach(p->p.bad=true);
				System.out.println("booo");
				markDirty();
				return false;
			}
			curentPoint=path.peek();
		}
		markDirty();
		
		return true;
	}
	
}
