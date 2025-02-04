package scenarios.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Headers {

//AIP Headers
  
  val commonHeader = Map(
    "accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
    "accept-encoding" -> "gzip, deflate, br, zstd",
    "accept-language" -> "en-GB,en-US;q=0.9,en;q=0.8",
    "cache-control" -> "max-age=0",
    "sec-fetch-dest" -> "document",
    "sec-fetch-mode" -> "navigate",
    "sec-fetch-site" -> "same-origin",
    "sec-fetch-user" -> "?1",
    "upgrade-insecure-requests" -> "1")

  val secondaryHeader = Map(
    "accept" -> "application/json, text/plain, */*",
    "accept-encoding" -> "gzip, deflate, br, zstd",
    "accept-language" -> "en-GB,en-US;q=0.9,en;q=0.8",
    "content-type" -> "application/json",
    "sec-fetch-dest" -> "empty",
    "sec-fetch-mode" -> "cors",
    "sec-fetch-site" -> "same-origin")
}