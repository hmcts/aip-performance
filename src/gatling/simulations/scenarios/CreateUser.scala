package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment
import utilities.StringUtils._

object CreateUser {

  val IdamAPIURL = Environment.idamAPIURL

  val newUserFeeder = Iterator.continually(Map(
    "emailAddress" -> ("aip-citizen-" + randomString(10) + "@mailinator.com"),
    "password" -> "Pa55word11",
    "role" -> "citizen"
  ))

  //takes an userType e.g. petitioner/respondent, to create unique users for each user
  val CreateCitizen = {
    feed(newUserFeeder)
    .group("CreateCitizen") {
      exec(http("CreateCitizen")
        .post(IdamAPIURL + "/testing-support/accounts")
        .body(ElFileBody("CreateUserTemplate.json")).asJson
        .check(status.is(201)))
    }
    
  }

}
