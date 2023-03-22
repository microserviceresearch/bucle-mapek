package es.upv.pros.microservices.rule.catalogue;

import java.util.ArrayList;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
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
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.json.JSONObject;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

public class Rule1 extends Rule{

	@Override
	public void execute(ModelInstance bigPicture, LocalChange change){
		ArrayList<FlowNode> toRemove = new ArrayList<FlowNode>();
		ArrayList<String> toRemoveIDs = new ArrayList<String>();
		String microservice=change.getMicrosercice();
		String newEvent = "";
		String target = "";
		String source = "";
		String sourceID = "";
		String targetID = "";
		double x = 0;
		double y = 0;
		FlowNode modifiedNode = null;
		FlowNode newSourceNode = null;
		
		Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
		for(Participant participant:participants){
			if(participant.getName().equalsIgnoreCase(microservice)) this.modifiedMicroservice=participant;
			else this.otherMicroservices.add(participant);
		}
		
		//TODO: Apply Rule 1 to the big picture
		if(change.getChange().equals("delete")) {
			for(Participant participant:participants){
				/*participant.setName(participant.getName()+"_MODIFIED");
				
				for(Task task:participant.getProcess().getChildElementsByType(Task.class)){
					task.setName(task.getName()+"_MODIFIED");
				}*/
				//Previous nodes and deleted node
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							//Modified microservice
							Query<FlowNode> previousNodes = n.getPreviousNodes();
							toRemove.add(n); //The deleted element by the modification
							toRemoveIDs.add(n.getId()); //The ID of the deleted element
							for(FlowNode nf:previousNodes.list()) {
								//Search for the previous event in the choreography
								previousNodes = nf.getPreviousNodes();
								nf = previousEvents(previousNodes);
								//nf is the communication element that contains the previous event
								newEvent = nf.getName();
								modifiedNode = nf; //modifiedNode contains the node in a global form
							}
						}
					}
					
					//Remove the sequence flow that has as a target the deleted element
					for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
						if(toRemoveIDs.contains(flows.getTarget().getId())) {
							toRemoveIDs.add(flows.getId());
						}
					}
				}
			}
			
			//Search for the complement of modifiedNode to update the MessageFlow
			for(Participant participant:participants) {
				for(FlowNode nf:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(!nf.equals(modifiedNode) &&  nf.getName()!=null && nf.getName().equals(modifiedNode.getName())) {
						newSourceNode = nf;
					}
				}
			}
			
			//Search for the complement of the deleted element
			for(Participant participant:participants) {
				for(FlowNode nf:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(!participant.getName().equals(change.getMicrosercice()) && nf.getName()!=null && nf.getName().equals(change.getModifiedEvent())) {
						target = nf.getName(); //Target has the name of the complement
						nf.setName(newEvent);  //The complement is modified to receive the new event
						source = participant.getName(); //The complement microservice
					}
				}
			}
			
			Collection<MessageFlow> mf=bigPicture.getModelElementsByType(MessageFlow.class);
			MessageFlow startReceiveEvent = null;
			String startReceiveEventID = "";
			for(MessageFlow msg:mf) {
				InteractionNode in = msg.getSource();
				FlowNode nf = (FlowNode) in;
				//Message interchanged between microservices
				if(nf.getName()!=null && nf.getName().equals(newEvent)) {
					sourceID = msg.getId();
				}
				if(nf.getName()!=null && nf.getName().equals(target)) {
					//If the newSourceNode is null, the modified event is the first event received by the choreography
					if(newSourceNode==null) {
						startReceiveEvent = msg;
						startReceiveEventID = msg.getId();
					}
					else {
						msg.setSource((InteractionNode) newSourceNode);
						targetID = msg.getId();
					}
				}
			}
			
			Collection<BpmnDiagram> BPMNdiagram = bigPicture.getModelElementsByType(BpmnDiagram.class);
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnShape> shapes = diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class);
				
				for(BpmnShape shape:shapes) {
					if(shape.getBpmnElement()!=null && toRemoveIDs.contains(shape.getBpmnElement().getId())) {
						diagram.getBpmnPlane().removeChildElement(shape);
					}
				}
				
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(sourceID)) {
						Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
						for(Waypoint waypoint:waypoints) {
							x = waypoint.getX();
							y = waypoint.getY();
							break;
						}
					}
				}
			}
			
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(targetID!=null && edge.getAttributeValue("bpmnElement").equals(targetID)) {
						if(x==0.0 && y==0.0) {
							diagram.getBpmnPlane().removeChildElement(edge);
						}
						else {
							Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
							for(Waypoint waypoint:waypoints) {
								waypoint.setX(x);
								waypoint.setY(y);
								break;
							}
						}
					}
				}
			}
			
			for(FlowNode nodeToRemove:toRemove) {
				if(nodeToRemove.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.EndEventImpl")) {
					String sequenceFlowID = "";
					for(Participant participant:participants) {
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemoveIDs.contains(flows.getId())) {
								sequenceFlowID = flows.getId();
							}
						}
						
						for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
							if(nodeToRemove.getId().equals(node.getId())) {
								participant.getProcess().removeChildElement(node);
							}
						}			
					}
					
					for(BpmnDiagram diagram:BPMNdiagram) {
						Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
						for(BpmnEdge edge:edges) {
							if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
								diagram.getBpmnPlane().removeChildElement(edge);
							}
						}
					}
					
					for(Participant participant:participants) {
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemoveIDs.contains(flows.getId())) {
								participant.getProcess().removeChildElement(flows);	
							}
						}
					}
					
				}else if(nodeToRemove.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl")) {
					FlowNode previousNode = null;
					String sequenceFlowID = "";
					for(Participant participant:participants) {
						for(FlowNode nodes:participant.getProcess().getChildElementsByType(FlowNode.class)) {
							if(toRemove.contains(nodes)) {
								Collection<FlowNode> previousNodes = nodes.getPreviousNodes().list();
								for(FlowNode node:previousNodes) {
									previousNode = node;
								}
							}
						}
					}
					
					for(Participant participant:participants) {
						for(SequenceFlow sequence:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(sequence.getSource().getId().equals(previousNode.getId())) {
								sequenceFlowID = sequence.getId();
							}
						}
					}
					
					for(BpmnDiagram diagram:BPMNdiagram) {
						Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
						for(BpmnEdge edge:edges) {
							if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
								Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
								for(Waypoint waypoint:waypoints) {
									x = waypoint.getX();
									y = waypoint.getY();
									break;
								}
							}
						}
						for(BpmnEdge edge:edges) {
							if(toRemoveIDs.contains(edge.getAttributeValue("bpmnElement"))) {
								diagram.getBpmnPlane().removeChildElement(edge);
							}
						}
					}
					
					for(Participant participant:participants) {
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemoveIDs.contains(flows.getId())) {
								participant.getProcess().removeChildElement(flows);
							}
						}
						
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemove.contains(flows.getSource())) {
								flows.setSource(previousNode);
								sequenceFlowID = flows.getId();
							}
						}
						
						for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
							if(nodeToRemove.getId().equals(node.getId())) {
								participant.getProcess().removeChildElement(node);
							}
						}
					}
					for(BpmnDiagram diagram:BPMNdiagram) {
						Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
						for(BpmnEdge edge:edges) {
							if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
								Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
								int i = 0;
								for(Waypoint waypoint:waypoints) {
									waypoint.setX(x);
									waypoint.setY(y);
									break;
								}
							}
						}
					}
				}
			}
			
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(startReceiveEventID)) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
				}
			}
			
			if(startReceiveEvent!=null) {
				Collection<Collaboration> collaborations = bigPicture.getModelElementsByType(Collaboration.class);
				for(Collaboration collaboration:collaborations) {
					collaboration.removeChildElement(startReceiveEvent);
				}
			}
		}else {		
			for(Participant participant:participants){
				//Previous nodes and deleted node
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							//Modified microservice
							Query<FlowNode> previousNodes = n.getPreviousNodes();
							for(FlowNode nf:previousNodes.list()) {
								//Search for the previous event in the choreography
								previousNodes = nf.getPreviousNodes();
								nf = previousEvents(previousNodes);
								//nf is the communication element that contains the previous event
								newEvent = nf.getName();
								modifiedNode = nf; //modifiedNode contains the node in a global form
							}
						}
					}
					
					//Remove the sequence flow that has as a target the deleted element
					for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
						if(toRemoveIDs.contains(flows.getTarget().getId())) {
							toRemoveIDs.add(flows.getId());
						}
					}
				}
			}
			
			//Search for the complement of modifiedNode to update the MessageFlow
			for(Participant participant:participants) {
				for(FlowNode nf:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(!nf.equals(modifiedNode) &&  nf.getName()!=null && nf.getName().equals(modifiedNode.getName())) {
						newSourceNode = nf;
					}
				}
			}
			
			//Search for the complement of the deleted element
			for(Participant participant:participants) {
				for(FlowNode nf:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(!participant.getName().equals(change.getMicrosercice()) && nf.getName()!=null && nf.getName().equals(change.getModifiedEvent())) {
						target = nf.getName(); //Target has the name of the complement
						nf.setName(newEvent);  //The complement is modified to receive the new event
						source = participant.getName(); //The complement microservice
					}
				}
			}
			
			Collection<MessageFlow> mf=bigPicture.getModelElementsByType(MessageFlow.class);
			MessageFlow startReceiveEvent = null;
			String startReceiveEventID = "";
			for(MessageFlow msg:mf) {
				InteractionNode in = msg.getSource();
				FlowNode nf = (FlowNode) in;
				//Message interchanged between microservices
				if(nf.getName()!=null && nf.getName().equals(newEvent)) {
					sourceID = msg.getId();
				}
				if(nf.getName()!=null && nf.getName().equals(target)) {
					//If the newSourceNode is null, the modified event is the first event received by the choreography
					if(newSourceNode==null) {
						startReceiveEvent = msg;
						startReceiveEventID = msg.getId();
					}
					else {
						msg.setSource((InteractionNode) newSourceNode);
						targetID = msg.getId();
					}
				}
			}
			
			Collection<BpmnDiagram> BPMNdiagram = bigPicture.getModelElementsByType(BpmnDiagram.class);
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnShape> shapes = diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class);
				
				for(BpmnShape shape:shapes) {
					if(shape.getBpmnElement()!=null && toRemoveIDs.contains(shape.getBpmnElement().getId())) {
						diagram.getBpmnPlane().removeChildElement(shape);
					}
				}
				
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(sourceID)) {
						Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
						for(Waypoint waypoint:waypoints) {
							x = waypoint.getX();
							y = waypoint.getY();
							break;
						}
					}
				}
			}
			
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(targetID!=null && edge.getAttributeValue("bpmnElement").equals(targetID)) {
						if(x==0.0 && y==0.0) {
							diagram.getBpmnPlane().removeChildElement(edge);
						}
						else {
							Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
							for(Waypoint waypoint:waypoints) {
								waypoint.setX(x);
								waypoint.setY(y);
								break;
							}
						}
					}
				}
			}
			
			for(FlowNode nodeToRemove:toRemove) {
				if(nodeToRemove.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.EndEventImpl")) {
					String sequenceFlowID = "";
					for(Participant participant:participants) {
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemoveIDs.contains(flows.getId())) {
								sequenceFlowID = flows.getId();
							}
						}
						
						for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
							if(nodeToRemove.getId().equals(node.getId())) {
								participant.getProcess().removeChildElement(node);
							}
						}			
					}
					
					for(BpmnDiagram diagram:BPMNdiagram) {
						Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
						for(BpmnEdge edge:edges) {
							if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
								diagram.getBpmnPlane().removeChildElement(edge);
							}
						}
					}
					
					for(Participant participant:participants) {
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemoveIDs.contains(flows.getId())) {
								participant.getProcess().removeChildElement(flows);	
							}
						}
					}
					
				}else if(nodeToRemove.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl")) {
					FlowNode previousNode = null;
					String sequenceFlowID = "";
					for(Participant participant:participants) {
						for(FlowNode nodes:participant.getProcess().getChildElementsByType(FlowNode.class)) {
							if(toRemove.contains(nodes)) {
								Collection<FlowNode> previousNodes = nodes.getPreviousNodes().list();
								for(FlowNode node:previousNodes) {
									previousNode = node;
								}
							}
						}
					}
					
					for(Participant participant:participants) {
						for(SequenceFlow sequence:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(sequence.getSource().getId().equals(previousNode.getId())) {
								sequenceFlowID = sequence.getId();
							}
						}
					}
					
					for(BpmnDiagram diagram:BPMNdiagram) {
						Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
						for(BpmnEdge edge:edges) {
							if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
								Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
								for(Waypoint waypoint:waypoints) {
									x = waypoint.getX();
									y = waypoint.getY();
									break;
								}
							}
						}
						for(BpmnEdge edge:edges) {
							if(toRemoveIDs.contains(edge.getAttributeValue("bpmnElement"))) {
								diagram.getBpmnPlane().removeChildElement(edge);
							}
						}
					}
					
					for(Participant participant:participants) {
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemoveIDs.contains(flows.getId())) {
								participant.getProcess().removeChildElement(flows);
							}
						}
						
						for(SequenceFlow flows:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
							if(toRemove.contains(flows.getSource())) {
								flows.setSource(previousNode);
								sequenceFlowID = flows.getId();
							}
						}
						
						for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
							if(nodeToRemove.getId().equals(node.getId())) {
								participant.getProcess().removeChildElement(node);
							}
						}
					}
					for(BpmnDiagram diagram:BPMNdiagram) {
						Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
						for(BpmnEdge edge:edges) {
							if(edge.getAttributeValue("bpmnElement").equals(sequenceFlowID)) {
								Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
								int i = 0;
								for(Waypoint waypoint:waypoints) {
									waypoint.setX(x);
									waypoint.setY(y);
									break;
								}
							}
						}
					}
				}
			}
			
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(startReceiveEventID)) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
				}
			}
			
			if(startReceiveEvent!=null) {
				Collection<Collaboration> collaborations = bigPicture.getModelElementsByType(Collaboration.class);
				for(Collaboration collaboration:collaborations) {
					collaboration.removeChildElement(startReceiveEvent);
				}
			}
		}
			
			
		System.out.println("Rule 1 is executed");
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
}
