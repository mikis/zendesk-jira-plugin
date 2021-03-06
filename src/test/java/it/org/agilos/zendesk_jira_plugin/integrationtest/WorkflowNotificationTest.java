package it.org.agilos.zendesk_jira_plugin.integrationtest;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

public class WorkflowNotificationTest extends AbstractNotificationTest {

	@Test (groups = {"regressionTests"} )
	public void testDefaultWorkflow() {
		fixture.tester.gotoPage("browse/"+issueKey);
		fixture.tester.assertTextPresent("Open");
		fixture.tester.clickLinkWithText("to me"); //Need to assign the issue to current user to gain access to all workflow actions

		fixture.tester.clickLinkWithText("Start Progress");
		fixture.tester.assertTextPresent("In Progress");
		assertEquals("Wrong response received setting issue 'In progress'", 
				TestDataFactory.getSoapResponse("testDefaultWorkflow.started"), 
				fixture.getNextRequest().getEntityAsText());

		fixture.tester.clickLinkWithText("Stop Progress");
		fixture.tester.assertTextPresent("Open");
		assertEquals("Wrong response received setting stopping progress on issue", 
				TestDataFactory.getSoapResponse("testDefaultWorkflow.stopped"), 
				fixture.getNextRequest().getEntityAsText());

		issueHandler.resolve();
		assertEquals("Wrong response received after resolving issue", 
				TestDataFactory.getSoapResponse("testDefaultWorkflow.resolved"), 
				fixture.getNextRequest().getEntityAsText());

		issueHandler.close();
		assertEquals("Wrong response received after closing issue", 
				TestDataFactory.getSoapResponse("testDefaultWorkflow.closed"), 
				fixture.getNextRequest().getEntityAsText());

		issueHandler.reopen();
		assertEquals("Wrong response received after reopening issue", 
				TestDataFactory.getSoapResponse("testDefaultWorkflow.reopened"), 
				fixture.getNextRequest().getEntityAsText());
	}
	
	//@Test (groups = {"regressionTests"} )
	public void testCustomWorkflow() {
		//ToDo Implement assignment of custom workflow to project, see http://jira.agilos.org/browse/ZEN-68
		
		fixture.tester.gotoPage("browse/"+issueKey);
		fixture.tester.assertTextPresent("Open");
		fixture.tester.clickLinkWithText("to me"); //Need to assign the issue to current user to gain access to all workflow actions
		
		fixture.tester.clickLinkWithText("Investigate");
		fixture.tester.assertTextPresent("Investigate");
		assertEquals("Wrong response received after setting issue to investigating", 
				TestDataFactory.getSoapResponse("testCustomWorkflow.investigating"), 
				fixture.getNextRequest().getEntityAsText());
	}
}
