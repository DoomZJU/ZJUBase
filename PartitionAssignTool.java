package scale.tool.map.partition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleTool;
import scale.tool.ScaleToolURN;
import scale.tool.map.partition.TestPartition;
import scale.utils.geo.ScaleGeo;

/**
 * 
 * 分配工具
 * 
 * @author Doom
 * 
 */

public class PartitionAssignTool extends ScaleTool {

	private static final Log log = LogFactory.getLog(PartitionAssignTool.class);

	public PartitionAssignTool() {
		super(ScaleToolURN.PartitionAssignTool);
	}

	// 生成有权图
	private WeightedGraph<Point2D, DefaultWeightedEdge> createWeightedGraph(
			List<Point2D> agentList, List<Point2D> aimList) {
		WeightedGraph<Point2D, DefaultWeightedEdge> g = new SimpleWeightedGraph<Point2D, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		// 添加node
		for (Point2D vertex : aimList) {
			g.addVertex(vertex);
		}
		for (Point2D vertex : agentList) {
			g.addVertex(vertex);
		}

		// 添加Edge
		for (int i = 0; i < agentList.size(); i++) {
			for (int j = 0; j < aimList.size(); j++) {
				g.addEdge(agentList.get(i), aimList.get(j));
				double w = ScaleGeo.getDistance(agentList.get(i),
						aimList.get(j));
				g.setEdgeWeight(g.getEdge(agentList.get(i), aimList.get(j)), w);
			}
		}
		return g;
	}

	// 算法主体部分
	public Map<StandardEntity, TestPartition> getMatching(
			Collection<StandardEntity> agentSet,
			Collection<TestPartition> partitionSet) {

		if (agentSet.size() != partitionSet.size()) {
			log.error("Agent set size not equals partition set size");
			return null;
		}

		Map<StandardEntity, TestPartition> match = new HashMap<StandardEntity, TestPartition>();

		Map<StandardEntity, Point2D> agentMap = new HashMap<StandardEntity, Point2D>();
		List<Point2D> agentList = new ArrayList<Point2D>();
		for (StandardEntity agent : agentSet) {
			Pair<Integer, Integer> loc = agent.getLocation(model);
			Point2D p = new Point2D(loc.first(), loc.second());
			agentMap.put(agent, p);
			agentList.add(p);
		}

		Map<Point2D, TestPartition> partitionMap = new HashMap<Point2D, TestPartition>();
		List<Point2D> aimList = new ArrayList<Point2D>();
		for (TestPartition partition : partitionSet) {
			Point2D p = partition.getCenter();
			partitionMap.put(p, partition);
			aimList.add(p);
		}

		WeightedGraph<Point2D, DefaultWeightedEdge> weightedGraph = createWeightedGraph(
				agentList, aimList);
		Map<Point2D, Point2D> map = new HashMap<Point2D, Point2D>();
		log.error(weightedGraph.toString());
		log.error(aimList.toString());
		log.error(agentList.toString());

		KuhnMunkresMinimalWeightBipartitePerfectMatching<Point2D, DefaultWeightedEdge> matchALg = new KuhnMunkresMinimalWeightBipartitePerfectMatching<Point2D, DefaultWeightedEdge>(
				weightedGraph, aimList, agentList);
		Set<DefaultWeightedEdge> edges = matchALg.getMatching();
		for (DefaultWeightedEdge edg : edges) {
			Point2D s = weightedGraph.getEdgeSource(edg);
			Point2D t = weightedGraph.getEdgeTarget(edg);
			map.put(s, t);
		}

		for (StandardEntity eid : agentSet) {
			Point2D s = agentMap.get(eid);
			Point2D t = map.get(s);
			TestPartition p = partitionMap.get(t);
			match.put(eid, p);
		}

		return match;
	}

	@Override
	protected boolean initialize() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean update(int time, ChangeSet changes,
			Collection<ScaleMessage> messages) {
		// TODO Auto-generated method stub
		return false;
	}

}
