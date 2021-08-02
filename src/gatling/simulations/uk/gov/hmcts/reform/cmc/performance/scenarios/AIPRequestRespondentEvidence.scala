
package uk.gov.hmcts.reform.cmc.performance.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{CsrfCheck, CurrentPageCheck, Environment, Headers}

/*AIP Appeal is a new Journey which allows an appellant to go through several eligibility questions and if successful
they can login to the new AIP portal and start an appeal. This script carries out this business journey all the way
until the appeal is submitted and then captures and stores the appeal reference which we can use in business journeys
which are still to be developed*/

object AIPRequestRespondentEvidence {

val IACURL = "https://manage-case.perftest.platform.hmcts.net"
val IdAMURL = Environment.idamURL
val MinThinkTime = Environment.minThinkTime
val MaxThinkTime = Environment.maxThinkTime


  //AIP Case management Hompage
  val IAChome = group ("AIP2_010_Homepage") {
    //exec(flushHttpCache).exec(flushSessionCookies).exec(flushCookieJar)
      exec(http("AIP2_010_Homepage")
        .get(IACURL + "/auth/login")
        .check(CurrentPageCheck.save)
        .check(css("input[name='_csrf']", "value").saveAs("csrf"))
        .check(regex("callback&state=(.*)&nonce=").saveAs("state"))
        .check(regex("nonce=(.*)&response_type=").saveAs("nonce"))
        .check(status.is(200))
        .check(substring("Sign in"))
        .headers(Headers.headers_2))
      .exitHereIfFailed

        .exec(http("AIP2_011_HomepageConfigUI")
          .get(IACURL + "/external/configuration-ui")
          .headers(Headers.headers_71))

        .exec(http("AIP2_012_HomepageConfigJson")
          .get(IACURL + "/assets/config/config.json")
          .headers(Headers.headers_10))

        .exec(http("AIP2_013_HomepageTCEnabled")
          .get(IACURL + "/api/configuration?configurationKey=termsAndConditionsEnabled")
        .headers(Headers.headers_10))

        .exec(http("AIP2_014_HomepageIsAuthenticated")
          .get(IACURL + "/auth/isAuthenticated")
          .headers(Headers.headers_10))

        /*
        .exec(http("AIP2_015_AuthLogin")
          .get(IACURL + "/auth/login")
          .headers(Headers.headers_10))
       //   .check(css("input[name='_csrf']", "value").saveAs("csrfToken"))
      //    .check(regex("manage-user%20create-user%20manage-roles&state=(.*)&client").saveAs("state")))
      */
    }
    .pause(1)

  //Login to Application as caseworker
   val IACLogin = group ("AIP2_020_Login") {
     (exec(http("AIP2_020_Login")
       .post(IdAMURL + "/login?client_id=xuiwebapp&redirect_uri=https://manage-case.perftest.platform.hmcts.net/oauth2/callback&state=${state}&nonce=${nonce}&response_type=code&scope=profile%20openid%20roles%20manage-user%20create-user&prompt=")
       .headers(Headers.headers_64)
       .formParam("username", s"${caseworkeruser}")
       .formParam("password", s"${caseworkerpassword}")
       .formParam("save", "Sign in")
       .formParam("selfRegistrationEnabled", "false")
       .check(status.is(200))
       .check(substring("Manage cases"))
       .formParam("_csrf", "${csrf}")))

       .exec(http("AIP2_021_Login")
         .get(IACURL + "/api/user/details")
         .headers(Headers.headers_10)
         .check(status.in(200, 304))
         .check(headerRegex("Set-Cookie","XSRF-TOKEN=(.*)").saveAs("xsrfToken")))
         //.check(headerRegex("Set-Cookie","__auth__=(.*)").saveAs("authToken")))

       .exec(http("AIP2_022_Login")
         .get(IACURL + "/auth/isAuthenticated")
         .headers(Headers.headers_10)
         .check(status.in(200, 304)))

       .exec(http("AIP2_023_Login")
         .get(IACURL + "/external/config/ui")
         .headers(Headers.headers_10)
         .check(status.in(200, 304)))
   }


     .pause(1)

  //Searches for Appeal cases Need to capture Case ID- [{"case_id":"1625580429075793","supplementary_data

  val IACSearchCase = group ("AIP2_030_SearchCase") {
    exec(http("AIP2_030_SearchCase")
    .post(IACURL + "/data/internal/searchCases?ctid=Asylum&use_case=WORKBASKET&view=WORKBASKET&page=1&case.appealReferenceNumber=PA/50086/2021")
      .check(jsonPath("$.results[0].case_id").saveAs("CaseNumber"))
      .check(status.is(200)))
   //   .header("x-xsrf-token", "${xsrfToken}")
     // .headers(Headers.headers_134)),
  }

    .exec { session =>
      println(session)
      session
    }


  //Case ID Needs to be passed here
  val IACSelectCase = group ("AIP2_040_SelectCase") {
    exec(http("AIP2_040_SelectCase")
      .get(IACURL + "/data/internal/cases/${CaseNumber}")
      .headers(Headers.headers_250)
     // .check(CsrfCheck.save)
      .check(status.is(200))
      .check(regex("Case record for"))),
  }

   ////Select 'Request Respondent Evidence' via the link  - Case ID Needs to be passed here
  val IACRequestRespondentEvidence = group ("AIP2_050_RequestRespondentEvidence") {
    exec(http("AIP2_050_RequestRespondentEvidence")
    //  .get(IACURL + "/data/internal/cases/${CaseNumber}/trigger/requestRespondentEvidence")
      //.get(IACURL + "/case/IA/Asylum/${CaseNumber}/trigger/requestRespondentEvidence")
      .get("https://manage-case.perftest.platform.hmcts.net/data/internal/cases/${CaseNumber}/event-triggers/requestRespondentEvidence?ignore-warning=false")
       .headers(Headers.headers_342)
     // .check(CsrfCheck.save)
      .check(status.is(200))
      .header("x-xsrf-token", "${xsrfToken}")
    //  .check(jsonPath("$.results[0].event_token").saveAs("event_token"))
      .check(jsonPath("$.event_token").saveAs("event")))
      //.check(headerRegex("Set-Cookie","XSRF-TOKEN=(.*)").saveAs("xsrfToken")))
      //.check(substring("HMCTS Manage cases"))),
  }


  //Select Submit Request
  val IACSubmitRespondentEvidence = group ("AIP2_060_SubmitRequestRespondentEvidence") {
    exec(http("AIP2_060_SubmitRequestRespondentEvidence")
      //.post(IACURL + "/data/case-types/Asylum/validate")
      .post(IACURL + "/data/case-types/Asylum/validate?pageId=requestRespondentEvidencerequestRespondentEvidence")
      .headers(Headers.headers_367)
     // .header("x-xsrf-token", "${xsrfToken}")
     // .header("__auth__", "${event}")
     // .header("__userid__", "6e0fdb4f-bbc1-4eb3-97fd-6d071834acef")
      .body(ElFileBody("SubmitRequestRespondentEvidence.json")))
 //     .check(regex("Check your answers"))),
  }

  val IACSendRespondentEvidence = group ("AIP2_070_Send") {
    exec(http("AIP2_060_SubmitRequestRespondentEvidence")
    .post(IACURL + "/data/cases/${CaseNumber}/events")
      .headers(Headers.headers_386)
      .check(substring("send direction"))
      .body(ElFileBody("SendDirection.json"))),
  }
}