package scale.tool.PF;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import scale.agent.ScaleAgent;
import scale.liaison.message.ScaleMessage;
import scale.tool.ScaleTool;
import scale.tool.ScaleToolURN;
import scale.tool.map.partition.CreateTestPartitionTool;
import scale.tool.map.partition.PartitionAssignTool;
import scale.tool.map.partition.TestPartition;

/**
 * 
 * 分配工具
 * 
 * @author Doom
 * 
 */

public class PFAssignTool extends ScaleTool {
	private static final Log log = LogFactory.getLog(PFAssignTool.class);
	private ScaleAgent<? extends Human> agent;
	private CreateTestPartitionTool cpTool;
	private PartitionAssignTool paTool;
	private TestPartition partition = null;

	public PFAssignTool() {
		super(ScaleToolURN.PFAssignTool);
	}

	@Override
	protected boolean initialize() {
		agent = (ScaleAgent<? extends Human>) element;
		
		cpTool = (CreateTestPartitionTool) toolKit
				.getTool(ScaleToolURN.CreateTestPartitionTool);
		paTool = (PartitionAssignTool) toolKit.getTool(ScaleToolURN.PartitionAssignTool);
		
		// 收集全体PF进List
		Set<StandardEntity> agentSet = new HashSet<StandardEntity>();
		for (StandardEntity se : model
				.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
			agentSet.add(se);
		}
		log.debug(agentSet.toString());
		
		// Partition set
		log.debug(cpTool.getPartitions());
		
		cpTool.create(agentSet.size());

		// Match
		Map<StandardEntity, TestPartition> match = paTool.getMatching(agentSet, cpTool.getPartitions());
		log.debug(match);
		
		if (match != null) {
			partition = match.get(model.getEntity(agent.getID()));
			if (partition == null)
				log.error("Cannot find corresponding partition.");
			log.error(match.toString());
			log.error(agent);
			log.error(partition);
		}
		else {
			log.error("Matching failed");
			return false;
		}
		
		return true;
	}

	@Override
	public boolean update(int time, ChangeSet changes,
			Collection<ScaleMessage> messages) {
		//TODO:
		return true;
	}

	public TestPartition getPartition() {
		return partition;
	}
}
