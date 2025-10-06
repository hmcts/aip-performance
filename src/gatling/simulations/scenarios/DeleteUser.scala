package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment

object DeleteUser {

  val IdamAPIURL = Environment.idamAPIURL

  val deleteUser =
    exec(http("IDAM_DeleteUser")
      .delete(IdamAPIURL + "/testing-support/accounts/#{emailAddress}")
      .header("Content-Type", "application/json")
      .check(status is 204))
}
