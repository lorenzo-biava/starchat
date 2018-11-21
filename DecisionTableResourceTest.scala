package com.getjenny.starchat.resources

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit._
import com.getjenny.starchat.StarChatService
import com.getjenny.starchat.entities._
import com.getjenny.starchat.serializers.JsonSupport
import com.getjenny.starchat.utils.Index
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class DecisionTableResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(10.seconds.dilated(system))

  val service = new StarChatService
  val routes = service.routes

  val testAdminCredentials = BasicHttpCredentials("admin", "adminp4ssw0rd")
  val testUserCredentials = BasicHttpCredentials("test_user", "p4ssw0rd")


  "StarChat" should {
    "return an HTTP code 200 when creating a new system index" in {
      Post(s"/system_index_management/create") ~> addCredentials(testAdminCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
        response.message should fullyMatch regex "IndexCreation: " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.systemIndexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\) " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.systemIndexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\)".r
      }
    }
  }
//testing search
  it sould {
    "return an HTTP code 200 when search success" in {
      val search.query = search(
        from = 0,
        size = 10,
        executionOrder = 1,
        minScore = 0.7,
        boostExactMatchFactor = 1000,
        state = "renew_insurance",
        evaluationClass = "default",
        queries = "text"
      )
      val search.state = search(
        total = 0,
        maxScore = 1,
        hits = {
          score = 0,
          document = (state, executionOrder, maxStateCount, analyzer)
          bubble = "text",
          action = "action",
          actionInput = "text to be shown on buttons"
          stateData = "Data",
          succerValue = "evaluation",
          failureValue = "dont_understand",
          evaluationClass = default,
          version = "version"
        }
      }

      )
      Post(s"/decisiontable_search") ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
    }
  }

//analyzer
  it should {
    "return an HTTP code 200 when request successfully executed" in {
      Post(s"decisiontable_analyzer") ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerResponse]
    }
  }

//next response
  it should {
    "return an HTTP code 200 when getting a next response " in {
      val next_response = next_response(
        id = "1234",
        user_input = "text",
        values = "values", (
          return_value = "",
          data = "{}"
        )
          threshold = 0,
        evaluationClass = "default",
        maxResults = 0
      )
      Post(s"response") ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }


  it should {
    "return an HTTP code 200 when creating a new user" in {
      val user = User(
        id = "test_user",
        password = "3c98bf19cb962ac4cd0227142b3495ab1be46534061919f792254b80c0f3e566f7819cae73bdc616af0ff555f7460ac96d88d56338d659ebd93e2be858ce1cf9",
        salt = "salt",
        permissions = Map[String, Set[Permissions.Value]]("index_getjenny_english_0" -> Set(Permissions.read, Permissions.write))
      )
      Post(s"/user", user) ~> addCredentials(testAdminCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  it should {
    "return an HTTP code 200 when creating a new index" in {
      Post(s"/index_getjenny_english_0/index_management/create") ~> addCredentials(testAdminCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
        response.message should fullyMatch regex "IndexCreation: " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.indexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\) " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.indexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\) " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.indexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\) " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.indexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\) " +
          "(?:[A-Za-z0-9_]+)\\(" + Index.indexMatchRegex + "\\.(?:[A-Za-z0-9_]+), true\\)".r
      }
    }
  }
//upload csv
  it should {
    "return an HTTP code 200 when uploading csv" in {
      val upload_file = getClass.getResourceAsStream("doc/decision_table_starchat_doc.csv")

      val multipartForm =
        Multipart.FormData(
          Multipart.FormData.BodyPart.Strict(
            "csv",
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, input_data),
            Map("filename" -> "data.csv")))
      Post(s"/index_getjenny_english_0/decisiontable_upload_csv", multipartForm) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
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

        Post(s"/index_getjenny_english_0/decisiontable_upload_csv", multipartForm) ~> addCredentials(testUserCredentials) ~> routes ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }

    it should {
      "return an HTTP code 200 when deleting an index" in {
        Delete(s"/index_getjenny_english_0/index_management") ~> addCredentials(testAdminCredentials) ~> routes ~> check {
          status shouldEqual StatusCodes.OK
          val response = responseAs[IndexManagementResponse]
        }
      }
    }

    it should {
      "return an HTTP code 200 when deleting an existing system index" in {
        Delete(s"/system_index_management") ~> addCredentials(testAdminCredentials) ~> routes ~> check {
          status shouldEqual StatusCodes.OK
          val response = responseAs[IndexManagementResponse]
        }
      }
    }

  }

}

