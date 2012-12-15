package de.bernhardunger.drools.jms;

import java.io.IOException;
import java.util.Date;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.drools.logger.KnowledgeRuntimeLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import de.bernhardunger.drools.model.EventComposite;
import static de.bernhardunger.drools.util.EventFatory.*;

/**
 * Unit-Test puts events for requirement 1 to a JMS topic or
 * queue with no filtering of events. 
 * Pre-Requirements: An active JMS-Provider like ActiveMQ for processing if JMS messages.
 * 
 * @author Bernhard Unger
 * 
 */
public class Requirement1JMSSenderTest {
	
	private int noOfEvents = 100;
	private int eventId = 0;
	private int resourceIdAppServer = 1;
	private int resourceIdDummy = 2;
	private int resourceDBConnection = 3;
	private ApplicationContext context;
	private JmsTemplate jmsTemplate;
	private long startTime;
	private long endTime;
	private PrototypeJMSReceiver prototype;

	Logger logger = LoggerFactory.getLogger(Requirement1JMSSenderTest.class);

	@Before
	public void prepare() throws IOException, InterruptedException {

		prototype = new PrototypeJMSReceiver();
		prototype.setDrlResource("/de/bernhardunger/drools/jms/requirement1.drl");
		prototype.setUseFilter(false);
		// Filter can be used with:
		//prototype.setUseFilter(true);
		//prototype.setFilterExpression("dummy event");
		prototype.start(new String[] {});
		Runtime rt = Runtime.getRuntime();
		rt.exec("Jconsole -interval=1");
		Thread.sleep(10000);
		context = new ClassPathXmlApplicationContext("sender-context.xml");
		jmsTemplate = (JmsTemplate) context.getBean("jmsTemplate");
	}
	
	/**
	 * Put events via JMS into a knowledge session that is active in the prototype.
	 * @throws Exception
	 */
	@Test
	public void start() throws Exception {

		startTime = new Date().getTime();

		// Insert 3 Appserver with status down
		for (int i = 1; i <= 3; i++) {
			sendEvent(makeSimpleEvent("app server is down", resourceIdAppServer, eventId, new Date().getTime()));
			eventId++;
		}

		// Insert dummy events, that are not in rules
		for (int j = 1; j <= noOfEvents; j++) {
			sendEvent(makeSimpleEvent("dummy event", resourceIdDummy, eventId, new Date().getTime()));
			eventId++;

		}
		// Insert 5 Appserver with status up
		for (int k = 1; k <= 5; k++) {
			sendEvent(makeSimpleEvent("app server is up", resourceIdAppServer, eventId, new Date().getTime()));
			eventId++;
		}
		// Insert db connection
		for (int l = 1; l <= 2; l++) {
			sendEvent(makeSimpleEvent("db connection in use", resourceDBConnection, eventId, new Date().getTime()));
		}
	}

	@After
	public void release() throws JMSException, IOException {
		endTime = new Date().getTime();
		long execTime = endTime - startTime;
		System.out.println("Total time: " + execTime);
		System.out.println("Total events: " + eventId);
		// Print the results of the output processor
		prototype.getOutputProcessor().processOutput(prototype.getResultList());
		prototype.close();
	}

	/**
	 * Send an event via Spring-JmsTemplate.
	 * 
	 * @param EventComposite event
	 * @throws InterruptedException
	 */
	private void sendEvent(final EventComposite event) throws InterruptedException {

		try {
			jmsTemplate.send(new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					ObjectMessage message = session.createObjectMessage();
					message.setObject(event);
					return message;
				}
			});
		} catch (Exception e) {
			System.out.println(e.toString());
			Thread.sleep(1000);
		}
		// Make a short delay, to give the JMS-Provider time for processing the
		// event
		Thread.sleep(40);
	}
}
