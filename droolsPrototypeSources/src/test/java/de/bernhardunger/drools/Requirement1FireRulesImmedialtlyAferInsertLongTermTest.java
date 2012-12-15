package de.bernhardunger.drools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.io.impl.ClassPathResource;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.rule.FactHandle;
import org.drools.time.SessionPseudoClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bernhardunger.drools.model.EventComposite;
import de.bernhardunger.drools.model.RuleResult;
import de.bernhardunger.drools.util.Statistic;

import static de.bernhardunger.drools.util.EventFatory.*;

/**
 * Unit-Test of requirement 1: Group Alarms should be generated
 * by any system events with boolean conjunction and absolute or relative range
 * of values of system events.
 * For decreasing of used heap memory, no statistic data will be saved into a list. 
 * 2000000 Events will be inserted in the knowledge session.
 * @author Bernhard Unger
 * 
 */
public class Requirement1FireRulesImmedialtlyAferInsertLongTermTest {

	Logger logger = LoggerFactory.getLogger(Requirement1FireRulesImmedialtlyAferInsertLongTermTest.class);

	private StatefulKnowledgeSession ksession;

	private KnowledgeRuntimeLogger knwlgLogger;

	private long startTime;

	private long endTime;

	int noOfEvents = 2000000;

	int totalEvents = 0;

	int eventId = 0;

	@Before
	public void prepare() throws IOException, InterruptedException {

		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("Jconsole -interval=1");
		Thread.sleep(5000);

	}

	@Test
	public void start() throws InterruptedException {
		startTime = new Date().getTime();
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		// Load rules from file
		kbuilder.add(new ClassPathResource("requirement1LongTerm.drl", getClass()), ResourceType.DRL);

		if (kbuilder.hasErrors()) {
			if (kbuilder.getErrors().size() > 0) {
				for (KnowledgeBuilderError kerror : kbuilder.getErrors()) {
					System.err.println(kerror);
				}
			}
		}

		KnowledgeBaseConfiguration config = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
		// Drools Fusion needs STREAMING
		config.setOption(EventProcessingOption.STREAM);

		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(config);
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());

		KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
		// Control the clock for test instead of system time
		conf.setOption(ClockTypeOption.get("pseudo"));
		ksession = kbase.newStatefulKnowledgeSession(conf, null);
		List<RuleResult> resultList = new ArrayList<RuleResult>();

		for (int i = 1; i <= noOfEvents; i++) {
			// Insert Appserver with status up
			insertEvent("app server is up", i);
			// Insert Appserver with status down
			insertEvent("app server is down", i);
			// Insert dummy event with status down
			insertEvent("dummy event", i);
			
			//saveStatisticEntry(i);
			ksession.fireAllRules();
			System.out.println("Inserted: " + i * 3+ " Events");
		}
		//resultList = (List<RuleResult>) ksession.getGlobal("resultList");
		// Assert.assertEquals(1, resultList.size());
		FactHandle handle = insertEvent("db connection in use", 10);
		ksession.fireAllRules();
		// Remove db connection
		ksession.retract(handle);
		ksession.fireAllRules();
	}

	/**
	 * Will be executed when the last test has been finished
	 */
	@After
	public void release() {
		// knwlgLogger.close();
		endTime = new Date().getTime();
		long execTime = endTime - startTime;
		ksession.halt();
		ksession.dispose();
		System.out.println("Total time: " + execTime);
	}

	/**
	 * Insert the events facts into the knowledge session
	 * 
	 */
	private FactHandle insertEvent(String eventDetail, int stepId) {
		SessionPseudoClock clock = ksession.getSessionClock();
		EventComposite event;
		long timestamp = clock.advanceTime(1, TimeUnit.SECONDS);
		event = makeSimpleEvent(eventDetail, stepId, eventId, timestamp);
		FactHandle handle = ksession.insert(event);
		eventId++;
		totalEvents++;
		return handle;
	}

	



}
