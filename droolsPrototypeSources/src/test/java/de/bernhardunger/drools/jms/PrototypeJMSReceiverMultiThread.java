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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
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
 * A simple component for consuming messages. Source has been copied from
 * examples of ActiveMQ installation, adapted for Junit-Tests of jms message
 * sender.
 * 
 * @author http://activemq.apache.org/
 * @author Bernhard Unger
 * 
 */
public class PrototypeJMSReceiverMultiThread extends Thread implements MessageListener, ExceptionListener {

	private boolean running;

	private Session session;
	private Destination destination;
	private MessageProducer replyProducer;
	private boolean useFilter = false;
	private boolean pauseBeforeShutdown = false;
	private boolean verbose = true;
	private int maxiumMessages;
	private static int parallelThreads = 1;
	private String subject = "ActiveMQ.Advisory.Producer.Topic.rhq.topic.event";
	// private String subject =
	// "ActiveMQ.Advisory.Producer.Topic.rhq.queue.event";
	private boolean topic = true;
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = ActiveMQConnection.DEFAULT_BROKER_URL + "?jms.watchTopicAdvisories=false";
	private boolean transacted;
	private boolean durable;
	private String clientId;
	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	private String consumerName = "RHQTest";
	private long sleepTime;
	private long receiveTimeOut;
	private long batch = 10; // Default batch size for CLIENT_ACKNOWLEDGEMENT or
								// SESSION_TRANSACTED
	private long messagesReceived = 0;

	private StatefulKnowledgeSession ksession;
	private List<RuleResult> resultList = new ArrayList<RuleResult>();
	private EventFilter eventFilter = new EventFilterImpl();

	public void start(String[] args) {
		ArrayList<PrototypeJMSReceiverMultiThread> threads = new ArrayList();
		PrototypeJMSReceiverMultiThread consumerTool = new PrototypeJMSReceiverMultiThread();
		String[] unknown = CommandLineSupport.setOptions(consumerTool, args);
		if (unknown.length > 0) {
			System.out.println("Unknown options: " + Arrays.toString(unknown));
			System.exit(-1);
		}
		consumerTool.showParameters();
		for (int threadCount = 1; threadCount <= parallelThreads; threadCount++) {
			consumerTool = new PrototypeJMSReceiverMultiThread();
			CommandLineSupport.setOptions(consumerTool, args);
			consumerTool.start();
			threads.add(consumerTool);
		}

		while (true) {
			Iterator<PrototypeJMSReceiverMultiThread> itr = threads.iterator();
			int running = 0;
			while (itr.hasNext()) {
				PrototypeJMSReceiverMultiThread thread = itr.next();
				if (thread.isAlive()) {
					running++;
				}
			}

			if (running <= 0) {
				System.out.println("All threads completed their work");
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
		Iterator<PrototypeJMSReceiverMultiThread> itr = threads.iterator();
		while (itr.hasNext()) {
			PrototypeJMSReceiverMultiThread thread = itr.next();
		}
	}

	public void showParameters() {
		System.out.println("Connecting to URL: " + url + " (" + user + ":" + password + ")");
		System.out.println("Consuming " + (topic ? "topic" : "queue") + ": " + subject);
		System.out.println("Using a " + (durable ? "durable" : "non-durable") + " subscription");
		System.out.println("Running " + parallelThreads + " parallel threads");
	}

	public void run() {
		try {

			setUpDroolsSession();

			running = true;

			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
			Connection connection = connectionFactory.createConnection();
			if (durable && clientId != null && clientId.length() > 0 && !"null".equals(clientId)) {
				connection.setClientID(clientId);
			}
			connection.setExceptionListener(this);
			connection.start();

			session = connection.createSession(transacted, ackMode);
			if (topic) {
				destination = session.createTopic(subject);
			} else {
				destination = session.createQueue(subject);
			}

			replyProducer = session.createProducer(null);
			replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			MessageConsumer consumer = null;
			if (durable && topic) {
				consumer = session.createDurableSubscriber((Topic) destination, consumerName);
			} else {
				consumer = session.createConsumer(destination);
			}

			if (maxiumMessages > 0) {
				consumeMessagesAndClose(connection, session, consumer);
			} else {
				if (receiveTimeOut == 0) {
					consumer.setMessageListener(this);
				} else {
					consumeMessagesAndClose(connection, session, consumer, receiveTimeOut);
				}
			}

		} catch (Exception e) {
			System.out.println("[" + this.getName() + "] Caught: " + e);
			e.printStackTrace();
		}
	}

	public void onMessage(Message message) {
		OutputProcessor outputProcessor = new OutputProcessor();
		messagesReceived++;

		try {

			if (message instanceof TextMessage) {
				TextMessage txtMsg = (TextMessage) message;
				if (verbose) {

					String msg = txtMsg.getText();
					int length = msg.length();
					if (length > 50) {
						msg = msg.substring(0, 50) + "...";
					}
					// System.out.println("[" + this.getName() + "] Received: '"
					// + msg + "' (length " + length + ")");
				}
			}

			else {
				if (verbose) {

					// System.out.println("[" + this.getName() + "] Received: '"
					// + message + "'");
					ObjectMessage objMessage = (ObjectMessage) message;
					EventComposite event = (EventComposite) objMessage.getObject();
					// Filter out dummy events
					if (useFilter) {
						event = eventFilter.filterByEventDetail(event, "dummy event");
						if (null != event) {
							ksession.insert(event);
							// System.out.println("Insert to ksession: " +
							// event);
						}
					} else {
						ksession.insert(event);
						System.out.println("Insert to ksession: " + event);
					}
					// Get the results from the rules engine
					List<RuleResult> results = (List<RuleResult>) ksession.getGlobal("resultList");
					resultList.addAll(results);
					// System.out.println(results.size());
					// outputProcessor.addResultList(results);
					// outputProcessor.processOutput();
					ksession.fireAllRules();
					// System.out.println("EventResultList contains " +
					// eventResultList.size() + " Elements");
					// System.out.println(event);
					SessionPseudoClock clock = ksession.getSessionClock();
					clock.advanceTime(1, TimeUnit.MILLISECONDS);
					objMessage.acknowledge();

				}
			}

			if (message.getJMSReplyTo() != null) {
				replyProducer.send(message.getJMSReplyTo(),
						session.createTextMessage("Reply: " + message.getJMSMessageID()));
			}

			if (transacted) {
				if ((messagesReceived % batch) == 0) {
					System.out.println("Commiting transaction for last " + batch + " messages; messages so far = "
							+ messagesReceived);
					session.commit();
				}
			} else if (ackMode == Session.CLIENT_ACKNOWLEDGE) {
				if ((messagesReceived % batch) == 0) {
					System.out.println("Acknowledging last " + batch + " messages; messages so far = "
							+ messagesReceived);
					message.acknowledge();
				}
			}

		} catch (JMSException e) {
			System.out.println("[" + this.getName() + "] Caught: " + e);
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

	public synchronized void onException(JMSException ex) {
		System.out.println("[" + this.getName() + "] JMS Exception occured.  Shutting down client.");
		running = false;
	}

	synchronized boolean isRunning() {
		return running;
	}

	protected void consumeMessagesAndClose(Connection connection, Session session, MessageConsumer consumer)
			throws JMSException, IOException {
		System.out.println("[" + this.getName() + "] We are about to wait until we consume: " + maxiumMessages
				+ " message(s) then we will shutdown");

		for (int i = 0; i < maxiumMessages && isRunning();) {
			Message message = consumer.receive(1000);
			if (message != null) {
				i++;
				onMessage(message);
			}
		}
		System.out.println("[" + this.getName() + "] Closing connection");
		consumer.close();
		session.close();
		connection.close();
		if (pauseBeforeShutdown) {
			System.out.println("[" + this.getName() + "] Press return to shut down");
			System.in.read();
		}
	}

	protected void consumeMessagesAndClose(Connection connection, Session session, MessageConsumer consumer,
			long timeout) throws JMSException, IOException {
		System.out.println("[" + this.getName()
				+ "] We will consume messages while they continue to be delivered within: " + timeout
				+ " ms, and then we will shutdown");

		Message message;
		while ((message = consumer.receive(timeout)) != null) {
			onMessage(message);
		}

		System.out.println("[" + this.getName() + "] Closing connection");
		consumer.close();
		session.close();
		connection.close();
		if (pauseBeforeShutdown) {
			System.out.println("[" + this.getName() + "] Press return to shut down");
			System.in.read();
		}
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
		ksession.setGlobal("resultList", resultList);

	}

	private void shutDownDroolsSession() {
		ksession.halt();
		ksession.dispose();
	}

	public void setAckMode(String ackMode) {
		if ("CLIENT_ACKNOWLEDGE".equals(ackMode)) {
			this.ackMode = Session.CLIENT_ACKNOWLEDGE;
		}
		if ("AUTO_ACKNOWLEDGE".equals(ackMode)) {
			this.ackMode = Session.AUTO_ACKNOWLEDGE;
		}
		if ("DUPS_OK_ACKNOWLEDGE".equals(ackMode)) {
			this.ackMode = Session.DUPS_OK_ACKNOWLEDGE;
		}
		if ("SESSION_TRANSACTED".equals(ackMode)) {
			this.ackMode = Session.SESSION_TRANSACTED;
		}
	}

	public void setClientId(String clientID) {
		this.clientId = clientID;
	}

	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public void setMaxiumMessages(int maxiumMessages) {
		this.maxiumMessages = maxiumMessages;
	}

	public void setPauseBeforeShutdown(boolean pauseBeforeShutdown) {
		this.pauseBeforeShutdown = pauseBeforeShutdown;
	}

	public void setPassword(String pwd) {
		this.password = pwd;
	}

	public void setReceiveTimeOut(long receiveTimeOut) {
		this.receiveTimeOut = receiveTimeOut;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setParallelThreads(int parallelThreads) {
		if (parallelThreads < 1) {
			parallelThreads = 1;
		}
		this.parallelThreads = parallelThreads;
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

	public void setBatch(long batch) {
		this.batch = batch;
	}

	public List<RuleResult> getResultList() {
		return resultList;
	}

}
