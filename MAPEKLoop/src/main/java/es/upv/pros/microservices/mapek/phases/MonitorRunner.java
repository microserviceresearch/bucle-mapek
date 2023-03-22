package es.upv.pros.microservices.mapek.phases;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import es.upv.pros.microservices.mapek.bpmn.LocalChange;
import es.upv.pros.microservices.mapek.events.EventCallback;
import es.upv.pros.microservices.mapek.events.EventManager;

@Component
public class MonitorRunner implements ApplicationRunner {
	
	@Autowired
	private EventManager eventManager;
	
	@Autowired
	private Analysis analysis;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		//Delete a status event
		LocalChange lc = new LocalChange();
		lc.setChange("update");
		lc.setMicroservice("PAYMENT");
		lc.setModifiedElement("ReceiveEvent");
		lc.setModifiedEvent("Enough Stock");
		lc.setUpdatedEvent("VIP Customer");
		
		analysis.run(lc);
		
		// The following code monitors the event bus and call the Analysis phase
		// if the published change is delete
		/*eventManager.registerChangeListener(new EventCallback(){
			public void execute(String event){
				try {
					LocalChange change=LocalChange.parseJSON(event);
					if(change.getChange().equals("delete"))
						analysis.run(change);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});*/
	}

}
