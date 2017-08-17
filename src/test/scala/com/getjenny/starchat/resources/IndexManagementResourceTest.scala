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

class IndexManagementResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val service = new StarChatService
  val routes = service.routes

  "StarChat" should {
    "return an HTTP code 200 when creating a new index" in {
      Post(s"/index_management/create") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
        response.message should fullyMatch regex "(create index: .+ create_index_ack\\(true\\))"
      }
    }
  }

  it should {
    "return an HTTP code 400 when trying to create again the same index" in {
      Post(s"/index_management/create") ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        val response = responseAs[IndexManagementResponse]
        response.message should fullyMatch regex "index \\[.*\\] already exists"
      }
    }
  }

  it should {
    "return an HTTP code 200 when calling elasticsearch index refresh" in {
      Post(s"/index_management/refresh") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  it should {
    "return an HTTP code 200 when getting informations from the index" in {
      Get(s"/index_management") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
        response.message shouldEqual "settings index: jenny-en-0 dt_type_check(state:true) kb_type_check(question:true) term_type_name(term:true)"
      }
    }
  }

  it should {
    "return an HTTP code 200 updating the index" in {
      Put(s"/index_management") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
      }
    }
  }

  it should {
    "return an HTTP code 400 when deleting an index" in {
      Delete(s"/index_management") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
      }
    }
  }
}


