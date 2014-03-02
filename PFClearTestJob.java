package scale.agent.job.pf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import scale.agent.command.ClearXYCommand;
import scale.agent.command.MoveCommand;
import scale.agent.command.ScaleCommand;
import scale.agent.job.ScaleJob;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleToolURN;
import scale.tool.PF.ClearBlockadeTool;
import scale.tool.map.area.AccessibleTool;
import scale.tool.map.path.SearchPathTool;
import scale.tool.stuck.StuckTool;
import scale.utils.geo.ScaleGeo;

public class PFClearTestJob extends ScaleJob<Human> {
	private static final Log log = LogFactory.getLog(PFClearTestJob.class);
	
	private enum JobState {
		Free,move,clean;
	}
	
	private JobState state;
	private StuckTool stkTool;
	private AccessibleTool acTool;
	private SearchPathTool spTool;
	private ClearBlockadeTool cbTool;
	private boolean isCenterClean = false;
	private boolean isStillWorking = false;
	private EntityID currentNode;
	private EntityID curretnID = null;
	private List<EntityID> realList = new ArrayList<EntityID>();
	private List<EntityID> myPath = new ArrayList<EntityID>();
	private List<Point2D> pointToCLean = new ArrayList<Point2D>();
	private Map<EntityID,List<Boolean>> myPathMap = new LinkedHashMap<EntityID,List<Boolean>>(); 
	
	@Override
	protected boolean initialize() {
		spTool = (SearchPathTool) toolKit.getTool(ScaleToolURN.SearchPathTool);
		cbTool = (ClearBlockadeTool) toolKit.getTool(ScaleToolURN.ClearBlockadeTool);
		acTool = (AccessibleTool) toolKit.getTool(ScaleToolURN.AccessibleTool);
		stkTool = (StuckTool) toolKit.getTool(ScaleToolURN.StuckTool);
		
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
		else if(isStillWorking && !cbTool.isBlockadeInSurf(me.getPosition(),myPath, new Point2D(me.getX(),me.getY()), pointToCLean.get(0))){
			log.error("平行清障结束");
			isStillWorking = false;
			 if (agent.getAgentViewer() != null) {
					agent.getAgentViewer().getMiscLayer()
							.addPolygon(cbTool.getClearArea(new Point2D(me.getX(),me.getY()), pointToCLean.get(0)), Color.black);			            
			}
			if((state == JobState.clean) && !isStillWorking){//&& isCenterClean 
				log.error("平行清障后的继续行动");
				state = JobState.move;
			}
		}
		else if((state == JobState.clean)&& isCenterClean && !isStillWorking){
			log.error("中点清了");
			state = JobState.move;
		}
		switch(state){
		case Free:{
			log.error("状态"+state);
			log.error("清障半径"+cbTool.getClearRad());
			EntityID startID = new EntityID(860);
			EntityID endID = new EntityID(991);
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
			curretnID = myPath.get(myPath.size()-1);
			state = JobState.move;
		}
		case move:{
			log.error("状态"+state);
			realList.clear();
			currentNode = me.getPosition();
			int currentIndex = myPath.indexOf(currentNode);
			if(!currentNode.equals(curretnID)){
				if(acTool.isBlocked(currentNode, myPath.get(currentIndex+1))){
					myPathMap.get(myPath.get(currentIndex)).set(0, true);
				}
				for(int i=0;i<=currentIndex;i++){
					int nowIndex = myPath.indexOf(me.getPosition());
					if(i!=0){
						if((!myPathMap.get(myPath.get(i)).get(0))&&(!acTool.isHalfBlocked(myPath.get(i), myPath.get(i-1))) || (i<nowIndex)){
							myPathMap.get(myPath.get(i)).set(1, true);
						}
					}
				}
				log.error("最终生成的路径"+myPathMap);
				for(EntityID currentID:myPath){
					if(!myPathMap.get(currentID).get(1)){
						realList.add(currentID);
					}
				}
				log.error("最终路径"+realList);
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
			Road nextroad = (Road)model.getEntity(myPath.get(myPath.indexOf(me.getPosition())+1));
			Point2D myRoadC = new Point2D(myRoad.getX(),myRoad.getY());
			Point2D edgePoint = ScaleGeo.getPointOnEdge(ScaleGeo.getPublicEdge(myRoad, lastRoad));
			Point2D edgePoint2 = ScaleGeo.getPointOnEdge(ScaleGeo.getPublicEdge(myRoad, nextroad));
//			log.error("我的路的宽度"+cbTool.getWidRoad(myRoad,myPath));
			pointToCLean = cbTool.getClearPoint(myRoad, myPath, edgePoint, edgePoint2, myRoadC, isStillWorking, isCenterClean);
			isStillWorking = cbTool.getWorkingFlag();
			isCenterClean = cbTool.getCenterFlag();
//			if(!(cbTool.getWidRoad(myRoad,myPath)<(cbTool.getClearRad()*2))){//(myRoad.getEdges().size()!=4) ||
//			log.error("钟摆式清障");
//				if(stkTool.isInBlockade(myRoadC, myRoad) && ((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX())))){
//					isCenterClean = false;
//					pointToCLean.clear();
//					log.error("中点被覆盖了,我在前半段");
//					if(cbTool.isBlockadeInSurf(me.getPosition(),myPath, edgePoint, myRoadC)){
//						Blockade blk = (Blockade)model.getEntity((cbTool.isInBlockade(myRoadC, myRoad)));
//						log.error("中点覆盖的路障"+cbTool.isInBlockade(myRoadC, myRoad));
//						int[] allApexes = blk.getApexes();
//						List<Point2D> blkVertex = new ArrayList<Point2D>();
//			            int count = allApexes.length / 2;
//			            for (int i = 0; i < count; ++i) {
//			            	blkVertex.add(new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]));
//			            }
//			            double length = Double.MIN_VALUE;
//			            Point2D bestP = null;
//			            if(cbTool.isAnyOneBetween(blkVertex,new Point2D(me.getX(),me.getY()),edgePoint)){
//			            	log.error("我和起点间有路障");
//				            for(Point2D point:blkVertex){
//				            	if((point.getX()<me.getX()) && (point.getX()>edgePoint.getX()) || ((point.getX()>me.getX()) && (point.getX()<edgePoint.getX()))){
//				            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
//				            			bestP = point;
//					            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
//					            	}
//				            	}
//				            }
//				            log.error("最佳点坐标"+bestP);
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(bestP, "最佳点", Color.black);			            
//								}
//				            bestP = cbTool.inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(cbTool.getGdP(), "过度点", Color.green);
//				            }
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(bestP, "计算出的点", Color.red);
//				            }
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(cbTool.getPP(), "pp", Color.red);
//				            }
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(cbTool.getEP(), "ep", Color.red);
//				            }
//			            }
//			            else{
//			            	log.error("我和起点间无路障");
//			            	for(Point2D point:blkVertex){
//				            	if((point.getX()<myRoadC.getX()) && (point.getX()>edgePoint.getX()) || ((point.getX()>myRoadC.getX()) && (point.getX()<edgePoint.getX()))){
//				            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
//				            			bestP = point;
//					            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
//					            	}
//				            	}
//				            }
//			            	log.error("最佳点坐标"+bestP);
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(bestP, "最佳点", Color.black);			            
//							}
//					        if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addLine(new Point2D(me.getX(),me.getY()), bestP, Color.blue);			            
//					        }
//			            	bestP = cbTool.inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
//			            	 if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addText(cbTool.getGdP(), "过度点", Color.green);
//					         }
//					         if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//											.addText(bestP, "计算出的点", Color.red);
//					         }
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addText(cbTool.getPP(), "pp", Color.red);
//					            }
//					            if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addText(cbTool.getEP(), "ep", Color.red);
//					            }
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addLine(cbTool.getGdP(), bestP, Color.black);			            
//					         }
//					         Vector2D v = new Vector2D((bestP.getX()-cbTool.getGdP().getX()),(bestP.getY()-cbTool.getGdP().getY()));
//					         double l = ScaleGeo.getDistance(bestP, cbTool.getGdP());
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addLine(new Point2D(me.getX(),me.getY()),new Point2D((me.getX()+l*v.normalised().getX()),(me.getY()+l*v.normalised().getY())), Color.black);			            
//					         }
//			            }
//			            log.error("最佳位置"+bestP);
//			            pointToCLean.add(bestP);
//					}
//				}
//				else if(!stkTool.isInBlockade(myRoadC, myRoad) && ((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX()))) && !isCenterClean){
//					log.error("中点清理完毕，前半段");
//					isCenterClean = true;
//				}
//				else if(stkTool.isInBlockade(myRoadC, myRoad) && !((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX())))){
//					isCenterClean = false;
//					pointToCLean.clear();
//					log.error("中点被覆盖了，我在后半段");
//					if(cbTool.isBlockadeInSurf(me.getPosition(),myPath, edgePoint2, myRoadC)){
//						Blockade blk = (Blockade)model.getEntity((cbTool.isInBlockade(myRoadC, myRoad)));
//						int[] allApexes = blk.getApexes();
//						List<Point2D> blkVertex = new ArrayList<Point2D>();
//			            int count = allApexes.length / 2;
//			            for (int i = 0; i < count; ++i) {
//			            	blkVertex.add(new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]));
//			            }
//			            double length = Double.MIN_VALUE;
//			            Point2D bestP = null;
//			            if(cbTool.isAnyOneBetween(blkVertex,new Point2D(me.getX(),me.getY()),myRoadC)){
//			            	log.error("我和终点间有路障");
//				            for(Point2D point:blkVertex){
//				            	if((point.getX()<me.getX()) && (point.getX()>myRoadC.getX()) || ((point.getX()>me.getX()) && (point.getX()<myRoadC.getX()))){
//				            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
//				            			bestP = point;
//					            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
//					            	}
//				            	}
//				            }
//				            log.error("最佳点坐标"+bestP);
//				            if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//										.addText(bestP, "最佳点", Color.black);			            
//							}
//			            	bestP = cbTool.inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
//			            	 if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addText(cbTool.getGdP(), "过度点", Color.green);
//					         }
//					         if (agent.getAgentViewer() != null) {
//								agent.getAgentViewer().getMiscLayer()
//											.addText(bestP, "计算出的点", Color.red);
//					         }
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addText(cbTool.getPP(), "pp", Color.red);
//					            }
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addText(cbTool.getEP(), "ep", Color.red);
//					         }
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addLine(cbTool.getGdP(), bestP, Color.black);			            
//					         }
//					         Vector2D v = new Vector2D((bestP.getX()-cbTool.getGdP().getX()),(bestP.getY()-cbTool.getGdP().getY()));
//					         double l = ScaleGeo.getDistance(bestP, cbTool.getGdP());
//					         if (agent.getAgentViewer() != null) {
//									agent.getAgentViewer().getMiscLayer()
//											.addLine(new Point2D(me.getX(),me.getY()),new Point2D((me.getX()+l*v.normalised().getX()),(me.getY()+l*v.normalised().getY())), Color.black);			            
//					         }
//			            }
//			            else{
//			            	log.error("我和起点间无路障");
//			            	for(Point2D point:blkVertex){
//				            	if((point.getX()<myRoadC.getX()) && (point.getX()>edgePoint2.getX()) || ((point.getX()>myRoadC.getX()) && (point.getX()<edgePoint2.getX()))){
//				            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
//				            			bestP = point;
//					            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
//					            	}
//				            	}
//				            }
//			            	bestP = cbTool.inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
//			            }
//			            log.error("最佳位置"+bestP);
//			            pointToCLean.add(bestP);
//					}
//				}
//				else if(!stkTool.isInBlockade(myRoadC, myRoad) && !((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX()))) && !isCenterClean){
//					log.error("中点清理完毕，后半段");
//					isCenterClean = true;
//				}
//				if((!stkTool.isInBlockade(myRoadC, myRoad)) && !((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX()))) && (cbTool.isBlockadeInSurf(me.getPosition(),myPath, edgePoint2, myRoadC)) && !isStillWorking){
//					pointToCLean.clear();
//					log.error("平行清障");
//					pointToCLean.add(cbTool.parrlP(new Point2D(me.getX(),me.getY()),myRoadC,edgePoint2));
//					if(cbTool.isBlockadeInSurf(me.getPosition(),myPath, new Point2D(me.getX(),me.getY()), pointToCLean.get(0))){
//						log.error("还没清完");
//						isStillWorking = true;
//					}
//				}
//			}
//			else{
//				pointToCLean.clear();
//				log.error("直接平行清障");
//				pointToCLean.add(cbTool.parrlP(new Point2D(me.getX(),me.getY()),myRoadC,edgePoint2));
//				if(cbTool.isBlockadeInSurf(me.getPosition(),myPath, new Point2D(me.getX(),me.getY()), pointToCLean.get(0))){
//					log.error("还没清完");
//					isStillWorking = true;
//				}
//			}
//          log.error("生成的清障点"+pointToCLean);
		}
		}
		if((state == JobState.move) && (!realList.isEmpty())){
			return new MoveCommand(time,realList);
		}
		if((state == JobState.clean) && (!pointToCLean.isEmpty())){
			if (agent.getAgentViewer() != null) {
				agent.getAgentViewer().getMiscLayer()
						.addPolygon(cbTool.getClearArea(new Point2D(me.getX(),me.getY()), pointToCLean.get(0)), Color.red);			            
	        }
			return new ClearXYCommand(time,(int)pointToCLean.get(0).getX(),(int)pointToCLean.get(0).getY());
		}
		else{
			return null;
		}
	}
	
	@Override
	public void overridden() {
		// TODO Auto-generated method stub
		
	}

}
