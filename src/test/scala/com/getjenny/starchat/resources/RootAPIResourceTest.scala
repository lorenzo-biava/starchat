import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.serializers.JsonSupport
import com.typesafe.config.ConfigFactory
import com.getjenny.starchat.StarChatService

import scala.util.matching.Regex

class RootAPIResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val service = new StarChatService
  val routes = service.routes

  "StarChat" should {
    "return an map with endpoints and supported methods" in {
      Get(s"/") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[RootAPIsDescription]
        val api_description = new RootAPIsDescription
        api_description shouldEqual response
      }
    }
  }
}

