package com.sctrcd.buspassws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.easyrules.api.RulesEngine;
import org.easyrules.core.RulesEngineBuilder;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sctrcd.buspassws.facts.BusPass;
import com.sctrcd.buspassws.facts.Person;

@Service
public class BusPassService {

	private static Logger logger = LoggerFactory.getLogger(BusPassService.class);

	private final KieContainer kieContainer;

	@Autowired
	public BusPassService(KieContainer kieContainer, ApplicationContext context) {
		logger.info("Initialising a new bus pass session.");
		this.kieContainer = kieContainer;
		this.context = context;
	}

	private final ApplicationContext context;

	/**
	 * Create a new session, insert a person's details and fire rules to
	 * determine what kind of bus pass is to be issued.
	 */
	public BusPass getBusPass(Person person) {
		DummyRule dummyRule = context.getBean(DummyRule.class);
		RulesEngine rulesEngine = RulesEngineBuilder.aNewRulesEngine().build();
		rulesEngine.registerRule(dummyRule);
		rulesEngine.fireRules();
		KieSession kieSession = kieContainer.newKieSession("BusPassSession");
		kieSession.insert(person);
		kieSession.fireAllRules();
		BusPass busPass = findBusPass(kieSession);
		kieSession.dispose();
		return busPass;
	}

	/**
	 * Search the {@link KieSession} for bus passes.
	 */
	private BusPass findBusPass(KieSession kieSession) {

		// Find all BusPass facts and 1st generation child classes of BusPass.
		ObjectFilter busPassFilter = object -> BusPass.class.equals(object.getClass()) || BusPass.class.equals(object.getClass().getSuperclass());

		printFactsMessage(kieSession);

		List<BusPass> facts = new ArrayList<>();
		for(FactHandle handle : kieSession.getFactHandles(busPassFilter)){
			facts.add((BusPass) kieSession.getObject(handle));
		}
		if(facts.size() == 0){
			return null;
		}
		// Assumes that the rules will always be generating a single bus pass.
		return facts.get(0);
	}

	/**
	 * Print out details of all facts in working memory.
	 * Handy for debugging.
	 */
	private void printFactsMessage(KieSession kieSession) {
		Collection<FactHandle> allHandles = kieSession.getFactHandles();

		String msg = "\nAll facts:\n";
		for(FactHandle handle : allHandles){
			msg += "    " + kieSession.getObject(handle) + "\n";
		}
		System.out.println(msg);
	}

}
