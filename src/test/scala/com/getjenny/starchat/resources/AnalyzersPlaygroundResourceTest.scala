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
    "return an HTTP code 400 when trying to create again the same index" in {
      val evaluateRequest: AnalyzerEvaluateRequest =
        AnalyzerEvaluateRequest(
          query = "",
          analyzer = """keyword("test")""",
          data = None
        )
      Post(s"/analyzers_playground") ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        val response = responseAs[AnalyzerEvaluateResponse]
        //response.message should fullyMatch regex "index \\[.*\\] already exists"
      }
    }
  }

  ```bash
  curl -v -H 'Content-Type: application/json' -X POST http://localhost:8888/analyzers_playground -d '
  {
    "analyzer": "keyword(\"test\")",
    "query": "this is a test",
    "data": {"item_list": [], "extracted_variables":{}}
  }
  '{
   "build_message" : "success",
   "build" : true,
   "value" : 0.25
  }
  ```


  Sample states analyzers

  ```bash
  curl -v -H 'Content-Type: application/json' -X POST http://localhost:8888/analyzers_playground -d '
  {
    "analyzer": "hasTravState(\"one\")",
    "query": "query",
    "data": {"item_list": ["one", "two"], "extracted_variables":{}}
  }
  '
  ```

  Sample output states analyzers

  ```json
  {
    "build_message" : "success",
    "build" : true,
    "value" : 1
  }
  ```

  Sample of pattern extraction through analyzers

  ```json
  curl -v -H 'Content-Type: application/json' -X POST http://localhost:8888/analyzers_playground -d'
  {
    "analyzer": "band(keyword(\"on\"), matchPatternRegex(\"[day,month,year](?:(0[1-9]|[12][0-9]|3[01])(?:[- \\\/\\.])(0[1-9]|1[012])(?:[- \\\/\\.])((?:19|20)\\d\\d))\"))",
    "query": "on 31-11-1900"
  }'
  ```

  Sample output

  ```json
  {
    "build_message" : "success",
    "variables" : {
      "month.0" : "11",
      "day.0" : "31",
      "year.0" : "1900"
    },
    "build" : true,
    "value" : 1
  }


}


