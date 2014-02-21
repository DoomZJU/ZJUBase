package scale.agent.job.pf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import scale.agent.command.MoveCommand;
import scale.agent.command.MoveXYCommand;
import scale.agent.command.ScaleCommand;
import scale.agent.job.ScaleJob;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleToolURN;
import scale.tool.PF.PFAssignTool;
import scale.tool.map.partition.District;
import scale.tool.map.partition.TestPartition;
import scale.tool.map.path.Path;
import scale.tool.map.path.SearchPathTool;
import scale.utils.geo.ScaleGeo;

/**
 * 
 * 
 * @author Doom
 * 
 */

public class PFTestJob extends ScaleJob<Human> {
	private static final Log log = LogFactory.getLog(PFTestJob.class);

	private enum JobState {
		preProcess, Free, goToDis, onMoving, LetMecc, clean;
	}

	// private StandardWorldModel world;
	private JobState state;
	private SearchPathTool spTool;
	// private StuckTool stuckTool;
	// private ClearChooseTool ccTool;
	// private CreateJigsawTool cjsTool;
	private PFAssignTool paTool;
	// private List<Pair<Integer, Integer>> pointList = new
	// ArrayList<Pair<Integer, Integer>>();
	private List<District> disList = new ArrayList<District>();
	private List<EntityID> finalPath = new ArrayList<EntityID>();
	// private Map<EntityID,TestPartition> result = new
	// HashMap<EntityID,TestPartition>();
	// private Map<Point2D,TestPartition> pMap = new
	// HashMap<Point2D,TestPartition>();
	// private Map<EntityID,Point2D> aMap = new HashMap<EntityID,Point2D>();
	private EntityID currentNode, lastNode;
	private EntityID curretnID = null;
	// private EntityID clearPoint = null;
	private EntityID errorID = null;
	// private Area neib = null;
	private District tempDt = null;
	private Path myPath = null;
	private Path errorPath = null;
	boolean stillWorking = false;
	boolean startWorking = false;

	@Override
	protected boolean initialize() {
		spTool = (SearchPathTool) toolKit.getTool(ScaleToolURN.SearchPathTool);
		// stuckTool = (StuckTool) toolKit.getTool(ScaleToolURN.StuckTool);
		// cjsTool = (CreateJigsawTool) toolKit
		// .getTool(ScaleToolURN.CreateJigsawTool);
		paTool = (PFAssignTool) toolKit.getTool(ScaleToolURN.PFAssignTool);

		state = JobState.preProcess;
		return true;
	}

	@Override
	public ScaleCommand execute(int time, ChangeSet changes,
			Collection<ScaleMessage> messages, ScaleCommand other) {

		switch (state) {
		case preProcess: {
			// make an assignment of PF and Partition;
			// result = aTool.getMatching();
			// log.debug("分区分配结果"+result);
			// pMap = aTool.getPMap();
			// log.debug("agent分配到的分区编号"+pMap);
			// aMap = aTool.getAMap();
			// log.debug("agent的坐标和ID"+result);
			// log.debug("pMap size:" + pMap.size());
			Point2D myPoint = new Point2D(me.getX(), me.getY());
			// Point2D pcenter = result.get(aMap.get(me.getID()));
			// TestPartition part = pMap.get(pcenter);
			TestPartition part = paTool.getPartition();
			for (District dt : part) {
				disList.add(dt);
			}
			log.debug("point=" + myPoint);
			log.debug(part);
			log.debug("distList=" + disList.toString());
			if (agent.getAgentViewer() != null) {
				agent.getAgentViewer().getMiscLayer()
						.addLine(myPoint, part.getCenter(), Color.red);
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(part.getCenter(),
								"coordinate " + part.getCenter().toString(),
								Color.BLACK);
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(
								new Point2D(part.getCenter().getX(), part
										.getCenter().getY() + 5000),
								"AgentID " + agent.getID().toString(),
								Color.green);
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(
								new Point2D(part.getCenter().getX(), part
										.getCenter().getY() - 5000),
								"Distance "
										+ ScaleGeo.getDistance(myPoint,
												part.getCenter()), Color.blue);
			}

			state = JobState.Free;
		}
		case Free: {
			if (agent.getAgentViewer() != null) {
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(new Point2D(me.getX(), me.getY()),
								"我当前的状态是" + state, Color.black);
			}
			finalPath.clear();

			double tempLength = Double.MAX_VALUE;
			Point2D point = new Point2D(me.getX(), me.getY());
			if (!disList.isEmpty()) {
				for (District dt : disList) {
					if (tempLength > ScaleGeo
							.getDistance(dt.getCenter(), point)) {
						tempLength = ScaleGeo
								.getDistance(dt.getCenter(), point);
						tempDt = dt;
					}
				}
				// if (disList.size() != 1) {
				disList.remove(tempDt);
				// }
			}
			log.debug("距离最近的社区" + tempDt);
			for (EntityID road : tempDt.getEntityIDList()) {
				finalPath.add(road);
			}
			log.debug("社区环路" + finalPath);

			if (finalPath.contains(me.getPosition())) {
				if (me.getPosition() != finalPath.get(0)) {
					curretnID = me.getPosition();
					int currentIndex = finalPath.indexOf(me.getPosition());
					for (int i = 0; i < currentIndex; i++) {
						EntityID id = finalPath.get(i);
						finalPath.add(id);
					}
					for (int i = 0; i < currentIndex; i++) {
						finalPath.remove(0);
					}
					log.debug("重排版后的list" + finalPath);
					finalPath.add(me.getPosition());
				} else {
					finalPath.add(currentNode);
					curretnID = finalPath.get(0);
				}
				state = JobState.onMoving;
				break;
			} else {
				state = JobState.goToDis;
				log.debug("Entering the goToDisJob");
				break;
			}

		}
		case goToDis: {
			if (agent.getAgentViewer() != null) {
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(new Point2D(me.getX(), me.getY()),
								"我当前的状态是" + state, Color.black);
			}

			EntityID startP = me.getPosition();
			EntityID endP = finalPath.get(0);
			log.debug("startpoint " + startP + " " + "endpoint" + endP);
			if (!startP.equals(endP)) {
				myPath = spTool.getPath(startP, endP, true);
				log.debug("My path to district" + myPath);
				break;
			} else {
				curretnID = finalPath.get(0);
				finalPath.add(curretnID);
				state = JobState.onMoving;
				myPath = null;
				break;
			}
		}
		case onMoving: {
			if (agent.getAgentViewer() != null) {
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(new Point2D(me.getX(), me.getY()),
								"我当前的状态是" + state, Color.black);
			}

			errorPath = null;
			currentNode = me.getPosition();
			if (currentNode != curretnID) {
				int currentIndex = finalPath.indexOf(currentNode);
				for (int i = 0; i < currentIndex; i++) {
					finalPath.remove(0);
				}
				log.error("path after think" + finalPath);
				if (!finalPath.contains(me.getPosition())) {
					log.error("我走错了");
					Area nearPlace = (Area) model.getEntity(me.getPosition());
					EntityID target = null;
					for (EntityID id : nearPlace.getNeighbours()) {
						Area idPlace = (Area) model.getEntity(id);
						if (finalPath.contains(id)) {
							int index = finalPath.indexOf(id);
							if ((index + 1) <= finalPath.size() - 1) {
								target = finalPath.get(index + 1);
							} else {
								target = finalPath.get(finalPath.size() - 1);
							}
						} else if (!idPlace.getNeighbours().isEmpty()) {
							for (EntityID id1 : idPlace.getNeighbours()) {
								if (finalPath.contains(id1)) {
									int index1 = finalPath.indexOf(id1);
									if ((index1 + 1) <= finalPath.size() - 1) {
										target = finalPath.get(index1 + 1);
									} else {
										target = finalPath
												.get(finalPath.size() - 1);
									}
								}
							}
						}
						else if(target == null){
							double tempLength = Double.MAX_VALUE;
							for(EntityID idd:finalPath){
								Area temp = (Area)model.getEntity(idd);
								if (tempLength > ScaleGeo
										.getDistance(new Point2D(me.getX(),me.getY()),new Point2D(temp.getX(),temp.getY()))) {
									tempLength = ScaleGeo
											.getDistance(new Point2D(me.getX(),me.getY()),new Point2D(temp.getX(),temp.getY()));
									target = idd;
								}
							}
						}
					}
					log.error("要返回的目的地" + target);
					errorPath = spTool.getPath(me.getPosition(), target, true);
					errorID = finalPath.get(0);
				}

			} else {
				log.error("走到了");
				state = JobState.Free;
				break;
			}
			// if(road.isBlockadesDefined()){//FIXME!!!没考虑周到！
			// log.error("有路障！");
			// state = JobState.LetMecc;
			// break;
			// }
			// if(stuckTool.isStuck()){
			// log.error("被卡住了！");
			// state = JobState.clean;
			// break;
			// }
		}
			break;
		// case LetMecc:
		// {
		//
		// }
		default:
			log.error("Undertermined state:" + state);
			break;
		}
		// case clean:
		// {
		// startWorking = true;
		// log.debug("当前状态 " + JobState.clean.toString());
		// if((clearPoint==null)&&(pointList==null)&&!(stillWorking)){
		// Road road = (Road)model.getEntity(me.getPosition());
		// if(ccTool.chooseClearMode(time, road)){
		// clearPoint = ccTool.tottalyClear(time, road);
		// log.debug("完全清障点 " + clearPoint.toString());
		// }
		// else{
		// pointList = ccTool.clearArea(time, road);
		// log.debug("清障三点 " + pointList.toString());
		// }
		// }
		// break;
		// }
		if ((myPath != null) && (state == JobState.goToDis)) {
			return new MoveCommand(time, myPath);
		} else if ((!finalPath.isEmpty()) && (finalPath.size() != 1)
				&& (state == JobState.onMoving) && (errorPath == null)) {
			return new MoveCommand(time, finalPath);
		} else if ((errorPath != null) && (errorID != me.getPosition())) {
			return new MoveCommand(time, errorPath);
		} else if ((finalPath.size() == 1) && (state == JobState.onMoving)) {
			if (agent.getAgentViewer() != null) {
				agent.getAgentViewer()
						.getMiscLayer()
						.addText(new Point2D(me.getX(), me.getY() + 10000),
								"社区遍历:完成！！", Color.red);
			}
			if (disList.size() != 0) {
				state = JobState.Free;
			} else {
				if (agent.getAgentViewer() != null) {
					agent.getAgentViewer()
							.getMiscLayer()
							.addText(new Point2D(me.getX(), me.getY() - 10000),
									"分区遍历:完成！！", Color.cyan);
				}
				return null;
			}
			return new MoveXYCommand(time, finalPath, -1, -1);
		} else {
			return new MoveXYCommand(time, finalPath, -1, -1);
		}

	}

	@Override
	public void overridden() {
		// TODO Auto-generated method stub

	}

}
