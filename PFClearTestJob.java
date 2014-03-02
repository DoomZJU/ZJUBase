package scale.agent.job.pf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import scale.agent.command.ClearXYCommand;
import scale.agent.command.MoveCommand;
import scale.agent.command.MoveXYCommand;
import scale.agent.command.ScaleCommand;
import scale.agent.job.ScaleJob;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleToolURN;
import scale.tool.PF.BlockadeChooseTool;
import scale.tool.PF.ClearBlockadeTool;
import scale.tool.PF.BlockadeChooseTool.Level;
import scale.tool.history.HistoryTool;
import scale.tool.map.area.AccessibleTool;
import scale.tool.map.path.Path;
import scale.tool.map.path.SearchPathTool;
import scale.tool.stuck.StuckTool;
import scale.utils.geo.ScaleGeo;
import scale.agent.command.*;

public class PFClearTestJob extends ScaleJob<Human> {
	private static final Log log = LogFactory.getLog(PFClearTestJob.class);
	
	private enum JobState {
		Free,move,clean;
	}
	
	private JobState state;
	private int type = 0;
	private SearchPathTool spTool;
	private ClearBlockadeTool cbTool;
	private AccessibleTool acTool;
	private StuckTool stkTool;
	private BlockadeChooseTool bcTool;
	private HistoryTool hTool;
	private int num = 0;
	private boolean stuckCondition = false;
	private EntityID currentNode;
	private EntityID curretnID = null;
	private EntityID nextRoad = null;
	private List<Point2D> pointList = new ArrayList<Point2D>();
	private List<EntityID> realList = new ArrayList<EntityID>();
	private List<EntityID> pointRoad = new ArrayList<EntityID>();
	private List<EntityID> blkList = new ArrayList<EntityID>();
	private List<EntityID> myPath = new ArrayList<EntityID>();
	private Map<EntityID,List<Boolean>> myPathMap = new LinkedHashMap<EntityID,List<Boolean>>(); 
	private List<Point2D> pointToCLean = new ArrayList<Point2D>();
	
	@Override
	protected boolean initialize() {
		spTool = (SearchPathTool) toolKit.getTool(ScaleToolURN.SearchPathTool);
		cbTool = (ClearBlockadeTool) toolKit.getTool(ScaleToolURN.ClearBlockadeTool);
		acTool = (AccessibleTool) toolKit.getTool(ScaleToolURN.AccessibleTool);
		stkTool = (StuckTool) toolKit.getTool(ScaleToolURN.StuckTool);
		bcTool = (BlockadeChooseTool) toolKit.getTool(ScaleToolURN.BlockadeChooseTool);
		hTool = (HistoryTool) toolKit.getTool(ScaleToolURN.HistoryTool);
		
		state = JobState.Free;
		return true;
	}

	@Override
	public ScaleCommand execute(int time, ChangeSet changes,
			Collection<ScaleMessage> messages, ScaleCommand other) {
		if(stkTool.isStuck()){
			log.error("被卡住了");
			state = JobState.clean;
		}
		switch(state){
		case Free:{
			log.error("状态"+state);
			log.error("清障半径"+cbTool.getClearRad());
			EntityID startID = new EntityID(280);
			EntityID endID = new EntityID(281);
			EntityID lastRoad = null;
			myPath = spTool.getPath(startID, endID, true).getIDList();
			for(EntityID roadid:myPath){
				log.debug("当前路"+roadid);
				log.debug("上一次的路"+lastRoad);
				List<Boolean> content = new ArrayList<Boolean>();
				if(me.getPosition().equals(roadid)){
					content.add(false);
					content.add(true);
					myPathMap.put(roadid, content);
					lastRoad = roadid;
					continue;
				}
				content.add(acTool.isBlocked(lastRoad, roadid));
				content.add(false);
				myPathMap.put(roadid, content);
				lastRoad = roadid;
				log.debug("迭代过程中的路径"+myPathMap);
			}
			log.error("最终生成的路径"+myPathMap);
			for(Entry<EntityID,List<Boolean>> ent:myPathMap.entrySet()){
				if(ent.getValue().get(0)){
					num++;
				}
			}
			curretnID = myPath.get(myPath.size()-1);
			state = JobState.move;
		}
		case move:{
			log.error("状态"+state);
			realList.clear();
			currentNode = me.getPosition();
			int currentIndex = myPath.indexOf(currentNode);
			if(!currentNode.equals(curretnID)){
				for(int i=0;i<=currentIndex;i++){
					myPathMap.get(myPath.get(i)).add(1, true);
				}
				for(EntityID currentID:myPath){
					if(!myPathMap.get(currentID).get(1)){
						realList.add(currentID);
					}
				}
				break;
			}
			else{
				log.error("我已走到");
				return null;
			}
		}
		case clean:{
			log.error("走不动了！！清障！");
			Road myRoad = (Road)model.getEntity(me.getPosition());
			Road lastRoad = (Road)model.getEntity(myPath.get(myPath.indexOf(me.getPosition())-1));
			Point2D myRoadC = new Point2D(myRoad.getX(),myRoad.getY());
			Point2D edgePoint = ScaleGeo.getPointOnEdge(ScaleGeo.getPublicEdge(myRoad, lastRoad));
			if(stkTool.isInBlockade(myRoadC, myRoad)){
				pointToCLean.clear();
				log.error("中点被覆盖了");
				if(cbTool.isBlockadeInSurf(me.getPosition(), edgePoint, myRoadC)){
					Blockade blk = (Blockade)model.getEntity((cbTool.isInBlockade(myRoadC, myRoad)));
					int[] allApexes = blk.getApexes();
					List<Point2D> blkVertex = new ArrayList<Point2D>();
		            int count = allApexes.length / 2;
		            for (int i = 0; i < count; ++i) {
		            	blkVertex.add(new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]));
		            }
		            double length = Double.MIN_VALUE;
		            Point2D bestP = null;
		            if(isAnyOneBetween(blkVertex,new Point2D(me.getX(),me.getY()),edgePoint)){
		            	log.error("我和起点间有路障");
			            for(Point2D point:blkVertex){
			            	if((point.getX()<me.getX()) && (point.getX()>edgePoint.getX()) || ((point.getX()>me.getX()) && (point.getX()<edgePoint.getX()))){
			            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
			            			bestP = point;
				            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
				            	}
			            	}
			            }
			            log.error("最佳点坐标"+bestP);
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(bestP, "最佳点", Color.black);			            
							}
			            bestP = cbTool.inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(cbTool.getGdP(), "过度点", Color.green);
			            }
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(bestP, "计算出的点", Color.red);
			            }
		            }
		            else{
		            	log.error("我和起点间无路障");
		            	for(Point2D point:blkVertex){
			            	if((point.getX()<myRoadC.getX()) && (point.getX()>edgePoint.getX()) || ((point.getX()>myRoadC.getX()) && (point.getX()<edgePoint.getX()))){
			            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
			            			bestP = point;
				            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
				            	}
			            	}
			            }
		            	bestP = cbTool.inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
		            }
		            log.error("最佳位置"+bestP);
		            pointToCLean.add(bestP);
		            log.error("生成的清障点"+pointToCLean);
				}
			}
			else if(acTool.isHalfBlocked(myRoad, lastRoad)){
				pointToCLean.clear();
				log.error("中点未被覆盖");
				Point2D myPoint = new Point2D(me.getX(),me.getY());
				if(ScaleGeo.getDistance(myPoint, myRoadC)<cbTool.getMaxDis()){
					pointToCLean.add(cbTool.fixP(myPoint, myRoadC));
				}
				else{
					pointToCLean.add(cbTool.relongP(myPoint, myRoadC));
				}
			}
			else if(!stkTool.isInBlockade(myRoadC, myRoad) && ((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX())))){
				log.error("先到中点再清障");
				state = JobState.move;
				break;
			}
			else{
				pointToCLean.clear();
				log.error("平行清障");
				pointToCLean.add(cbTool.parrlP(new Point2D(me.getX(),me.getY()), edgePoint,myRoadC));
			}
		}
		}
		if((state == JobState.move) && (!realList.isEmpty())){
			return new MoveCommand(time,realList);
		}
		if((state == JobState.clean) && (!pointToCLean.isEmpty())){
			return new ClearXYCommand(time,(int)pointToCLean.get(0).getX(),(int)pointToCLean.get(0).getY());
		}
		else{
			return null;
		}
	}
//			log.error("状态"+state);
//			EntityID startID = new EntityID(280);
//			EntityID endID = new EntityID(281);
//			EntityID lastRoad = null;
//			myPath = spTool.getPath(startID, endID, true).getIDList();
//			for(EntityID roadid:myPath){
//				log.debug("当前路"+roadid);
//				log.debug("上一次的路"+lastRoad);
//				List<Boolean> content = new ArrayList<Boolean>();
//				if(me.getPosition().equals(roadid)){
//					content.add(false);
//					content.add(true);
//					myPathMap.put(roadid, content);
//					lastRoad = roadid;
//					continue;
//				}
//				content.add(acTool.isBlocked(lastRoad, roadid));
//				content.add(false);
//				myPathMap.put(roadid, content);
//				lastRoad = roadid;
//				log.debug("迭代过程中的路径"+myPathMap);
//			}
//			log.error("最终生成的路径"+myPathMap);
//			for(Entry<EntityID,List<Boolean>> ent:myPathMap.entrySet()){
//				if(ent.getValue().get(0)){
//					num++;
//				}
//			}
//			curretnID = myPath.get(myPath.size()-1);
//			state = JobState.move;
//		case move:{
//			
//		}
//			log.error("状态"+state);
//			currentNode = me.getPosition();
//			int currentIndex = myPath.indexOf(currentNode);
//			if(!currentNode.equals(curretnID)){
//				for(int i=0;i<=currentIndex;i++){
//					myPathMap.get(myPath.get(i)).add(1, true);
//				}
//				if(num == 0){
//					type = 0;
//					realList.clear();
//					for(EntityID rid:myPath){
//						if(!myPathMap.get(rid).get(1)){
//							realList.add(rid);
//						}
//					}
//					break;
//				}
//				else{
//					type = 1;
//					pointRoad.clear();
//					for(EntityID rid:myPath){
//						if(myPathMap.get(rid).get(0)){
//							pointRoad.add(rid);
//							break;
//						}
//						else if(!myPathMap.get(rid).get(1)){
//							pointRoad.add(rid);
//						}
//					}
//					break;
//				}
//			}
//			else{
//				log.error("我以走到！");
//				return null;
//			}
//		}
//				if(!(realList.isEmpty()) && (me.getPosition()
//											.equals(realList.get(realList.size()-1)))){
//					if(){
//						log.error("开始清理！");
//						myPathMap.get(myPath.get(currentIndex+1)).add(1, true);
//						nextRoad = myPath.get(currentIndex+1);
//						state = JobState.clean;
//					}
//				}
//				realList.clear();
//				for(Entry<EntityID,List<Boolean>> ent:myPathMap.entrySet()){
//					if(num == 0){
//						for(int i=currentIndex+1;i<myPath.size();i++){
//							realList.add(myPath.get(i));
//						}
//						break;
//					}
//					else if((ent.getValue().get(0)) && !(ent.getValue().get(1))){
//						int index = myPath.indexOf(ent.getKey());
//						for(int i=currentIndex;i<index;i++){
//							realList.add(myPath.get(i));
//						}
//						num--;
//						break;
//					}
//				}
//				log.error("真实路径"+realList);
			
//		case clean:{
//			Road myRoad = (Road)model.getEntity(me.getPosition());
//			Road lastRoad = (Road)model.getEntity(myPath.get(myPath.indexOf(myRoad)-1));
//			Point2D myRoadC = new Point2D(myRoad.getX(),myRoad.getY());
//			if(stkTool.isInBlockade(myRoadC, myRoad)){
//				pointToCLean.clear();
//				log.error("中点被覆盖了");
//				Point2D edgePoint = ScaleGeo.getPointOnEdge(ScaleGeo.getPublicEdge(myRoad, lastRoad));
//				if(cbTool.isBlockadeInSurf(me.getPosition(), edgePoint, myRoadC)){
//					Blockade blk = new Blockade(cbTool.isInBlockade(myRoadC, myRoad));
//					int[] allApexes = blk.getApexes();
//		            int count = allApexes.length / 2;
//		            double length = Double.MAX_VALUE;
//		            Point2D bestP = null;
//		            for (int i = 0; i < count; ++i) {
//		            Point2D blkP = new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]);
//		            	if((blkP.getX()<myRoadC.getX()) && (blkP.getX()>edgePoint.getX()) || ((blkP.getX()<myRoadC.getX()) && (blkP.getX()>edgePoint.getX()))){
//		            		if(ScaleGeo.getDistance(blkP, new Point2D(me.getX(),me.getY()))<length){
//		            			bestP = blkP;
//		            			length = ScaleGeo.getDistance(blkP, new Point2D(me.getX(),me.getY()));
//		            		}
//		            	}
//		            }
//		            pointToCLean.add(bestP);
//				}
//			}
//			else if(acTool.isHalfBlocked(myRoad, lastRoad)){
//				pointToCLean.clear();
//				log.error("中点未被覆盖");
//				Point2D myPoint = new Point2D(me.getX(),me.getY());
//				if(ScaleGeo.getDistance(myPoint, myRoadC)<cbTool.getMaxDis()){
//					pointToCLean.add(cbTool.fixP(myPoint, myRoadC));
//				}
//				else{
//					pointToCLean.add(cbTool.relongP(myPoint, myRoadC));
//				}
//			}
//			else{
//				log.error("先到中点再清障");
//				state = JobState.move;
//				break;
//			}
//		}
		
//		}
//		if((state == JobState.move) && (!realList.isEmpty()) && (type == 0) && (!stkTool.isStuck())){
//			EntityID endID = new EntityID(281);
//			Road endR = (Road)model.getEntity(endID);
//			return this.arriveAtTarget(time, changes, endR);
//		}
//		else if((state == JobState.move) && (!pointRoad.isEmpty()) && (type == 1) && (!stkTool.isStuck())){
//			EntityID endID = new EntityID(281);
//			Road endR = (Road)model.getEntity(endID);
//			return this.arriveAtTarget(time, changes, endR);
//		}
//		else if((state == JobState.move) && (stkTool.isStuck())){
//			state = JobState.clean;
//			return new ClearXYCommand(time,(int)pointToCLean.get(0).getX(),(int)pointToCLean.get(0).getY());
//		}
//		return null;
//	}
//		else if((state == JobState.doclean)&&(!pointToCLean.isEmpty())){
//			Point2D myPoint = new Point2D(me.getX(),me.getY());
//			int index = clearList.indexOf(pointToCLean.get(0));
//			Road nextroad = (Road)model.getEntity(nextRoad);
//			if(clearList.get(index+1).equals(clearList.get(clearList.size()-1))){
//				if((!stkTool.isStuck())){
//					if((ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),clearList.get(clearList.size()-1))>cbTool.getMaxDis())&&(stkTool.isInBlockade(clearList.get(index+1), nextroad))){
//						log.error("还没走到清障范围内");
//						return new MoveCommand(time,realList);
//					}
//					else if(stkTool.isInBlockade(clearList.get(index+1), nextroad)){
//						log.error("把中点清理出来");
//						Point2D fixp = cbTool.fixP(new Point2D(me.getX(),me.getY()), new Point2D(nextroad.getX(),nextroad.getY()));
//						return new ClearXYCommand(time,(int)fixp.getX(),(int)fixp.getY());
//					}
//					else if(!myPoint.equals(clearList.get(index+1))){
//						log.error("走到中点");
////						return new ClearXYCommand(time,(int)clearList.get(index).getX(),(int)clearList.get(index).getY());
////						return new MoveXYCommand(time,testList,clearList.get(index+1)); 
//						return new MoveCommand(time,realList);
//					}
//					else{
//						log.error("清通隧道");
//						return new ClearXYCommand(time,(int)clearList.get(index).getX(),(int)clearList.get(index).getY());
////						return new ClearXYCommand(time,(int)nextroad.getX(),(int)nextroad.getY());
//					}
//				}
//				else if(stkTool.isInBlockade(clearList.get(index+1), nextroad)){
//					Point2D fixp = cbTool.fixP(new Point2D(me.getX(),me.getY()), new Point2D(nextroad.getX(),nextroad.getY()));
//					return new ClearXYCommand(time,(int)fixp.getX(),(int)fixp.getY());
//				}
//				else if(!myPoint.equals(clearList.get(index))){
//					return new MoveCommand(time,realList);
//				}
//				else{
//					return new ClearXYCommand(time,(int)clearList.get(index).getX(),(int)clearList.get(index).getY());
//				}
//			}
//			else{
//				if(!myPoint.equals(clearList.get(index))){
//					return new MoveXYCommand(time,testList,clearList.get(index)); 
//				}
//				else{
//					return new ClearXYCommand(time,(int)clearList.get(index+1).getX(),(int)clearList.get(index+1).getY());
//				}
//			}
//		}

	private boolean isAnyOneBetween(List<Point2D> pList,Point2D p1,Point2D p2){
		for(Point2D p:pList){
			if(!((p.getX()<p1.getX()) && (p.getX()>p2.getX()) || ((p.getX()>p1.getX()) && (p.getX()<p2.getX())))){
				continue;
			}
			else{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void overridden() {
		// TODO Auto-generated method stub
		
	}

}
