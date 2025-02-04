package scenarios.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Random

object Common {

  val rnd = new Random()
  val now = LocalDate.now()
  val IACURL = Environment.caseURL
  val patternDay = DateTimeFormatter.ofPattern("dd")
  val patternMonth = DateTimeFormatter.ofPattern("MM")
  val patternYear = DateTimeFormatter.ofPattern("yyyy")

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }
  
  def randomRepeatInteger() : Integer=
  {
    val list=List(300, 280, 260, 250, 240, 200,220,350)
    //val list=List(2,3)
    val start = 200
    val end   = 300
    val rnd = new scala.util.Random
    list(rnd.nextInt(list.length))
  }
  
  
  def getDay(): String = {
    (1 + rnd.nextInt(28)).toString
  }

  def getMonth(): String = {
    (1 + rnd.nextInt(12)).toString
  }
  /*
  def getDay(): String = {
    (10 + rnd.nextInt(28)).toString.format(patternDay).reverse.padTo(2, '0').reverse //pads single-digit dates with a leading zero
  }

  def getMonth(): String = {
    (11 + rnd.nextInt(12)).toString.format(patternMonth).reverse.padTo(2, '0').reverse //pads single-digit dates with a leading zero
  }
  */
  //Dod <= 21 years
  def getYear(): String = {
    now.minusYears(30 + rnd.nextInt(50)).format(patternYear)
  }

  val configurationUI =
    exec(http("Common_000_ConfigurationUI")
      .get(IACURL + "/external/configuration-ui/")
      .headers(Headers.secondaryHeader)
      .header("sec-fetch-site", "none")
      .check(substring("ccdGatewayUrl")))

  val configJson =
    exec(http("Common_000_ConfigJson")
      .get(IACURL + "/assets/config/config.json")
      .headers(Headers.secondaryHeader)
      .check(substring("caseEditorConfig")))

  val configTsAndCs =
    exec(http("Common_000_TsAndCs")
      .get(IACURL + "/api/configuration?configurationKey=termsAndConditionsEnabled")
      .headers(Headers.secondaryHeader)
      .check(substring("false")))

  val configUI =
    exec(http("Common_000_ConfigUI")
      .get(IACURL + "/external/config/ui")
      .headers(Headers.secondaryHeader)
      .check(substring("ccdGatewayUrl")))

  val userDetailsHome =
    exec(http("Common_000_UserDetailsHome")
      .get(IACURL + "/api/user/details?refreshRoleAssignments=undefined")
      .headers(Headers.secondaryHeader)
      .check(status.is(401)))

  val userDetailsLoggedIn =
    exec(http("Common_000_UserDetailsLoggedIn")
      .get(IACURL + "/api/user/details?refreshRoleAssignments=true")
      .headers(Headers.secondaryHeader)
      .check(status.in(200, 304)))

  val isAuthenticatedHome =
    exec(http("Common_000_IsAuthenticatedHome")
      .get(IACURL + "/auth/isAuthenticated")
      .headers(Headers.secondaryHeader)
      .check(regex("false")))

  val isAuthenticatedLoggedIn =
    exec(http("Common_000_IsAuthenticatedLoggedIn")
      .get(IACURL + "/auth/isAuthenticated")
      .headers(Headers.secondaryHeader)
      .check(regex("true")))

  val monitoringTools =
    exec(http("Common_000_MonitoringTools")
      .get(IACURL + "/api/monitoring-tools")
      .headers(Headers.secondaryHeader)
      .check(jsonPath("$.key").notNull))

  val healthCheck =
    exec(http("Common_000_HealthCheck")
      .get(IACURL + "/api/healthCheck?path=%2Fwork%2Fmy-work%2Flist")
      .headers(Headers.secondaryHeader)
      .check(substring("healthState")))

  val regionLocation =
    exec(http("Common_000_RegionLocation")
      .post(IACURL + "/workallocation/region-location")
      .body(StringBody("""{"serviceIds": []}""".stripMargin))
      .headers(Headers.secondaryHeader)
      .check(status.is(200)))

  val getMyAccess =
    exec(http("Common_000_GetMyAccess")
      .get(IACURL + "/api/role-access/roles/get-my-access-new-count")
      .headers(Headers.secondaryHeader)
      .check(substring("count")))

  val typesOfWork =
    exec(http("Common_000_TypesOfWork")
      .get(IACURL + "/workallocation/task/types-of-work")
      .headers(Headers.secondaryHeader)
      .check(status.is(200)))

  val jurisdiction =
    exec(http("Common_000_Jurisdiction")
      .get(IACURL + "/api/wa-supported-jurisdiction/get")
      .headers(Headers.secondaryHeader)
      .check(substring("IA")))

  val manageLabelling =
    exec(http("Common_000_ManageLabellingRoleAssignment")
      .post(IACURL + "/api/role-access/roles/manageLabellingRoleAssignment/#{CaseNumber}")
      .headers(Headers.secondaryHeader)
      .check(status.is(204)))

  val caseworkerTasks =
    exec(http("Common_000_CaseWorkerTasks")
      .post(IACURL + "/workallocation/task")
      .body(StringBody("""{"searchRequest": {"search_parameters": [ {"key": "user", "operator": "IN",
                         |"values": ["#{caseworkerId}"]}, {"key": "state", "operator": "IN", "values": ["assigned"]} ],
                         |"sorting_parameters": [], "search_by": "caseworker"}, "view": "MyTasks"}""".stripMargin))
      .headers(Headers.secondaryHeader)
      .check(substring("total_records")))

  val getUsersByServiceName =
    exec(http("Common_000_GetUsersByServiceName")
      .post(IACURL + "/workallocation/caseworker/getUsersByServiceName")
      .body(StringBody("""{"services": ["IA", "CIVIL", "PRIVATELAW", "PUBLICLAW", "SSCS", "ST_CIC",
          |"EMPLOYMENT"]}""".stripMargin))
      .headers(Headers.secondaryHeader)
      .check(substring("BIRMINGHAM CIVIL AND FAMILY JUSTICE CENTRE")))

  val profile =
    exec(http("Common_000_Profile")
      .get(IACURL + "/data/internal/profile")
      .headers(Headers.secondaryHeader)
      .header("experimental", "true")
      .check(substring("http://gateway-ccd")))

/*
  val reasonForAppealing = "Social Security and Child Support forms including notices of appeal to the Department of Work and Pensions and HM Revenue and Customs.
    You can appeal a decision about your entitlement to benefits, for example Personal Independence Payment (PIP), Employment and Support Allowance (ESA) and Universal Credit.
    Appeals are decided by the Social Security and Child Support Tribunal. The tribunal is impartial and independent of government. The tribunal will listen to both sides before making a decision.
    You can appeal a decision about your entitlement to benefits, for example Personal Independence Payment (PIP), Employment and Support Allowance (ESA) or Universal Credit.
    If you do not need a mandatory reconsideration your decision letter will say why. Include this explanation when you submit your appeal.
    You’ll need to choose whether you want to go to the tribunal hearing to explain your appeal in person. If you do not attend, your appeal will be decided on your appeal form and any supporting evidence you provide."

  val anythingElse = "Things that were not considered are. 
    You can appoint someone as a ‘representative’ to help you with your appeal. A representative can.help you submit your appeal or prepare your evidence act on your behalf give you advice. Anyone can be a representative, including friends and family.
    You might also be able to find a representative through a library or from an organisation in your area that gives advice on claiming benefits, such as Citizens Advice.
    Your representative will have permission to act on your behalf, for example to respond to letters. They’ll be sent all the information about your appeal, including any medical evidence."
*/
}