package es.upv.pros.microservices.rule.catalogue;

import java.util.Collection;

import org.camunda.bpm.model.bpmn.Query;
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
import org.camunda.bpm.model.bpmn.instance.di.Node;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.ModelInstance;
import org.json.JSONObject;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

public class Rule10 extends Rule{

	@Override
	public void execute(ModelInstance bigPicture, LocalChange change){
		String complement = "";
		
		Collection<Participant> participants=bigPicture.getModelElementsByType(Participant.class);
		
		for(Participant participant:participants) {
			if(participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(n.getName()!=null && n.getName().equals(change.getModifiedEvent())) {
						n.setName(change.getUpdatedEvent());
					}
				}
			}
		}
		
		for(Participant participant:participants) {
			if(!participant.getName().equals(change.getMicrosercice())) {
				for(FlowNode n:participant.getProcess().getChildElementsByType(FlowNode.class)) {
					if(n.getName()!=null && n.getName().equals(change.getModifiedEvent())) {
						n.setName(change.getUpdatedEvent());
						complement = participant.getName();
						
						change.setMicroservice(complement);
						change.setModifiedElement("SendEvent");
						Rule rule = null;
						try {
							rule = (Rule)Class.forName("es.upv.pros.microservices.rule.catalogue.Rule7").newInstance();
						} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						rule.execute(bigPicture, change);
					}
				}
			}
		}
		
		System.out.println("Rule 10 is executed");
	}
	
}
