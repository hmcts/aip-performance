package scenarios.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Environment {
  
  val httpConfig = scala.util.Properties.envOrElse("httpConfig", "http")
//  val baseURL = scala.util.Properties.envOrElse("baseURL", "https://manage-case.#{env}.platform.hmcts.net")
  val baseURL = scala.util.Properties.envOrElse("baseURL", "https://immigration-appeal.${env}.platform.hmcts.net")
  val idamURL = "https://idam-web-public.perftest.platform.hmcts.net"
  val idamAPIURL = "https://idam-api.perftest.platform.hmcts.net"
  val idamCookieName="SESSION_ID"
  val HttpProtocol = http
  
  val minThinkTime = 1//20//80//100//140//5
  val maxThinkTime = 1//40//80//100//150//10

}