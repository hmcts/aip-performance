package uk.gov.hmcts.reform.cmc.performance.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import uk.gov.hmcts.reform.cmc.performance.scenarios._
import uk.gov.hmcts.reform.cmc.performance.scenarios.utils.{EmailNotification, Environment}

class CMCSimulation extends Simulation {

  val BaseURL = Environment.baseURL
  val aipuserdetails = csv("AIPUserDetails.csv").circular
  val holetterdetails = csv("HOLetterDetails.csv").circular
  val personaldetails = csv("PersonalDetails.csv").circular

  val httpProtocol = Environment.HttpProtocol
    .baseUrl(BaseURL)
    //.doNotTrackHeader("1")
    .inferHtmlResources()
    .silentResources
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-GB,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0")


  // below scenario is for user data creation for an AIP Citizen User
 /* val UserCreationScenario = scenario("CMC User Creation")
    .exec(
      CreateUser.CreateCitizen("citizen")
        .pause(20)
    )
*/
  val AIPAppeal = scenario("AIP Appeal Journey")
    .repeat(1) {
    exec(AIP_Appeal.home)
    .exec(AIP_Appeal.eligibility)
    .exec(AIP_Appeal.Login)
    .exec(AIP_Appeal.AboutAppeal)
    .exec(AIP_Appeal.HomeOffice)
    .exec(AIP_Appeal.PersonalDetails)
    .exec(AIP_Appeal.ContactDetails)
    .exec(AIP_Appeal.TypeofAppeal)
    .exec(AIP_Appeal.CheckAnswers)
    .exec(AIP_Appeal.AppealOverview)
    .exec(AIP_Appeal.AIPLogout)
    }



  //Scenario to create users - users will be output to AIPUserDetails.csv
/*
    setUp(
  UserCreationScenario.inject(nothingFor(1),rampUsers(100) during (1200))
).protocols(httpProtocol)
*/

  //Scenario which runs through the AIP Appeal Journey.  The Appeal reference number is output into AIPAppealRef.csv
  setUp(
    AIPAppeal.inject(nothingFor(1),rampUsers(1) during (1))
  ).protocols(httpProtocol)
}