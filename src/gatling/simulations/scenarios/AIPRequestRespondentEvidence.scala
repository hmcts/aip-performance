
package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.utils.{Environment, Headers, Common}

import scala.concurrent.duration.DurationInt

/*AIP Request Respondent Evidence is the follow-up to the Appeal journey. Once an appeal has been made, a case worker
logs into XUI and searches using the HMCTS reference captured beforehand. They htne request evidence from the
respondent which will have to be provided from a home office worker.*/

object AIPRequestRespondentEvidence {

val IACURL = Environment.caseURL
val IdAMURL = Environment.idamURL
val MinThinkTime = Environment.minThinkTime
val MaxThinkTime = Environment.maxThinkTime


  //AIP Case management Hompage
  val IAChome = group ("AIP2_010_Homepage") {

    //exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
    exec(http("AIP2_010_Homepage")
      .get(IACURL)
      .check(status.is(200))
      .check(substring("HMCTS Manage cases"))
      .headers(Headers.commonHeader)
      .header("sec-fetch-site", "none"))
      .exitHereIfFailed

    .exec(Common.configurationUI)
    .exec(Common.configJson)
    .exec(Common.configTsAndCs)
    .exec(Common.configUI)
    .exec(Common.userDetailsHome)
    .exec(Common.isAuthenticatedHome)

    .exec(http("AIP2_011_HomePageAuth")
      .get(IACURL + "/auth/login")
      .headers(Headers.commonHeader)
      .check(substring("Sign in"))
      .check(currentLocationRegex("state=(.*?)&").saveAs("state"))
      .check(currentLocationRegex("nonce=(.*?)&").saveAs("nonce"))
      .check(regex("_csrf\" value=\"(.*)\"").saveAs("csrf")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Login to Application as caseworker
  val IACLogin = group ("AIP2_020_Login") {
    exec(_.set("caseworkerId", "6e0fdb4f-bbc1-4eb3-97fd-6d071834acef"))
    .exec(http("AIP2_020_Login")
      .post(IdAMURL + "/login?client_id=xuiwebapp&redirect_uri=" + IACURL + "/oauth2/callback&state=#{state}" +
         "&nonce=#{nonce}&response_type=code&scope=profile%20openid%20roles%20manage-user%20create-user&prompt=")
      .headers(Headers.commonHeader)
      .formParam("username", "ia.caseofficer.ccd@gmail.com")
      .formParam("password", "AldgateT0wer")
      .formParam("selfRegistrationEnabled", "false")
      .formParam("azureLoginEnabled", "true")
      .formParam("mojLoginEnabled", "true")
      .formParam("_csrf", "#{csrf}")
      .check(status.is(200))
      .check(substring("HMCTS Manage cases")))

    .exec(Common.configurationUI)
    .exec(Common.configUI)
    .exec(Common.configTsAndCs)
    .exec(Common.userDetailsLoggedIn)
    .exec(Common.isAuthenticatedLoggedIn)
    .exec(Common.monitoringTools)
    .exec(Common.isAuthenticatedLoggedIn)
    .exec(Common.healthCheck)
    .exec(Common.regionLocation)
    .exec(Common.getMyAccess)
    .exec(Common.getMyAccess)
    .exec(Common.typesOfWork)
    .exec(Common.jurisdiction)
    .exec(Common.caseworkerTasks)
    .exec(Common.jurisdiction)
    .exec(Common.getUsersByServiceName)
  }
  
 .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Searches for Appeal cases Need to capture Case ID- [{"case_id":"1625580429075793","supplementary_data
  val IACSearchCase = group ("AIP2_030_SearchCase") {
    exec(http("AIP2_030_ReadAccess")
      .get(IACURL + "/aggregated/caseworkers/:uid/jurisdictions?access=read")
      .headers(Headers.secondaryHeader)
      .check(substring("callback_get_case_url")))

    .exec(http("AIP2_031_WorkBasket")
      .get(IACURL + "/data/internal/case-types/Asylum/work-basket-inputs")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-workbasket-input-details.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .check(substring("workbasketInputs")))

    .exec(http("AIP2_032_SearchCase")
      .post(IACURL + "/data/internal/searchCases?ctid=Asylum&use_case=WORKBASKET&view=WORKBASKET&page=1" +
        "&case.appealReferenceNumber=#{AppealRef}")
      .headers(Headers.secondaryHeader)
      .check(jsonPath("$.results[0].case_id").saveAs("CaseNumber"))
      .check(status.is(200)))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Case ID Needs to be passed here
  val IACSelectCase = group ("AIP2_040_SelectCase") {
    exec(http("AIP2_040_SelectCase")
      .get(IACURL + "/data/internal/cases/#{CaseNumber}")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json")
      .header("experimental", "true")
      .check(status.is(200))
      .check(regex("Case record for")))

    .exec(Common.manageLabelling)
    .exec(Common.jurisdiction)
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  val IACValidateDetails= group ("AIP2_050_ValidateDetails") {
    exec(http("AIP2_050_ValidateDetails")
      .get(IACURL + "/case/IA/Asylum/#{CaseNumber}/trigger/requestHomeOfficeData")
      .headers(Headers.commonHeader)
      .check(substring("HMCTS Manage cases")))

    .exec(Common.configurationUI)
    .exec(Common.configUI)
    .exec(Common.configTsAndCs)
    .exec(Common.userDetailsLoggedIn)
    .exec(Common.isAuthenticatedLoggedIn)
    .exec(Common.monitoringTools)
    .exec(Common.monitoringTools)

    .exec(http("AIP2_051_ValidateDetails")
      .get(IACURL + "/workallocation/case/tasks/#{CaseNumber}/event/requestHomeOfficeData/caseType/Asylum/jurisdiction/IA")
      .headers(Headers.secondaryHeader)
      .check(substring("task_required_for_event")))

    .exec(http("AIP2_052_ValidateDetails")
      .get(IACURL + "/data/internal/cases/#{CaseNumber}")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json")
      .header("experimental", "true")
      .check(regex("Case record for")))

    .exec(Common.profile)

    .exec(http("AIP2_053_ValidateDetails")
      .get(IACURL + "/data/internal/cases/#{CaseNumber}/event-triggers/requestHomeOfficeData?ignore-warning=false")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-event-trigger.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .check(substring("requestHomeOfficeData"))
      .check(jsonPath("$.event_token").saveAs("event_token")))

    .exec(Common.isAuthenticatedLoggedIn)

    //Wrong duplicate? Happens while idle? True for other instances too?
    .exec(http("AIP2_054_ValidateDetails")
      .get(IACURL + "/workallocation/case/tasks/#{CaseNumber}/event/requestHomeOfficeData/caseType/Asylum/jurisdiction/IA")
      .headers(Headers.secondaryHeader)
      .check(substring("task_required_for_event")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  .group("AIP2_060_RequestHomeOfficeData") {
    exec(http("AIP2_060_ValidateDetails")
      .post(IACURL + "/data/case-types/Asylum/validate?pageId=requestHomeOfficeDatarequestHomeOfficeData")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .body(ElFileBody("RequestHomeOfficeData.json"))
      .check(substring("appellantFullName")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  .group("AIP2_070_CheckYourAnswers") {
    exec(http("AIP2_070_CheckYourAnswers")
      .post(IACURL + "/data/cases/#{CaseNumber}/events")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .body(ElFileBody("MatchAppellantDetails.json"))
      .check(substring("callback_response_status_code")))

    .exec(http("AIP2_071_CheckYourAnswers")
      .get(IACURL + "/data/internal/cases/#{CaseNumber}")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-case-view.v2+json")
      .header("experimental", "true")
      .check(regex("Case record for")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  .group("AIP2_080_ReturnToCase") {
    exec(Common.manageLabelling)
    .exec(Common.jurisdiction)
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Select 'Request Respondent Evidence' via the link  - Case ID Needs to be passed here
  val IACRequestRespondentEvidence = group("AIP2_090_RequestRespondentEvidence") {
    exec(http("AIP2_090_RequestRespondentEvidence")
      .get(IACURL + "/data/internal/cases/#{CaseNumber}/event-triggers/requestRespondentEvidence?ignore-warning=false")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.ui-start-event-trigger.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .check(substring("requestRespondentEvidence"))
      .check(jsonPath("$.event_token").saveAs("event_token")))

    .exec(http("AIP2_091_RequestRespondentEvidence")
      .get(IACURL + "/workallocation/case/tasks/#{CaseNumber}/event/requestRespondentEvidence/caseType/Asylum/jurisdiction/IA")
      .headers(Headers.secondaryHeader)
      .check(substring("task_required_for_event")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  .exec(_.set("deadlineDate", Environment.currentDate.plusDays(14).toString))

  //Select Submit Request
  .group("AIP2_100_SubmitRequestRespondentEvidence") {
    exec(http("AIP2_100_SubmitRequestRespondentEvidence")
      .post(IACURL + "/data/case-types/Asylum/validate?pageId=requestRespondentEvidencerequestRespondentEvidence")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .body(ElFileBody("SubmitRequestRespondentEvidence.json"))
      .check(substring("sendDirectionParties")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Send Direction
  .group("AIP2_110_SendDirection") {
    exec(http("AIP2_110_SendDirection")
      .post(IACURL + "/data/cases/#{CaseNumber}/events")
      .headers(Headers.secondaryHeader)
      .header("accept", "application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v2+json;charset=UTF-8")
      .header("experimental", "true")
      .body(ElFileBody("SendDirection.json"))
      .check(substring("sendDirectionActionAvailable")))
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  .group("AIP2_120_ReturnToCase") {
    exec(Common.manageLabelling)
    .exec(Common.jurisdiction)
  }

  .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Sign out
  val IACLogout = group("AIP2_130_Logout") {
    exec(http("AIP2_130_Logout")
      .get(IACURL + "/auth/logout")
      .headers(Headers.commonHeader)
      .check(substring("Sign in")))
  }





  
//   //Select 'Request Respondent Evidence' via the link  - Case ID Needs to be passed here
//  val IACRequestRespondentEvidence = group ("AIP2_050_RequestRespondentEvidence") {
//    exec(http("AIP2_050_RequestRespondentEvidence")
//      .get("https://manage-case.perftest.platform.hmcts.net/data/internal/cases/#{CaseNumber}/event-triggers/requestRespondentEvidence?ignore-warning=false")
//     //  .headers(Headers.headers_342)
//      .headers(Headers.commonHeader)
//     // .check(CsrfCheck.save)
//      .check(status.is(200))
//      .header("x-xsrf-token", "#{xsrfToken}")
//    //  .check(jsonPath("$.results[0].event_token").saveAs("event_token"))
//      .check(jsonPath("$.event_token").saveAs("event")))
//
//  }
//
//
//  //Select Submit Request
//  val IACSubmitRespondentEvidence = group ("AIP2_060_SubmitRequestRespondentEvidence") {
//    exec(http("AIP2_060_SubmitRequestRespondentEvidence")
//      //.post(IACURL + "/data/case-types/Asylum/validate")
//      .post(IACURL + "/data/case-types/Asylum/validate?pageId=requestRespondentEvidencerequestRespondentEvidence")
//    //  .headers(Headers.headers_367)
//      .headers(Headers.commonHeader)
//      .body(ElFileBody("SubmitRequestRespondentEvidence.json"))
//      .check(regex("Check your answers")))
//  }
//
//  val IACSendRespondentEvidence = group ("AIP2_070_Send") {
//    exec(http("AIP2_060_SubmitRequestRespondentEvidence")
//    .post(IACURL + "/data/cases/#{CaseNumber}/events")
//     // .headers(Headers.headers_386)
//      .headers(Headers.commonHeader)
//      .check(substring("send direction"))
//      .body(ElFileBody("SendDirection.json")))
//  }
}