import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

import java.net.*;
import java.io.*;
import groovy.xml.*

def Message processData(Message message) {
    //Body 
    def messageContent = message.getBody(java.lang.String);
	messageLog = messageLogFactory.getMessageLog(message)
	def deletedItemMap = [:]
	def userId
	def payCode
	def key
	
	def feed = new XmlSlurper().parseText(messageContent)
	def output = new XmlSlurper().parseText('<recurringDeductionsMasterList/>')
	feed?.children()?.each { item ->
		def temp = new XmlSlurper().parseText('<recurringDeductionsMasterList/>')
		userId = item?.userId?.text()
		if(!userId) {
			userId = item?.RecurringDeduction_userSysId?.text()
		}
		payCode = item?.payComponent?.text()
		if(!payCode) {
			payCode = item?.payComponentType?.text()
		}
		key = userId + ':' + payCode
		if(deletedItemMap[key] == null) {
			temp.appendNode(item)
			output.appendNode(temp)
			deletedItemMap[key] = key
		}
	}
	
	messageContent = XmlUtil.serialize(output)
	messageLog.addAttachmentAsString("Recurring Deductions MasterList", messageContent, "text/plain")
	message.setBody(messageContent)
	
	return message
}

