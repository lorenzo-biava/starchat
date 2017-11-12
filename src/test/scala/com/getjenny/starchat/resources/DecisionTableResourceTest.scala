import java.io.{File, FileInputStream}
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.serializers.JsonSupport
import com.typesafe.config.ConfigFactory
import com.getjenny.starchat.StarChatService
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes

import scala.util.matching.Regex

class DecisionTableResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
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
    "return an HTTP code 200 when indexing a decision table from csv file" in {

      val input_file = getClass.getResourceAsStream("/doc/decision_table_starchat_doc.csv")
      val input_data = scala.io.Source.fromInputStream(input_file).mkString

      // tests:
      val multipartForm =
        Multipart.FormData(
          Multipart.FormData.BodyPart.Strict(
            "csv",
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, input_data),
            Map("filename" -> "data.csv")))

      Post(s"/decisiontable_upload_csv", multipartForm) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
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

