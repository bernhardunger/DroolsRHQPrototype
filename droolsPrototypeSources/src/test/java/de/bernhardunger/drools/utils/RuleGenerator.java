package de.bernhardunger.drools.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class RuleGenerator {

	private static String CRLF = System.getProperty("line.separator");
	private static String HEADER = "" + "package de.bernhardunger.drools;" + CRLF + CRLF
			+ "import de.bernhardunger.drools.model.EventComposite;" + CRLF + CRLF + "dialect \"mvel\"" + CRLF
			+ "declare EventComposite" + CRLF + "	@role( event )" + CRLF + "    @timestamp( timestamp )" + CRLF
			+ "	@expires( 60m )" + CRLF + "end" + CRLF;

	private static String RESOURCE_ID = "ResourceId";
	private static String EVENTDETAIL = "EventDetail";

	public static void main(String[] argv) throws IOException {
		int noOfRules = 250;
		List<String> rules = new ArrayList<String>();
		rules.add(HEADER);
		String fileName = "src/main/resources/de/bernhardunger/drools/testRuleLogicalAnd" + noOfRules + ".drl";
		File file = new File(fileName);
		FileUtils.deleteQuietly(file);
		FileUtils.write(file, HEADER, "UTF-8", true);
		for (int i = 1; i <= noOfRules; i++) {
			String resource = makeDeclare(RESOURCE_ID + i);
			String eventDetail = makeDeclare(EVENTDETAIL + i);
			FileUtils.write(file, resource, "UTF-8", true);
			FileUtils.write(file, eventDetail, "UTF-8", true);
		}
		FileUtils.write(file, CRLF, "UTF-8", true);
		for (int i = 1; i <= noOfRules; i++) {
			String rule = makeRule(i);
			FileUtils.write(file, rule, "UTF-8", true);
			String ruleExists = makeRuleExists(1, i);
			FileUtils.write(file, ruleExists, "UTF-8", true);
		}
		FileUtils.write(file, CRLF, "UTF-8", true);
	}

	private static String makeDeclare(String id) {
		String declare = "declare " + id + "  @role( event ) @expires( 60m ) end" + CRLF;
		return declare;
	}

	private static String makeRule(int id) {
		String rule = "" + "rule \"rule" + id + "\"" + CRLF + "when" + CRLF
				+ "	EventComposite( eventDetail == \"eventDetail" + id + "\", resourceId == " + id + ")" + CRLF
				+ "then" + CRLF + "	insertLogical( new ResourceId" + id + "() );" + CRLF
				+ "	insertLogical( new EventDetail" + id + "() );" + CRLF + "end" + CRLF;
		return rule;
	}

	private static String makeExists(int from, int to) {
		String rule = "";
		for (int i = from; i <= to; i++) {
			rule += "    exists ( ResourceId" + i + "() ) " + CRLF;
			rule += "    exists ( EventDetail" + i + "() ) " + CRLF;
		}
		return rule;
	}

	private static String makeRuleExists(int from, int to) {
		String rule = CRLF +
		"rule \"ruleTestExists_" + from + "_" + to + "\"" + CRLF + "when" + CRLF;
		rule += makeExists(from, to);
		rule += "then" + CRLF + "	System.out.println( \"Status ResourceId" + from + "-" + to + "() and EventDetail"
				+ from + "-" + to + "()\");" + CRLF 
		+ "end" + CRLF;
		return rule;
	}
}
