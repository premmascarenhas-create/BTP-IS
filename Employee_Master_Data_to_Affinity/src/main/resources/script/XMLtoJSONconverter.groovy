import groovy.json.JsonOutput;
import java.text.SimpleDateFormat

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.xml.internal.bind.v2.runtime.output.XmlOutput;
import groovy.xml.XmlUtil;
import java.text.SimpleDateFormat
import java.util.Date
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.*
import org.apache.camel.converter.stream.InputStreamCache

def Message processData(Message message) {

    String newLine = System.getProperty("line.separator");

    def messageContent = message.getBody(java.lang.String);
    def messageLog = messageLogFactory.getMessageLog(message)

    ObjectMapper mapper = new ObjectMapper()
    def outputdata = new StringBuffer()
    def data = new XmlSlurper().parseText(messageContent)

    def posData = data?.Position
    def posFields = mapper.createArrayNode()
    def posValues
    setPositionFieldNames(posFields)

    //last run datetime values from interface
    def props = message.getProperties()
    String dateNowStr = props.get('DATE_NOW')
    String lastRunStr = props.get('LAST_RUN_DATETIME')

    Date dateNow = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", dateNowStr)
    long dateNowTime = dateNow.getTime()
    Date lastRun = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", lastRunStr)
    long lastRunTime = lastRun.getTime()

    posData.each() { position ->
        def posNode = mapper.createObjectNode()
        posValues = mapper.createArrayNode()
        setPositionFieldValues(posValues, position)
        posNode.put("TableName", "POSTS")
        posNode.put('FieldNames', posFields)
        posNode.put('FieldValues', posValues)
        posNode.put("UpdateMode", "0")
        posNode.put("EffectiveDate", position?.effectiveStartDate?.text()?.take(10))
        if (outputdata.size() == 0){
            outputdata.append('\n')
        }
        outputdata.append(posNode.toString() + '\n')

    }
    data = data?.employee

    data.each{ employeeData ->

        def country = employeeData?.EmpJob?.countryOfCompany?.text()
        def payGroup = employeeData?.EmpCompensation[0]?.payGroup?.text();
        def iscontingentworker = employeeData?.PerPerson?.employmentNav?.EmpEmployment?.isContingentWorker?.text()

        def allowedCountries = ['NZL','AUS'];
        def allowedPayGroups = ['AUS_AFF_MON','NZL_AFF_FOR','NZL_AFF_MON','EXP_MON','NZL_AFF_FOR_ZIT']

        if((!allowedCountries.contains(country) && !allowedPayGroups.contains(payGroup) )|| payGroup == "") {
            return
        }
        
        long validationDatePaymentInformationV3
        long validationDatePerAddressDEFLT
        long validationDateDateEmpJob
        long validationDatePerPersonal
        long validationDateEmpCompensation
        long validationDateEmpCompensationRecurringPayComp
        long validationDateEmpCompensationRecurringPayCompAllowances
        long validationDateNationalID
        long validationDateBiographicalData
        long validationDateEmploymentNav
        long validationDateEmpCostDistribution
        int compensationfilter = 0
        int tempcompensationfilter = 1
        if (employeeData?.PaymentInformationV3 != "" && (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PaymentInformationV3?.lastModifiedDateTime?.text()).getTime() >  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PaymentInformationV3?.effectiveStartDate?.text()).getTime()))
        {
            validationDatePaymentInformationV3 = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PaymentInformationV3?.lastModifiedDateTime?.text()).getTime()
        }
        else if (employeeData?.PaymentInformationV3 != "" && (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PaymentInformationV3?.lastModifiedDateTime?.text()).getTime() <  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PaymentInformationV3?.effectiveStartDate?.text()).getTime()))
        {
            validationDatePaymentInformationV3 = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PaymentInformationV3?.effectiveStartDate?.text()).getTime()
        }
        def PerAddressDEFLTSize = employeeData?.PerAddressDEFLT?.size()
        if (PerAddressDEFLTSize > 0)
        {
            if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerAddressDEFLT?.lastModifiedDateTime?.text()).getTime() >  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerAddressDEFLT?.startDate?.text()).getTime())
            {
                validationDatePerAddressDEFLT = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerAddressDEFLT?.lastModifiedDateTime?.text()).getTime()
            }
            else
            {
                validationDatePerAddressDEFLT = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerAddressDEFLT?.startDate?.text()).getTime()
            }
        }
        if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpJob?.lastModifiedDateTime?.text()).getTime() >  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpJob?.startDate?.text()).getTime())
        {
            validationDateDateEmpJob = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpJob?.lastModifiedDateTime?.text()).getTime()
        }
        else
        {
            validationDateDateEmpJob = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpJob?.startDate?.text()).getTime()
        }

        if (employeeData?.PerPersonal != "" && (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPersonal?.lastModifiedDateTime?.text()).getTime() >  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPersonal?.startDate?.text()).getTime()))
        {
            validationDatePerPersonal = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPersonal?.lastModifiedDateTime?.text()).getTime()
        }
        else if (employeeData?.PerPersonal != "" && (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPersonal?.lastModifiedDateTime?.text()).getTime() <  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPersonal?.startDate?.text()).getTime()))
        {
            validationDatePerPersonal = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPersonal?.startDate?.text()).getTime()
        }
        
        def EmpEmpCompensationSize = employeeData?.EmpCompensation?.size()
        long complastmodifieldcheck = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[compensationfilter]?.lastModifiedDateTime?.text()).getTime()
        long compstartdatecheck = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[compensationfilter]?.startDate?.text()).getTime()
        while(tempcompensationfilter<EmpEmpCompensationSize){
            if (complastmodifieldcheck<Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[tempcompensationfilter]?.lastModifiedDateTime?.text()).getTime() || compstartdatecheck <Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[tempcompensationfilter]?.startDate?.text()).getTime()){
                compensationfilter = tempcompensationfilter
            }
            tempcompensationfilter++
        }


            if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[compensationfilter]?.lastModifiedDateTime?.text()).getTime() >  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[compensationfilter]?.startDate?.text()).getTime())
            {
                validationDateEmpCompensation = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[compensationfilter]?.lastModifiedDateTime?.text()).getTime()
            }
            else
            {
                validationDateEmpCompensation = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCompensation[compensationfilter]?.startDate?.text()).getTime()
            }

        def EmpCostDistributionSize = employeeData?.EmpCostDistribution?.items?.EmpCostDistributionItem?.size()
        if(EmpCostDistributionSize > 0){
            if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCostDistribution?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCostDistribution?.effectiveStartDate?.text()).getTime()) {
                validationDateEmpCostDistribution = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCostDistribution?.lastModifiedDateTime?.text()).getTime()
            } else {
                validationDateEmpCostDistribution = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.EmpCostDistribution?.effectiveStartDate?.text()).getTime()
            }
        }

        def NationalIDSize = employeeData?.PerPerson?.nationalIdNav?.PerNationalId?.size()
        if (NationalIDSize > 0) {
            if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.nationalIdNav?.PerNationalId?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime()) {
                validationDateNationalID = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.nationalIdNav?.PerNationalId?.lastModifiedDateTime?.text()).getTime()
            } else {
                validationDateNationalID = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime()
            }
        }
        if(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime())
        {
            validationDateBiographicalData = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.lastModifiedDateTime?.text()).getTime()
        }
        else
        {
            validationDateBiographicalData = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime()
        }
        if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.lastModifiedDateTime?.text()).getTime() >  Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime())
        {
            validationDateEmploymentNav = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.lastModifiedDateTime?.text()).getTime()
        }
        else
        {
            validationDateEmploymentNav = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime()
        }

        def rootPersonEffPersonal = mapper.createObjectNode()
        def rootPersonEffJob = mapper.createObjectNode()
        def rootPerson = mapper.createObjectNode()
        def rootEmp = mapper.createObjectNode()
        //cost distribution
        def rootEmpCostDistribution = mapper.createObjectNode();
        def rootEmpEffJob = mapper.createObjectNode()
        def rootEmpEffComp = mapper.createObjectNode()
        //def rootEmpEffBank = mapper.createObjectNode()
        //def root3 = mapper.createObjectNode()
        //def root4 = mapper.createObjectNode()
        def root5 = mapper.createObjectNode()
        def root6 = mapper.createObjectNode()
        def root7 = mapper.createObjectNode()
        //def root8 = mapper.createObjectNode()
        //def root9 = mapper.createObjectNode()

        def personEffDate
        def empEffDate
        def historyEffDate
        //def postsEffDate
        def groupdEffDate
        def divisionEffDate
        def deptEffDate
        //def cstEffDate

        def personFieldNamesEffPersonal = mapper.createArrayNode()
        def personFieldValuesEffPersonal = mapper.createArrayNode()
        def personFieldNamesEffJob = mapper.createArrayNode()
        def personFieldValuesEffJob = mapper.createArrayNode()
        def personFieldNames = mapper.createArrayNode()
        def personFieldValues = mapper.createArrayNode()
        def empFieldNames = mapper.createArrayNode()
        def empFieldValues = mapper.createArrayNode()
        def empFieldNamesEffJob = mapper.createArrayNode()
        def empFieldValuesEffJob = mapper.createArrayNode()
        def empFieldNamesEffComp = mapper.createArrayNode()
        def empFieldValuesEffComp = mapper.createArrayNode()
        //cost distribution
        def empCostDistributionFieldNames = mapper.createArrayNode();
        def empCostDistributionFieldValues = mapper.createArrayNode();

        def groupdFieldNames = mapper.createArrayNode()
        def groupdFieldValues = mapper.createArrayNode()
        def divisionFieldNames = mapper.createArrayNode()
        def divisionFieldValues = mapper.createArrayNode()
        def deptFieldNames = mapper.createArrayNode()
        def deptFieldValues = mapper.createArrayNode()

        def userid = employeeData?.PerPersonal?.personIdExternal?.text()

        //updated entity

        setPersonFieldNamesEffPersonal(personFieldNamesEffPersonal)
        setPersonFieldValuesEffPersonal(personFieldValuesEffPersonal, employeeData)

        setPersonFieldNamesEffJob(personFieldNamesEffJob)
        setPersonFieldValuesEffJob(personFieldValuesEffJob, employeeData)

        setPersonFieldNames(personFieldNames)
        setPersonFieldValues(personFieldValues, employeeData)

        setEmpFieldNames(empFieldNames, employeeData)
        setEmpFieldValues(empFieldValues, employeeData)

        setEmpFieldNamesEffJob(empFieldNamesEffJob, employeeData)
        setEmpFieldValuesEffJob(empFieldValuesEffJob, employeeData)


            def compensationRecurringNavSize = employeeData?.EmpCompensation[compensationfilter]?.empPayCompRecurringNav?.EmpPayCompRecurring.size()

            if( compensationRecurringNavSize > 0 ) {
                setEmpFieldNamesEffComp(empFieldNamesEffComp, employeeData)
                setEmpFieldValuesEffComp(empFieldValuesEffComp, employeeData, compensationfilter)
                def compList = employeeData?.EmpCompensation[compensationfilter]?.empPayCompRecurringNav?."*"
                compList.each() { comp ->
                    if ((comp?.payComponent?.text() == 'NZL_ORDSAL' || comp?.payComponent?.text() == 'AUS_BASE' || comp?.payComponent?.text() == 'NZL_ORDWAGE')) {
                        if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", comp?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", comp?.startDate?.text()).getTime())
                        {
                            validationDateEmpCompensationRecurringPayComp = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", comp?.lastModifiedDateTime?.text()).getTime()
                        }
                        else{
                            validationDateEmpCompensationRecurringPayComp = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", comp?.startDate?.text()).getTime()
                        }
                    }

                }
            }


        //cost distribution
        if( EmpCostDistributionSize > 0 ){
            setEmpFieldNames(empCostDistributionFieldNames,employeeData)
            setEmpFieldValues(empCostDistributionFieldValues,employeeData)

        }

        setGroupdFieldNames(groupdFieldNames)
        setGroupdFieldValues(groupdFieldValues, employeeData)

        setDivisionFieldNames(divisionFieldNames)
        setDivisionFieldValues(divisionFieldValues, employeeData)

        setDeptFieldNames(deptFieldNames)
        setDeptFieldValues(deptFieldValues, employeeData)

        groupdEffDate = getGroupdEffDate(employeeData)
        divisionEffDate = getDivisionEffDate(employeeData)
        deptEffDate = getDeptEffDate(employeeData)

        //	build JSON -> PERSON table

        if(timeInBetween(lastRunTime, dateNowTime, validationDatePerPersonal)) {
            personEffDate = employeeData?.PerPersonal?.startDate?.text()?.take(10)
            rootPersonEffPersonal.put("TableName", "PERSON")
            addEffectiveReason(rootPersonEffPersonal, "EffectiveReason", "")
            addFieldNames(rootPersonEffPersonal, "FieldNames", personFieldNamesEffPersonal)
            addFieldValues(rootPersonEffPersonal, "FieldValues", personFieldValuesEffPersonal)
            addEffectiveDate(rootPersonEffPersonal, "EffectiveDate", personEffDate)
            addUpdateMode(rootPersonEffPersonal, "UpdateMode", "0")
            addFieldValuesOld(rootPersonEffPersonal, "FieldValuesOld", "")
            addKeyValue(rootPersonEffPersonal, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootPersonEffPersonal.toString() + '\n')
        }


        if((timeInBetween(lastRunTime, dateNowTime, validationDateDateEmpJob)) || (timeInBetween(lastRunTime, dateNowTime, validationDateEmploymentNav)) || (timeInBetween(lastRunTime, dateNowTime, validationDateEmpCompensation))) {
            personEffDate = employeeData?.EmpJob?.startDate?.text()?.take(10)
            rootPersonEffJob.put("TableName", "PERSON")
            addEffectiveReason(rootPersonEffJob, "EffectiveReason", "")
            addFieldNames(rootPersonEffJob, "FieldNames", personFieldNamesEffJob)
            addFieldValues(rootPersonEffJob, "FieldValues", personFieldValuesEffJob)
            addEffectiveDate(rootPersonEffJob, "EffectiveDate", personEffDate)
            addUpdateMode(rootPersonEffJob, "UpdateMode", "0")
            addFieldValuesOld(rootPersonEffJob, "FieldValuesOld", "")
            addKeyValue(rootPersonEffJob, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootPersonEffJob.toString() + '\n')
        }
        if(timeInBetween(lastRunTime, dateNowTime, validationDatePerAddressDEFLT) || timeInBetween(lastRunTime, dateNowTime, validationDateBiographicalData)) {
            if(validationDatePerAddressDEFLT > validationDateBiographicalData){

                personEffDate = employeeData?.PerAddressDEFLT?.startDate?.text().take(10)

            }else{

                newdate = new Date(validationDateBiographicalData).format("yyyy-MM-dd")
                personEffDate = newdate.toString()
            }
            rootPerson.put("TableName", "PERSON")
            addEffectiveReason(rootPerson, "EffectiveReason", "")
            addFieldNames(rootPerson, "FieldNames", personFieldNames)
            addFieldValues(rootPerson, "FieldValues", personFieldValues)
            addEffectiveDate(rootPerson, "EffectiveDate", personEffDate)
            addUpdateMode(rootPerson, "UpdateMode", "0")
            addFieldValuesOld(rootPerson, "FieldValuesOld", "")
            addKeyValue(rootPerson, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootPerson.toString() + '\n')
        }
        //	build JSON -> EMP table
        if(timeInBetween(lastRunTime, dateNowTime, validationDateDateEmpJob) || timeInBetween(lastRunTime, dateNowTime, validationDateNationalID) || timeInBetween(lastRunTime, dateNowTime, validationDateEmpCompensation)) {
            if (Math.max(validationDateDateEmpJob, validationDateNationalID) == validationDateDateEmpJob){
                if(Math.max(validationDateDateEmpJob,validationDateEmpCompensation) == validationDateDateEmpJob){
                    empEffDate = employeeData?.EmpJob?.startDate?.text()?.take(10)
                }
                else {
                    empEffDate = employeeData?.EmpCompensation[compensationfilter]?.startDate?.text()?.take(10)
                }
            }
            else if(Math.max(validationDateDateEmpJob, validationDateNationalID) == validationDateNationalID){
                if(Math.max(validationDateNationalID,validationDateEmpCompensation) == validationDateNationalID){
                    empEffDate = employeeData?.PerPerson?.nationalIdNav?.PerNationalId?.lastModifiedDateTime?.text()?.take(10)
                }
                else {
                    empEffDate = employeeData?.EmpCompensation[compensationfilter]?.startDate?.text()?.take(10)
                }
            }
            rootEmp.put("TableName", "EMP")
            addEffectiveReason(rootEmp, "EffectiveReason", "")
            addFieldNames(rootEmp, "FieldNames", empFieldNames)
            addFieldValues(rootEmp, "FieldValues", empFieldValues)
            addEffectiveDate(rootEmp, "EffectiveDate", empEffDate)
            addUpdateMode(rootEmp, "UpdateMode", "0")
            addFieldValuesOld(rootEmp, "FieldValuesOld", "")
            addKeyValue(rootEmp, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootEmp.toString() + '\n')
        }

        // build JSON -> EMP table for alternative cost distribution
        if(timeInBetween(lastRunTime, dateNowTime, validationDateEmpCostDistribution)){
            empEffDate = employeeData?.EmpCostDistribution?.effectiveStartDate?.text()?.take(10)
            rootEmpCostDistribution.put("TableName", "EMP")
            addEffectiveReason(rootEmpCostDistribution, "EffectiveReason", "")
            addFieldNames(rootEmpCostDistribution, "FieldNames", empCostDistributionFieldNames)
            addFieldValues(rootEmpCostDistribution, "FieldValues", empCostDistributionFieldValues)
            addEffectiveDate(rootEmpCostDistribution, "EffectiveDate", empEffDate)
            addUpdateMode(rootEmpCostDistribution, "UpdateMode", "0")
            addFieldValuesOld(rootEmpCostDistribution, "FieldValuesOld", "")
            addKeyValue(rootEmpCostDistribution, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootEmpCostDistribution.toString() + '\n')
        }
        // end JSON build

        if(timeInBetween(lastRunTime, dateNowTime, validationDateDateEmpJob)) {
            empEffDate = employeeData?.EmpJob?.startDate?.text()?.take(10)
            rootEmpEffJob.put("TableName", "EMP")
            addEffectiveReason(rootEmpEffJob, "EffectiveReason", "")
            addFieldNames(rootEmpEffJob, "FieldNames", empFieldNamesEffJob)
            addFieldValues(rootEmpEffJob, "FieldValues", empFieldValuesEffJob)
            addEffectiveDate(rootEmpEffJob, "EffectiveDate", empEffDate)
            addUpdateMode(rootEmpEffJob, "UpdateMode", "0")
            addFieldValuesOld(rootEmpEffJob, "FieldValuesOld", "")
            addKeyValue(rootEmpEffJob, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootEmpEffJob.toString() + '\n')
        }
        if(timeInBetween(lastRunTime, dateNowTime, validationDateEmpCompensationRecurringPayComp)) {
            empEffDate = employeeData?.EmpCompensation[compensationfilter]?.startDate?.text()?.take(10)
            rootEmpEffComp.put("TableName", "EMP")
            addEffectiveReason(rootEmpEffComp, "EffectiveReason", "")
            addFieldNames(rootEmpEffComp, "FieldNames", empFieldNamesEffComp)
            addFieldValues(rootEmpEffComp, "FieldValues", empFieldValuesEffComp)
            addEffectiveDate(rootEmpEffComp, "EffectiveDate", empEffDate)
            addUpdateMode(rootEmpEffComp, "UpdateMode", "0")
            addFieldValuesOld(rootEmpEffComp, "FieldValuesOld", "")
            addKeyValue(rootEmpEffComp, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(rootEmpEffComp.toString() + '\n')
        }
        //	build JSON -> GROUPD table
        if(timeInBetween(lastRunTime, dateNowTime, validationDateDateEmpJob)) {
            root5.put("TableName", "GROUPD")
            addEffectiveReason(root5, "EffectiveReason", "")
            addFieldNames(root5, "FieldNames", groupdFieldNames)
            addFieldValues(root5, "FieldValues", groupdFieldValues)
            addEffectiveDate(root5, "EffectiveDate", groupdEffDate.take(10))    //new Date().format('yyyy-MM-dd'))
            addUpdateMode(root5, "UpdateMode", "0")
            addFieldValuesOld(root5, "FieldValuesOld", "")
            addKeyValue(root5, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(root5.toString() + '\n')
        }
        //	build JSON -> DIVISION table
        if(timeInBetween(lastRunTime, dateNowTime, validationDateDateEmpJob)) {
            root6.put("TableName", "DIVISION")
            addEffectiveReason(root6, "EffectiveReason", "")
            addFieldNames(root6, "FieldNames", divisionFieldNames)
            addFieldValues(root6, "FieldValues", divisionFieldValues)
            addEffectiveDate(root6, "EffectiveDate", divisionEffDate.take(10))    //new Date().format('yyyy-MM-dd'))
            addUpdateMode(root6, "UpdateMode", "0")
            addFieldValuesOld(root6, "FieldValuesOld", "")
            addKeyValue(root6, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(root6.toString() + '\n')
        }
        //	build JSON -> DEPT table
        if(timeInBetween(lastRunTime, dateNowTime, validationDateDateEmpJob)) {
            root7.put("TableName", "DEPT")
            addEffectiveReason(root7, "EffectiveReason", "")
            addFieldNames(root7, "FieldNames", deptFieldNames)
            addFieldValues(root7, "FieldValues", deptFieldValues)
            addEffectiveDate(root7, "EffectiveDate", deptEffDate.take(10))    //new Date().format('yyyy-MM-dd'))
            addUpdateMode(root7, "UpdateMode", "0")
            addFieldValuesOld(root7, "FieldValuesOld", "")
            addKeyValue(root7, "IntegrationId", userid)
            if (outputdata.size() == 0){
                outputdata.append('\n')
            }
            outputdata.append(root7.toString() + '\n')
        }
        def compRecord = employeeData?.EmpCompensation
        //	build JSON -> EMP table
        //	PAYMENT INFORMATION
        if(timeInBetween(lastRunTime, dateNowTime, validationDatePaymentInformationV3)) {
            def piList = employeeData?.PaymentInformationV3
            if (piList.size() > 0) {
                def piFields = mapper.createArrayNode()
                def piValues
                setEmpFieldNamesEffBank(piFields)
                piList.each { piData ->
                    def piNode = mapper.createObjectNode()
                    piValues = mapper.createArrayNode()
                    setEmpFieldValuesEffBank(piValues, piData, compRecord)
                    piNode.put("TableName", "EMP")
                    piNode.put("FieldNames", piFields)
                    piNode.put("FieldValues", piValues)
                    piNode.put("UpdateMode", "0")
                    piNode.put("EffectiveDate", piData?.effectiveStartDate?.text().take(10))
                    if (outputdata.size() == 0){
                        outputdata.append('\n')
                    }
                    outputdata.append(piNode.toString() + '\n')
                }
            }
        }
        //	build JSON -> EMPAD table
        //	ONETIMEDEDUCTION
        def otdList 		= employeeData?.OneTimeDeduction
        if (otdList.size() > 0) {
            def otdFields = mapper.createArrayNode()
            def otdValues
            setTimesheetFieldNames(otdFields)
            otdList.each { otdData ->
                long validationDateOneTimeDeduction
                if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", otdData?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", otdData?.deductionDate?.text()).getTime())
                {
                    validationDateOneTimeDeduction = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", otdData?.lastModifiedDateTime?.text()).getTime()
                }
                else
                {
                    validationDateOneTimeDeduction = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", otdData?.deductionDate?.text()).getTime()
                }
                if(timeInBetween(lastRunTime, dateNowTime, validationDateOneTimeDeduction)) {
                    def otdNode = mapper.createObjectNode()
                    otdValues = mapper.createArrayNode()
                    setTimesheetFieldValuesOneTimeDeduction(otdValues,otdData,compRecord)
                    otdNode.put("TableName","TIMESHEET")
                    otdNode.put("FieldNames",otdFields)
                    otdNode.put("FieldValues",otdValues)
                    otdNode.put("UpdateMode","1")
                    otdNode.put("EffectiveDate", otdData?.deductionDate?.text().take(10))
                    if (outputdata.size() == 0){
                        outputdata.append('\n')
                    }
                    outputdata.append(otdNode.toString() + '\n')
                }//end of timeInBetween
                //}//end of lastModifiedStr
            }//end of otdData
        }
        //	EMPPAYCOMPNONRECURRING
        def nrList = employeeData?.EmpPayCompNonRecurring
        if (nrList.size() > 0) {
            def nrFields = mapper.createArrayNode()
            def nrValues
            setTimesheetFieldNames(nrFields)
            nrList.each { nrData ->

                lastModifiedStr = nrData?.lastModifiedDateTime?.text()
                if(lastModifiedStr != '' && lastModifiedStr != null) {
                    long lastModified = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModifiedStr).getTime()
                    if(timeInBetween(lastRunTime, dateNowTime, lastModified)) {
                        if(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", nrData?.payDate?.text()).getTime() >= Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", employeeData?.PerPerson?.employmentNav?.EmpEmployment?.startDate?.text()).getTime()){
                            def nrNode = mapper.createObjectNode()
                            nrValues = mapper.createArrayNode()
                            setTimesheetFieldValuesNonRecurring(nrValues,nrData,compRecord)
                            nrNode.put("TableName","TIMESHEET")
                            nrNode.put("FieldNames",nrFields)
                            nrNode.put("FieldValues",nrValues)
                            nrNode.put("UpdateMode","1")
                            nrNode.put("EffectiveDate", nrData?.payDate?.text().take(10))
                            if (outputdata.size() == 0){
                                outputdata.append('\n')
                            }
                            outputdata.append(nrNode.toString() + '\n')
                        }
                    }//end of timeInBetween
                }//end of lastModifiedStr
            }
        }
        //	RECURRINGDEDUCTIONITEM
        def activeRecurringDeductions = []
        def rdList = employeeData?.RecurringDeduction?.recurringItems?.RecurringDeductionItem
        if (rdList.size() > 0) {
            def rdFields = mapper.createArrayNode()
            def rdValues
            setEmpadFieldNames(rdFields)
            rdList.each { rdData ->
                long validationDateRecurringDeductionItem
                if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.RecurringDeduction_effectiveStartDate?.text()).getTime())
                {
                    validationDateRecurringDeductionItem = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.lastModifiedDateTime?.text()).getTime()
                }
                else
                {
                    validationDateRecurringDeductionItem = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.RecurringDeduction_effectiveStartDate?.text()).getTime()
                }

                if (timeInBetween(lastRunTime, dateNowTime, validationDateRecurringDeductionItem)) {
                    if (rdData?.payComponentType?.text()) {
                        activeRecurringDeductions.add(rdData?.payComponentType?.text())
                    }

                    def rdNode = mapper.createObjectNode()
                    rdValues = mapper.createArrayNode()

                    def userId = employeeData?.RecurringDeduction?.userSysId?.text();
                    setEmpadFieldValuesRecurringDeductions(rdValues, rdData, compRecord, userId)
                    rdNode.put("TableName", "EMPAD")
                    rdNode.put("FieldNames", rdFields)
                    rdNode.put("FieldValues", rdValues)
                    rdNode.put("UpdateMode", "0")
                    rdNode.put("EffectiveDate", rdData?.RecurringDeduction_effectiveStartDate?.text().take(10))
                    if (outputdata.size() == 0){
                        outputdata.append('\n')
                    }
                    outputdata.append(rdNode.toString() + '\n')
                }
                //}
            }
        }

        //	RECURRINGDEDUCTIONITEM-DELETE REQUEST
        def recurringDeductionItem = employeeData?.RecurringDeduction?.recurringItems?.RecurringDeductionItem
        if (recurringDeductionItem.size() > 0) {
            def rdFields = mapper.createArrayNode()
            def rdValues
            setEmpadFieldNames(rdFields)
            recurringDeductionItem.each { rdData ->
                long validationDateRecurringDeductionItem
                if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.RecurringDeduction_effectiveStartDate?.text()).getTime())
                {
                    validationDateRecurringDeductionItem = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.lastModifiedDateTime?.text()).getTime()
                }
                else
                {
                    validationDateRecurringDeductionItem = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rdData?.RecurringDeduction_effectiveStartDate?.text()).getTime()
                }
                if (timeInBetween(lastRunTime, dateNowTime, validationDateRecurringDeductionItem)) {
                    def RecurringDeductionEndDate = rdData?.endDate?.text()
                    if (RecurringDeductionEndDate != "") {
                        def rdNode = mapper.createObjectNode()
                        rdValues = mapper.createArrayNode()
                        setEmpadDeletedRecurringDeductions(rdValues, rdData, compRecord, activeRecurringDeductions)
                        rdNode.put("TableName", "EMPAD")
                        rdNode.put("FieldNames", rdFields)
                        rdNode.put("FieldValues", rdValues)
                        rdNode.put("UpdateMode", "2")
                        rdNode.put("EffectiveDate", rdData?.endDate?.text().take(10))
                        if (outputdata.size() == 0){
                            outputdata.append('\n')
                        }
                        outputdata.append(rdNode.toString() + '\n')
                    }
                }
                //}
            }
        }

        //	EMPPAYCOMPRECURRING
        def rList = employeeData?.EmpCompensation?.empPayCompRecurringNav?."*"
        if (rList.size() > 0) {
            def rFields = mapper.createArrayNode()
            def rValues
            setEmpadFieldNames(rFields)
            rList.each { rData ->
                if (Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rData?.lastModifiedDateTime?.text()).getTime() > Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rData?.startDate?.text()).getTime())
                {
                    validationDateEmpCompensationRecurringPayCompAllowances = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rData?.lastModifiedDateTime?.text()).getTime()
                }
                else{
                    validationDateEmpCompensationRecurringPayCompAllowances = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rData?.startDate?.text()).getTime()
                }
                if (timeInBetween(lastRunTime, dateNowTime, validationDateEmpCompensationRecurringPayCompAllowances)) {
                    def payComponent = rData?.payComponent?.text()
                    if (payComponent != 'NZL_ORDSAL' && payComponent != 'NZL_ORDWAGE' && payComponent != 'AUS_BASE') {
                        def rNode = mapper.createObjectNode()
                        rValues = mapper.createArrayNode()
                        def AffinityStatus = "0"
                        setEmpadFieldValues(rValues, rData, compRecord, AffinityStatus)
                        rNode.put("TableName", "EMPAD")
                        rNode.put("FieldNames", rFields)
                        rNode.put("FieldValues", rValues)
                        rNode.put("UpdateMode", "0")
                        rNode.put("EffectiveDate", rData?.startDate?.text().take(10))
                        if (outputdata.size() == 0){
                            outputdata.append('\n')
                        }
                        outputdata.append(rNode.toString() + '\n')
                    }
                    //   }

                }
            }
        }

        //	EMPPAYCOMPRECURRING - DELETEALOWANCES
        def rAllowancesList = employeeData?.EmpCompensation?.empPayCompRecurringNav?."*"
        if (rAllowancesList.size() > 0) {
            def rFields = mapper.createArrayNode()
            def rValues
            setEmpadFieldNames(rFields)
            rAllowancesList.each { rData ->
                def payComponent = rData?.payComponent?.text()
                if (rData?.endDate?.text().take(10) != "9999-12-31") {
                    if (payComponent != 'NZL_ORDSAL' && payComponent != 'NZL_ORDWAGE' && payComponent != 'AUS_BASE') {
                        def rNode = mapper.createObjectNode()
                        rValues = mapper.createArrayNode()
                        def AffinityStatus = "1"
                        long oldenddate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", rData?.endDate?.text()).getTime()
                        newenddate = new Date(oldenddate).plus(1)format("yyyy-MM-dd")
                        setEmpadFieldValues(rValues, rData, compRecord, AffinityStatus)
                        rNode.put("TableName", "EMPAD")
                        rNode.put("FieldNames", rFields)
                        rNode.put("FieldValues", rValues)
                        rNode.put("UpdateMode", "0")
                        rNode.put("EffectiveDate", newenddate)
                        if (outputdata.size() == 0){
                            outputdata.append('\n')
                        }
                        outputdata.append(rNode.toString() + '\n')
                    }
                }
            }
        }
    }



    //added by regie
    outputdata = outputdata.toString()
    if( outputdata == "" ){
        message.setProperty("HAS_REQUEST","NO")
        messageLog.addAttachmentAsString("Request ( empty ) ", " 0 Request ", 'text/plain')
    }else{
        messageLog.addAttachmentAsString("Request", outputdata.toString(), 'text/plain')
    }

    message.setBody(outputdata)
    message.setHeader('X-CSRF-Header', '')
    message.setHeader('Content-Type', 'application/json')
    return message
}

void putNodeValue(ObjectNode parentNode, String name, String value) {
    if(value) {
        parentNode.put(name, value)
    } else {
        parentNode.putNull(name)
    }
}

void putNodeValueArray(ObjectNode parentNode, String name, Object value) {
    if(value!=null) {
        parentNode.put(name, value)
    } else {
        parentNode.putNull(name)
    }
}

void addEmployeeNumber(ObjectNode root, Object employeeData) {
    def fieldName = "EmployeeNumber"
    def text = employeeData?.PerPersonal?.personIdExternal?.text()
    putNodeValue(root, fieldName, text)
}

void addFieldIds(ObjectNode root, Object fieldIds, Object value) {
    def text = value
    putNodeValue(root, fieldIds, text)
}

void addFieldNames(ObjectNode root, Object fieldNames, Object value) {
    //		def text = value
    //		putNodeValue(root, fieldNames, text)
    putNodeValueArray(root, fieldNames, value)
}

void addKeyValue(ObjectNode root, Object keyValue, Object value) {
    def text = value
    putNodeValue(root, keyValue, text)
}

void addSubkeyValues(ObjectNode root, Object subkeyValues, Object value) {
    def text = value
    putNodeValue(root, subkeyValues, text)
}

void addFieldValues(ObjectNode root, Object fieldValues, Object value) {
    putNodeValueArray(root, fieldValues, value)
}


void addEffectiveDate(ObjectNode root, Object effectiveDate, Object value) {
    def text = value
    putNodeValue(root, effectiveDate, text)
}

void addUpdateMode(ObjectNode root, Object updateMode, Object value) {
    def text = value
    putNodeValue(root, updateMode, text)
}

void addEffectiveReason(ObjectNode root, Object effectiveReason, Object value) {
    def text = value
    putNodeValue(root, effectiveReason, text)
}

void addFieldValuesOld(ObjectNode root, Object fieldValuesOld, Object value) {
    def text = value
    putNodeValue(root, fieldValuesOld, text)
}

void setPositionFieldNames(Object fieldNames) {
    fieldNames.add('POSITION_CODE')
    fieldNames.add('POSITION_TITLE')
    fieldNames.add('REPORTS_TO')
    fieldNames.add('ARCHIVE')
}

void setPositionFieldValues(Object fieldValues, Object data) {

    /**
     *
     * Make sure we trim the values to the following rules :
     *
     * POSITION_TITLE: 40
     *
     * Updated by Regie - 03/28/2019 - Request by Jorge
     *
     */

    def position = data?.externalName_defaultValue?.text()
    def parentPosition = data?.parentPosition?."*"?.code?.text()
    if(position?.length() > 40) {
        position = position?.take(40)	// get first 40 characters only
    }
    fieldValues.add(data?.code?.text())
    fieldValues.add(position)
    fieldValues.add(parentPosition)
    if(data?.effectiveStatus?.text()) {
        fieldValues.add('F')
    } else {
        fieldValues.add('T')
    }
}

void setTeamFieldNames(Object fieldNames) {
    fieldNames.add('CODE')
    fieldNames.add('DESCRIPTION')
    fieldNames.add('ARCHIVE')
}

void setTeamFieldValues(Object fieldValues, Object data) {
    fieldValues.add(data?.externalCode?.text())
    fieldValues.add(data?.externalName?.text())
    fieldValues.add('F')
    //if(data?.mdfSystemStatus?.text() == 'A') {
    //	fieldValues.add('F')
    //} else {
    //	fieldValues.add('T')
    //}
}

void setManagerFieldNames(Object fieldNames) {
    fieldNames.add('CODE')
    fieldNames.add('DESCRIPTION')
}

void setManagerFieldValues(Object fieldValues, Object data) {
    fieldValues.add(data?.EmpJob?.managerUserNav?."*"?.empId?.text())
    fieldValues.add(data?.EmpJob?.managerUserNav?."*"?.firstName?.text() + ' ' + data?.EmpJob?.managerUserNav?."*"?.lastName?.text())
}

void setPersonFieldNamesEffPersonal(Object fieldNames) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("SURNAME")
    fieldNames.add("FIRST_NAMES")

    fieldNames.add("EMP_PREFERRED_NAME")
    //fieldNames.add("TITLE")
    fieldNames.add("SEX")
}

void setPersonFieldValuesEffPersonal(Object fieldValues, Object eData) {

    def userId = detectUserId(eData);

    fieldValues.add(userId)	// EMPLOYEE_NO
    fieldValues.add(eData?.PerPersonal?.lastName?.text()) 			// SURNAME
    fieldValues.add(eData?.PerPersonal?.firstName?.text())	        // FIRST_NAMES

    fieldValues.add(eData?.PerPersonal?.preferredName?.text())		// EMP_PREFERRED_NAME
    //fieldValues.add(eData?.PerPersonal?.salutationNav?."*"?.externalCode?.text())	// TITLE
    fieldValues.add(eData?.PerPersonal?.gender?.text())				// SEX
}


void setPersonFieldNamesEffJob(Object fieldNames) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("PERSON_POSITION")
    fieldNames.add("PERSON_GROUP")
    fieldNames.add("DIVISION")
    fieldNames.add("DEPARTMENT")
    fieldNames.add("BRANCH")
    fieldNames.add("START_DATE")
    fieldNames.add("TERMINATION_DATE")
    fieldNames.add("WORKER_TYPE")
    fieldNames.add("PAY_POINT")
    //fieldNames.add("PAY_PERIOD")
    fieldNames.add("PAY_TYPE")
    //fieldNames.add("PAY_METHOD")
    fieldNames.add("AWARD_ID")
    //fieldNames.add("PAY_LOCATION")
}

void setPersonFieldValuesEffJob(Object fieldValues, Object eData) {

    def userId = detectUserId(eData);

    /**
     *
     * Make sure we trim the values to the following rules :
     * DIVISION: 12
     * DEPT: 12
     * BRANCH: 12
     * PAY_LOCATION: 12 - not in use
     *
     * Updated by Regie - 03/28/2019 - Request by Jorge
     *
     */

    def division_val    = ''
    def dept_val        = ''
    def branch_val      = ''

    division_val = eData?.EmpJob?.division?.text()
    dept_val     = eData?.EmpJob?.department?.text()
    branch_val   = eData?.EmpJob?.customString8Nav?."*"?.externalCode?.text()

    division_val = division_val?.take(12)
    dept_val     = dept_val?.take(12)
    branch_val   = branch_val?.take(12)

    fieldValues.add(userId)	// EMPLOYEE_NO
    fieldValues.add(eData?.EmpJob?.positionNav?."*"?.code?.text())	// PERSON_POSITION
    fieldValues.add(eData?.EmpJob?.businessUnit?.text())	// PERSON_GROUP
    fieldValues.add(division_val)		// DIVISION
    fieldValues.add(dept_val)		// DEPT
    fieldValues.add(branch_val)	// BRANCH
    fieldValues.add(eData?.PerPerson?.employmentNav?."*"?.seniorityDate?.text().take(10)) // START_DATE
    fieldValues.add(eData?.PerPerson?.employmentNav?."*"?.endDate?.text().take(10)) // TERMINATION_DATE

    //fieldValues.add(translateEmpClass(eData?.EmpJob?.positionNav?."*"?.employeeClassNav?."*"?.externalCode?.text())) 	// WORKER_TYPE
    //fieldValues.add(translateEmpClass(eData?.EmpJob?.employeeClassNav?."*"?.externalCode?.text())) 	// WORKER_TYPE - new map
    fieldValues.add(translateEmpClass(eData?.EmpJob?.employeeClassNav?."*"?.externalCode?.text(),eData?.EmpJob?.isFulltimeEmployee.text()))	// WORKER_TYPE - new logic

    // PAY_POINT: If none provided then Affinity will default to 999.
    /*
    String payPoint = eData?.EmpCompensation?.payGroup?.text()
    if (!payPoint) {
        payPoint = "999"
    }
    fieldValues.add(payPoint)
    */

    fieldValues.add(translatePayPoint(eData?.EmpCompensation[0]?.payGroup?.text())) // PAY_POINT

    // PAY_PERIOD
    /*String payPeriodValue = ''
    def noSalary = true
    def compList = eData?.EmpCompensation?.empPayCompRecurringNav?."*"
    compList.each() { comp ->
        if(comp?.payComponent?.text() == 'ANN') {
            payPeriodValue = comp?.frequencyNav?."*"?.externalCode?.text()
            switch(payPeriodValue) {
                case "WKL": case "1": 	payPeriodValue = "1"; break;
                case "FORT": case "2": 	payPeriodValue = "2"; break;
                case "MON": case "M13": case "M14": case "M15": case "M16": case "3": payPeriodValue = "3"; break;
            }
            fieldValues.add(payPeriodValue)
            noSalary = false
        }

    }
    if(noSalary) {
        fieldValues.add('')
    }*/

    def payType = eData?.EmpJob?.employmentTypeNav?."*"?.externalCode?.text()
    if (payType != 'S') {
        payType = 'W'
    }
    fieldValues.add(payType)	// PAY_TYPE
    //fieldValues.add(eData?.EmpCompensation?.payTypeNav?."*"?.externalCode?.text()) // PAY_TYPE - bulls commented field removed

    /*String payMethod = eData?.EmpCompensation?.customString1Nav?."*"?.externalCode?.text()
    if (!payMethod) {
        payMethod = "6"
    }*/
    // PAY_METHOD
    //String payMethod = eData?.PaymentInformationV3?.toPaymentInformationDetailV3?."*"?.payMethod?.text()
    //fieldValues.add(payMethod)

    /** Award ID Logic - added by regie Feb 18, 2019 **/

    def award_id = '4N37.5'; //default value

    def payGroupVal     = eData?.EmpCompensation[0]?.payGroup?.text();
    def empClassVal     = eData?.EmpJob?.employeeClassNav?."*"?.externalCode?.text();
    def standardHoursVal    = eData?.EmpJob?.standardHours?.text();
    def payTypeVal      = eData?.EmpJob?.employmentTypeNav?."*"?.externalCode?.text();

    if(payGroupVal == 'AUS_AFF_MON' ){
        award_id = 'AUST';
    }else if( empClassVal == 'P'){
        award_id = 'CAS'
    }else if(standardHoursVal =='40.0'){
        if(payTypeVal == 'S' ){
            award_id = '4N40'
        }else{
            award_id = '4-40NW'
        }
    }else if(standardHoursVal != '40.0'){
        if(payTypeVal == 'S' ){
            award_id = '4N37.5'
        }else{
            award_id = '4N375W'
        }
    }

    /** End Award ID logic **/

    //fieldValues.add(eData?.EmpJob?.customString12Nav?."*"?.externalCode?.text())	// AWARD_ID
    fieldValues.add(award_id)	// AWARD_ID with logic
    //fieldValues.add(eData?.EmpJob?.location?.text())		// PAY_LOCATION
}

void setPersonFieldNames(Object fieldNames) {
    fieldNames.add("EMPLOYEE_NO")

    fieldNames.add("RES_ADDRESS1")
    fieldNames.add("RES_ADDRESS2")
    fieldNames.add("RES_ADDRESS3")
    fieldNames.add("RES_POSTCODE")
    //fieldNames.add("RES_COUNTRY")	// Following the order in the Spec
    fieldNames.add("EMAIL_WORK")	// Following the order in the Spec
    fieldNames.add("POST_ADDRESS1")
    fieldNames.add("POST_ADDRESS2")
    fieldNames.add("POST_ADDRESS3")
    fieldNames.add("POST_POSTCODE")
    //fieldNames.add("POST_COUNTRY")
    fieldNames.add("PHONE_HOME")	// Following the order in the Spec
    fieldNames.add("MOBILE_PHONE_NO")	// Following the order in the Spec
    fieldNames.add("PHONE_WORK")	// Following the order in the Spec

    fieldNames.add("BIRTH_DATE") 	// Following the order in the Spec

    //fieldNames.add("TERMINATION_DATE")
    fieldNames.add("EMERG_CONTACT")
    fieldNames.add("CONTACT_ADDRESS1")
    fieldNames.add("CONTACT_ADDRESS2")
    fieldNames.add("CONTACT_ADDRESS3")
    fieldNames.add("CONTACT_POSTCODE")
    //	fieldNames.add("CONTACT_COUNTRY")
    fieldNames.add("RELATIONSHIP")
    fieldNames.add("CONTACT_PHONE_HOME")
    fieldNames.add("CONTACT_PHONE_WORK")
    fieldNames.add("CONTACT_MOBILE_PHONE")
    fieldNames.add("PERSONFX3")
}

void setPersonFieldValues(Object fieldValues, Object eData) {

    def userId = detectUserId(eData);

    fieldValues.add(userId)	// EMPLOYEE_NO

    // Processing for Address Data
    def addressData = eData?.PerAddressDEFLT
    int size = addressData.size()
    int counter = 0
    String hasData = null // Flag variable
    // Process addressType = 'home'

    /**
     *
     * Make sure we trim the values to the following rules :
     * RES_ADDRESS1: 25
     * RES_ADDRESS2: 25
     * RES_ADDRESS3: 25
     * RES_COUNTRY: 20 - not in use
     *
     * Updated by Regie - 03/28/2019 - Request by Jorge
     *
     */

    def res_address1_val = ''
    def res_address2_val = ''
    def res_address3_val = ''
    def res_country_val = ''

    while (counter < size) {
        if (eData.PerAddressDEFLT[counter].addressType.text() == "home") {
            hasData = "X" // Flag value to note there is data

            //res_address1_val = eData?.PerAddressDEFLT[counter]?.address2?.text() + ' ' + eData?.PerAddressDEFLT?.address4[counter]?.text()
            res_address1_val = eData?.PerAddressDEFLT[counter]?.address1?.text()
            //res_address2_val = eData?.PerAddressDEFLT[counter]?.county?.text()
            res_address2_val = eData?.PerAddressDEFLT[counter]?.address3?.text()
            res_address3_val = eData?.PerAddressDEFLT[counter]?.city?.text()

            res_address1_val = res_address1_val?.take(25)
            res_address2_val = res_address2_val?.take(25)
            res_address3_val = res_address3_val?.take(25)

            /*
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.address2?.text() + ' ' + eData?.PerAddressDEFLT?.address4[counter]?.text())	// RES_ADDRESS1
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.county?.text())	// RES_ADDRESS2
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.city?.text())		// RES_ADDRESS3
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.zipCode?.text())	// RES_POSTCODE
            //fieldValues.add(translateCountryCode(eData?.PerAddressDEFLT[counter]?.country?.text()))	// RES_COUNTRY
            */

            fieldValues.add(res_address1_val)	// RES_ADDRESS1
            fieldValues.add(res_address2_val)	// RES_ADDRESS2
            fieldValues.add(res_address3_val)	// RES_ADDRESS3
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.zipCode?.text())	// RES_POSTCODE

        }
        counter++
    }
    if (!hasData) {
        fieldValues.add("") // Initialize null to RES_ADDRESS1
        fieldValues.add("") // Initialize null to RES_ADDRESS2
        fieldValues.add("") // Initialize null to RES_ADDRESS3
        fieldValues.add("") // Initialize null to RES_POSTCODE
        //fieldValues.add("") // Initialize null to RES_COUNTRY
    }
    // Process email
    def emailData = eData?.PerPerson?.emailNav?.PerEmail
    size = emailData.size()
    counter = 0
    hasData = null
    while (counter < size) {
        if (eData.PerPerson.emailNav.PerEmail[counter].emailTypeNav.PicklistOption.externalCode.text() == "B") {
            hasData = "X" // Flag value to note there is data
            fieldValues.add(eData?.PerPerson?.emailNav?.PerEmail[counter]?.emailAddress?.text())	// EMAIL_WORK
        }
        counter++
    }
    if (!hasData) {
        fieldValues.add("") // Initialize null to EMAIL_WORK
    }
    // Process addressType = 'postal'
    size = addressData.size()
    counter = 0
    hasData = null // Flag variable
    // Process addressType = 'postal'
    while (counter < size) {
        if (eData.PerAddressDEFLT[counter].addressType.text() == "postal") {
            hasData = "X" // Flag value to note there is data
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.address2?.text() + ' ' + eData?.PerAddressDEFLT?.address4[counter]?.text())	// POST_ADDRESS1
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.county?.text())	// POST_ADDRESS2
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.city?.text())		// POST_ADDRESS3
            fieldValues.add(eData?.PerAddressDEFLT[counter]?.zipCode?.text())	// POST_POSTCODE
            //fieldValues.add(translateCountryCode(eData?.PerAddressDEFLT[counter]?.country?.text()))	// POST_COUNTRY
        }
        counter++
    }
    if (!hasData) {
        fieldValues.add("") // Initialize null to POST_ADDRESS1
        fieldValues.add("") // Initialize null to POST_ADDRESS2
        fieldValues.add("") // Initialize null to POST_ADDRESS3
        fieldValues.add("") // Initialize null to POST_POSTCODE
        //fieldValues.add("") // Initialize null to POST_COUNTRY
    }
    // Process phone
    def phoneData = eData?.PerPerson?.phoneNav?.PerPhone
    size = phoneData.size()
    counter = 0
    hasData = null
    // Process phoneType = 'Landline-Personal'
    while (counter < size) {
        if (eData.PerPerson.phoneNav.PerPhone[counter].phoneTypeNav.PicklistOption.externalCode.text() == "H") {
            hasData = "X" // Flag value to note there is data
            fieldValues.add(eData?.PerPerson?.phoneNav?.PerPhone[counter]?.phoneNumber?.text())	// PHONE_HOME
        }
        counter++
    }
    if (!hasData) {
        fieldValues.add("") // Initialize null to PHONE_HOME
    }
    //
    counter = 0
    hasData = null
    // Process phoneType = 'Mobile-Company'
    while (counter < size) {
        if (eData.PerPerson.phoneNav.PerPhone[counter].phoneTypeNav.PicklistOption.externalCode.text() == "C") {
            hasData = "X" // Flag value to note there is data
            fieldValues.add(eData?.PerPerson?.phoneNav?.PerPhone[counter]?.phoneNumber?.text())	// MOBILE_PHONE_NO
        }
        counter++
    }
    if (!hasData) {
        fieldValues.add("") // Initialize null to MOBILE_PHONE_NO
    }
    //
    counter = 0
    hasData = null
    // Process phoneType = 'Landline-Company'
    while (counter < size) {
        if (eData.PerPerson.phoneNav.PerPhone[counter].phoneTypeNav.PicklistOption.externalCode.text() == "B") {
            hasData = "X" // Flag value to note there is data
            fieldValues.add(eData?.PerPerson?.phoneNav?.PerPhone[counter]?.phoneNumber?.text())	// PHONE_WORK
        }
        counter++
    }
    if (!hasData) {
        fieldValues.add("") // Initialize null to PHONE_WORK
    }
    //
    fieldValues.add(eData?.PerPerson?.dateOfBirth?.text().take(10)) 								// BIRTH_DATE

    // Emergency Contact
    fieldValues.add(eData?.PerEmergencyContacts?.name?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.customString1?.text() + ' ' + eData?.PerEmergencyContacts?.customString2?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.customString3?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.customString4?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.customString5?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.relationshipNav?."*"?."*"?."*"?.label[0]?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.phone?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.secondPhone?.text())
    fieldValues.add(eData?.PerEmergencyContacts?.customString6?.text())
    //fieldValues.add(eData?.EmpJob?.managerUserNav?."*"?.empId?.text()) // PERSONFX3
    fieldValues.add("") // PERSONFX3
}

String getPersonEffDate(Object eData) {
    String lastModPerPersonal = eData?.PerPersonal?.lastModifiedDateTime?.text()
    String lastModPerAddress = eData?.PerAddressDEFLT?.lastModifiedDateTime[0]?.text()
    String lastModEmpJob = eData?.EmpJob?.lastModifiedDateTime?.text()
    String lastModEmpComp = eData?.EmpCompensation[0]?.lastModifiedDateTime?.text()
    String lastModPerPerson = eData?.PerPerson?.lastModifiedDateTime?.text()

    Long PP
    Long PA
    Long EJ
    Long EC
    Long PPS

    if(lastModPerPersonal) {
        //long
        PP = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModPerPersonal).getTime()
    } else {
        PP = 0
    }
    if(lastModPerAddress) {
        //long
        PA = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModPerAddress).getTime()
    } else {
        PA = 0
    }
    if(lastModEmpJob) {
        //long
        EJ = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModEmpJob).getTime()
    } else {
        EJ = 0
    }
    if(lastModEmpComp) {
        //long
        EC = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModEmpComp).getTime()
    } else {
        EC = 0
    }
    if(lastModPerPerson) {
        //long
        PPS = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModPerPerson).getTime()
    } else {
        PPS = 0
    }
    //messageLog.addAttachmentAsString("DATE ATTACHMENT 1", lastModPerPersonal, "text/plain")
    //messageLog.addAttachmentAsString("DATE ATTACHMENT 2", lastModPerAddress, "text/plain")

    long tempDate
    String latestDate
    if(PP > PA) {
        tempDate = PP
        latestDate = lastModPerPersonal
    } else {
        tempDate = PA
        latestDate = lastModPerAddress
    }
    if(EJ > tempDate) {
        tempDate = EJ
        latestDate = lastModEmpJob
    }
    if(EC > tempDate) {
        tempDate = EC
        latestDate = lastModEmpComp
    }
    if(PPS > tempDate) {
        tempDate = PPS
        latestDate = lastModEmpComp
    }

    return latestDate
}

void setEmpFieldNames(Object fieldNames, Object eData) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("COST1")
    fieldNames.add("COST1_PCT")

    fieldNames.add('COST2')
    fieldNames.add('COST2_PCT')
    fieldNames.add('COST3')
    fieldNames.add('COST3_PCT')
    fieldNames.add('COST4')
    fieldNames.add('COST4_PCT')
    fieldNames.add('COST5')
    fieldNames.add('COST5_PCT')

    fieldNames.add("TAX_NUMBER")
    fieldNames.add("TAX_CODE")
}

void setCostDistributionFieldNames(Object fieldNames, Object eData,costCenterCount) {

    fieldNames.add("EMPLOYEE_NO")

    switch(costCenterCount){
        case 1:
            fieldNames.add('COST2')
            fieldNames.add('COST2_PCT')
            break;
        case 2:
            fieldNames.add('COST2')
            fieldNames.add('COST2_PCT')
            fieldNames.add('COST3')
            fieldNames.add('COST3_PCT')
            break;
        case 3:
            fieldNames.add('COST2')
            fieldNames.add('COST2_PCT')
            fieldNames.add('COST3')
            fieldNames.add('COST3_PCT')
            fieldNames.add('COST4')
            fieldNames.add('COST4_PCT')
            break;
        case 4:
            fieldNames.add('COST2')
            fieldNames.add('COST2_PCT')
            fieldNames.add('COST3')
            fieldNames.add('COST3_PCT')
            fieldNames.add('COST4')
            fieldNames.add('COST4_PCT')
            fieldNames.add('COST5')
            fieldNames.add('COST5_PCT')
            break;

        default:
            fieldNames.add('COST2')
            fieldNames.add('COST2_PCT')
            fieldNames.add('COST3')
            fieldNames.add('COST3_PCT')
            fieldNames.add('COST4')
            fieldNames.add('COST4_PCT')
            fieldNames.add('COST5')
            fieldNames.add('COST5_PCT')
            break;
    }

}

void setCostDistributionFieldValues(Object fieldValues, Object eData) {

    def userId = detectUserId(eData);

    fieldValues.add(userId) //EMPLOYEE_NO
    def maxCostCenterEntry = 4;
    def costCenterEntry = 0;
    //additional
    def additionalCostCenters = eData?.EmpCostDistribution?.items?.EmpCostDistributionItem

    additionalCostCenters.each{acc->
        if( costCenterEntry < maxCostCenterEntry ){
            def costCenter = ''
            def percentage = ''

            costCenter = acc?.costCenter?.text();
            percentage = acc?.percentage?.text();

            fieldValues.add(costCenter)
            fieldValues.add(percentage)
            costCenterEntry++;
        }

    }

}

void setEmpFieldValues(Object fieldValues, Object eData) {

    def userId = detectUserId(eData);

    fieldValues.add(userId) //EMPLOYEE_NO

    // Cost Center
    fieldValues.add(eData?.EmpJob?.costCenter?.text() + '')  //COST1
    def altCostList = eData?.EmpCostDistribution?.items?.EmpCostDistributionItem
    def altCostCount = altCostList?.size()

    if(altCostCount) {
        def mainPct = new BigDecimal('100')
        def tempPct
        def temp
        altCostList.each() { altCost ->
            temp = altCost?.percentage?.text()
            tempPct = new BigDecimal(temp)
            mainPct = mainPct.subtract(tempPct)
        }
        fieldValues.add(mainPct.toString()) //COST1_PCT

        altCostList.each() { altCost ->
            fieldValues.add(altCost?.costCenter?.text())
            fieldValues.add(altCost?.percentage?.text())
        }

    } else {
        fieldValues.add('100')
    }
    // Delete any possible Affinity cost centers

    while(altCostCount < 4) {
        fieldValues.addNull()
        fieldValues.addNull()
        altCostCount++
    }

    def taxNumber = eData?.PerPerson?.nationalIdNav?."*"?.nationalId?.text()
    taxNumberSize = eData?.PerPerson?.nationalIdNav?."*"?.size()
    if (taxNumberSize > 1){
        taxNumberList = eData?.PerPerson?.nationalIdNav?.PerNationalId
        taxNumberList.each(){ ird ->
            if (ird?.cardType?.text() == "IRD"){
                taxNumber = ird?.nationalId?.text()
            }
        }
    }
    taxNumber = taxNumber.replaceAll("\\s","")
    fieldValues.add(taxNumber) //TAX_NUMBER
    fieldValues.add(eData?.EmpCompensation[0]?.customString1Nav?.PicklistOption?.mdfExternalCode?.text())    //TAX_CODE

}

void setEmpFieldNamesEffJob(Object fieldNames, Object eData) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("STD_HOURS_PER_WEEK")
    fieldNames.add("STD_HOURS_PER_DAY")
    fieldNames.add("EMPFX1")
    fieldNames.add("EMPFX2")
    fieldNames.add("EMPFX3")
}

void setEmpFieldValuesEffJob(Object fieldValues, Object eData) {

    def userId = detectUserId(eData);

    fieldValues.add(userId) //EMPLOYEE NO.
    fieldValues.add(eData?.EmpJob?.standardHours?.text())
    // Standard Hours per Day
    if(eData?.EmpJob?.workscheduleCodeNav?."*"?.averageWorkingDaysPerWeek?.text()) {
        fieldValues.add(eData?.EmpJob?.workscheduleCodeNav?."*"?.averageHoursPerDay?.text())
    } else {
        //	String hours = eData?.EmpJob?.standardHours?.text() / 5
        //	fieldValues.add(hours)
        String numberStr = eData?.EmpJob?.standardHours?.text()
        if(!numberStr) {
            numberStr = '0'
        }
        BigDecimal number = new BigDecimal(numberStr)
        BigDecimal number2 = new BigDecimal('5')
        String result = number.divide(number2, 2, BigDecimal.ROUND_HALF_UP).toString()
        fieldValues.add(result)
    }
    // Emp FX fields
    fieldValues.add(eData?.EmpJob?.customString11Nav?."*"?.externalCode?.text())	// EMPFX1
    fieldValues.add(eData?.EmpJob?.customString9Nav?."*"?.externalCode?.text())		// EMPFX2
    fieldValues.add(eData?.EmpJob?.customString10Nav?."*"?.externalCode?.text())	// EMPFX3
}

void setEmpFieldNamesEffComp(Object fieldNames, Object eData) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("ANNUAL_SALARY")
    fieldNames.add("RATE1")
    //fieldNames.add("RATE2")
    //fieldNames.add("RATE3")
}

void setEmpFieldValuesEffComp(Object fieldValues, Object eData, int compfilter) {

    def userId = detectUserId(eData);

    fieldValues.add(userId) //EMPLOYEE_NO
    def noSalary = true
    def standardHours;

    standardHours = eData?.EmpJob?.standardHours?.text();
    standardHours = new BigDecimal(standardHours)

    def compList = eData?.EmpCompensation[compfilter]?.empPayCompRecurringNav?."*"
    // Annual salary
    //fieldValues.add(eData?.EmpCompensation?.customLong1?.text()) //ANNUAL_SALARY
    boolean has_annual_salary = false;
    boolean has_ordwage = false;
    def annual_salary_value;
    def ordwage_value;
    def rate1_value;
    //new loop for Annual salary
    compList.each() { comp ->
        if( (comp?.payComponent?.text() == 'NZL_ORDSAL' || comp?.payComponent?.text() == 'AUS_BASE' ) ) {
            //fieldValues.add(comp?.paycompvalue?.text())
            annual_salary_value = comp?.paycompvalue?.text()
            //rate1 computation
            //annual_salary_value_converted = new BigDecimal(annual_salary_value)

            //BigDecimal divisor = new BigDecimal('2')
            //divisor = standardHours*52;

            //if( divisor.toString() == "0.0" ){
            //    rate1_value = '0'
            //}else{
            //    rate1_value = annual_salary_value_converted.divide(divisor,5,BigDecimal.ROUND_HALF_UP).toString();
            //}
            rate1_value = ''
            has_annual_salary = true;
        }

        if(comp?.payComponent?.text() == 'NZL_ORDWAGE') {
            //fieldValues.add(comp?.paycompvalue?.text())
            ordwage_value = comp?.paycompvalue?.text();
            //rate1 computation
            //ordwage_value_converted = new BigDecimal(ordwage_value)
            //annual_salary_value = standardHours * 52 * ordwage_value_converted
            //annual_salary_value = annual_salary_value.toString();
            annual_salary_value = ''
            rate1_value = ordwage_value
            has_ordwage = true;
        }
    }


    if( has_annual_salary ){

        fieldValues.add(annual_salary_value)
        fieldValues.add(rate1_value)


    }else {

        if( has_ordwage ){
            fieldValues.add(annual_salary_value)
            fieldValues.add(rate1_value)
        }else{
            fieldValues.add('')
            fieldValues.add('')
        }
    }

}

//void setEmpFieldNamesEffBank(Object fieldNames, Object eData) {
void setEmpFieldNamesEffBank(Object fieldNames) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("BAL_ACCT")
    fieldNames.add("BAL_ACCT_NAME")
    fieldNames.add("PAY_METHOD")

}

void setEmpFieldValuesEffBank(Object fieldValues, Object eData,Object empData) {
    //fieldValues.add(eData?.PerPersonal?.personIdExternal?.text()) //EMPLOYEE_NO

    def payroll_id = empData?.payrollId[0]?.text();
    def userId = "";

    if( payroll_id == "" ){

        userId = eData?.worker?.text()

    }else{

        userId 	= payroll_id;

    }

    //fieldValues.add(eData?.worker?.text()) //EMPLOYEE_NO
    fieldValues.add(userId)

    // Bank Information
    def bankList = eData?.toPaymentInformationDetailV3?."*"
    def bankType
    //def noMainBank = true
    def accountNumber = ''
    bankList.each() { bank ->
        bankType = bank?.payType?.text()
        if(bankType == 'MAIN') {
            //accountNumber = bank?.cust_bank?.text() + '-' + bank?.cust_branch?.text() + '-' + bank?.accountNumber?.text() + '-' + bank?.cust_suffix?.text()
            accountNumber = bank?.routingNumber?.text() + bank?.accountNumber?.text() //EMPLOYEE_NO
            fieldValues.add(accountNumber) //BAL_ACCT
            fieldValues.add(bank?.accountOwner?.text()) //BAL_ACCT_NAME
            fieldValues.add('') //PAY_METHOD
            //noMainBank = false
        }
    }
}

String getEmpEffDate(Object eData) {
    String lastModEmpJob = eData?.EmpJob?.lastModifiedDateTime?.text()
    String lastModEmpComp = eData?.EmpCompensation[0]?.lastModifiedDateTime?.text()
    String lastModPerPerson = eData?.PerPerson?.lastModifiedDateTime?.text()
    String lastModPayInfoV3 = eData?.PaymentInformationV3?.lastModifiedDateTime?.text()

    Long EJ
    Long EC
    Long PPS
    Long PIV

    if(lastModEmpJob) {
        //long
        EJ = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModEmpJob).getTime()
    } else {
        EJ = new Long('0')
    }
    if(lastModEmpComp) {
        //long
        EC = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModEmpComp).getTime()
    } else {
        EC = new Long('0')
    }
    if(lastModPerPerson) {
        //long
        PPS = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModPerPerson).getTime()
    } else {
        PPS = new Long('0')
    }
    if(lastModPayInfoV3) {
        //long
        PIV = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", lastModPayInfoV3).getTime()
    } else {
        PIV = new Long('0')
    }


    long tempDate
    String latestDate
    if(EJ > EC) {
        tempDate = EJ
        latestDate = lastModEmpJob
    } else {
        tempDate = EC
        latestDate = lastModEmpComp
    }
    if(PPS > tempDate) {
        tempDate = PPS
        latestDate = lastModPerPerson
    }
    if(PIV > tempDate) {
        tempDate = PIV
        latestDate = lastModPayInfoV3
    }

    return latestDate

}

void setHistoryFieldNames(Object fieldNames) {
    fieldNames.add("REASON")
    //fieldNames.add("EFFECTIVE_DATE")
}

void setHistoryFieldValues(Object fieldValues, Object eData) {
    fieldValues.add(eData?.EmpJob?.positionNav?."*"?.changeReason?.text())
    //fieldValues.add(eData?.EmpCompensation?.empPayCompRecurringNav?."*"?.startDate?.text())
}
String getHistoryEffDate(Object eData) {
    String latestDate = eData?.EmpJob?.lastModifiedDateTime?.text()

    return latestDate
}

void setGroupdFieldNames(Object fieldNames) {
    fieldNames.add("CODE")
    fieldNames.add("DESCRIPTION")

}

void setGroupdFieldValues(Object fieldValues, Object eData) {
    fieldValues.add(eData?.EmpJob?.businessUnit?.text())
    fieldValues.add(eData?.EmpJob?.businessUnitNav?."*"?.name?.text())
}

String getGroupdEffDate(Object eData) {
    String latestDate = eData?.EmpJob?.startDate?.text()

    return latestDate
}

void setDivisionFieldNames(Object fieldNames) {
    fieldNames.add("CODE")
    fieldNames.add("DESCRIPTION")
}

void setDivisionFieldValues(Object fieldValues, Object eData) {
    fieldValues.add(eData?.EmpJob?.division?.text())
    fieldValues.add(eData?.EmpJob?.divisionNav?."*"?.name?.text())
}

String getDivisionEffDate(Object eData) {
    String latestDate = eData?.EmpJob?.startDate?.text()

    return latestDate
}

void setDeptFieldNames(Object fieldNames) {
    fieldNames.add("CODE")
    fieldNames.add("DESCRIPTION")
}

void setDeptFieldValues(Object fieldValues, Object eData) {
    fieldValues.add(eData?.EmpJob?.department?.text())
    fieldValues.add(eData?.EmpJob?.departmentNav?."*"?.name?.text())
}

String getDeptEffDate(Object eData) {
    String latestDate = eData?.EmpJob?.startDate?.text()

    return latestDate
}

/*void setCstFieldNames(Object fieldNames) {
 fieldNames.add("COST1_DESC")
 }
 void setCstFieldValues(Object fieldValues, Object eData) {
 fieldValues.add(eData?.EmpJob?.costCentertNav?."*"?.name?.text())
 }
 String getCstEffDate(Object eData) {
 String latestDate = eData?.EmpJob?.lastModifiedDateTime?.text()
 return latestDate
 }*/
void setTimesheetFieldNames(Object fieldNames){
    /*
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("TS_CODE")
    fieldNames.add("TS_NET")
    fieldNames.add("TS_DATE")
    fieldNames.add("TS_STATUS")
    */

    /*Updated fields after this is not a deffect but an additionl request*/
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("TS_CODE")
    fieldNames.add("TS_NET")  //will have a defaut value of '1.0'
    fieldNames.add("TS_RATE") //new field will hold the amount of pay component
    fieldNames.add("TS_DATE")
    fieldNames.add("TS_STATUS")

}
void setTimesheetFieldValuesOneTimeDeduction(Object fieldValues, Object data,Object empData) {

    def payroll_id = empData?.payrollId[0]?.text();
    def userId = "";

    if( payroll_id == "" ){

        userId = data?.userSysId?.text()
        if (!userId) {
            userId = data?.userId?.text()
            if(!userId) {
                userId = data?.RecurringDeduction_userSysId?.text()
            }
        }

    }else{

        userId 	= payroll_id;

    }


    def payComponent = data?.payComponentType?.text()
    if (!payComponent) {
        payComponent = data?.payComponent?.text()
        if (!payComponent) {
            payComponent = data?.payComponentCode?.text()
        }

    }

    payComponent = translatePayComponent(payComponent)

    def amount = data?.amount?.text()
    if (!amount) {
        amount = data?.value?.text()
        if (!amount) {
            amount = data?.paycompvalue?.text()
        }
    }

    def deductionDate = data?.deductionDate?.text().take(10)

    fieldValues.add(userId)
    fieldValues.add(payComponent)
    fieldValues.add("1.0")
    fieldValues.add(amount)
    fieldValues.add(deductionDate)
    fieldValues.add("3")
}

void setTimesheetFieldValuesNonRecurring(Object fieldValues, Object data,Object empData) {

    def payroll_id = empData?.payrollId[0]?.text();
    def userId = "";

    if( payroll_id == "" ){

        userId = data?.userSysId?.text()
        if (!userId) {
            userId = data?.userId?.text()
            if(!userId) {
                userId = data?.RecurringDeduction_userSysId?.text()
            }
        }

    }else{

        userId 	= payroll_id;

    }


    def payComponent = data?.payComponentType?.text()
    if (!payComponent) {
        payComponent = data?.payComponent?.text()
        if (!payComponent) {
            payComponent = data?.payComponentCode?.text()
        }

    }

    payComponent = translatePayComponent(payComponent)

    def amount = data?.amount?.text()
    if (!amount) {
        amount = data?.value?.text()
        if (!amount) {
            amount = data?.paycompvalue?.text()
        }
    }

    def payDate = data?.payDate?.text().take(10)

    fieldValues.add(userId)
    fieldValues.add(payComponent)
    fieldValues.add("1.0")
    fieldValues.add(amount)
    fieldValues.add(payDate)
    fieldValues.add("3")
}

void setEmpadFieldNames(Object fieldNames) {
    fieldNames.add("EMPLOYEE_NO")
    fieldNames.add("CODE")
    fieldNames.add("RATE")
    fieldNames.add("STATUS")
}

void setEmpadFieldValues(Object fieldValues, Object data,Object empData, String AffinityStatus) {

    def payroll_id = empData?.payrollId[0]?.text();
    def userId = "";

    if( payroll_id == "" ){

        userId = data?.userSysId?.text()
        if (!userId) {
            userId = data?.userId?.text()
            if(!userId) {
                userId = data?.RecurringDeduction_userSysId?.text()
            }
        }

    }else{

        userId 	= payroll_id;

    }


    def payComponent = data?.payComponentType?.text()
    if (!payComponent) {
        payComponent = data?.payComponent?.text()
        if (!payComponent) {
            payComponent = data?.payComponentCode?.text()
        }

    }

    payComponent = translatePayComponent(payComponent)

    def amount = data?.amount?.text()
    if (!amount) {
        amount = data?.value?.text()
        if (!amount) {
            amount = data?.paycompvalue?.text()
        }
    }

    fieldValues.add(userId)
    fieldValues.add(payComponent)
    fieldValues.add(amount)
    fieldValues.add(AffinityStatus)
}

void setEmpadFieldValuesRecurringDeductions(Object fieldValues, Object data,Object empData,userIdFromRecurringDeduction) {

    def payroll_id = empData?.payrollId[0]?.text();
    def userId = "";

    if( payroll_id == "" ){

        userId = userIdFromRecurringDeduction

    }else{

        userId 	= payroll_id;

    }

    println XmlUtil.serialize(data)

    def payComponent = data?.payComponentType?.text()
    if (!payComponent) {
        payComponent = data?.payComponent?.text()
        if (!payComponent) {
            payComponent = data?.payComponentCode?.text()
        }
    }
    payComponent = translatePayComponent(payComponent)

    def amount = data?.amount?.text()
    if (!amount) {
        amount = data?.value?.text()
        if (!amount) {
            amount = data?.paycompvalue?.text()
        }
    }

    fieldValues.add(userId)
    fieldValues.add(payComponent)
    fieldValues.add(amount)

    def endDate = data?.endDate?.text();
    if( endDate && endDate !=''){
        fieldValues.add("1")
    }else{
        fieldValues.add("0")
    }

}

void setEmpadDeletedRecurringDeductions(Object fieldValues, Object data,Object empData,activeRecurringDeductions) {

    def payroll_id = empData?.payrollId[0]?.text();
    def userId = "";

    if( payroll_id == "" ){
        userId = data?.userSysId?.text()
        if (!userId) {
            userId = data?.userId?.text()
            if(!userId) {
                userId = data?.RecurringDeduction_userSysId?.text()
            }
        }

    }else{

        userId 	= payroll_id;

    }

    def payComponent = data?.payComponentType?.text()
    if (!payComponent) {
        payComponent = data?.payComponent?.text()
        if (!payComponent) {
            payComponent = data?.payComponentCode?.text()
        }
    }
    payComponent = translatePayComponent(payComponent)

    def amount = data?.amount?.text()
    if (!amount) {
        amount = data?.value?.text()
        if (!amount) {
            amount = data?.paycompvalue?.text()
        }
    }

    fieldValues.add(userId)
    fieldValues.add(payComponent)
    fieldValues.add(amount)
    fieldValues.add("1")

}

String translateCountryCode(String sfCountryCode) {
    switch(sfCountryCode) {
        case 'NZL':
            return 'NZ'
        default:
            return ''
    }
}

String translateEmpClass(String empClass,String isFulltimeEmployee) {
    switch(empClass) {
        case ['A','E']: //Full-time
            if (isFulltimeEmployee == 'true') {
                return '1'
            } else {
                return '2'
            }
        case 'P': //Casual Paid
            return '3'
        case ['I','M']: //Temporary
            return '0'
        case 'U': //Apprentice
            return '6'
        case ['R','T']: //Contractor
            return '7'
        default:
            return empClass
    }
}

String translatePayPoint(String payGroup) {
    switch(payGroup) {
        case 'AUS_AFF_MON':
            return '10'
        case 'NZL_AFF_FOR':
            return '1'
        case ['NZL_AFF_MON','NZL_EXP_MON']:
            return '2'
        case ['NZL_AFF_FOR_ZIT']:
            return '12'
        case ['EXP_MON']:
            return '11'
        default:
            return '999'
    }
}

String translatePayComponent(String payComponent) {
    switch(payComponent) {
        case 'NZL_BONUS':
            return 'BONUS'
        case 'NZL_ACC1WEEK':
            return 'ACC1WK'
        case 'NZL_CASHOUT':
            return 'ALCASH'
        case 'AUS_PAIDAL':
            return 'ALCASH'
        case 'AUS_BONUS':
            return 'BONUSA'
        case 'NZL_BACKPAY':
            return 'BPAY'
        case 'AUS_BACKPAY':
            return 'BPAYA'
        case 'NZL_COLA':
            return 'COLA'
        case 'NZL_DEATHDISAB':
            return 'D&D'
        case 'NZL_EXPENSES':
            return 'EXP RE'
        case 'NZL_MISC':
            return 'MISC'
        case 'NZL_NOTICE':
            return 'NOTICE'
        case 'NZL_ONCALL':
            return 'ONCALL'
        case 'NZL_REDUND':
            return 'REDUN1'
        case 'NZL_TAXABLE':
            return 'TAX AL'
        case 'NZL_SOCIALCLUB':
            return 'TGASC'
        case 'NZL_AMB':
            return 'TOURS4'
        case 'NZL_IRD':
            return 'IRD'
        case 'NZL_CHILD':
            return 'IRDNCP'
        case 'NZL_JURY':
            return 'JURY'
        case 'NZL_SUPERLIFE':
            return 'SUPLIF'
        case 'NZL_TAXEQ':
            return 'TAXEQL'
        case 'NZL_SOCIALCLUBEVENT':
            return 'TGASCF'
        case 'NZL_MILEAGE':
            return 'MILE76'
        case 'NZL_SUNDRY':
            return 'SUNDRY'
        case 'NZL_SETTLE':
            return 'SETTLE'
        case 'NZL_HDA':
            return 'HDA'
        case 'NZL_ADA':
            return 'ADA'
        case 'NZL_SONBONUS':
            return 'SBONUS'
        case 'NZL_COMPLETEBONUS':
            return 'CBONUS'
        case 'ERP_REWARD':
            return 'ERP'
        default:
            return payComponent
    }
}

String detectUserId(Object eData){

    def user_id	= eData?.EmpCompensation[0]?.payrollId[0]?.text();

    if( user_id == "" ){
        user_id = eData?.PerPersonal?.personIdExternal?.text()
    }

    return user_id;
}

boolean timeInBetween(long lastRunTime, long dateNowTime, long lastModified) {
    if(lastRunTime < lastModified && dateNowTime >= lastModified) {
        return true
    }
    return false
}