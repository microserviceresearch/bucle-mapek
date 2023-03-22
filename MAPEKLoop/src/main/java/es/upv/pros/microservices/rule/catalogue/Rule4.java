package es.upv.pros.microservices.rule.catalogue;

import java.util.ArrayList;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.InteractionNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.di.Node;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.ModelInstance;
import org.json.JSONObject;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

public class Rule4 extends Rule{

	@Override
	public void execute(ModelInstance bigPicture, LocalChange change){
		String microservice=change.getMicrosercice();
		FlowNode affectedNode = null;
		FlowNode originalNode = null;
		FlowNode nf = null;
		String sourceID = "";
		String targetID = "";
		String messageID = "";
		String originalMicroservice = change.getMicrosercice();
		ArrayList<FlowNode> affectedNodes = new ArrayList<FlowNode>();
		ArrayList<String> toRemoveIDs = new ArrayList<String>();
		
		Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
						targetID = n.getId();
						toRemoveIDs.add(targetID);
						originalNode = n;
						affectedNode = nextEventsSend(n.getSucceedingNodes());
					}else {
						if(n.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl") || n.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.EndEventImpl")) {
							FlowNode previousNode = previousStartEvent(n.getPreviousNodes());
							//null Node is for exceptions
							if(previousNode==null) {
								ArrayList<String> boundaryEventTasks = new ArrayList<String>();
								for(FlowNode n2:participant.getProcess().getChildElementsByType(FlowNode.class)) {
									if(n2.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.BoundaryEventImpl")) {
										boundaryEventTasks.add(n2.getAttributeValue("attachedToRef"));
									}
								}
								for(FlowNode n2:participant.getProcess().getChildElementsByType(FlowNode.class)) {
									if(boundaryEventTasks.contains(n2.getId())) {
										previousNode = previousStartEvent(n2.getPreviousNodes());
										if(previousNode.equals(originalNode) && n.getName()!=null) {
											affectedNodes.add(n);
										}
									}
								}
							}
							if(previousNode.equals(originalNode)) {
								if(n.getName()!=null) affectedNodes.add(n);
							}
						}
					}
				}
			}
			else if(!participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(n.getName()!=null && n.getName().equals(change.getModifiedEvent())) {
						sourceID = n.getId();
					}
				}
			}
		}
		
		Collection<MessageFlow> messages = bigPicture.getModelElementsByType(MessageFlow.class);
		for(MessageFlow message:messages) {
			if(message.getSource().getId().equals(sourceID) && message.getTarget().getId().equals(targetID)) {
				messageID = message.getId();
			}
		}
		
		Boolean found = false;
		ArrayList<FlowNode> endEvents = new ArrayList<FlowNode>();
		for(FlowNode n:affectedNodes) {
			for(Participant participant:participants) {
				if(!participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n2:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n2.getName()!=null && n2.getName().equals(n.getName())) {
							found = true;
						}
					}
				}
			}
			if(!found) {
				endEvents.add(n);
			}
		}
		
		for(FlowNode n:affectedNodes) {
			change.setModifiedElement("SendEvent");
			change.setModifiedEvent(n.getName());
			change.setChange("deleteReceive");
			
			if(!n.getChildElementsByType(DataInputAssociation.class).isEmpty()) {
				try {
					Rule rule=(Rule)Class.forName("es.upv.pros.microservices.rule.catalogue.Rule3").newInstance();
					rule.execute(bigPicture, change);
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				try {
					Rule rule=(Rule)Class.forName("es.upv.pros.microservices.rule.catalogue.Rule1").newInstance();
					rule.execute(bigPicture, change);
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Collection<BpmnDiagram> BPMNdiagram = bigPicture.getModelElementsByType(BpmnDiagram.class);
		for(BpmnDiagram diagram:BPMNdiagram) {
			Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement").equals(messageID)) {
					Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
					for(Waypoint waypoint:waypoints) {
						waypoint.setX(0);
						waypoint.setY(0);
					}
				}
			}
		}
		
		String sequenceFlowID = "";
		for(Participant participant:participants) {
			if(participant.getName().equals(originalMicroservice)) {
				for(SequenceFlow sequences:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
					if(sequences.getSource()!=null && sequences.getSource().getName()!=null && sequences.getSource().getName().equals(originalNode.getName())) {
						sequenceFlowID = sequences.getId();
					}
				}
			}
		}
		
		for(BpmnDiagram diagram:BPMNdiagram) {
			Collection<BpmnShape> shapes = diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class);
			
			for(BpmnShape shape:shapes) {
				if(shape.getBpmnElement()!=null && toRemoveIDs.contains(shape.getBpmnElement().getId())) {
					diagram.getBpmnPlane().removeChildElement(shape);
				}
			}
			
			Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
					diagram.getBpmnPlane().removeChildElement(edge);
				}
				if(edge.getAttributeValue("bpmnElement").equals(messageID)) {
					diagram.getBpmnPlane().removeChildElement(edge);
				}
			}
		}
		
		for(Participant participant:participants) {
			for(SequenceFlow sequences:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
				if(sequences.getId().equals(sequenceFlowID)) {
					participant.getProcess().removeChildElement(sequences);
				}
			}
		}
		
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode nodes:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(nodes.getName()!=null && nodes.getName().equals(originalNode.getName())) {
						participant.getProcess().removeChildElement(nodes);
					}
				}
			}
		}
		
		Collection<Collaboration> collaborations = bigPicture.getModelElementsByType(Collaboration.class);
		for(Collaboration collaboration:collaborations) {
			Collection<MessageFlow> messageFlows = collaboration.getChildElementsByType(MessageFlow.class);
			for(MessageFlow message:messageFlows) {
				if(message.getId().equals(messageID)) {
					collaboration.removeChildElement(message);
				}
			}
		}
		
		
		System.out.println("Rule 4 is executed");
	}
	
	public FlowNode nextEventsSend(Query<FlowNode> nextNodes) {
		FlowNode res = null;
		for(FlowNode nf:nextNodes.list()) {
			if(nf.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl") || nf.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.EndEventImpl")) {
				return nf;
			}
			else {
				Query<FlowNode> nextNodes2 = nf.getSucceedingNodes();
				res = nextEventsSend(nextNodes2);
				
			}
		}
		return res;
	}
	
	public FlowNode previousEvents(Query<FlowNode> previousNodes) {
		FlowNode res = null;
		for(FlowNode nf:previousNodes.list()) {
			if(nf.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateCatchEventImpl") || nf.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl")) {
				return nf;
			}
			else {
				Query<FlowNode> previousNodes2 = nf.getPreviousNodes();
				res = previousEvents(previousNodes2);
				
			}
		}
		return res;
	}
	
	public FlowNode previousStartEvent(Query<FlowNode> previousNodes) {
		FlowNode res = null;
		for(FlowNode nf:previousNodes.list()) {
			if(nf.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl")) {
				return nf;
			}
			else {
				Query<FlowNode> previousNodes2 = nf.getPreviousNodes();
				res = previousStartEvent(previousNodes2);
				
			}
		}
		return res;
	}
}
