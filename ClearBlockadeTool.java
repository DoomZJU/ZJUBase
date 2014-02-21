package scale.tool.PF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
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
import scale.tool.stuck.StuckTool;
import scale.utils.geo.ScaleGeo;

public class ClearBlockadeTool extends ScaleTool {
	private static final Log log = LogFactory.getLog(ClearBlockadeTool.class);	
	private CreateJigsawTool cjsTool;
	private StuckTool stkTool;

	protected ClearBlockadeTool() {
		super(ScaleToolURN.ClearBlockadeTool);
	}

	@Override
	protected boolean initialize() {
		cjsTool = (CreateJigsawTool) toolKit.getTool(ScaleToolURN.CreateJigsawTool);
		stkTool = (StuckTool) toolKit.getTool(ScaleToolURN.StuckTool);
		return true;
	}

	@Override
	public boolean update(int time, ChangeSet changes,
			Collection<ScaleMessage> messages) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public Point2D chooseBestPosition(EntityID targetRoad,EntityID myPos,Point2D myPoint){
		int type = 0;
		Road road = (Road) model.getEntity(targetRoad);
		List<Point2D> pList = getThreePoint(myPos,targetRoad);
		if(stkTool.isInBlockade(pList.get(0), road)){
			Point2D desPoint = null;
			List<Point2D> sfList = new ArrayList<Point2D>();
			List<Point2D> fmList = new ArrayList<Point2D>();
			Line2D sfLine = null;
			Line2D fmLine = null;
			sfList.add(myPoint);
			sfList.add(pList.get(0));
			fmList.add(pList.get(0));
			fmList.add(pList.get(1));
			sfLine = ScaleGeo.pointsToLines(sfList).get(0);
			fmLine = ScaleGeo.pointsToLines(fmList).get(0);
			if(!ScaleGeo.parallel(sfLine, fmLine)){
				
			}
		}
	}
	
	public boolean needTotallyClear(EntityID roadID){
		Road road = (Road)model.getEntity(roadID);
		if((cjsTool.getJigsaw((road)) instanceof Entrance)||(cjsTool.getJigsaw(road) instanceof Cross)){
			return true;
		}
		else{
			return false;
		}
	}
	
	private boolean isOverMaxDistance(Point2D myPoint,Point2D targetP){
		if(ScaleGeo.getDistance(myPoint, targetP) > config.getIntValue(Constants.KEY_CLEAR_REPAIR_DISTANCE,-1)){
			return true;
		}
		else{
			return false;
		}
	}
	
	private List<Point2D> getThreePoint(EntityID myPos,EntityID roadID){
		Road road = (Road)model.getEntity(roadID);
		Edge firstEdge = road.getEdgeTo(myPos);
		List<Point2D> pList = new ArrayList<Point2D>();
		Point2D firstPoint = ScaleGeo.getPointOnEdge(firstEdge);
		Point2D midPoint = new Point2D(road.getX(),road.getY());
		Point2D secondPoint = null;
		for(Edge edge:road.getEdges()){
			Area edgeNeib = (Area)model.getEntity(edge.getNeighbour());
			if((edge.isPassable())&&(edge.getNeighbour()!=myPos)&&!(cjsTool.getJigsaw(edgeNeib) instanceof Entrance)){
				secondPoint = ScaleGeo.getPointOnEdge(edge);
			}
		}
		pList.add(firstPoint);
		pList.add(midPoint);
		pList.add(secondPoint);
		return pList;
	}
	
}
