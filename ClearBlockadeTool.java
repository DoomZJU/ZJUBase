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
