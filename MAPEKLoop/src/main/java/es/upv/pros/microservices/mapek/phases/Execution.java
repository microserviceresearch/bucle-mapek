package es.upv.pros.microservices.mapek.phases;

import java.io.File;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.microservices.mapek.bpmn.BPMNManager;
import es.upv.pros.microservices.mapek.bpmn.LocalChange;
import es.upv.pros.microservices.mapek.files.FileManager;
import es.upv.pros.microservices.rule.catalogue.Rule;

@Component
public class Execution {

	@Autowired
	private FileManager fileManager;
	
	@Autowired
	private BPMNManager bpmnManager;
	
	public void run(Integer ruleNumber, LocalChange change) {
		try{
			BpmnModelInstance bigPicture=bpmnManager.getBigPicture();
			
			Rule rule=(Rule)Class.forName("es.upv.pros.microservices.rule.catalogue.Rule"+ruleNumber).newInstance();

			if(rule!=null){
				rule.execute(bigPicture, change);
				
				String filePath=System.getProperty("user.dir")+File.separator+"bpmn"+File.separator+"bigPictureModified.bpmn";
				fileManager.saveBPMNFile(filePath, Bpmn.convertToString(bigPicture));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
}
