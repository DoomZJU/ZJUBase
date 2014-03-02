package scale.tool.PF;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import scale.Constants;
import scale.agent.ScaleAgent;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleTool;
import scale.tool.ScaleToolURN;
import scale.tool.stuck.StuckTool;
import scale.utils.geo.ScaleGeo;

/**
 * PF清障工具
 * @author Doom
 *
 */

public class ClearBlockadeTool extends ScaleTool {
	private static final Log log = LogFactory.getLog(ClearBlockadeTool.class);
	private ScaleAgent<? extends Human> agent;
	private Human me;
	private StuckTool stkTool;
	private Point2D gdP = null;
	private Point2D pp = null;
	private Point2D ep = null;
	private int Max_dis;
	private int clearRad;
	private boolean workingFlag = false;
	private boolean centerFlag = false;
	List<Point2D> pointToCLean = new ArrayList<Point2D>();
	
	public ClearBlockadeTool() {
		super(ScaleToolURN.ClearBlockadeTool);
	}

	@Override
	protected boolean initialize() {
		agent = (ScaleAgent<? extends Human>) element;
		me = agent.getMe();
		stkTool = (StuckTool) toolKit.getTool(ScaleToolURN.StuckTool);
		Max_dis = config.getIntValue(Constants.KEY_CLEAR_REPAIR_DISTANCE,-1);
		clearRad = config.getIntValue(Constants.KEY_CLEAR_REPAIR_RAD, -1);
		
		return true;
	}
	
	@Override
	public boolean update(int time, ChangeSet changes,
			Collection<ScaleMessage> messages) {
		
		return true;
	}
	
	public List<Point2D> getClearPoint(Road myRoad,List<EntityID> myPath,Point2D edgePoint,Point2D edgePoint2,Point2D myRoadC,boolean isStillWorking,boolean isCenterClean){
		if(!(getWidRoad(myRoad,myPath)<(getClearRad()*2))){//(myRoad.getEdges().size()!=4) ||
			log.error("钟摆式清障");
			if(stkTool.isInBlockade(myRoadC, myRoad) && ((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX())))){
				isCenterClean = false;
//				log.error("清空清障点1");
				pointToCLean.clear();
				log.error("中点被覆盖了,我在前半段");
				if(isBlockadeInSurf(me.getPosition(),myPath, edgePoint, myRoadC)){
					Blockade blk = (Blockade)model.getEntity((isInBlockade(myRoadC, myRoad)));
					log.error("中点覆盖的路障"+isInBlockade(myRoadC, myRoad));
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
			            bestP = inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(getGdP(), "过度点", Color.green);
			            }
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(bestP, "计算出的点", Color.red);
			            }
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(getPP(), "pp", Color.red);
			            }
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(getEP(), "ep", Color.red);
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
		            	log.error("最佳点坐标"+bestP);
			            if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addText(bestP, "最佳点", Color.black);			            
						}
				        if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
									.addLine(new Point2D(me.getX(),me.getY()), bestP, Color.blue);			            
				        }
		            	bestP = inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
		            	 if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addText(getGdP(), "过度点", Color.green);
				         }
				         if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
										.addText(bestP, "计算出的点", Color.red);
				         }
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addText(getPP(), "pp", Color.red);
				            }
				            if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addText(getEP(), "ep", Color.red);
				            }
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addLine(getGdP(), bestP, Color.black);			            
				         }
				         Vector2D v = new Vector2D((bestP.getX()-getGdP().getX()),(bestP.getY()-getGdP().getY()));
				         double l = ScaleGeo.getDistance(bestP, getGdP());
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addLine(new Point2D(me.getX(),me.getY()),new Point2D((me.getX()+l*v.normalised().getX()),(me.getY()+l*v.normalised().getY())), Color.black);			            
				         }
		            }
		            log.error("最佳位置"+bestP);
		            pointToCLean.add(bestP);
				}
			}
			else if(!stkTool.isInBlockade(myRoadC, myRoad) && ((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX()))) && !isCenterClean){
				log.error("中点清理完毕，前半段");
				isCenterClean = true;
			}
			else if(stkTool.isInBlockade(myRoadC, myRoad) && !((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX())))){
				isCenterClean = false;
//				log.error("清空清障点2");
				pointToCLean.clear();
				log.error("中点被覆盖了，我在后半段");
				if(isBlockadeInSurf(me.getPosition(),myPath, edgePoint2, myRoadC)){
					Blockade blk = (Blockade)model.getEntity((isInBlockade(myRoadC, myRoad)));
					int[] allApexes = blk.getApexes();
					List<Point2D> blkVertex = new ArrayList<Point2D>();
		            int count = allApexes.length / 2;
		            for (int i = 0; i < count; ++i) {
		            	blkVertex.add(new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]));
		            }
		            double length = Double.MIN_VALUE;
		            Point2D bestP = null;
		            if(isAnyOneBetween(blkVertex,new Point2D(me.getX(),me.getY()),myRoadC)){
		            	log.error("我和终点间有路障");
			            for(Point2D point:blkVertex){
			            	if((point.getX()<me.getX()) && (point.getX()>myRoadC.getX()) || ((point.getX()>me.getX()) && (point.getX()<myRoadC.getX()))){
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
		            	bestP = inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
		            	 if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addText(getGdP(), "过度点", Color.green);
				         }
				         if (agent.getAgentViewer() != null) {
							agent.getAgentViewer().getMiscLayer()
										.addText(bestP, "计算出的点", Color.red);
				         }
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addText(getPP(), "pp", Color.red);
				            }
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addText(getEP(), "ep", Color.red);
				         }
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addLine(getGdP(), bestP, Color.black);			            
				         }
				         Vector2D v = new Vector2D((bestP.getX()-getGdP().getX()),(bestP.getY()-getGdP().getY()));
				         double l = ScaleGeo.getDistance(bestP, getGdP());
				         if (agent.getAgentViewer() != null) {
								agent.getAgentViewer().getMiscLayer()
										.addLine(new Point2D(me.getX(),me.getY()),new Point2D((me.getX()+l*v.normalised().getX()),(me.getY()+l*v.normalised().getY())), Color.black);			            
				         }
		            }
		            else{
		            	log.error("我和起点间无路障");
		            	for(Point2D point:blkVertex){
			            	if((point.getX()<myRoadC.getX()) && (point.getX()>edgePoint2.getX()) || ((point.getX()>myRoadC.getX()) && (point.getX()<edgePoint2.getX()))){
			            		if(ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point)>length){
			            			bestP = point;
				            		length = ScaleGeo.getDistance(new Point2D(me.getX(),me.getY()),point);
				            	}
			            	}
			            }
		            	bestP = inLineP(new Point2D(me.getX(),me.getY()), bestP, blk);
		            }
		            log.error("最佳位置"+bestP);
		            pointToCLean.add(bestP);
				}
			}
			else if(!stkTool.isInBlockade(myRoadC, myRoad) && !((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX()))) && !isCenterClean){
				log.error("中点清理完毕，后半段");
				isCenterClean = true;
			}
			if((!stkTool.isInBlockade(myRoadC, myRoad)) && !((me.getX()<myRoadC.getX()) && (me.getX()>edgePoint.getX()) || ((me.getX()>myRoadC.getX()) && (me.getX()<edgePoint.getX()))) && (isBlockadeInSurf(me.getPosition(),myPath, edgePoint2, myRoadC)) && !isStillWorking){
//				log.error("清空清障点3");
				pointToCLean.clear();
				log.error("平行清障");
				pointToCLean.add(parrlP(new Point2D(me.getX(),me.getY()),myRoadC,edgePoint2));
				if(isBlockadeInSurf(me.getPosition(),myPath, new Point2D(me.getX(),me.getY()), pointToCLean.get(0))){
					log.error("还没清完");
					isStillWorking = true;
				}
			}
		}
		else{
//			log.error("清空清障点4");
			pointToCLean.clear();
			log.error("直接平行清障");
			pointToCLean.add(parrlP(new Point2D(me.getX(),me.getY()),myRoadC,edgePoint2));
			if(isBlockadeInSurf(me.getPosition(),myPath, new Point2D(me.getX(),me.getY()), pointToCLean.get(0))){
				log.error("还没清完");
				isStillWorking = true;
			}
		}
		workingFlag = isStillWorking;
		centerFlag = isCenterClean;
		log.error("生成的清障点"+pointToCLean);
		return pointToCLean;
	}
	
	public double getWidRoad(Road myroad,List<EntityID> pathList){
		Edge aimEdge = null;
		Edge baseEdge = null;
		int index = pathList.indexOf(myroad.getID());
//		log.error("index"+index);
		if(index!=0){
			aimEdge = myroad.getEdgeTo(pathList.get(index-1));
//			log.error("aimEdge"+aimEdge);
//			log.error("全部edge"+myroad.getEdges());
			for(Edge e:myroad.getEdges()){
				if((e.getStart().equals(aimEdge.getEnd()) || e.getStart().equals(aimEdge.getStart()) || e.getEnd().equals(aimEdge.getEnd()) || e.getEnd().equals(aimEdge.getStart())) && (!e.equals(aimEdge))){
					baseEdge = e;
					break;
				}
			}
//			log.error("baseEdge"+baseEdge);
			Vector2D v1 = new Vector2D(aimEdge.getEndX()-aimEdge.getStartX(),aimEdge.getEndY()-aimEdge.getStartY());
//			log.error("v1"+v1);
			Vector2D v2 = new Vector2D(baseEdge.getEndX()-baseEdge.getStartX(),baseEdge.getEndY()-baseEdge.getStartY());
//			log.error("v2"+v2);
			double touying = (Math.abs(v1.getX()*v2.getX()+v1.getY()*v2.getY()))/ScaleGeo.getDistance(baseEdge.getStart(),baseEdge.getEnd());
			double wid = Math.sqrt(Math.pow((ScaleGeo.getDistance(aimEdge.getStart(),aimEdge.getEnd())),2)-Math.pow(touying,2));
			return wid;
		}
		else{
			for(EntityID id:myroad.getNeighbours()){
				Area idArea = (Area)model.getEntity(id);
				if((idArea instanceof Road)&&(!myroad.getNeighbours().contains(id))){
					aimEdge = myroad.getEdgeTo(id);
					break;
				}
			}
			for(Edge e:myroad.getEdges()){
				if(e.getStart().equals(aimEdge.getEnd()) || e.getStart().equals(aimEdge.getStart()) || e.getEnd().equals(aimEdge.getEnd()) || e.getEnd().equals(aimEdge.getStart())){
					baseEdge = e;
					break;
				}
			}
			Vector2D v1 = new Vector2D(aimEdge.getEndX()-aimEdge.getStartX(),aimEdge.getEndY()-aimEdge.getStartY());
			Vector2D v2 = new Vector2D(baseEdge.getEndX()-baseEdge.getStartX(),baseEdge.getEndY()-baseEdge.getStartY());
			double touying = Math.abs(v1.getX()*v2.getX()+v1.getY()*v2.getY());
//			log.error("投影长度"+touying);
			double wid = Math.sqrt(Math.pow((ScaleGeo.getDistance(aimEdge.getStart(),aimEdge.getEnd())),2)-Math.pow(touying,2));
			return wid;
		}
	}
	
	public List<Point2D> getClearArea(Point2D myPoint,
			  Point2D targetPoint){
		Line2D line = new Line2D(myPoint,targetPoint);
		Line2D leftLine = ScaleGeo.getParallelLineLeft(line,clearRad);
		Line2D rightLine = ScaleGeo.getParallelLineRight(line, clearRad);
		Point2D p1 = leftLine.getOrigin();
		Point2D p2 = leftLine.getEndPoint();
		Point2D p3 = rightLine.getOrigin();
		Point2D p4 = rightLine.getEndPoint();
		List<Point2D> vertex2D = new ArrayList<Point2D>();
		vertex2D.add(p1);
		vertex2D.add(p2);
		vertex2D.add(p4);
		vertex2D.add(p3);
		return vertex2D;
	}
	
	public List<Point2D> getPolygon(Point2D myPoint,
				Point2D targetPoint){
		double length = ScaleGeo.getDistance(myPoint, targetPoint)-100;
		Vector2D v = new Vector2D(targetPoint.getX()-myPoint.getX(),targetPoint.getY()-myPoint.getY());	
		targetPoint = new Point2D(myPoint.getX()+length*v.normalised().getX(),myPoint.getY()+length*v.normalised().getY());
		Line2D line = new Line2D(myPoint,targetPoint);
		Line2D leftLine = ScaleGeo.getParallelLineLeft(line,clearRad/2);
		Line2D rightLine = ScaleGeo.getParallelLineRight(line, clearRad/2);
		Point2D p1 = leftLine.getOrigin();
		Point2D p2 = leftLine.getEndPoint();
		Point2D p3 = rightLine.getOrigin();
		Point2D p4 = rightLine.getEndPoint();
		List<Point2D> vertex2D = new ArrayList<Point2D>();
		vertex2D.add(p1);
		vertex2D.add(p2);
		vertex2D.add(p4);
		vertex2D.add(p3);
		return vertex2D;
	}
	
	public boolean getWorkingFlag(){
		return this.workingFlag;
	}
	
	public boolean getCenterFlag(){
		return this.centerFlag;
	}
	
	public double getMaxDis(){
		return this.Max_dis;
	}
	
	public double getClearRad(){
		return this.clearRad;
	}
	
	public Point2D getGdP(){
		return this.gdP;
	}
	
	public Point2D getPP(){
		return this.pp;
	}
	
	public Point2D getEP(){
		return this.ep;
	}
	
	public Point2D inLineP(Point2D srcP,Point2D targetP,Blockade blk){
		int flag = 0;
		double x0 = srcP.getX();
		double y0 = srcP.getY();
		double x1 = targetP.getX();
		double y1 = targetP.getY();
		double R = this.clearRad;
		double L = ScaleGeo.getDistance(srcP, targetP);
		double coss = 0.0;
		if(R <= L){
			coss = R/L;
		}
		else{
			Vector2D vv = new Vector2D(targetP.getX()-srcP.getX(),targetP.getY()-srcP.getY());
			gdP = new Point2D(srcP.getX()+Max_dis*vv.normalised().getX(),srcP.getY()+Max_dis*vv.normalised().getY());
			return gdP;
		}
		double sins = Math.sin(Math.acos(coss));
		double tans = Math.tan(Math.acos(coss));
		double l = R*sins;
		double AD = l*tans;
		double Xd = x1-AD*(x1-x0)/L;
		double Yd = y1-AD*(y1-y0)/L;
		double kab = (y1-y0)/(x1-x0);
		double kcd = -1/kab;
		double x = Xd + Math.sqrt(l*l/(1+kcd*kcd));
		double y = kcd*(x-Xd)+Yd;
		Vector2D v = new Vector2D(srcP.getX()-x,srcP.getY()-y);
		pp = new Point2D(srcP.getX()+R*v.normalised().getX(),srcP.getY()+R*v.normalised().getY());
		Vector2D v1 = new Vector2D(targetP.getX()-x,targetP.getY()-y);
		ep = new Point2D(pp.getX()+Max_dis*v1.normalised().getX(),pp.getY()+Max_dis*v1.normalised().getY());
		Line2D ll = new Line2D(pp,ep);
//		log.error("ep"+ep+" , "+"pp"+pp);
//		log.error("路障顶点 "+ScaleGeo.getVertexPoint(blk.getApexes()));
		for(Line2D l2:ScaleGeo.pointsToLines(ScaleGeo.getVertexPoint(blk.getApexes()),true)){
			if(ScaleGeo.getSegmentIntersectionPoint(ll,l2)!=null){
				flag = 1; 
				break;
			}
		}
		if(flag == 0){
			x = Xd - Math.sqrt(l*l/(1+kcd*kcd));
			y = kcd*(x-Xd)+Yd;
			v = new Vector2D(srcP.getX()-x,srcP.getY()-y);
			pp = new Point2D(srcP.getX()+R*v.normalised().getX(),srcP.getY()+R*v.normalised().getY());
			v1 = new Vector2D(targetP.getX()-x,targetP.getY()-y);
			ep = new Point2D(pp.getX()+Max_dis*v1.normalised().getX(),pp.getY()+Max_dis*v1.normalised().getY());
		}
		gdP = new Point2D(x,y);
//		log.error("过度点"+gdP);
		return new Point2D(srcP.getX()+Max_dis*v1.normalised().getX(),srcP.getY()+Max_dis*v1.normalised().getY()); 
	}
	
	public double p2lDis(Point2D p,Point2D src,Point2D end){
		double x0 = src.getX();
		double y0 = src.getY();
		double x1 = end.getX();
		double y1 = end.getY();
		double x = p.getX();
		double y = p.getY();
		double k = (x1-x0)/(y1-y0);
		double b = y0-k*x0;
		double dis = Math.abs(k*x-y+b)/Math.sqrt(1+k*k);
		return dis;
	}
	
	public Point2D fixP(Point2D srcP,Point2D targetP){
		double x1 = srcP.getX();
		double y1 = srcP.getY();
		double x2 = targetP.getX();
		double y2 = targetP.getY();
		double length = ScaleGeo.getDistance(srcP, targetP);
		double delta = Max_dis - length;
		double y = y2+Math.sqrt(delta*delta/(1+((x2-x1)/(y2-y1))*((x2-x1)/(y2-y1))));
		if(((y>y1)&&(y<y2))||((y<y1)&&(y>y2))){
			y = y2-Math.sqrt(delta*delta/(1+((x2-x1)/(y2-y1))*((x2-x1)/(y2-y1))));
		}
		double x = (y-y2)*(x2-x1)/(y2-y1)+x2;
		return new Point2D(x,y);
	}
	
	public Point2D relongP(Point2D srcP,Point2D targetP){
		double x1 = srcP.getX();
		double y1 = srcP.getY();
		double x2 = targetP.getX();
		double y2 = targetP.getY();
		double x = x1+Math.sqrt(Max_dis*Max_dis/(1+((y2-y1)/(x2-x1))*((y2-y1)/(x2-x1))));
		if(!((x2<x) && (x2>x1)) || ((x2>x) && (x2<x1))){
			x = x1-Math.sqrt(Max_dis*Max_dis/(1+((y2-y1)/(x2-x1))*((y2-y1)/(x2-x1))));
		}
		double y = (x-x1)*(y2-y1)/(x2-x1)+y1;
		return new Point2D(x,y);
	}
	
	public Point2D parrlP(Point2D myP,Point2D srcP,Point2D targetP){
		double x0 = myP.getX();
		double y0 = myP.getY();
		double x1 = srcP.getX();
		double y1 = srcP.getY();
		double x2 = targetP.getX();
		double y2 = targetP.getY();
		Vector2D v1 = new Vector2D(x2-x1,y2-y1); 
		return new Point2D(x0+Max_dis*v1.normalised().getX(),y0+Max_dis*v1.normalised().getY());
	}
	
	public boolean isAnyOneBetween(List<Point2D> pList,Point2D p1,Point2D p2){
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
	
	public boolean isBlockadeInSurf(EntityID targetRoad,List<EntityID> pathList,Point2D myPoint,
			 Point2D targetPoint){
		double length = ScaleGeo.getDistance(myPoint, targetPoint)-100;
		Vector2D v = new Vector2D(targetPoint.getX()-myPoint.getX(),targetPoint.getY()-myPoint.getY());	
		targetPoint = new Point2D(myPoint.getX()+length*v.normalised().getX(),myPoint.getY()+length*v.normalised().getY());
		Line2D line = new Line2D(myPoint,targetPoint);
		Line2D leftLine = ScaleGeo.getParallelLineLeft(line,clearRad/2);
		Line2D rightLine = ScaleGeo.getParallelLineRight(line, clearRad/2);
		Point2D p1 = leftLine.getOrigin();
		Point2D p2 = leftLine.getEndPoint();
		Point2D p3 = rightLine.getOrigin();
		Point2D p4 = rightLine.getEndPoint();
		Road targetroad = (Road)model.getEntity(targetRoad);
		Road nextroad = null;
		if(!pathList.get((pathList.indexOf(targetRoad)+1)).equals(null)){
			nextroad = (Road)model.getEntity(pathList.get((pathList.indexOf(targetRoad)+1)));
		}
		List<Point2D> vertex2D = new ArrayList<Point2D>();
		vertex2D.add(p1);
		vertex2D.add(p2);
		vertex2D.add(p4);
		vertex2D.add(p3);
		List<EntityID> blkList = targetroad.getBlockades();
		List<Blockade> blokList = new ArrayList<Blockade>();
		List<Blockade> blokList2 = new ArrayList<Blockade>();
		if(!nextroad.equals(null) && nextroad.isBlockadesDefined()){
			List<EntityID> blkList2 = nextroad.getBlockades();
			for(EntityID blk2:blkList2){
				Blockade blok2 = (Blockade)model.getEntity(blk2);
				blokList2.add(blok2);
			}
		}
		for(EntityID blk:blkList){
			Blockade blok = (Blockade)model.getEntity(blk);
			blokList.add(blok);
		}
		int flag = 0;
		for(Blockade blk:blokList){
			int[] allApexes = blk.getApexes();
			List<Point2D> blkVertex = new ArrayList<Point2D>();
			int count = allApexes.length / 2;
			for (int i = 0; i < count; ++i) {
				blkVertex.add(new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]));
			}
			for(Point2D point:blkVertex){
				if(ScaleGeo.inPolygon(vertex2D,point)){
					log.error("有路障在我的清障区域内");
					flag = 1;
					return true;
				}
				else{
					for(Point2D p:vertex2D){
						if(ScaleGeo.inPolygon(blkVertex,p)){
							log.error("有路障在我的清障区域内");
							flag = 1;
							return true;
						}
					}
				}
			}
		}
		if(flag == 0){
			if(!nextroad.equals(null) && !blokList2.isEmpty()){
				for(Blockade blk2:blokList2){
					int[] allApexes = blk2.getApexes();
					List<Point2D> blkVertex = new ArrayList<Point2D>();
					int count = allApexes.length / 2;
					for (int i = 0; i < count; ++i) {
						blkVertex.add(new Point2D(allApexes[i * 2],allApexes[i * 2 + 1]));
					}
					for(Point2D point:blkVertex){
						if(ScaleGeo.inPolygon(vertex2D,point)){
							log.error("有路障在我的清障区域内");
							flag = 1;
							return true;
						}
						else{
							for(Point2D p:vertex2D){
								if(ScaleGeo.inPolygon(blkVertex,p)){
									log.error("有路障在我的清障区域内");
									flag = 1;
									return true;
								}
							}
						}
					}
				}
				return false;
			}
			else{
				return false;
			}
		}
		else{
			return true;
		}
	}
	
	public EntityID isInBlockade(Point2D pos,Road road){
		if(road.isBlockadesDefined()){
			for(EntityID next : road.getBlockades()){
				Blockade b=(Blockade)model.getEntity(next);
				if(ScaleGeo.inPolygon(b.getApexes(), pos)){
					return next;
				}
			}
		}
		return null;
	}
	
}	
//	private boolean needTotallyClear(EntityID roadID){
//		Road road = (Road)model.getEntity(roadID);
//		if((cjsTool.getJigsaw((road)) instanceof Entrance)
//			||(cjsTool.getJigsaw(road) instanceof Cross)){
//			log.error("在"+roadID+"需要全部清障");
//			return true;
//		}
//		else{
//			log.error("在"+roadID+"需要部分清障");
//			return false;
//		}
//	}
	
//	private List<Point2D> getThreePoint(EntityID myPos,EntityID targetID){
//		Road targetRoad = (Road)model.getEntity(targetID);
//		Road myRoad = (Road)model.getEntity(myPos);
//		Edge betweenEdge = targetRoad.getEdgeTo(myPos);
//		List<Point2D> pList = new ArrayList<Point2D>();
//		Point2D firstP = new Point2D(myRoad.getX(),myRoad.getY());
//		Point2D midP = ScaleGeo.getPointOnEdge(betweenEdge);
//		Point2D secondP = new Point2D(targetRoad.getX(),targetRoad.getY());
//		pList.add(firstP);
//		pList.add(midP);
//		pList.add(secondP);
//		return pList;
//	}

//	public List<Point2D> chooseFirstPosition(EntityID targetRoad,EntityID myPos,
//			Point2D myPoint){
//List<Point2D> resList = new ArrayList<Point2D>();
//List<Point2D> pList = getThreePoint(myPos,targetRoad);
//double mfLength = ScaleGeo.getDistance(pList.get(0), pList.get(1));
//if(mfLength < Max_dis){
//resList.add(pList.get(1));
//}
//else{
//double x1 = pList.get(0).getX();
//double y1 = pList.get(0).getY();
//double x2 = pList.get(1).getX();
//double y2 = pList.get(1).getY();
//double y = y1+Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
//if(!((y>y1)&&(y<y2))||((y<y1)&&(y>y2))){
//y = y1-Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
//}
//double x = (y-y1)*(x1-x2)/(y1-y2)+x1;
//resList.add(new Point2D(x,y));
//mfLength = mfLength-Max_dis;
//int index = (int)Math.round(mfLength/Max_dis);
//if(mfLength>Max_dis){
//for(int i=1;i<=index;i++){
//resList.add(ScaleGeo.getFinalPoint(pList.get(0), resList.get(0), i+1));
//}
//}
//else{
//resList.add(pList.get(1));
//}
//}
//return resList;
//}
	
//	public List<Point2D> chooseDoublePosition(EntityID targetRoad,EntityID myPos,
//			  Point2D myPoint){
//List<Point2D> resList = new ArrayList<Point2D>();
//for(Point2D fp:chooseFirstPosition(targetRoad,myPos,myPoint)){
//resList.add(fp);
//}
//for(Point2D sp:chooseSecondPosition(targetRoad,myPos,myPoint)){
//resList.add(sp);
//}
//return resList;
//}
//
//
//
//public List<Point2D> chooseSecondPosition(EntityID targetRoad,EntityID myPos,
//Point2D myPoint){
//List<Point2D> resList = new ArrayList<Point2D>();
//List<Point2D> pList = getThreePoint(myPos,targetRoad);
//double fmLength = ScaleGeo.getDistance(pList.get(1), pList.get(2));
//if(fmLength < Max_dis){
//resList.add(pList.get(2));
//}
//else{
//double x1 = pList.get(1).getX();
//double y1 = pList.get(1).getY();
//double x2 = pList.get(2).getX();
//double y2 = pList.get(2).getY();
//double y = y1+Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
//if(!((y>y1)&&(y<y2))||((y<y1)&&(y>y2))){
//y = y1-Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
//}
//double x = (y-y1)*(x1-x2)/(y1-y2)+x1;
//Point2D fe = new Point2D(x,y);
//resList.add(fe);
//fmLength = fmLength-Max_dis;
//int index = (int)Math.round(fmLength/Max_dis);
//if(fmLength>Max_dis){
//for(int i=1;i<=index;i++){
//resList.add(ScaleGeo.getFinalPoint(pList.get(1), fe, i+1));
//}
//}
//else{
//resList.add(pList.get(2));
//}
//}
//return resList;
//}
//
//public int clearCondition(EntityID myPos,EntityID targetRoad){
//int flag = 0;
//if((needTotallyClear(myPos)) && (needTotallyClear(targetRoad))){
//log.error("两部分都需要全清");
//flag = 4;
//}
//else if((needTotallyClear(myPos)) && (!needTotallyClear(targetRoad))){
//log.error("第一部分需要全清");
//flag = 3;
//}
//else if((!needTotallyClear(myPos)) && (needTotallyClear(targetRoad))){
//log.error("第二部分需要全清");
//flag = 2;
//}
//else if((!needTotallyClear(myPos)) && (!needTotallyClear(targetRoad))){
//log.error("两部分都不需要全清");
//flag = 1;
//}
//else{
//log.error("出错了！");
//}
//return flag;
//}
	
//	public boolean isInFirstRoad(Point2D point,EntityID myPos,EntityID targetID){
//		double x = point.getX();
//		List<Point2D> pList = getThreePoint(myPos,targetID);
//		double x1 = pList.get(0).getX();
//		double x2 = pList.get(1).getX();
//		if(point.equals(pList.get(1))){
//			return true;
//		}
//		else if(((x<x1) && (x>x2)) || ((x>x1) && (x<x2))){
//			return true;
//		}
//		else{
//			return false;
//		}
//	}

