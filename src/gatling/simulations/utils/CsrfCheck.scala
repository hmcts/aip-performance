package utils

import io.gatling.core.Predef._
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.css.CssCheckType
import jodd.lagarto.dom.NodeSelector

object CsrfCheck {

  //Allow an optional argument
  def save: CheckBuilder[CssCheckType, NodeSelector] = save(optional = false)

  def save(optional: Boolean): CheckBuilder[CssCheckType, NodeSelector] = {
    val check = css("input[name='_csrf']", "value")

    if (optional) check.optional.saveAs("csrf")
    else check.saveAs("csrf")
  }

}
