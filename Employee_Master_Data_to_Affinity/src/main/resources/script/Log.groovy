import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

import groovy.xml.*
import groovy.json.*
import org.apache.camel.converter.stream.InputStreamCache

import com.jcraft.jsch.*


def Message processData(Message message) {
    //Body 
    def body = message.getBody();
	def messageLog = messageLogFactory.getMessageLog(message)
	def messageContent = ''
	
	if(body instanceof String) {
		messageContent = body.toString()
	} else if(body instanceof InputStreamCache) {
		messageContent = body.text
	} else if(body instanceof org.apache.camel.converter.stream.ByteArrayInputStreamCache) {
		messageContent = body.text
	} else {
		messageContent = new String(body)
	}

	/*def finalXml = new XmlSlurper().parseText(messageContent)
 	messageContent_1 = XmlUtil.serialize(finalXml)
 	message.setBody(messageContent_1)*/

	/*def finalJson = new JsonSlurper().parseText(messageContent)
 	messageContent_1 = JsonOutput.toJson(finalJson)
 	message.setBody(messageContent_1)*/
    
    // def props = message.getProperties();

    // def jsonRequest = props.get("RECORD_REQUEST");
    // def recordLog   = "---REQUEST---\n\n" + jsonRequest +"\n\n"+messageContent+"---END OF REQUEST---"
    
    //def encodedLog = recordLog.bytes.encodeBase64().toString();
    def encodedLog = messageContent.bytes.encodeBase64().toString();
	def datetime_now= new Date().format("yyyy_mm_dd'T'hh_mm");
	
	java.util.Properties config = new java.util.Properties()
	config.put "StrictHostKeyChecking", "no"

	/*
	JSch ssh = new JSch()
	Session sess = ssh.getSession "", "sftp10.successfactors.com", 22
	sess.with {
		setConfig config
		setPassword ""
		connect()
		Channel chan = openChannel "sftp"
		chan.connect()
		ChannelSftp sftp = (ChannelSftp) chan;
		def sessionsFile = new File('test.txt')
		sessionsFile.write encodedLog
		sessionsFile.withInputStream { istream -> 
			//def fileDateStr = dateNowStr.replaceAll(':','').replaceAll('/','')
			//def fileDateStr = dateNowStr.replaceAll(':','').replaceAll('/','').replaceAll('-','').replaceAll('T','_').replaceAll('Z','')
			logFilename = '/outgoing/logs/EMD/EMD_' + datetime_now + '.txt'
			sftp.put(istream, logFilename)
		}
		chan.disconnect()
		disconnect()
	}
    */
	
	if(messageLog != null) {
		messageLog.addAttachmentAsString("Request Output",messageContent, "text/plain")
    }
    return message;
}