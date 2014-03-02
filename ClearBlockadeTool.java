package scale.tool.PF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import scale.Constants;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleTool;
import scale.tool.ScaleToolURN;
import scale.tool.map.jigsaw.CreateJigsawTool;
import scale.tool.map.jigsaw.Cross;
import scale.tool.map.jigsaw.Entrance;
import scale.utils.geo.ScaleGeo;

public class ClearBlockadeTool extends ScaleTool {
	private static final Log log = LogFactory.getLog(ClearBlockadeTool.class);
	private CreateJigsawTool cjsTool;
	private int Max_dis;
	private int clearRad;
	private Point2D gdP = null;

	public ClearBlockadeTool() {
		super(ScaleToolURN.ClearBlockadeTool);
	}

	@Override
	protected boolean initialize() {
		cjsTool = (CreateJigsawTool) toolKit.getTool(ScaleToolURN.CreateJigsawTool);
		Max_dis = config.getIntValue(Constants.KEY_CLEAR_REPAIR_DISTANCE,-1);
		clearRad = config.getIntValue(Constants.KEY_CLEAR_REPAIR_RAD, -1);
		
		return true;
	}
	
	@Override
	public boolean update(int time, ChangeSet changes,
			Collection<ScaleMessage> messages) {
		
		return true;
	}
	
	public List<Point2D> chooseDoublePosition(EntityID targetRoad,EntityID myPos,
											  Point2D myPoint){
		List<Point2D> resList = new ArrayList<Point2D>();
		for(Point2D fp:chooseFirstPosition(targetRoad,myPos,myPoint)){
			resList.add(fp);
		}
		for(Point2D sp:chooseSecondPosition(targetRoad,myPos,myPoint)){
			resList.add(sp);
		}
		return resList;
	}
	
	public List<Point2D> chooseFirstPosition(EntityID targetRoad,EntityID myPos,
											Point2D myPoint){
		List<Point2D> resList = new ArrayList<Point2D>();
		List<Point2D> pList = getThreePoint(myPos,targetRoad);
		double mfLength = ScaleGeo.getDistance(pList.get(0), pList.get(1));
		if(mfLength < Max_dis){
			resList.add(pList.get(1));
		}
		else{
			double x1 = pList.get(0).getX();
			double y1 = pList.get(0).getY();
			double x2 = pList.get(1).getX();
			double y2 = pList.get(1).getY();
			double y = y1+Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
			if(!((y>y1)&&(y<y2))||((y<y1)&&(y>y2))){
				y = y1-Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
			}
			double x = (y-y1)*(x1-x2)/(y1-y2)+x1;
			resList.add(new Point2D(x,y));
			mfLength = mfLength-Max_dis;
			int index = (int)Math.round(mfLength/Max_dis);
			if(mfLength>Max_dis){
				for(int i=1;i<=index;i++){
					resList.add(ScaleGeo.getFinalPoint(pList.get(0), resList.get(0), i+1));
				}
			}
			else{
				resList.add(pList.get(1));
			}
		}
		return resList;
	}
	
	public List<Point2D> chooseSecondPosition(EntityID targetRoad,EntityID myPos,
							  Point2D myPoint){
		List<Point2D> resList = new ArrayList<Point2D>();
		List<Point2D> pList = getThreePoint(myPos,targetRoad);
		double fmLength = ScaleGeo.getDistance(pList.get(1), pList.get(2));
		if(fmLength < Max_dis){
			resList.add(pList.get(2));
		}
		else{
			double x1 = pList.get(1).getX();
			double y1 = pList.get(1).getY();
			double x2 = pList.get(2).getX();
			double y2 = pList.get(2).getY();
			double y = y1+Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
			if(!((y>y1)&&(y<y2))||((y<y1)&&(y>y2))){
				y = y1-Math.sqrt(Max_dis*Max_dis/(1+((x1-x2)/(y1-y2))*((x1-x2)/(y1-y2))));
			}
			double x = (y-y1)*(x1-x2)/(y1-y2)+x1;
			Point2D fe = new Point2D(x,y);
			resList.add(fe);
			fmLength = fmLength-Max_dis;
			int index = (int)Math.round(fmLength/Max_dis);
			if(fmLength>Max_dis){
				for(int i=1;i<=index;i++){
					resList.add(ScaleGeo.getFinalPoint(pList.get(1), fe, i+1));
				}
			}
			else{
				resList.add(pList.get(2));
			}
		}
		return resList;
	}
	
	public int clearCondition(EntityID myPos,EntityID targetRoad){
		int flag = 0;
		if((needTotallyClear(myPos)) && (needTotallyClear(targetRoad))){
			log.error("两部分都需要全清");
			flag = 4;
		}
		else if((needTotallyClear(myPos)) && (!needTotallyClear(targetRoad))){
			log.error("第一部分需要全清");
			flag = 3;
		}
		else if((!needTotallyClear(myPos)) && (needTotallyClear(targetRoad))){
			log.error("第二部分需要全清");
			flag = 2;
		}
		else if((!needTotallyClear(myPos)) && (!needTotallyClear(targetRoad))){
			log.error("两部分都不需要全清");
			flag = 1;
		}
		else{
			log.error("出错了！");
		}
		return flag;
	}
	
	public List<Point2D> getPolygon(Point2D myPoint,
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
	
	public boolean isBlockadeInSurf(EntityID targetRoad,Point2D myPoint,
									 Point2D targetPoint){
		Line2D line = new Line2D(myPoint,targetPoint);
		Line2D leftLine = ScaleGeo.getParallelLineLeft(line,clearRad);
		Line2D rightLine = ScaleGeo.getParallelLineRight(line, clearRad);
		Point2D p1 = leftLine.getOrigin();
		Point2D p2 = leftLine.getEndPoint();
		Point2D p3 = rightLine.getOrigin();
		Point2D p4 = rightLine.getEndPoint();
		Road targetroad = (Road)model.getEntity(targetRoad);
		List<Point2D> vertex2D = new ArrayList<Point2D>();
		vertex2D.add(p1);
		vertex2D.add(p2);
		vertex2D.add(p4);
		vertex2D.add(p3);
		List<EntityID> blkList = targetroad.getBlockades();
		List<Blockade> blokList = new ArrayList<Blockade>();
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
			return false;
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
	
	public boolean isInFirstRoad(Point2D point,EntityID myPos,EntityID targetID){
		double x = point.getX();
		List<Point2D> pList = getThreePoint(myPos,targetID);
		double x1 = pList.get(0).getX();
		double x2 = pList.get(1).getX();
		if(point.equals(pList.get(1))){
			return true;
		}
		else if(((x<x1) && (x>x2)) || ((x>x1) && (x<x2))){
			return true;
		}
		else{
			return false;
		}
	}
	
	public Point2D inLineP(Point2D srcP,Point2D targetP,Blockade blk){
		double x0 = srcP.getX();
		double y0 = srcP.getY();
		double x1 = targetP.getX();
		double y1 = targetP.getY();
		double R = this.clearRad;
		double L = ScaleGeo.getDistance(srcP, targetP);
		double coss = R/L;
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
		if(ScaleGeo.inPolygon(blk.getApexes(), new Point2D(x,y))){
			x =  Xd - Math.sqrt(l*l/(1+kcd*kcd));
			y = kcd*(x-Xd)+Yd;
		}
		gdP = new Point2D(x,y);
		log.error("过度点"+gdP);
		Line2D line = new Line2D(srcP,gdP);
		Point2D reso = ScaleGeo.getParallelLineLeft(line, L).getOrigin();
		Point2D rese = ScaleGeo.getParallelLineLeft(line, L).getOrigin();
		if(reso.equals(new Point2D(x1,y1))){
			return rese;
		}
		else{
			return reso;
		}
//		double xx = x1-Math.sqrt(R*R/(1+Math.pow((y-y0)/(x-x0), 2)));
//		double yy = y1+(xx-x1)*(y-y0)/(x-x0);
//		if(ScaleGeo.inPolygon(blk.getApexes(), new Point2D(xx,yy))){
//			xx = x1-Math.sqrt(R*R/(1+Math.pow((y-y0)/(x-x0), 2)));
//			yy = y1+(xx-x1)*(y-y0)/(x-x0);
//		}
//		return new Point2D(xx,yy); 
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
		double y = y1+Math.sqrt(Max_dis*Max_dis/(1+((x2-x1)/(y2-y1))*((x2-x1)/(y2-y1))));
		if(!((y>y1)&&(y<y2))||((y<y1)&&(y>y2))){
			y = y1-Math.sqrt(Max_dis*Max_dis/(1+((x2-x1)/(y2-y1))*((x2-x1)/(y2-y1))));
		}
		double x = (y-y1)*(x2-x1)/(y2-y1)+x1;
		return new Point2D(x,y);
	}
	
	public Point2D parrlP(Point2D myP,Point2D srcP,Point2D targetP){
		double x0 = myP.getX();
		double y0 = myP.getY();
		double x1 = srcP.getX();
		double y1 = srcP.getY();
		double x2 = targetP.getX();
		double y2 = targetP.getY();
		double y = y0+Math.sqrt(Max_dis*Max_dis/(1+((x2-x1)/(y2-y1))*((x2-x1)/(y2-y1))));
		double x = (y-y0)*(x2-x1)/(y2-y1)+x0;
		if((((x2-x1)/(y2-y1))<0 && ((x-x0)/(y-y0))>0) || (((x2-x1)/(y2-y1))>0 && ((x-x0)/(y-y0))<0)){
			y = y0-Math.sqrt(Max_dis*Max_dis/(1+((x2-x1)/(y2-y1))*((x2-x1)/(y2-y1))));
		}
		x = (y-y0)*(x2-x1)/(y2-y1)+x0;
		return new Point2D(x,y);
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
	
	private boolean needTotallyClear(EntityID roadID){
		Road road = (Road)model.getEntity(roadID);
		if((cjsTool.getJigsaw((road)) instanceof Entrance)
			||(cjsTool.getJigsaw(road) instanceof Cross)){
			log.error("在"+roadID+"需要全部清障");
			return true;
		}
		else{
			log.error("在"+roadID+"需要部分清障");
			return false;
		}
	}
	
	private List<Point2D> getThreePoint(EntityID myPos,EntityID targetID){
		Road targetRoad = (Road)model.getEntity(targetID);
		Road myRoad = (Road)model.getEntity(myPos);
		Edge betweenEdge = targetRoad.getEdgeTo(myPos);
		List<Point2D> pList = new ArrayList<Point2D>();
		Point2D firstP = new Point2D(myRoad.getX(),myRoad.getY());
		Point2D midP = ScaleGeo.getPointOnEdge(betweenEdge);
		Point2D secondP = new Point2D(targetRoad.getX(),targetRoad.getY());
		pList.add(firstP);
		pList.add(midP);
		pList.add(secondP);
		return pList;
	}
	
}
