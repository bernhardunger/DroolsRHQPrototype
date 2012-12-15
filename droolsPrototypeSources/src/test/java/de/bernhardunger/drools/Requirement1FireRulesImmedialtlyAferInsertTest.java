package de.bernhardunger.drools;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
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
 * Events will be fired directly after the insertion of each events.
 * @author Bernhard Unger
 * 
 */
public class Requirement1FireRulesImmedialtlyAferInsertTest {

	Logger logger = LoggerFactory.getLogger(Requirement1FireRulesImmedialtlyAferInsertTest.class);

	private StatefulKnowledgeSession ksession;

	private KnowledgeRuntimeLogger knwlgLogger;

	private long startTime;

	private long endTime;

	private List<Statistic> statistics = new ArrayList<Statistic>();

	int noOfEvents = 1000000;

	int totalEvents = 0;

	int eventId = 0;

	@Before
	public void prepare() throws IOException, InterruptedException {

		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("Jconsole -interval=1");
		Thread.sleep(5000);

	}

	@Test
	public void start() {
		startTime = new Date().getTime();
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		// Load rules from file
		kbuilder.add(new ClassPathResource("requirement1.drl", getClass()), ResourceType.DRL);

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
		ksession.setGlobal("resultList", resultList);

		// Logging is a performance issue, so disabling for exhaustive tests is
		// neccessary
		// knwlgLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession,
		// "logs/myKnowledgeLogfile");

		// Insert 3 Appserver with status down
		for (int i = 1; i <= 3; i++) {
			insertEvent("app server is down", i);
			endTime = new Date().getTime();
			saveStatisticEntry(i);
			ksession.fireAllRules();
		}
		// Insert dummy events
		for (int i = 1; i <= noOfEvents; i++) {
			insertEvent("dummy event", i);
			endTime = new Date().getTime();
			saveStatisticEntry(i);
			ksession.fireAllRules();
		}
		// Insert 5 Appserver with status up
		for (int step = 1; step <= 5; step++) {
			insertEvent("app server is up", step);
			endTime = new Date().getTime();
			saveStatisticEntry(step);
			ksession.fireAllRules();
		}

		resultList = (List<RuleResult>) ksession.getGlobal("resultList");
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
		ksession.halt();
		ksession.dispose();
		printStatistics();
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

	/**
	 * Save the statistic entry
	 * 
	 * @param step
	 */
	private void saveStatisticEntry(int step) {

		long execTime = endTime - startTime;

		MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();

		OperatingSystemMXBean osMBean = null;
		try {
			osMBean = ManagementFactory.newPlatformMXBeanProxy(mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
					OperatingSystemMXBean.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		double systemLoadAverage = osMBean.getSystemLoadAverage();
		Statistic statistic = new Statistic(step, totalEvents, execTime, systemLoadAverage);
		statistics.add(statistic);
	}

	/**
	 * Log statistics
	 */
	private void printStatistics() {

		logger.debug("Step;" + "Total Events;" + "Events/ms;" + "Execution Time;" + "Max CPU usage");
		for (Statistic stat : statistics) {
			logger.debug(stat.getStep() + ";" + stat.getNoOfEvents() + ";" + (double) stat.getEventsPerSecond() + ";"
					+ stat.getExecTime() + ";" + stat.getAverageCpuLoad());
		}

	}

}
