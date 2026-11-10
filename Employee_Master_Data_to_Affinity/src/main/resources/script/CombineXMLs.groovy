import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.*
import org.apache.camel.converter.stream.InputStreamCache

def Message processData(Message message) {
	
	//Body 
	def messageContent = message.getBody(java.lang.String);
	def messageLog = messageLogFactory.getMessageLog(message)
	def empXml = '<employee></employee>'
	
	//messageLog.addAttachmentAsString("Raw XML", messageContent, "text/plain")
	
	def empData
	def empDataMap= [:]
	def rootData
	def positionData
	//def teamData
	
	def feed = new XmlSlurper().parseText(messageContent).declareNamespace('multimap':'http://sap.com/xi/XI/SplitAndMerge')
	def dataArray = feed?.'multimap:Message1'
	def ee_global_assignment = [:];
	
	feed.children().each { data ->	// iterate Messages node
		data.children().each() { data1 ->
			if(data1.name() == 'Position') {
				positionData = data1
			}  else {
				data1.children().each { data2 ->
				
		        //checking for employees that has global assignment.
				boolean has_multiple_personid = false;
				
				if( data2?.name() == "PerPersonal" || "PerAddressDEFLT"){
					
					//check 
					def entityNode = data2?.personNav?.PerPerson?.employmentNav?.EmpEmployment;
					
					if( entityNode?.size() > 1 ){
						has_multiple_personid = true;
						def userKey = entityNode?.userId?.text()
						def multipleUserIdMap = [];
						entityNode.each{en->
							
							multipleUserIdMap.push(en?.userId?.text())
						}
						ee_global_assignment[userKey] = multipleUserIdMap
					}
					
				}
				if( data2?.name() == "PerPerson" ){
					
					//check
					def entityNode = data2?.employmentNav?.EmpEmployment;
					
					if( entityNode?.size() > 1 ){
						has_multiple_personid = true;
						def userKey = entityNode?.userId?.text()
						def multipleUserIdMap = [];
						entityNode.each{en->
							
							multipleUserIdMap.push(en?.userId?.text())
						}
						ee_global_assignment[userKey] = multipleUserIdMap
					}
					
				}		
        				
				
					// create full employee data
					def userId = data2?.userId?.text()
					if(!userId) {
						userId = data2?."*"?."*"?.userId?.text()
					}
					if(!userId) {
						userId = data2?."*"?."*"?."*"?.userId?.text()
					}
					if(!userId) {
						userId = data2?."*"?."*"?."*"?."*"?.userId?.text()
					}
					if(!userId) {
						userId = data2?."*"?."*"?."*"?."*"?."*"?.userId?.text()
					}
					if(!userId) {
						userId = data2?."*"?."*"?."*"?."*"?."*"?."*"?.userId?.text()
					}
					if(!userId) {
						userId = data2?.RecurringDeduction_userSysId?.text()
					}
					if(!userId) {
						userId = data2?.usersSysId?.text()
					}
					if(!userId) {
						userId = data2?.userSysId?.text()
					}
					if(!userId) {
					    userId = data2?.worker?.text()
					}
					//for the masterlist of recurring deduction items
					if(!userId) {
					    userId = data2?."*"?."*"?.RecurringDeduction_userSysId?.text()
					}
					if(userId) { //hard code emp id during unit test
					    
						if( has_multiple_personid ){
							ee_global_assignment[userId].each{mu->
								//println "userID " + mu
								empData = empDataMap[mu]
								
								if(empData) {
									empData.appendNode(data2)
									empDataMap[mu] = empData
								} else {
									def empRoot = new XmlSlurper().parseText(empXml)
									empRoot.appendNode(data2)
									empDataMap[mu] = empRoot
								}
								
							}
						}else{
							
							empData = empDataMap[userId]
							
							if(empData) {
								empData.appendNode(data2)
								empDataMap[userId] = empData
							} else {
								def empRoot = new XmlSlurper().parseText(empXml)
								empRoot.appendNode(data2)
								empDataMap[userId] = empRoot
							}
						}
					}
				}
			}
		}
	}

	def empDataStr = '''<RawXML/>'''
	rootData = new XmlSlurper().parseText(empDataStr)
	int modifCount = 0
	
	// Add logging of time frame
	def props = message.getProperties()
	String dateNowStr = props.get('DATE_NOW')
	String lastRunStr = props.get('LAST_RUN_DATETIME')
	
	Date dateNow = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", dateNowStr)
	long dateNowTime = dateNow.getTime()
	Date lastRun = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", lastRunStr)
	long lastRunTime = lastRun.getTime()
	
	// TODO: TEMP ONLY, FOR PARALLEL TEST - get time from last 15 minutes
	//long lastRunTime = (dateNowTime - 300000 - (dateNowTime % 60000))
	
	Date date1 = new Date(lastRunTime)
	Date date2 = new Date(dateNowTime)
	messageLog.addAttachmentAsString('DATE RANGE PROCESSED', date1.toString() + ' - ' + date2.toString(), 'text/plain')
    
    // Checking of Modified Data
	empDataMap.each {key,data ->
	   
        if(modifiedEmployee(data, message.getProperties())) {
			rootData.appendNode(data)
			modifCount++
		} else {
			//do nothing, skip entry
		}
	}
	
	// Add modified position
	positionData.children().each() { pos ->
		if(modifiedObject(pos, message.getProperties())) {
			rootData.appendNode(pos)
			modifCount++
		} else {
			//do nothing, skip entry
		}
	}
	
	// Add modified team
	/**
	teamData.children().each() { team ->
		if(modifiedObject(team, message.getProperties())) {
			rootData.appendNode(team)
			modifCount++
		} else {
			//do nothing, skip entry
		}
	}
	**/

	messageContent = XmlUtil.serialize(rootData)
	message.setBody(messageContent)
	
	if(modifCount == 0) {
		message.setHeader('SKIP_FLAG', 'SKIP')
	}

	if(messageLog != null) {
		messageLog.setStringProperty("Modified EE Count", String.valueOf(modifCount))
		messageLog.addAttachmentAsString("Combined Raw XML", messageContent, "text/plain")
    }
	
	return message;
}

boolean modifiedEmployee(Object data, Map props) {
	def feed = XmlUtil.serialize(data)
	data = new XmlSlurper().parseText(feed)
	
	String dateNowStr = props.get('DATE_NOW')
	String lastRunStr = props.get('LAST_RUN_DATETIME')
	String lastModifiedStr = data?.PerPersonal?.lastModifiedDateTime?.text()
	
	Date dateNow = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", dateNowStr)
	long dateNowTime = dateNow.getTime()
	Date lastRun = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", lastRunStr)
	long lastRunTime = lastRun.getTime()
	
	// TODO: TEMP ONLY, FOR PARALLEL TEST - get time from last 15 minutes
	//long lastRunTime = (dateNowTime - 300000 - (dateNowTime % 60000))
	
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.PerPersonal?.startDate?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.PerAddressDEFLT?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.PerAddressDEFLT?.startDate?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.EmpJob?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.EmpJob?.startDate?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	/*lastModifiedStr = data?.EmpCompensation?.lastModifiedDateTime?.text()
    if(lastModifiedStr != '' && lastModifiedStr != null) {
        long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
        if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
            return true
        }
    }*/
    boolean PayComponentChange = false;
    EmpPayCompRecurringChange = data?.EmpCompensation
    EmpPayCompRecurringChange.each() { EmpPCR ->
        lastModifiedStr = EmpPCR?.lastModifiedDateTime?.text()
        if(lastModifiedStr != '' && lastModifiedStr != null) {
            long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
            if (timeInBetween(lastRunTime, dateNowTime, lastModified)) {
                PayComponentChange = true
                return
            }
        }
    }
    if( PayComponentChange ){
        return true;
    }
    boolean PayComponentStartDate = false;
    EmpPayCompRecurringStartDate = data?.EmpCompensation
    EmpPayCompRecurringStartDate.each() { EmpPCR ->
        lastModifiedStr = EmpPCR?.startDate?.text()
        if(lastModifiedStr != '' && lastModifiedStr != null) {
            long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
            if (timeInBetween(lastRunTime, dateNowTime, lastModified)) {
                PayComponentStartDate = true
                return
            }
        }
    }
    if( PayComponentStartDate ){
        return true;
    }
    /*lastModifiedStr = data?.EmpCompensation?.startDate?.text()
    if(lastModifiedStr != '' && lastModifiedStr != null) {
        long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
        if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
            return true
        }
    }*/
	lastModifiedStr = data?.PaymentInformationV3?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
    lastModifiedStr = data?.PaymentInformationV3?.effectiveStartDate?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.PerPerson?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.PerPerson?.nationalIdNav?."*"?.lastModifiedDateTime?.text()
    if(lastModifiedStr != '' && lastModifiedStr != null) {
        long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
        if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
            return true
        }
    }
	lastModifiedStr = data?.PerPerson?.employmentNav?."*"?.endDate?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.PerPerson?.employmentNav?."*"?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.OneTimeDeduction?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	lastModifiedStr = data?.OneTimeDeduction?.deductionDate?.text()
    if(lastModifiedStr != '' && lastModifiedStr != null) {
        long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
        if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
            return true
        }
    }
	//added this line to detect the changes in contact portlet 
	boolean hasPerPhoneUpdates = false;
	perPhoneNav = data?.PerPerson?.phoneNav?.PerPhone
	perPhoneNav.each{pn->
	    lastModifiedStr = pn?.lastModifiedDateTime?.text()
	    if(lastModifiedStr != '' && lastModifiedStr != null) {
    		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
    		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
    			hasPerPhoneUpdates = true;
    			return;
    		}
    	}
	}
	
	if( hasPerPhoneUpdates ){
		return true;
	}
	//end of perphone update Checking
	
	//added this line to detect the changes in email portlet
    boolean hasPerEmailUpdates = false;
    perEmailNav = data?.PerPerson?.emailNav?.PerEmail
    perEmailNav.each{pn->
        lastModifiedStr = pn?.lastModifiedDateTime?.text()
        if(lastModifiedStr != '' && lastModifiedStr != null) {
            long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
            if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
                hasPerEmailUpdates = true;
                return;
            }
        }
    }

    if( hasPerEmailUpdates ){
        return true;
    }
    //end of email update Checking
	
	//added this line to detect the changes in alternative cost distribution portlet 
	boolean hastOtherCostCenters = false;
	costCenterDistributionItem = data?.EmpCostDistribution?.items?.EmpCostDistributionItem
	
	costCenterDistributionItem.each{ecd->
        lastModifiedStr = ecd?.lastModifiedDateTime?.text()
        effcostCenterDistributionStr = data?.EmpCostDistribution?.effectiveStartDate?.text()
        if((lastModifiedStr != '' && lastModifiedStr != null) || (effcostCenterDistributionStr != '' && effcostCenterDistributionStr != null)) {
            long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
            long effcostCenterDistributionlong = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", effcostCenterDistributionStr).getTime()
            if((timeInBetween(lastRunTime, dateNowTime, lastModified)) || (timeInBetween(lastRunTime, dateNowTime, effcostCenterDistributionlong))) {
                hastOtherCostCenters = true;
                return;
            }
        }
    }
	
	if( hastOtherCostCenters ){
		return true;
	}
	//end of alternative cost distribution update Checking
	
	/*lastModifiedStr = data?.RecurringDeductionItem?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}*/
	
	boolean hasRecurringDeductionUpdates = false;
	
    data?.RecurringDeductionItem.each{rd->
        lastModifiedStr = rd?.lastModifiedDateTime?.text()
        effectivedateStr = rd?.RecurringDeduction_effectiveStartDate?.text()
        if((lastModifiedStr != '' && lastModifiedStr != null) || (effectivedateStr != '' && effectivedateStr != null)) {

            long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
            long effectivedate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", effectivedateStr).getTime()
            if((timeInBetween(lastRunTime, dateNowTime, lastModified) )|| (timeInBetween(lastRunTime, dateNowTime, effectivedate))) {
                //return true
                hasRecurringDeductionUpdates = true;
                return;
            }
        }
    }
	
	if( hasRecurringDeductionUpdates ){
		return true;
	}
	
	lastModifiedStr = data?.RecurringDeduction[0]?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	
	lastModifiedStr = data?.EmpPayCompNonRecurring?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	/*lastModifiedStr = data?.PerEmergencyContacts?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}*/
	/*lastModifiedStr = data?.EmpCostDistribution?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}*/
	return false
}

boolean modifiedObject(Object data, Map props) {
	def feed = XmlUtil.serialize(data)
	data = new XmlSlurper().parseText(feed)
	
	String dateNowStr = props.get('DATE_NOW')
	String lastRunStr = props.get('LAST_RUN_DATETIME')
	
	Date dateNow = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", dateNowStr)
	Date lastRun = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", lastRunStr)
	long dateNowTime = dateNow.getTime()
	long lastRunTime = lastRun.getTime()
	
	String lastModifiedStr = data?.lastModifiedDateTime?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
	
	lastModifiedStr = data?.effectiveStartDate?.text()
	if(lastModifiedStr != '' && lastModifiedStr != null) {
		long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
		if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
			return true
		}
	}
}

boolean timeInBetween(long lastRunTime, long dateNowTime, long lastModified) {
	if(lastRunTime < lastModified && dateNowTime >= lastModified) {
		return true
	}
	return false
}