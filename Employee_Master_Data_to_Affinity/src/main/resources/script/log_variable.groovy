import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

import org.apache.camel.converter.stream.InputStreamCache

import com.jcraft.jsch.*

def Message processData(Message message) {
    //Body 
    def messageContent = message.getBody(java.lang.String);
	def props = message.getProperties()
	def messageLog = messageLogFactory.getMessageLog(message)
	
	String dateNow = props.get('DATE_NOW')
	String userLastRun = props.get('USER_SET_LAST_RUN_DATETIME')
	String lastRun = props.get('LAST_RUN_DATETIME')
	
	
	
	if(messageLog != null){
    	messageLog.setStringProperty("DATE NOW", dateNow);
    	messageLog.setStringProperty("User Last Run", userLastRun);
    	messageLog.setStringProperty("Last Run", lastRun);
    	
    	 messageLog.addAttachmentAsString("Dates:",dateNow+" "+userLastRun+" "+lastRun, "text/plain");
	}

    return message;
}