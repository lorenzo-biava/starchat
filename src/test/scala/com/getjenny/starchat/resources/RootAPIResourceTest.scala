import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.serializers.JsonSupport
import com.typesafe.config.ConfigFactory
import com.getjenny.starchat.StarChatService
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import scala.concurrent.duration._
import akka.testkit._
import scala.util.matching.Regex
import akka.http.scaladsl.model.HttpMethods._

class RootAPIResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(10.seconds.dilated(system))
  val service = new StarChatService
  val routes: Route = service.routes

  "StarChat" should {
    "return a 200 if the service responds (Health Check)" in {
      Get(s"/") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}

