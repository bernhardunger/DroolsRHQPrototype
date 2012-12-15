package de.bernhardunger.drools.jms;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.protobuf.compiler.CommandLineSupport;
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
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.time.SessionPseudoClock;

import de.bernhardunger.drools.model.EventComposite;
import de.bernhardunger.drools.model.OutputProcessor;
import de.bernhardunger.drools.model.RuleResult;
import de.bernhardunger.drools.util.EventFilter;
import de.bernhardunger.drools.util.EventFilterImpl;

/**
 * A prototype component for consuming messages. Source has been copied from
 * examples of ActiveMQ installation. Adapted for JUnit-Tests with a JMS-Message
 * provider and consumer for testing of Drools-Prototype.
 * 
 * @author http://activemq.apache.org/
 * @author Bernhard Unger
 * 
 */
public class PrototypeJMSReceiver implements MessageListener, ExceptionListener {
	private KnowledgeRuntimeLogger knwlgLogger;
	private Session session;
	private Destination destination;
	private boolean verbose = true;
	private String subject = "ActiveMQ.Advisory.Producer.Topic.rhq.topic.event";
	private boolean topic = true;
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = ActiveMQConnection.DEFAULT_BROKER_URL + "?jms.watchTopicAdvisories=false";
	private boolean transacted;
	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	private long sleepTime;
	private long messagesReceived = 0;
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private MessageConsumer consumer;
	private boolean useFilter = false;
	private StatefulKnowledgeSession ksession;
	private List<RuleResult> resultList = new ArrayList<RuleResult>();
	private String filterExpression;
	private EventFilter eventFilter = new EventFilterImpl();
	private OutputProcessor outputProcessor = new OutputProcessor();
	private String drlResource;
	
	public void start(String[] args) {

		PrototypeJMSReceiver consumerTool = new PrototypeJMSReceiver();
		String[] unknown = CommandLineSupport.setOptions(consumerTool, args);
		if (unknown.length > 0) {
			System.out.println("Unknown options: " + Arrays.toString(unknown));
			System.exit(-1);
		}
		consumerTool.showParameters();
		run();
	}

	public void showParameters() {
		System.out.println("Connecting to URL: " + url + " (" + user + ":" + password + ")");
		System.out.println("Consuming " + (topic ? "topic" : "queue") + ": " + subject);
	}

	private void run() {
		try {
			// Set up the Drools session
			setUpDroolsSession();
			// Set up JMS connection
			connectionFactory = new ActiveMQConnectionFactory(user, password, url);
			connection = connectionFactory.createConnection();
			connection.setExceptionListener(this);
			connection.start();
			// Set up JMS session
			session = connection.createSession(transacted, ackMode);
			// Create a destination queue or topic
			if (topic) {
				destination = session.createTopic(subject);
			} else {
				destination = session.createQueue(subject);
			}
			// Create a JMS consumer
			consumer = session.createConsumer(destination);
			// Set the message listener, to receive messages
			consumer.setMessageListener(this);
		} catch (Exception e) {
			System.out.println("Caught: " + e);
			e.printStackTrace();
		}
	}

	/**
	 * Listener method that will be fired, when messages are arriving
	 */
	@SuppressWarnings("unchecked")
	public void onMessage(Message message) {

		messagesReceived++;

		try {
			if (verbose) {

				ObjectMessage objMessage = (ObjectMessage) message;
				EventComposite event = (EventComposite) objMessage.getObject();
				// Filter out events by filterExpression
				if (useFilter) {
					if (null != filterExpression) {
						// Returns null when filter matches the filterExpression
						event = eventFilter.filterByEventDetail(event, filterExpression);
					}
				}
				if (null != event) {
					ksession.insert(event);
					System.out.println("Insert to ksession: " + event);
				}
				// Get the results from the rules engine
				resultList = (List<RuleResult>) ksession.getGlobal("resultList");
				ksession.fireAllRules();
				// Add the pseudo clock for testing time behavior of the rules
				// engine
				SessionPseudoClock clock = ksession.getSessionClock();
				clock.advanceTime(1, TimeUnit.MILLISECONDS);
				objMessage.acknowledge();
				
			}
		} catch (JMSException e) {
			System.out.println("Caught: " + e);
			e.printStackTrace();
		} finally {
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void onException(JMSException ex) {
		System.out.println("JMS Exception occured.  Shutting down client.");

	}

	/**
	 * Close JMS consumer, JMS session JMS connection and Drools session
	 * 
	 * @throws JMSException
	 * @throws IOException
	 */
	public void close() throws JMSException, IOException {
		System.out.println("Closing  JMS connection, received " + messagesReceived + " messages.");
		consumer.close();
		session.close();
		connection.close();
		shutDownDroolsSession();
	}

	/**
	 * Set up the drools session
	 */
	private void setUpDroolsSession() {
		// -------------------------------------------------
		// Initialize the drools knowledge base
		// -------------------------------------------------
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		// Load rules from file
		kbuilder.add(new ClassPathResource(drlResource, getClass()), ResourceType.DRL);
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
		ksession.setGlobal("resultList", resultList);
		knwlgLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession, "logs/myKnowledgeLogfile");
		
	}

	private void shutDownDroolsSession() {
		knwlgLogger.close();
		ksession.halt();
		ksession.dispose();
	}

	public void setPassword(String pwd) {
		this.password = pwd;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setTopic(boolean topic) {
		this.topic = topic;
	}

	public void setQueue(boolean queue) {
		this.topic = !queue;
	}

	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public List<RuleResult> getResultList() {
		return resultList;
	}

	public OutputProcessor getOutputProcessor() {
		return outputProcessor;
	}

	public void setUseFilter(boolean useFilter) {
		this.useFilter = useFilter;
	}

	public void setFilterExpression(String filterExpression) {
		this.filterExpression = filterExpression;
	}

	public void setDrlResource(String drlResource) {
		this.drlResource = drlResource;
	}

}
