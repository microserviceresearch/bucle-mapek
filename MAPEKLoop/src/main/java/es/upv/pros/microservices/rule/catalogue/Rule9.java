package es.upv.pros.microservices.rule.catalogue;

import java.util.ArrayList;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.impl.instance.SourceRef;
import org.camunda.bpm.model.bpmn.impl.instance.TargetRef;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
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
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.bpmn.instance.di.Node;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.ModelInstance;
import org.json.JSONObject;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

public class Rule9 extends Rule{

	@Override
	public void execute(ModelInstance bigPicture, LocalChange change){
		
		Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
		
		int dataContained = 0;
		int i = 0;
		
		String dataRef = "";
		String complementDataRef = "";
		String modifiedNodeDataRef = "";
		String possibleNode = "";
		FlowNode modifiedNode = null;
		FlowNode newSourceNode = null;
		
		ArrayList<String> toRemoveIDs = new ArrayList<String>();
		ArrayList<String> dataName = new ArrayList<String>();
		ArrayList<String> dataType = new ArrayList<String>();
		ArrayList<String> modifiedDataRef = new ArrayList<String>();
		ArrayList<String> modifiedNodeName = new ArrayList<String>();
		ArrayList<String> possibleNodes = new ArrayList<String>();
		
		FlowNode nextNode = null;
		boolean found = false;
		
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				Collection<FlowNode> nodes = participant.getProcess().getChildElementsByType(FlowNode.class);
				for(FlowNode node:nodes) {
					if(node.getName()!=null && node.getName().equals(change.getModifiedEvent())) {
						node.setName(change.getUpdatedEvent());
						Collection <DataInputAssociation> dataInput = node.getChildElementsByType(DataInputAssociation.class);
						for(DataInputAssociation data:dataInput) {
							Collection<SourceRef> sourceRef = data.getChildElementsByType(SourceRef.class);
							for(SourceRef s:sourceRef) {
								dataRef = s.getRawTextContent(); //dataRef is the reference to the data object 
							}
						}
					}
				}
			}else if(!participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
						Collection <DataOutputAssociation> dataOutput = n.getChildElementsByType(DataOutputAssociation.class);
						for(DataOutputAssociation data:dataOutput) {
							Collection<TargetRef> targetRef = data.getChildElementsByType(TargetRef.class);
							for(TargetRef s:targetRef) {
								complementDataRef = s.getRawTextContent(); //the reference to the data object received by the complement
							}
						}
					}
				}
			}
		}
		
		//Data contained in the updated event: dataName and dataType
		for(Participant participant:participants) {
				Collection <DataObjectReference> datas = participant.getProcess().getChildElementsByType(DataObjectReference.class);
				for(DataObjectReference data:datas) {
					if(data.getId().equals(complementDataRef)) {
						Collection<ExtensionElements> extensions = data.getChildElementsByType(ExtensionElements.class);
						for(ExtensionElements extension:extensions) {
							Collection<CamundaProperties> properties = extension.getChildElementsByType(CamundaProperties.class);
							for(CamundaProperties pp:properties) {
								Collection<CamundaProperty> property = pp.getChildElementsByType(CamundaProperty.class);
								for(CamundaProperty p:property) {
									dataName.add(p.getCamundaName());
									dataType.add(p.getCamundaValue());
								}
							}
						}
					}
				}
		}
		
		//Identify the modifiedNode to maintain the integrity of the affected microservices
		for(Participant participant:participants) {
			Collection<DataObjectReference> datas = participant.getProcess().getChildElementsByType(DataObjectReference.class);
			for(DataObjectReference data:datas) {
				dataContained = 0;
					Collection<ExtensionElements> extensions = data.getChildElementsByType(ExtensionElements.class);
					for(ExtensionElements extension:extensions) {
						Collection<CamundaProperties> properties = extension.getChildElementsByType(CamundaProperties.class);
						for(CamundaProperties pp:properties) {
							Collection<CamundaProperty> property = pp.getChildElementsByType(CamundaProperty.class);
							for(CamundaProperty p:property) {
								if(dataName.contains(p.getCamundaName())) {
									i = dataName.indexOf(p.getCamundaName());
									if(dataType.get(i).equals(p.getCamundaValue())) {
										dataContained++;
									}
								}
							}
						}
					}
					if(dataContained == dataName.size()) {
						modifiedDataRef.add(data.getId());
					}
			}
		}
		
		if(!dataRef.equals(complementDataRef)) {
			modifiedDataRef.remove(complementDataRef);
			toRemoveIDs.add(complementDataRef);
		}
		
		if(modifiedDataRef.isEmpty()) {
			System.out.println("No rule can be applied.");
		}else {
		
		boolean updatedNodeData = false;
		//For each possible node, we obtain the name
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				Collection<FlowNode> nodes = participant.getProcess().getChildElementsByType(FlowNode.class);
				for(FlowNode node:nodes) {
					if(node.getName()!=null && node.getName().equals(change.getUpdatedEvent())) {
						Collection <DataInputAssociation> dataInput = node.getChildElementsByType(DataInputAssociation.class);
						for(DataInputAssociation data:dataInput) {
							Collection<SourceRef> sourceRef = data.getChildElementsByType(SourceRef.class);
							for(SourceRef s:sourceRef) {
								if(modifiedDataRef.contains(s.getRawTextContent())) {
									modifiedNodeName.add(node.getName());
									updatedNodeData = true;
								}
							}
						}
					}else {
						Collection <DataOutputAssociation> dataOutput = node.getChildElementsByType(DataOutputAssociation.class);
						for(DataOutputAssociation data:dataOutput) {
							Collection<TargetRef> targetRef = data.getChildElementsByType(TargetRef.class);
							for(TargetRef s:targetRef) {
								if(modifiedDataRef.contains(s.getRawTextContent())) {
									modifiedNodeName.add(node.getName());
								}
							}
						}
					}
				}
			}else {
				Collection<FlowNode> nodes = participant.getProcess().getChildElementsByType(FlowNode.class);
				for(FlowNode node:nodes) {				
					Collection <DataOutputAssociation> dataOutput = node.getChildElementsByType(DataOutputAssociation.class);
					for(DataOutputAssociation data:dataOutput) {
						Collection<TargetRef> targetRef = data.getChildElementsByType(TargetRef.class);
						for(TargetRef s:targetRef) {
							if(modifiedDataRef.contains(s.getRawTextContent())) {
								modifiedNodeName.add(node.getName());
							}
						}
					}
				}
			}
		}
		
		if(!updatedNodeData) {
			for(int j = 0; j<modifiedNodeName.size();j++) {
				possibleNode = modifiedNodeName.get(j);
				found = false;
				while(!found) {
					nextNode = null;
					for(Participant participant:participants) {
						if(!found) {
							for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
								if(node.getName()!=null && node.getName().equals(possibleNode) && (node.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateCatchEventImpl") || node.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl"))) {
									nextNode = nextEventsSend(node.getSucceedingNodes());
									possibleNode = nextNode.getName();
									if(possibleNode.equals(change.getUpdatedEvent())) {
										found = true;
									}
								}
							}
						}
					}
				}
				if(found) possibleNodes.add(modifiedNodeName.get(j));
			}
		
			//Modified node found
			String participantName = "";
			
			for(Participant participant:participants) {
				Collection<FlowNode> nodes = participant.getProcess().getChildElementsByType(FlowNode.class);
				for(FlowNode node:nodes) {
					if(node.getName()!=null && possibleNodes.contains(node.getName()) && (node.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.IntermediateCatchEventImpl") || node.getClass().toString().equals("class org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl"))) {
						modifiedNode = node;
						participantName = participant.getName();
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
		
			//Reference to the data contained in the Modified Node
			for(Participant participant:participants){
				//The reference to the data that has been deleted
				if(participant.getName().equals(participantName)) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(modifiedNode.getName())) {
							Collection <DataOutputAssociation> dataOutput = n.getChildElementsByType(DataOutputAssociation.class);
							for(DataOutputAssociation data:dataOutput) {
								modifiedNodeDataRef = data.getId();
							}
						}
					}
				}
			}
		}else {
			for(Participant participant:participants) {
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(node.getName()!=null && node.getName().equals(change.getUpdatedEvent())) {
							modifiedNode = node;
						}
					}
				}
			}
			
			for(Participant participant:participants){
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(modifiedNode.getName())) {
							Collection <DataInputAssociation> dataInput = n.getChildElementsByType(DataInputAssociation.class);
							for(DataInputAssociation data:dataInput) {
								modifiedNodeDataRef = data.getId();
							}
						}
					}
				}
			}
			
			newSourceNode = modifiedNode;
		}
		
		//Set the new event to be received by the affected microservices
		String targetDataRef = "";
		String target = "";
		String source = "";
		
		for(Participant participant:participants) {
			for(FlowNode nf:participant.getProcess().getChildElementsByType(FlowNode.class)) {
				if(!participant.getName().equals(change.getMicrosercice()) && nf.getName()!=null && nf.getName().equals(change.getModifiedEvent())) {
					target = nf.getName();
					nf.setName(modifiedNode.getName());
					source = participant.getName();
					Collection <DataOutputAssociation> dataOutput = nf.getChildElementsByType(DataOutputAssociation.class);
					for(DataOutputAssociation data:dataOutput) {
							targetDataRef = data.getId();
							Collection<TargetRef> targetRef = data.getChildElementsByType(TargetRef.class);
							for(TargetRef s:targetRef) {
								Collection <DataOutputAssociation> dataOutput2 = modifiedNode.getChildElementsByType(DataOutputAssociation.class);
								for(DataOutputAssociation data2:dataOutput2) {
									Collection<TargetRef> targetRef2 = data2.getChildElementsByType(TargetRef.class);
									for(TargetRef s2:targetRef2) {
										s.setTextContent(s2.getRawTextContent());
									}
								}
								
								Collection <DataInputAssociation> dataInput2 = modifiedNode.getChildElementsByType(DataInputAssociation.class);
								for(DataInputAssociation data2:dataInput2) {
									Collection<SourceRef> sourceRef2 = data2.getChildElementsByType(SourceRef.class);
									for(SourceRef s2:sourceRef2) {
										s.setTextContent(s2.getRawTextContent());
									}
								}
							}
						}
					}
				}
			}
		
		//Change the message flow of the choreography
		Collection<MessageFlow> mf=bigPicture.getModelElementsByType(MessageFlow.class);
		MessageFlow startReceiveEvent = null;
		String startReceiveEventID = "";
		String sourceID = "";
		String targetID = "";
		
		for(MessageFlow msg:mf) {
			InteractionNode in = msg.getSource();
			FlowNode nf = (FlowNode) in;
			//Message interchanged between microservices
			if(nf.getName()!=null && nf.getName().equals(modifiedNode.getName())) {
				sourceID = msg.getId();
			}
			if(nf.getName()!=null && nf.getName().equals(change.getUpdatedEvent())) {
				//If the newSourceNode is null, the modified event is the first event received by the choreography
				if(newSourceNode==null) {
					startReceiveEvent = msg;
					startReceiveEventID = msg.getId();
					toRemoveIDs.add(msg.getId());
				}
				else {
					msg.setSource((InteractionNode) newSourceNode);
					targetID = msg.getId();
				}
			}
		}
		
		double x = 0.0;
		double y = 0.0;
		double x2 = 0.0;
		double y2 = 0.0;
		
		Collection<BpmnDiagram> BPMNdiagram = bigPicture.getModelElementsByType(BpmnDiagram.class);
		for(BpmnDiagram diagram:BPMNdiagram) {
			
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
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement").equals(modifiedNodeDataRef) && !updatedNodeData) {
					Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
					for(Waypoint waypoint:waypoints) {
						x2 = waypoint.getX();
						y2 = waypoint.getY();
					}
				}else if(edge.getAttributeValue("bpmnElement").equals(modifiedNodeDataRef) && updatedNodeData) {
					Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
					for(Waypoint waypoint:waypoints) {
						x2 = waypoint.getX();
						y2 = waypoint.getY();
						break;
					}
				}
			}
		}
		
		double x3 = 0.0;
		double y3 = 0.0;
		
		for(BpmnDiagram diagram:BPMNdiagram) {
			Collection<BpmnShape> shapes = diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class);
			
			for(BpmnShape shape:shapes) {
				if(shape.getBpmnElement()!=null && toRemoveIDs.contains(shape.getBpmnElement().getId())) {
					diagram.getBpmnPlane().removeChildElement(shape);
				}
			}
			
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
			
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement").equals(targetDataRef)) {
					Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
					int j = 0;
					if(updatedNodeData) {
						for(Waypoint waypoint:waypoints) {
							x3 = waypoint.getX();
							y3 = waypoint.getY();
							break;
						}
					}
				}
			}
			
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement").equals(targetDataRef)) {
					Collection<Waypoint> waypoints = edge.getChildElementsByType(Waypoint.class);
					int j = 0;
					if(updatedNodeData) {
						for(Waypoint waypoint:waypoints) {
							if(j==0) {
								waypoint.setX(x3);
								waypoint.setY(y3);
							}
							else {
								waypoint.setX(x2);
								waypoint.setY(y2);
							}
							j++;
						}
					}else {
						for(Waypoint waypoint:waypoints) {
							if(j>0) {
								waypoint.setX(x2);
								waypoint.setY(y2);
							}
							j++;
						}
					}
				}
			}
			
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement").equals(complementDataRef)) {
					diagram.getBpmnPlane().removeChildElement(edge);
				}
			}
		}
		
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				Collection<DataObjectReference> objects = participant.getProcess().getChildElementsByType(DataObjectReference.class);
				for(DataObjectReference object:objects) {
					if(toRemoveIDs.contains(object.getId())) {
						toRemoveIDs.add(object.getAttributeValue("dataObjectRef"));
						participant.getProcess().removeChildElement(object);
					}
				}
				
				Collection<DataObject> dataObjects = participant.getProcess().getChildElementsByType(DataObject.class);
				for(DataObject object:dataObjects) {
					if(toRemoveIDs.contains(object.getId())) {
						participant.getProcess().removeChildElement(object);
					}
				}
			}
		}
		
		for(BpmnDiagram diagram:BPMNdiagram) {
			Collection<BpmnEdge> edges = diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class);
			for(BpmnEdge edge:edges) {
				if(edge.getAttributeValue("bpmnElement")!=null && edge.getAttributeValue("bpmnElement").equals(startReceiveEventID)) {
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
		
		System.out.println("Rule 9 is executed");
		}
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
	
}
