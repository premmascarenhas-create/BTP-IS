
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;  
import org.apache.camel.converter.stream.InputStreamCache 
import com.jcraft.jsch.* 
import java.util.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
/******************************************************************************************
    Main Program
******************************************************************************************/
def Message processData(Message message) {
    
	// Get Body from Payload 
	body = message.getBody(java.lang.String);
	def headers = message.getHeaders()
	def props   = message.getProperties();
	
	messageLog = messageLogFactory.getMessageLog(message)

	messageContent = body 
	
    messageLog.addAttachmentAsString(" EmpPayCompNonRecurring Response ",messageContent.toString(),"text/plain")
  
    return message

} 