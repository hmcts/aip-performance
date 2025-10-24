
package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utilities.DateUtils
import utils.{CsrfCheck, Environment, Headers}

import scala.concurrent.duration._

/*AIP Appeal is a new Journey which allows an appellant to go through several eligibility questions and if successful
they can login to the new AIP portal and start an appeal. This script carries out this business journey all the way
until the appeal is submitted and then captures and stores the appeal reference for processing by a caseworker*/

object AIP_CreateAppeal {

  val BaseURL = Environment.baseURL
  val IdamURL = Environment.idamURL

  val MinThinkTime = Environment.minThinkTime
  val MaxThinkTime = Environment.maxThinkTime

  val postcodeFeeder = csv("postcodes.csv").random

  //AIP Start Appeal HomePage
  val Homepage =

    exec(flushHttpCache)
    .exec(flushCookieJar)
    .exec(_.setAll(
      //Home office letter should have been received within the last 14 days
      "randomDay" -> DateUtils.getDatePast("dd", days = 10),
      "randomMonth" -> DateUtils.getDatePast("MM", days = 10),
      "letterYear" -> DateUtils.getDatePast("yyyy", days = 10),
      "dobYear" -> DateUtils.getDatePastRandom("yyyy", minYears = 20, maxYears = 50),
      "deadlineDate" -> DateUtils.getDateFuture("yyyy-MM-dd", days = 14)
    ))

    .group ("AIP_010_Homepage") {

      exec(http("AIP_010_Homepage")
        .get(BaseURL + "/")
        .headers(Headers.commonHeader)
        .check(substring("Appeal an immigration or asylum decision")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  val Eligibility =

    group("AIP_020_StartEligibility") {

      exec(http("AIP_020_010_StartEligibility")
        .get(BaseURL + "/eligibility")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("Are you currently in detention")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    .group("AIP_030_InDetention") {

      exec(http("AIP_030_010_InDetention")
        .post(BaseURL + "/eligibility")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("questionId", "0")
        .formParam("answer", "no")
        .formParam("continue", "")
        .check(substring("You can use the new service")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    .group("AIP_040_ContinueToAppeal") {

      exec(http("AIP_040_010_ContinueToAppeal")
        .get(BaseURL + "/login?register=true")
        .headers(Headers.commonHeader)
        .check(regex("client_id=iac&state=([0-9a-z-]+?)&nonce").saveAs("state"))
        .check(substring("Create an account or sign in")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Sign in to continue with your appeal
  val LoginLandingPage =

    group("AIP_050_LoadLoginPage") {

      exec(http("AIP_050_010_LoadLoginPage")
        .get(IdamURL + "/login?redirect_uri=" + BaseURL + "%2fredirectUrl&client_id=iac&state=#{state}&nonce=&scope=")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("Sign in or create an account")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Login into Application with an IAC Citizen account
  val Login =

    group("AIP_060_Login") {

      exec(http("AIP_060_010_Login")
        .post(IdamURL + "/login?redirect_uri=" + BaseURL + "%2fredirectUrl&client_id=iac&state=#{state}&nonce=&scope=")
        .headers(Headers.commonHeader)
        .formParam("username", "#{emailAddress}")
        .formParam("password", "#{password}")
        .formParam("selfRegistrationEnabled", "true")
        .formParam("_csrf", "#{csrf}")
        .check(substring("Your appeal details")))

      }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //User gets the About Appeal Page after they have logged in
  val AboutAppeal =

    group("AIP_070_AboutAppeal") {

      exec(http("AIP_070_010_AboutAppeal")
        .get(BaseURL + "/about-appeal")
        .headers(Headers.commonHeader)
        .check(substring("Tell us about your appeal")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Go to Type of Appeal Page
  val TypeOfAppeal =

    group("AIP_080_TypeOfAppeal") {

      exec(http("AIP_080_010_TypeOfAppeal")
        .get(BaseURL + "/in-the-uk")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("Are you currently living in the United Kingdom")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //In The UK - Yes
    .group("AIP_090_InTheUk") {

      exec(http("AIP_090_010_InTheUk")
        .post(BaseURL + "/in-the-uk")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("questionId", "")
        .formParam("answer", "Yes")
        .formParam("continue", "")
        .check(CsrfCheck.save)
        .check(substring("What is your appeal type")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Enter Type of appeal - Protection
    .group("AIP_100_AppealType") {

      exec(http("AIP_100_010_AppealType")
        .post(BaseURL + "/appeal-type")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("appealType", "protection")
        .formParam("saveAndContinue", "")
        .check(substring("Tell us about your appeal"))
        .check(regex("""(?s)title='Appeal type'[^<]*<\/a>.*?<strong class="govuk-tag[^"]*">\s*([^<]+?)\s*<\/strong>""").is("Saved")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //User clicks on Home Office & gets Home Office Reference Number Page
  val HomeOfficeAndPersonalDetails =

    group("AIP_110_HomeOfficeDetails") {

      exec(http("AIP_110_010_HomeOfficeDetails")
        .get(BaseURL + "/home-office-reference-number")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("What is your Home Office reference number")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Enter Home Office Letter Reference - this can be any 9-digit number
    .group("AIP_120_HomeOfficeRef") {

      exec(http("AIP_120_010_HomeOfficeRef")
        .post(BaseURL + "/home-office-reference-number")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("homeOfficeRefNumber", "123456789")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("What is your name")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Enter Name Details
    .group("AIP_130_Name") {

      exec(http("AIP_130_010_Name")
        .post(BaseURL + "/name")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("givenNames", "Perf Test")
        .formParam("familyName", "IAC")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("What is your date of birth")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Enter Date of Birth Details
    .group("AIP_140_DateOfBirth") {

      exec(http("AIP_140_010_DateOfBirth")
        .post(BaseURL + "/date-birth")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("day", "#{randomDay}")
        .formParam("month", "#{randomMonth}")
        .formParam("year", "#{dobYear}")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("What is your nationality")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Enter Nationality
    .group("AIP_150_Nationality") {

      exec(http("AIP_150_010_Nationality")
        .post(BaseURL + "/nationality")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("nationality", "AF")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("What date was your decision letter sent")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Date Home Office Letter Sent this date has to be within the last 14 days
    .group("AIP_160_DateLetterSent") {

      exec(http("AIP_160_010_DateLetterSent")
        .post(BaseURL + "/date-letter-sent")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("day", "#{randomDay}")
        .formParam("month", "#{randomMonth}")
        .formParam("year", "#{letterYear}")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("Upload your Home Office decision letter")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //User uploads the decision letter
    .group("AIP_170_UploadDecisionLetter") {

      exec(http("AIP_170_010_UploadDecisionLetter")
        .post(BaseURL + "/home-office-upload-decision-letter/upload?_csrf=#{csrf}")
        .headers(Headers.commonHeader)
        .header("content-type", "multipart/form-data")
        .bodyPart(RawFileBodyPart("file-upload", "HORefusal.pdf")
          .fileName("HORefusal.pdf")
          .transferEncoding("binary"))
        .asMultipartForm
        .check(CsrfCheck.save)
        .check(substring("HORefusal.pdf")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Save the Decision letter file
    .group("AIP_180_UploadDecisionLetterSubmit") {

      exec(http("AIP_180_010_UploadDecisionLetterSubmit")
        .post(BaseURL + "/home-office-upload-decision-letter")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("file-upload", "")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("Has a deportation order been made against you")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Has a deportation order been made against you? - No
    .group("AIP_190_DeportationOrder") {

      exec(http("AIP_190__010_DeportationOrder")
        .post(BaseURL + "/deportation-order")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("answer", "No")
        .formParam("saveAndContinue", "")
        .check(substring("Tell us about your appeal"))
        .check(regex("""(?s)title='Your Home Office and personal details'[^<]*<\/a>.*?<strong class="govuk-tag[^"]*">\s*([^<]+?)\s*<\/strong>""").is("Saved")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Go to Your Contact Details page
  val ContactDetails =

    group("AIP_200_ContactDetails") {

      exec(http("AIP_200_010_ContactDetails")
        .get(BaseURL + "/contact-preferences")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("How do you want us to contact you")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Enter contact preferences
    .group("AIP_210_ContactPreferences") {

      exec(http("AIP_210_010_ContactPreferences")
        .post(BaseURL + "/contact-preferences")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("selections", "email")
        .formParam("email-value", "#{emailAddress}")
        .formParam("text-message-value", "")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("What is your address")))

    }

    .feed(postcodeFeeder)

    //Enter postcode and capture a random address from the returned list
    .group("AIP_220_PostcodeLookup") {

      exec(http("AIP_220_010_PostcodeLookup")
        .post(BaseURL + "/address")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("postcode", "#{postcode}")
        .formParam("findAddress", "submit")
        .check(CsrfCheck.save)
        .check(substring("Select an address"))
        .check(regex("""<option value="([0-9]+)">""").findRandom.saveAs("addressCode")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Submit address chosen from the returned list
    .group("AIP_230_SelectAddress") {

      exec(http("AIP_230_010_SelectAddress")
        .post(BaseURL + "/select-address")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("address", "#{addressCode}")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(css("input[name='address-line-1']", "value").saveAs("addressLine1"))
        .check(css("input[name='address-line-2']", "value").optional.saveAs("addressLine2"))
        .check(css("input[name='address-town']", "value").optional.saveAs("addressTown"))
        .check(css("input[name='address-county']", "value").optional.saveAs("addressCounty"))
        .check(css("input[name='address-postcode']", "value").saveAs("addressPostcode"))
        .check(substring("What is your address")))

    }

    //Populate any non-value address lines
    .doIf("#{addressLine2.isUndefined()}") { exec(_.set("addressLine2", "")) }
    .doIf("#{addressTown.isUndefined()}") { exec(_.set("addressTown", "")) }
    .doIf("#{addressCounty.isUndefined()}") { exec(_.set("addressCounty", "")) }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Confirm address
    .group("AIP_240_ConfirmAddress") {

      exec(http("AIP_240_010_ConfirmAddress")
        .post(BaseURL + "/manual-address")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("address-line-1", "#{addressLine1}")
        .formParam("address-line-2", "#{addressLine2}")
        .formParam("address-town", "#{addressTown}")
        .formParam("address-county", "#{addressCounty}")
        .formParam("address-postcode", "#{addressPostcode}")
        .formParam("saveAndContinue", "")
        .check(substring("Do you have a sponsor")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Do you have a sponsor
    .group("AIP_250_Sponsor") {

      exec(http("AIP_250_010_Sponsor")
        .post(BaseURL + "/has-sponsor")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("questionId", "")
        .formParam("answer", "No")
        .formParam("continue", "")
        .check(substring("Tell us about your appeal"))
        .check(regex("""(?s)title='Your contact details'[^<]*<\/a>.*?<strong class="govuk-tag[^"]*">\s*([^<]+?)\s*<\/strong>""").is("Saved")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Go to Decision Type page
  val DecisionType =

    group("AIP_260_DecisionType") {

      exec(http("AIP_260_010_DecisionType")
        .get(BaseURL + "/decision-type")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("How do you want your appeal to be decided")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Appeal without a hearing
    .group("AIP_270_SubmitDecisionType") {

      exec(http("AIP_270_010_SubmitDecisionType")
        .post(BaseURL + "/decision-type")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("answer", "decisionWithoutHearing")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("Do you want to pay for the appeal now")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Pay the appeal later - this leads to equality and diversity questions which are not included in the journey
    .group("AIP_280_PayLater") {

      exec(http("AIP_280_010_PayLater")
        .post(BaseURL + "/pay-now")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("answer", "payLater")
        .formParam("saveAndContinue", "")
        .check(regex("Equality and diversity questions|Tell us about your appeal")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Return to the dashboard (bypass PCQ questions)
    .group("AIP_290_ReturnToDashboard") {

      exec(http("AIP_290_010_ReturnToDashboard")
        .get(BaseURL + "/about-appeal")
        .headers(Headers.commonHeader)
        .check(substring("Tell us about your appeal"))
        .check(regex("""(?s)title='Decision with or without a hearing'[^<]*<\/a>.*?<strong class="govuk-tag[^"]*">\s*([^<]+?)\s*<\/strong>""").is("Saved")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Support with fees
  val FeeSupport =

    group("AIP_300_FeeSupport") {

      exec(http("AIP_300_010_FeeSupport")
        .get(BaseURL + "/fee-support")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("Do you have to pay the fee")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Do not request support
    .group("AIP_310_SubmitFeeSupport") {

      exec(http("AIP_310_010_SubmitFeeSupport")
        .post(BaseURL + "/fee-support")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("answer", "noneOfTheseStatements")
        .formParam("saveAndContinue", "")
        .check(CsrfCheck.save)
        .check(substring("Help with paying the fee")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Pay for the appeal now
    .group("AIP_320_HelpWithFees") {

      exec(http("AIP_320_010_HelpWithFees")
        .post(BaseURL + "/help-with-fees")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("answer", "willPayForAppeal")
        .formParam("saveAndContinue", "")
        .check(substring("Tell us about your appeal"))
        .check(regex("""(?s)title='Support to pay the fee'[^<]*<\/a>.*?<strong class="govuk-tag[^"]*">\s*([^<]+?)\s*<\/strong>""").is("Saved")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //Verifies the information entered is valid
  val CheckAndSend =

    group("AIP_330_CheckAndSend") {

      exec(http("AIP_330_010_CheckAndSend")
        .get(BaseURL + "/check-answers")
        .headers(Headers.commonHeader)
        .check(CsrfCheck.save)
        .check(substring("Check your answers"))
        .check(substring("I believe the information I have given is true")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

    //Accepts the details are valid and submits the appeal
    .group("AIP_340_CheckYourAnswers") {

      exec(http("AIP_340_010_CheckYourAnswers")
        .post(BaseURL + "/check-answers")
        .headers(Headers.commonHeader)
        .formParam("_csrf", "#{csrf}")
        .formParam("statement", "acceptance")
        .check(substring("You have sent your appeal details")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)


  //User goes to the Appeal Overview page and gets the reference number for the appeal.
  val AppealOverview =

    group("AIP_350_AppealOverview") {

      exec(http("AIP_350_010_AppealOverview")
        .get(BaseURL + "/appeal-overview")
        .headers(Headers.commonHeader)
        .check(regex("""Appeal reference: ([a-zA-Z0-9/]+?)</p>""").saveAs("appealRef")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

  //Logout
  val AIPLogout =

    group("AIP_360_Logout") {

      exec(http("AIP_360_010_Logout")
        .get(BaseURL + "/logout")
        .headers(Headers.commonHeader)
        .check(substring("Appeal an immigration or asylum decision")))

    }

    .pause(MinThinkTime.seconds, MaxThinkTime.seconds)

}