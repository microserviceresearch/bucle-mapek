package es.upv.pros.microservices.mapek.phases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.impl.instance.FlowElementImpl;
import org.camunda.bpm.model.bpmn.instance.CatchEvent;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import es.upv.pros.microservices.mapek.bpmn.BPMNManager;
import es.upv.pros.microservices.mapek.bpmn.LocalChange;

@Component
public class Analysis{
	
	@Autowired
	private Planning planning;
	
	@Autowired
	private BPMNManager bpmnManager;
	
	public void run(LocalChange change) throws IOException, InterruptedException {
		String featureVector="";
		String[] events = new String[7];
		int i = 0;
		boolean search = false;
		FlowNode nf = null;
		FlowNode nf2 = null;
		FlowNode nf3 = null;
		
		//Get all events that participate in the composition
		BpmnModelInstance bigPicture=bpmnManager.getBigPicture();
		
		Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
		for(Participant participant:participants){
			for(StartEvent st:participant.getProcess().getChildElementsByType(StartEvent.class)) {
				events[i] = st.getName();
				i++;
			}
			for(IntermediateCatchEvent ice:participant.getProcess().getChildElementsByType(IntermediateCatchEvent.class)) {
				events[i] = ice.getName();
				i++;
			}
		}
		
		//F1: Modified element
		String modifiedElement = change.getModifiedElement();
		if(modifiedElement.contains("SendEvent")){
			featureVector = "0,";
		}else {
			featureVector = "1,";			
		}
		
		//F2: Change action
		String action = change.getChange();
		if(action.equals("delete")) {
			featureVector = featureVector.concat("0,");
		}else if(action.equals("update")) {
			featureVector = featureVector.concat("1,");
		}else {
			featureVector = featureVector.concat("2,");
		}
		
		//F3: The change introduces new messages
		if(action.equals("delete")) {
			featureVector = featureVector.concat("0,");
		}
		else {
			for(i=0;i<events.length;i++) {
				if(events[i].contains(change.getUpdatedEvent())) {
					featureVector = featureVector.concat("0,");
					search = true;
				}
			}
			if(!search) {
				featureVector = featureVector.concat("1,");				
			}
		}
		
		//F4: The message has attached data
		Boolean data = false;
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode node:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(node.getName()!=null && node.getName().equals(change.getModifiedEvent())) {
						/*Collection<FlowNode> previousNodes = node.getPreviousNodes().list();
						for(FlowNode previousNode:previousNodes) {
							Collection<DataInputAssociation> inputData = previousNode.getChildElementsByType(DataInputAssociation.class);
							if(!inputData.isEmpty()) {
								data = true;
							}
						}*/
						if(change.getModifiedElement().equals("SendEvent")) {
							Collection<DataInputAssociation> inputData = node.getChildElementsByType(DataInputAssociation.class);
							if(!inputData.isEmpty()) {
								data = true;
							}
						}
						else {
							Collection<DataOutputAssociation> outputData = node.getChildElementsByType(DataOutputAssociation.class);
							if(!outputData.isEmpty()) {
								data = true;
							}
						}
					}
				}
			}
			/*
			for(DataObjectReference obj:participant.getProcess().getChildElementsByType(DataObjectReference.class)) {
				data = true;
			}*/
		}
		
		if(data) {
			featureVector = featureVector.concat("1,");
		}else {
			featureVector = featureVector.concat("0,");
		}
		
		//F5: Type of update
		if(data) {
			featureVector = featureVector.concat("1,");
		}else {
			featureVector = featureVector.concat("0,");
		}
		
		//F6: Throw element affected
		if(change.getModifiedElement().equals("SendEvent")) {
			featureVector = featureVector.concat("0,");	
		}else {
			if(change.getModifiedElement().equals("ReceiveEvent") && change.getChange().equals("update")) {
				featureVector = featureVector.concat("0,");	
			}
			else featureVector = featureVector.concat("1,");	
		}
		
		//F7: Catch element affected
		if(change.getModifiedElement().equals("ReceiveEvent")) {
			if(change.getModifiedElement().equals("ReceiveEvent") && change.getChange().equals("update")) {
				featureVector = featureVector.concat("1,");	
			}
			else featureVector = featureVector.concat("0,");	
		}else {
			featureVector = featureVector.concat("1,");	
		}
		
		//F8 previous message send by the affected microservice
		if(change.getModifiedElement().equals("SendEvent")) {
			for(Participant participant:participants) {
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							Query<FlowNode> q = n.getPreviousNodes();
							if(q.list().isEmpty()) {
								nf = n;
							}
							else {
								nf = previousEvents(q);
							}
						}
					}
				}
			}
			
			for(Participant participant:participants) {
				if(!participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							Query<FlowNode> q = n.getPreviousNodes();
							if(q.list().isEmpty()) {
								nf2 = n;
							}
							else {
								nf2 = previousEventsSend(q);
							}
							if(nf2.getName().equals(nf.getName())) {
								featureVector = featureVector.concat("1");
							}
							else {
								featureVector = featureVector.concat("0");
							}
						}
					}
				}
			}	
		}else {
			for(Participant participant:participants) {
				if(participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(change.getModifiedEvent())) {
							Query<FlowNode> q = n.getSucceedingNodes();
							nf = nextEventsSend(q);
							nf3 = previousEvents(q);
						}
					}
				}
			}
			
			for(Participant participant:participants) {
				if(!participant.getName().equals(change.getMicrosercice())) {
					for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
						if(n.getName()!=null &&	n.getName().equals(nf.getName())) {
							Query<FlowNode> q = n.getPreviousNodes();
							if(q.list().isEmpty()) {
								nf2 = n;
							}
							else {
								nf2 = previousEventsSend(q);
							}
							if(nf2.getName().equals(nf3.getName())) {
								featureVector = featureVector.concat("1");
							}
							else {
								featureVector = featureVector.concat("0");
							}
						}
					}
				}
			}	
		}
		
		System.out.println(featureVector);
		planning.run(featureVector, change);
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
