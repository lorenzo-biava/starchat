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

import scala.util.matching.Regex

class AnalyzersPlaygroundResourceTest extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
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
    "return an HTTP code 200 when evaluating a simple keyword analyzer with an empty query" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "",
          analyzer = """keyword("test")""",
          data = Option{Data()}
        )

      Post(s"/analyzers_playground", evaluateRequest) ~> routes ~> check {
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

      Post(s"/analyzers_playground", evaluateRequest) ~> routes ~> check {
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

      Post(s"/analyzers_playground", evaluateRequest) ~> routes ~> check {
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

      Post(s"/analyzers_playground", evaluateRequest) ~> routes ~> check {
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

      Post(s"/analyzers_playground", evaluateRequest) ~> routes ~> check {
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

      Post(s"/analyzers_playground", evaluateRequest) ~> routes ~> check {
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
    "return an HTTP code 400 when deleting an index" in {
      Delete(s"/index_management") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[IndexManagementResponse]
      }
    }
  }

}


