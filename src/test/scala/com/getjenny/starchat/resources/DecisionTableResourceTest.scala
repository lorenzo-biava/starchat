import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.serializers.JsonSupport
import com.getjenny.starchat.StarChatService
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import scala.concurrent.duration._
import akka.testkit._

class DecisionTableResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(10.seconds.dilated(system))

  val service = new StarChatService
  val routes = service.routes

  "StarChat" should {
    "return an HTTP code 200 when creating a new system index" in {
      Post(s"/system_index_management/create") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
        response.message should be ("IndexCreation: system(starchat_system_0.refresh_decisiontable,true)")
      }
    }
  }


  it should {
    "return an HTTP code 200 when creating a new index" in {
      Post(s"/index_0/english/index_management/create") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val index_name_regex = "index_(?:[A-Za-z0-9_]+)"
        val response = responseAs[IndexManagementResponse]
        response.message should fullyMatch regex "IndexCreation: " +
          "decisiontable\\(" + index_name_regex + "\\.state,true\\) " +
          "knowledgebase\\(" + index_name_regex + "\\.question,true\\) " +
          "term\\(" + index_name_regex + "\\.term,true\\)".r
      }
    }
  }

  it should {
    "return an HTTP code 200 when indexing a decision table from csv file" in {

      val input_file = getClass.getResourceAsStream("/doc/decision_table_starchat_doc.csv")
      val input_data = scala.io.Source.fromInputStream(input_file).mkString

      val multipartForm =
        Multipart.FormData(
          Multipart.FormData.BodyPart.Strict(
            "csv",
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, input_data),
            Map("filename" -> "data.csv")))

      Post(s"/index_0/decisiontable_upload_csv", multipartForm) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  it should {
    "return an HTTP code 200 when deleting an index" in {
      Delete(s"/index_0/index_management") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
      }
    }
  }

  it should {
    "return an HTTP code 200 when deleting an existing system index" in {
      Delete(s"/system_index_management") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
      }
    }
  }

}

