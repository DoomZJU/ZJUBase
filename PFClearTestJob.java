package scale.agent.job.pf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import scale.agent.command.ScaleCommand;
import scale.agent.job.ScaleJob;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleToolURN;
import scale.tool.PF.ClearBlockadeTool;
import scale.tool.map.area.AccessibleTool;
import scale.tool.map.path.Path;
import scale.tool.map.path.SearchPathTool;
import scale.agent.command.*;

public class PFClearTestJob extends ScaleJob<Human> {
	private static final Log log = LogFactory.getLog(PFClearTestJob.class);
	
	private enum JobState {
		Free,move,clean;
	}
	
	private JobState state;
	private SearchPathTool spTool;
	private ClearBlockadeTool cbTool;
	private AccessibleTool acTool;
	private int num = 0;
	private EntityID currentNode;
	private EntityID curretnID = null;
	private EntityID nextRoad = null;
	private List<Point2D> clearList = new ArrayList<Point2D>();
	private List<EntityID> realList = new ArrayList<EntityID>();
	private List<EntityID> blkList = new ArrayList<EntityID>();
	private List<EntityID> myPath = new ArrayList<EntityID>();
	private Map<EntityID,List<Boolean>> myPathMap = new LinkedHashMap<EntityID,List<Boolean>>(); 
	private List<Point2D> pointToCLean = new ArrayList<Point2D>();
	
	@Override
	protected boolean initialize() {
		spTool = (SearchPathTool) toolKit.getTool(ScaleToolURN.SearchPathTool);
		cbTool = (ClearBlockadeTool) toolKit.getTool(ScaleToolURN.ClearBlockadeTool);
		acTool = (AccessibleTool) toolKit.getTool(ScaleToolURN.AccessibleTool);
		
		state = JobState.Free;
		return true;
	}

	@Override
	public ScaleCommand execute(int time, ChangeSet changes,
			Collection<ScaleMessage> messages, ScaleCommand other) {
		switch(state){
		case Free:{
			log.error("状态"+state);
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
			currentNode = me.getPosition();
			int currentIndex = myPath.indexOf(currentNode);
			if(!currentNode.equals(curretnID)){
				for(int i=0;i<=currentIndex;i++){
					myPathMap.get(myPath.get(i)).add(1, true);
				}
				if(!(realList.isEmpty()) && (me.getPosition()
											.equals(realList.get(realList.size()-1)))){
					log.error("开始清理！");
					myPathMap.get(myPath.get(currentIndex+1)).add(1, true);
					nextRoad = myPath.get(currentIndex+1);
					state = JobState.clean;
				}
				realList.clear();
				for(Entry<EntityID,List<Boolean>> ent:myPathMap.entrySet()){
					if(num == 0){
						for(int i=currentIndex+1;i<myPath.size();i++){
							realList.add(myPath.get(i));
						}
						break;
					}
					else if((ent.getValue().get(0)) && !(ent.getValue().get(1))){
						int index = myPath.indexOf(ent.getKey());
						for(int i=currentIndex;i<index;i++){
							realList.add(myPath.get(i));
						}
						num--;
						break;
					}
				}
				log.error("真实路径"+realList);
			}
			if((!me.getPosition().equals(realList.get(realList.size()-1)))){
				break;
			}
			else{
				log.error("我以走到！");
				return null;
			}
		}
		case clean:{
			log.error("状态"+state);
			int flag = cbTool.clearCondition(me.getPosition(),nextRoad);
			switch(flag){
			case 1:{
				clearList = cbTool.chooseDoublePosition(nextRoad, me.getPosition(), 
													new Point2D(me.getX(),me.getY()));
				Point2D firstP = new Point2D(me.getX(),me.getY());
				for(Point2D p:clearList){
						if(cbTool.isInFirstRoad(p,me.getPosition(),nextRoad)){
							if (agent.getAgentViewer() != null) {
								agent.getAgentViewer()
										.getMiscLayer()
										.addPolygon(cbTool.getPolygon(firstP, p), Color.green);
							}
							if(cbTool.isBlockadeInSurf(me.getPosition(),firstP,p)){
								if (agent.getAgentViewer() != null) {
									agent.getAgentViewer()
											.getMiscLayer()
											.addPointCross(p, Color.green, 500);
								}
							}
							else{
								if (agent.getAgentViewer() != null) {
									agent.getAgentViewer()
											.getMiscLayer()
											.addPointCross(p, Color.black, 500);
								}
							}
							firstP = p;
						}
						else{
							if (agent.getAgentViewer() != null) {
								agent.getAgentViewer()
										.getMiscLayer()
										.addPolygon(cbTool.getPolygon(firstP, p), Color.green);
							}
							if(cbTool.isBlockadeInSurf(nextRoad,firstP,p)){
								if (agent.getAgentViewer() != null) {
									agent.getAgentViewer()
											.getMiscLayer()
											.addPointCross(p, Color.green, 500);
								}
							}
							else{
								if (agent.getAgentViewer() != null) {
									agent.getAgentViewer()
											.getMiscLayer()
											.addPointCross(p, Color.black, 500);
								}
							}
							firstP = p;
						}
				}
				break;
			}
			case 2:{
				clearList = cbTool.chooseFirstPosition(nextRoad, me.getPosition(), 
													 new Point2D(me.getX(),me.getY()));
				for(Point2D p:clearList){
					if (agent.getAgentViewer() != null) {
						agent.getAgentViewer()
								.getMiscLayer()
								.addText(p,"模式2关键点", Color.black);
					}
				}
				break;
			}
			case 3:{
				clearList = cbTool.chooseSecondPosition(nextRoad, me.getPosition(), 
													new Point2D(me.getX(),me.getY()));
				for(Point2D p:clearList){
					if (agent.getAgentViewer() != null) {
						agent.getAgentViewer()
								.getMiscLayer()
								.addText(p,"模式3关键点", Color.black);
					}
				}
				break;
			}
			case 4:{
				log.error("模式4");
				break;
			}
			case 0:{
				log.error("错了！");
				break;
			}
			}
		}
		
		}
		if((state == JobState.move) && (!realList.isEmpty())){
			return new MoveCommand(time,realList);
		}
//		else if((state == JobState.clean)){
//			
//		}
		return null;
	}

	@Override
	public void overridden() {
		// TODO Auto-generated method stub
		
	}

}
