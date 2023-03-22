package es.upv.pros.microservices.rule.catalogue;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.xml.ModelInstance;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;

public abstract class Rule {
	
	protected Participant modifiedMicroservice;
	protected List<Participant> otherMicroservices=new ArrayList<Participant>();

	public abstract void execute(ModelInstance bigPicture, LocalChange change);

}
