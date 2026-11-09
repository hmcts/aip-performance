package utils

object Environment {

  val baseURL = "https://immigration-appeal.#{env}.platform.hmcts.net"
  val idamURL = "https://idam-web-public.#{env}.platform.hmcts.net"
  val idamAPIURL = "https://idam-api.#{env}.platform.hmcts.net"
  val microsoftURL = "https://login.microsoftonline.com"
  val apiGatewayURL = "https://cft-api-mgmt.#{env}.platform.hmcts.net"
  
  val minThinkTime = 5
  val maxThinkTime = 7

}