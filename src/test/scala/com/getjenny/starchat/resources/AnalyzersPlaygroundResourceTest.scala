import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import com.getjenny.starchat.entities._
import com.getjenny.analyzer.expressions.Data
import com.getjenny.starchat.serializers.JsonSupport
import com.typesafe.config.ConfigFactory
import com.getjenny.starchat.StarChatService
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import scala.concurrent.duration._
import akka.testkit._
import scala.util.matching.Regex
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.Authorization

class AnalyzersPlaygroundResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
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
        response.message should be ("IndexCreation: system(starchat_system_0.refresh_decisiontable,true) user(starchat_system_0.user, true)")
      }
    }
  }

  it should {
    "return an HTTP code 200 when creating a new index" in {
      Post(s"/index_0/english/index_management/create") ~> addCredentials(testAdminCredentials) ~> routes ~> check {
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
    "return an HTTP code 200 when evaluating a simple keyword analyzer with an empty query" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "",
          analyzer = """keyword("test")""",
          data = Option{Data()}
        )

      Post(s"/index_0/analyzers_playground", evaluateRequest) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerEvaluateResponse]
        response.build should be (true)
        response.build_message should be ("success")
        response.value should be (0.0)
      }
    }
  }

  it should {
    "return an HTTP code 200 when evaluating a simple keyword analyzer" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "this is a test",
          analyzer = """keyword("test")""",
          data = Option{Data()}
        )

      Post(s"/index_0/analyzers_playground", evaluateRequest) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerEvaluateResponse]
        response.build should be (true)
        response.build_message should be ("success")
        response.value should be (0.25)
      }
    }
  }

  it should {
    "return an HTTP code 200 when checking if a value exists in the traversed states list" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "query",
          analyzer = """hasTravState("one")""",
          data = Option{
            Data(item_list=List("one", "two"), extracted_variables = Map.empty[String, String])
          }
        )

      Post(s"/index_0/analyzers_playground", evaluateRequest) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerEvaluateResponse]
        response.build should be (true)
        response.build_message should be ("success")
        response.value should be (1)
      }
    }
  }

  it should {
    "return an HTTP code 200 when checking if the last value of the traversed states is correct" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "query",
          analyzer = """lastTravStateIs("two")""",
          data = Option{
            Data(item_list=List("one", "two"), extracted_variables = Map.empty[String, String])
          }
        )

      Post(s"/index_0/analyzers_playground", evaluateRequest) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerEvaluateResponse]
        response.build should be (true)
        response.build_message should be ("success")
        response.value should be (1)
      }
    }
  }

  it should {
    "return an HTTP code 200 when checking if the previous value of the traversed states is correct" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "query",
          analyzer = """prevTravStateIs("one")""",
          data = Option{
            Data(item_list=List("one", "two"), extracted_variables = Map.empty[String, String])
          }
        )

      Post(s"/index_0/analyzers_playground", evaluateRequest) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerEvaluateResponse]
        response.build should be (true)
        response.build_message should be ("success")
        response.value should be (1)
      }
    }
  }

  it should {
    "return an HTTP code 200 when checking the variable extraction analyzer" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "on 31-11-1900",
          analyzer =
            """band(prevTravStateIs("one"),keyword("on"),matchPatternRegex("[day,month,year](?:(0[1-9]|[12][0-9]|3[01])(?:[- \/\.])(0[1-9]|1[012])(?:[- \/\.])((?:19|20)\d\d))"))""",
          data = Option{
            Data(item_list=List("one", "two"),
              extracted_variables =
                Map[String, String](
                  "month.0" -> "11",
                  "day.0" -> "31",
                  "year.0" -> "1900"))
          }
        )

      Post(s"/index_0/analyzers_playground", evaluateRequest) ~> addCredentials(testUserCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[AnalyzerEvaluateResponse]
        response.build should be (true)
        response.build_message should be ("success")
        response.value should be (1)
        response.data.isDefined should be (true)
        response.data.getOrElse(Data()).extracted_variables.exists(_ == ("month.0", "11")) should be (true)
        response.data.getOrElse(Data()).extracted_variables.exists(_ == ("day.0", "31")) should be (true)
        response.data.getOrElse(Data()).extracted_variables.exists(_ == ("year.0", "1900")) should be (true)
      }
    }
  }

  it should {
    "return an HTTP code 200 when deleting an index" in {
      Delete(s"/index_0/index_management") ~> addCredentials(testAdminCredentials) ~> routes ~> check {
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


