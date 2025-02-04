package simulations

import io.gatling.core.Predef._
import io.gatling.core.pause.PauseType
import io.gatling.core.scenario.Simulation
import scenarios.{AIPRequestRespondentEvidence, AIP_Appeal, CreateUser, DeleteUser}
import scenarios.utils.Environment


class AIPSimulation extends Simulation {

  val BaseURL = Environment.baseURL
  val aipuserdetails = csv("AIPUserDetails.csv").circular
  val holetterdetails = csv("HOLetterDetails.csv").circular
  val personaldetails = csv("PersonalDetails.csv").circular

  /* ******************************** */

  /* TEST TYPE DEFINITION */
  /* pipeline = nightly pipeline against the AAT environment (see the Jenkins_nightly file) */
  /* perftest (default) = performance test against the perftest environment */
  val testType = scala.util.Properties.envOrElse("TEST_TYPE", "pipeline")

  //set the environment based on the test type
  val environment = testType match {
   case "perftest" => "perftest"
   case "aat" => "aat"
   case "pipeline" => "aat"
   case _ => "perftest"
  }
    /* ******************************** */

    /* ADDITIONAL COMMAND LINE ARGUMENT OPTIONS */
    val debugMode = System.getProperty("debug", "off") //runs a single user e.g. ./gradle gatlingRun -Ddebug=on (default: off)
    val env = System.getProperty("env", environment) //manually override the environment aat|perftest e.g. ./gradle gatlingRun -Denv=aat
    /* ******************************** */
  
  //If running in debug mode, disable pauses between steps
  val pauseOption: PauseType = debugMode match {
    case "off" => constantPauses
    case _ => disabledPauses
  }
  /* ******************************** */
  
  /* PIPELINE CONFIGURATION */
  val numberOfPipelineUsers: Double = 5
  /* ******************************** */
  
  
  val httpProtocol = Environment.HttpProtocol
   .baseUrl(Environment.baseURL.replace("${env}", s"${env}"))
    //.doNotTrackHeader("1")
    .inferHtmlResources()
    .silentResources
  
  before {
    println("Test Type: #{testType}")
    println("Test Environment: #{env}")
    println("Debug Mode: #{debugMode}")
  }
  

  // below scenario is for user data creation for an AIP Citizen User
  val UserCreationScenario = scenario("CMC User Creation")
    .exec(_.set("env", s"${env}"))
    .exec(
      CreateUser.CreateCitizen("citizen")
        .pause(20)
    )

  val AIPAppeal = scenario("AIP Appeal Journey")
    .exitBlockOnFail{
      exec(_.set("env", s"${env}")
      .set("caseType", "AIP"))
      .exec(AIP_Appeal.home)
//      .exec(AIP_Appeal.eligibility)
      .exec(AIP_Appeal.LoginHomePage)
      .exec(AIP_Appeal.Login)
//      .exec(AIP_Appeal.AppealOverview1)
      .exec(AIP_Appeal.AboutAppeal)
      .exec(AIP_Appeal.TypeofAppeal)
      .exec(AIP_Appeal.HomeOffice)
      .exec(AIP_Appeal.PersonalDetails)
      .exec(AIP_Appeal.ContactDetails)
      .exec(AIP_Appeal.DecisionType)
      .exec(AIP_Appeal.PayNow)
//      .exec(AIP_Appeal.Equality)
      .exec(AIP_Appeal.FeeSupport)
      .exec(AIP_Appeal.CheckAnswers)
      .exec(AIP_Appeal.AppealOverview)
      .exec(AIP_Appeal.AIPLogout)
    }
  

  val AIPRequestRespondent = scenario("IAC Request Respondent Evidence")
    .exitBlockOnFail{
      exec(_.set("env", s"${env}"))
      .exec(AIPRequestRespondentEvidence.IAChome)
      .exec(AIPRequestRespondentEvidence.IACLogin)
      .exec(AIPRequestRespondentEvidence.IACSearchCase)
      .exec(AIPRequestRespondentEvidence.IACSelectCase)
      .exec(AIPRequestRespondentEvidence.IACValidateDetails)
      .exec(AIPRequestRespondentEvidence.IACRequestRespondentEvidence)
      .exec(AIPRequestRespondentEvidence.IACLogout)
//      .exec(AIPRequestRespondentEvidence.IACSubmitRespondentEvidence)
//      .exec(AIPRequestRespondentEvidence.IACSendRespondentEvidence)
    }

  val CombinedScenario = scenario("IAC e2e")
    .exitBlockOnFail {
      exec(_.set("env", s"${env}"))
        .exec(
          CreateUser.CreateCitizen("citizen")
          .pause(10))
        .exec(AIP_Appeal.home)
        .exec(AIP_Appeal.LoginHomePage)
        .exec(AIP_Appeal.Login)
        .exec(AIP_Appeal.AboutAppeal)
        .exec(AIP_Appeal.TypeofAppeal)
        .exec(AIP_Appeal.HomeOffice)
        .exec(AIP_Appeal.PersonalDetails)
        .exec(AIP_Appeal.ContactDetails)
        .exec(AIP_Appeal.DecisionType)
        .exec(AIP_Appeal.PayNow)
        .exec(AIP_Appeal.FeeSupport)
        .exec(AIP_Appeal.CheckAnswers)
        .exec(AIP_Appeal.AppealOverview)
        .exec(AIP_Appeal.AIPLogout)
        .exec(AIPRequestRespondentEvidence.IAChome)
        .exec(AIPRequestRespondentEvidence.IACLogin)
        .exec(AIPRequestRespondentEvidence.IACSearchCase)
        .exec(AIPRequestRespondentEvidence.IACSelectCase)
        .exec(AIPRequestRespondentEvidence.IACValidateDetails)
        .exec(AIPRequestRespondentEvidence.IACRequestRespondentEvidence)
        .exec(AIPRequestRespondentEvidence.IACLogout)
    }
    .exec(DeleteUser.deleteUser)


  //Scenario to create users - users will be output to AIPUserDetails.csv

/* setUp(
  UserCreationScenario.inject(nothingFor(1),rampUsers(100) during (600))
).protocols(httpProtocol)
*/

  //Scenario which runs through the AIP Appeal Journey.  The Appeal reference number is output into AIPAppealRef.csv
  //this was run previous for 69 users with a 2400 rampup
 setUp(
    //AIPAppeal.inject(nothingFor(1),rampUsers(1) during (100))
    //AIPRequestRespondent.inject(nothingFor(1),rampUsers(1) during (1))
   CombinedScenario.inject(rampUsers(5).during(250))
  ).protocols(httpProtocol)


  // CaseWorker progressing the case to request respondent evidence
 /* setUp(
    AIPRequestRespondent.inject(nothingFor(1),rampUsers(1) during (1))
  ).protocols(httpProtocol)
*/
}