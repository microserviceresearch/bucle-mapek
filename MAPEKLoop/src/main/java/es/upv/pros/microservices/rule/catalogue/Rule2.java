package es.upv.pros.microservices.rule.catalogue;

import java.util.ArrayList;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.ModelInstance;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

public class Rule2 extends Rule{

	@Override
	public void execute(ModelInstance bigPicture, LocalChange change){
		String microservice=change.getMicrosercice();
		ArrayList<String> toRemoveIDs = new ArrayList<String>();
		ArrayList<FlowNode> toRemove=new ArrayList<FlowNode>();
		FlowNode nf = null;
		FlowNode nf2 = null;
		String firstSequence = "";
		String secondSequence = "";
		String thirdSequence = "";
		double x = 0;
		double y = 0;
		
		if(change.getChange().equals("delete")) {
			
			Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
			for(Participant participant:participants){
				if(participant.getName().equalsIgnoreCase(microservice)) this.modifiedMicroservice=participant;
				else this.otherMicroservices.add(participant);
			}
			
			//Search for the deleted element
			for(Participant participant:participants) {
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							nf = n;
							toRemove.add(nf);
							toRemoveIDs.add(nf.getId());
						}
					}
					for(SequenceFlow flow:participant.getProcess().getChildElementsByType(SequenceFlow.class)) {
						if(toRemoveIDs.contains(flow.getTarget().getId())) {
							toRemoveIDs.add(flow.getId());
						}
					}
				}
			}
			
			//Search for the affected elements
			for(Participant participant:participants) {
				if(!participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							//nf2 is an affected element
							nf2 = n;
							toRemove.add(nf2);
							toRemoveIDs.add(nf2.getId());
							if(nf2.getName().equals(nf.getName())) {
								Query<FlowNode> previousNodes = nf2.getPreviousNodes();
								Query<FlowNode> nextNodes = nf2.getSucceedingNodes();
								Collection<SequenceFlow> sequences = participant.getProcess().getChildElementsByType(SequenceFlow.class);
								for(SequenceFlow sequence:sequences) {
									for(FlowNode node:previousNodes.list()) {
										if(node.getId().equals(sequence.getSource().getId().toString()) && nf2.getId().equals(sequence.getTarget().getId().toString())) {
											for(FlowNode node2:nextNodes.list()) {
												sequence.setTarget(node2);
											}
											firstSequence = sequence.getId();
										}
									}
								}
								for(SequenceFlow sequence:sequences) {
									for(FlowNode node:nextNodes.list()) {
										if(nf2.getId().equals(sequence.getSource().getId().toString()) && node.getId().equals(sequence.getTarget().getId().toString())){
											for(FlowNode node2:previousNodes.list()) {
												sequence.setSource(node2);
											}
											secondSequence = sequence.getId();
										}
									}
								}
								Collection<MessageFlow> messages = bigPicture.getModelElementsByType(MessageFlow.class);
								for(MessageFlow message:messages) {
									if(nf.getId().equals(message.getSource().getId().toString()) && nf2.getId().equals(message.getTarget().getId().toString())){
										thirdSequence = message.getId();
									}
								}
							}
						}
					}
				}
			}	
			
			//Modify the visual structure of the diagram
			Collection<BpmnDiagram> BPMNdiagram = bigPicture.getModelElementsByType(BpmnDiagram.class);
			Collection<Collaboration> collaborations = bigPicture.getModelElementsByType(Collaboration.class);
			
			for(Collaboration collaboration:collaborations) {
				Collection<MessageFlow> messages = collaboration.getMessageFlows();
				for(MessageFlow message:messages) {
					if(toRemoveIDs.contains(message.getTarget().getId()) || toRemoveIDs.contains(message.getSource().getId())) {
						toRemoveIDs.add(message.getId());
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
					if(edge.getBpmnElement()!=null && toRemoveIDs.contains(edge.getBpmnElement().getId())) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
					if(edge.getAttributeValue("bpmnElement").equals(secondSequence)) {
						Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
						for(Waypoint waypoint:waypoints) {
							x = waypoint.getX();
							y = waypoint.getY();
						}
					}
				}
				
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(secondSequence)) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
				}
				
			}
			
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(firstSequence)) {
						Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
						int i = 0;
						for(Waypoint waypoint:waypoints) {
							if(i>0) {
								waypoint.setX(x);
								waypoint.setY(y);
							}
							i++;
						}
					}
					if(edge.getAttributeValue("bpmnElement").equals(thirdSequence)) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
				}
			}
			
			for(Participant participant:participants) {
				Collection<SequenceFlow> flows = participant.getProcess().getChildElementsByType(SequenceFlow.class);
				for(SequenceFlow flow:flows) {
					for(String id:toRemoveIDs) {
						if(flow.getTarget().getId().equals(id)) {
							participant.getProcess().removeChildElement(flow);
						}
					}
				}
			}
			
			for(Participant participant:participants) {
				Collection<FlowNode> nodes = participant.getProcess().getChildElementsByType(FlowNode.class);
				for(FlowNode node:nodes) {
					for(FlowNode nodeToRemove:toRemove) {
						if(nodeToRemove.getId().equals(node.getId())) {
							participant.getProcess().removeChildElement(node);
						}
					}
				}
			}
			
			for(Collaboration collaboration:collaborations) {
				Collection<MessageFlow> messages = collaboration.getMessageFlows();
				for(MessageFlow message:messages) {
					if(message.getTarget()==null || message.getSource()==null) {
						collaboration.removeChildElement(message);
					}
				}
			}
		}
		else {
			Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
			for(Participant participant:participants){
				if(participant.getName().equalsIgnoreCase(microservice)) this.modifiedMicroservice=participant;
				else this.otherMicroservices.add(participant);
			}
			
			//Search for the deleted element
			for(Participant participant:participants) {
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							nf = n;
						}
					}
				}
			}
			
			//Search for the affected elements
			for(Participant participant:participants) {
				if(!participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							//nf2 is an affected element
							nf2 = n;
							toRemove.add(nf2);
							toRemoveIDs.add(nf2.getId());
							if(nf2.getName().equals(nf.getName())) {
								Query<FlowNode> previousNodes = nf2.getPreviousNodes();
								Query<FlowNode> nextNodes = nf2.getSucceedingNodes();
								Collection<SequenceFlow> sequences = participant.getProcess().getChildElementsByType(SequenceFlow.class);
								for(SequenceFlow sequence:sequences) {
									for(FlowNode node:previousNodes.list()) {
										if(node.getId().equals(sequence.getSource().getId().toString()) && nf2.getId().equals(sequence.getTarget().getId().toString())) {
											for(FlowNode node2:nextNodes.list()) {
												sequence.setTarget(node2);
											}
											firstSequence = sequence.getId();
										}
									}
								}
								for(SequenceFlow sequence:sequences) {
									for(FlowNode node:nextNodes.list()) {
										if(nf2.getId().equals(sequence.getSource().getId().toString()) && node.getId().equals(sequence.getTarget().getId().toString())){
											for(FlowNode node2:previousNodes.list()) {
												sequence.setSource(node2);
											}
											secondSequence = sequence.getId();
										}
									}
								}
								Collection<MessageFlow> messages = bigPicture.getModelElementsByType(MessageFlow.class);
								for(MessageFlow message:messages) {
									if(nf.getId().equals(message.getSource().getId().toString()) && nf2.getId().equals(message.getTarget().getId().toString())){
										thirdSequence = message.getId();
									}
								}
							}
						}
					}
				}
			}	
			
			//Modify the visual structure of the diagram
			Collection<BpmnDiagram> BPMNdiagram = bigPicture.getModelElementsByType(BpmnDiagram.class);
			Collection<Collaboration> collaborations = bigPicture.getModelElementsByType(Collaboration.class);
			
			for(Collaboration collaboration:collaborations) {
				Collection<MessageFlow> messages = collaboration.getMessageFlows();
				for(MessageFlow message:messages) {
					if(toRemoveIDs.contains(message.getTarget().getId()) || toRemoveIDs.contains(message.getSource().getId())) {
						toRemoveIDs.add(message.getId());
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
					if(edge.getBpmnElement()!=null && toRemoveIDs.contains(edge.getBpmnElement().getId())) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
					if(edge.getAttributeValue("bpmnElement").equals(secondSequence)) {
						Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
						for(Waypoint waypoint:waypoints) {
							x = waypoint.getX();
							y = waypoint.getY();
						}
					}
				}
				
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(secondSequence)) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
				}
				
			}
			
			for(BpmnDiagram diagram:BPMNdiagram) {
				Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
				for(BpmnEdge edge:edges) {
					if(edge.getAttributeValue("bpmnElement").equals(firstSequence)) {
						Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
						int i = 0;
						for(Waypoint waypoint:waypoints) {
							if(i>0) {
								waypoint.setX(x);
								waypoint.setY(y);
							}
							i++;
						}
					}
					if(edge.getAttributeValue("bpmnElement").equals(thirdSequence)) {
						diagram.getBpmnPlane().removeChildElement(edge);
					}
				}
			}
			
			for(Participant participant:participants) {
				Collection<SequenceFlow> flows = participant.getProcess().getChildElementsByType(SequenceFlow.class);
				for(SequenceFlow flow:flows) {
					for(String id:toRemoveIDs) {
						if(flow.getTarget().getId().equals(id)) {
							participant.getProcess().removeChildElement(flow);
						}
					}
				}
			}
			
			for(Participant participant:participants) {
				Collection<FlowNode> nodes = participant.getProcess().getChildElementsByType(FlowNode.class);
				for(FlowNode node:nodes) {
					for(FlowNode nodeToRemove:toRemove) {
						if(nodeToRemove.getId().equals(node.getId())) {
							participant.getProcess().removeChildElement(node);
						}
					}
				}
			}
			
			for(Collaboration collaboration:collaborations) {
				Collection<MessageFlow> messages = collaboration.getMessageFlows();
				for(MessageFlow message:messages) {
					if(message.getTarget()==null || message.getSource()==null) {
						collaboration.removeChildElement(message);
					}
				}
			}
		}
		
		System.out.println("Rule 2 is executed");
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
	
	public FlowNode previousEventsSend(Query<FlowNode> previousNodes) {
		FlowNode res = null;
		for(FlowNode nf:previousNodes.list()) {
			if(nf.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl")) {
				return nf;
			}
			else {
				Query<FlowNode> previousNodes2 = nf.getPreviousNodes();
				res = previousEventsSend(previousNodes2);
				
			}
		}
		return res;
	}
}
