package simulations

import io.gatling.http.Predef._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.pause.PauseType
import scenarios._
import utils.Environment
import ccd._

import scala.concurrent.duration._

class Aip_Simulation extends Simulation {

  val BaseURL = Environment.baseURL

  val CaseworkerFeeder = csv("Caseworkers.csv").random

  /* TEST TYPE DEFINITION */
  /* pipeline = nightly pipeline against the AAT environment (see the Jenkins_nightly file) */
  /* perftest (default) = performance test against the perftest environment */
  val testType = scala.util.Properties.envOrElse("TEST_TYPE", "perftest")

  //set the environment based on the test type
  val environment = testType match{
    case "perftest" => "perftest"
    case "pipeline" => "perftest"
    case _ => "**INVALID**"
  }
  /* ******************************** */

  /* ADDITIONAL COMMAND LINE ARGUMENT OPTIONS */
  val debugMode = System.getProperty("debug", "off") //runs a single user e.g. ./gradle gatlingRun -Ddebug=on (default: off)
  val env = System.getProperty("env", environment) //manually override the environment aat|perftest e.g. ./gradle gatlingRun -Denv=aat
  /* ******************************** */

  /* PERFORMANCE TEST CONFIGURATION */
  val rampUpDurationMins = 5
  val rampDownDurationMins = 5
  val testDurationMins = 60

  //Must be doubles to ensure the calculations result in doubles not rounded integers
  val aipHourlyTarget:Double = 100
  val aipRatePerSec = aipHourlyTarget / 3600

  //If running in debug mode, disable pauses between steps
  val pauseOption:PauseType = debugMode match{
    case "off" => constantPauses
    case _ => customPauses(1000.toLong) //pause in debug otherwise the call to ES to retrieve the case number may not have been indexed in time
  }

  /* ******************************** */
  /* PIPELINE CONFIGURATION */
  val numberOfPipelineUsers:Double = 5
  /* ******************************** */

  val httpProtocol = http
    .baseUrl(BaseURL)
    .inferHtmlResources()
    .silentResources
    .disableCaching //temporarily disabling cache, as Set-Cookie headers in the static resource responses is breaking the flow for multiple users

  before{
    println(s"Test Type: ${testType}")
    println(s"Test Environment: ${env}")
    println(s"Debug Mode: ${debugMode}")
  }

  val AIP_Appeal = scenario("AIP Appeal Journey")
    .exitBlockOnFail{
      exec(_.set("env", s"${env}"))
      .exec(CreateUser.CreateCitizen)
      .exec(AIP_CreateAppeal.Homepage)
      .exec(AIP_CreateAppeal.Eligibility)
      .exec(AIP_CreateAppeal.LoginLandingPage)
      .exec(AIP_CreateAppeal.Login)
      .exec(AIP_CreateAppeal.AboutAppeal)
      .exec(AIP_CreateAppeal.TypeOfAppeal)
      .exec(AIP_CreateAppeal.HomeOfficeAndPersonalDetails)
      .exec(AIP_CreateAppeal.ContactDetails)
      .exec(AIP_CreateAppeal.DecisionType)
      .exec(AIP_CreateAppeal.FeeSupport)
      .exec(AIP_CreateAppeal.CheckAndSend)
      .exec(AIP_CreateAppeal.AppealOverview)
      .exec(AIP_CreateAppeal.AIPLogout)
      //Caseworker requests Home Office Data then Requests Respondent Evidence
      .feed(CaseworkerFeeder)
      //Fetch the case by searching CCD by appealRef
      .exec(CcdHelper.searchCases("#{cw-username}", "#{cw-password}", CcdCaseTypes.IA_Asylum, "bodies/CCD_searchCaseByAppealRef.json",
        additionalChecks = Seq(
          jsonPath("$.cases[0].case_data.appealReferenceNumber").is("#{appealRef}"),
          jsonPath("$.cases[0].id").saveAs("caseId")
        )))
      .pause(1)
      .exec(CcdHelper.addCaseEvent("#{cw-username}", "#{cw-password}", CcdCaseTypes.IA_Asylum, "#{caseId}", "requestHomeOfficeData", "bodies/CCD_requestHomeOfficeData.json"))
      .pause(1)
      .exec(CcdHelper.addCaseEvent("#{cw-username}", "#{cw-password}", CcdCaseTypes.IA_Asylum, "#{caseId}", "requestRespondentEvidence", "bodies/CCD_requestRespondentEvidence.json"))
      .exec{
        session =>
          println(session)
        session
      }
    }
    .doIf("#{emailAddress.exists()}") {
      exec(DeleteUser.deleteUser)
    }

  //defines the Gatling simulation model, based on the inputs
  def simulationProfile(simulationType: String, userPerSecRate: Double, numberOfPipelineUsers: Double): Seq[OpenInjectionStep] = {
    simulationType match {
      case "perftest" =>
        if (debugMode == "off") {
          Seq(
            rampUsersPerSec(0.00) to (userPerSecRate) during (rampUpDurationMins.minutes),
            constantUsersPerSec(userPerSecRate) during (testDurationMins.minutes),
            rampUsersPerSec(userPerSecRate) to (0.00) during (rampDownDurationMins.minutes)
          )
        }
        else{
          Seq(atOnceUsers(1))
        }
      case "pipeline" =>
        Seq(rampUsers(numberOfPipelineUsers.toInt) during (2.minutes))
      case _ =>
        Seq(nothingFor(0))
    }
  }

  //defines the test assertions, based on the test type
  def assertions(simulationType: String): Seq[Assertion] = {
    simulationType match {
      case "perftest" | "pipeline" => //currently using the same assertions for a performance test and the pipeline
        if (debugMode == "off") {
          Seq(global.successfulRequests.percent.gte(95),
            details("CCD_SubmitEvent_requestRespondentEvidence").successfulRequests.percent.gte(80),
          )
        }
        else {
          Seq(global.successfulRequests.percent.is(100))
        }
      case _ =>
        Seq()
    }
  }

  setUp(
    AIP_Appeal.inject(simulationProfile(testType, aipRatePerSec, numberOfPipelineUsers)).pauses(pauseOption)
  ).protocols(httpProtocol)
    .assertions(assertions(testType))

}