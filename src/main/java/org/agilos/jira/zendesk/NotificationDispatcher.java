package org.agilos.jira.zendesk;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.fields.CustomField;

public class NotificationDispatcher {	
	private Logger log = Logger.getLogger(NotificationDispatcher.class.getName());
	private String zendeskServerURL;
	private CustomField ticketField;
	private ChallengeResponse authentication;
	
	void setZendeskServerURL(String url) {
		zendeskServerURL = url;
		log.info("Zendesk url set to "+url);
	}
	
	void setAuthentication(String user, String password) {
		log.info("Using HTTP basic authentication for Zendesk access with user "+user+" and password "+password);
		ChallengeScheme scheme = ChallengeScheme.HTTP_BASIC;
		authentication = new ChallengeResponse(scheme,
				user, password);
	}
	
	public void sendIssueChangeNotification(IssueEvent issueEvent) {
		ClientResource resource = new ClientResource(zendeskServerURL+"/tickets/"+getTicketID(issueEvent.getIssue().getKey())+".xml");
		resource.setReferrerRef(getBaseUrl());
		resource.setChallengeResponse(authentication);
		
		try {
			Representation representation = getRepresentation(issueEvent);
			setSize(representation);
			log.debug("Dispatching: put "+representation.getText());
			resource.put(representation);
			if (resource.getStatus().isSuccess()
	                && resource.getResponseEntity().isAvailable()) {
				log.debug("Received response"+resource.getResponseEntity());
	        } else if (resource.getResponseEntity() != null && resource.getResponseEntity().isAvailable()){
	        	log.warn("No success in sending notification, response was:\n"+resource.getResponseEntity().getText());
	        } else {
	        	log.warn("No response received");
	        }

		} catch (ResourceException e) {
			log.error("Failed to send issue change notification", e);
		} catch (IOException e) {
			log.warn("Failed to represent request as text", e);
		}
	}
	
	private String getBaseUrl() {
        final ApplicationProperties ap = ManagerFactory.getApplicationProperties();
        return ap.getString(APKeys.JIRA_BASEURL);
    }
	
	/**
	 * Generates a REST representation of the issue change
	 * @param issueEvent
	 * @return The REST representation of the issue change if any relevant changes are found, else null;
	 * @throws ResourceException
	 * @throws IOException
	 */
    private Representation getRepresentation(IssueEvent issueEvent) throws ResourceException, IOException {
    	DomRepresentation representation = new DomRepresentation(MediaType.APPLICATION_XML);           
	    representation.setCharacterSet(CharacterSet.UTF_8);	      	
	    
    	Document xml = representation.getDocument();
    	Node commentRoot = xml;

    	if (issueEvent.getEventTypeId() == EventType.ISSUE_UPDATED_ID) {
    		Element ticket = representation.getDocument().createElement("ticket");
    		xml.appendChild(ticket);

    		if (issueEvent.getIssue().getSummary() != null) {
            	Element subject = xml.createElement("subject");
            	subject.setTextContent(issueEvent.getIssue().getSummary());
    			ticket.appendChild(subject);
    		}
    		
    		if (issueEvent.getIssue().getDescription() != null) {
            	Element description = xml.createElement("description");
            	description.setTextContent(issueEvent.getIssue().getDescription());
    			ticket.appendChild(description);
    		}
    		
    		if (issueEvent.getComment() != null) {
    			Element comments = xml.createElement("comments");
    			comments.setAttribute("type", "array");
    			ticket.appendChild(comments);
    			commentRoot = comments;
    		}
    		
    		// If no relevant changes have been added to the tcket root node, exit.
    		if (ticket.getChildNodes().getLength() > 0 ) return representation;
    		else return null;
    	}
    	
    	else if (issueEvent.getEventTypeId() == EventType.ISSUE_COMMENTED_ID) {
        	Element comment = xml.createElement("comment");
        	commentRoot.appendChild(comment);
        	
        	Element isPublic = xml.createElement("is-public");
        	isPublic.setTextContent("true");
        	comment.appendChild(isPublic);
        	
        	Element value = xml.createElement("value");
        	value.setTextContent(issueEvent.getComment().getBody());
        	comment.appendChild(value);
            return representation;        	
        }
    	
    	else return null;
    }

	public void setTicketFieldValue(String ticketFieldName) {
		ticketField = ManagerFactory.getCustomFieldManager().getCustomFieldObjectByName(ticketFieldName);		
	}
    
    private String getTicketID(String issueKey) {
    	return (String)ManagerFactory.getIssueManager().getIssueObject(issueKey).getCustomFieldValue(ticketField);
    }
    
    private void setSize(Representation representation) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	representation.write(out);
    	representation.setSize(out.size());
    }
}