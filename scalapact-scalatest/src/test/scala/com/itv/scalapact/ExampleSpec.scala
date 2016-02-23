package com.itv.scalapact

import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._
import org.json4s.native.Serialization._
import org.scalatest.{FunSpec, Matchers}

import scala.xml.XML
import scalaj.http.{Http, HttpRequest}

import ScalaPactForger._

class ExampleSpec extends FunSpec with Matchers {

  private implicit val formats = DefaultFormats

  describe("Example CDC Integration tests") {

    it("Should be able to create a contract for a simple GET") {

      val endPoint = "/hello"

      forgePact
        .between("My Consumer")
        .and("Their Provider Service")
        .describing("a simple get example")
        .addInteraction(
          interaction
            .description("Fetch a greeting")
            .uponReceiving(endPoint)
            .willRespondWith(200, "Hello there!")
        )
        .runConsumerTest { mockConfig =>

          val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map.empty)

          result.status should equal(200)
          result.body should equal("Hello there!")

        }

    }

    it("Should be able to create a contract for a simple GET with arbitrary headers") {

      val endPoint = "/hello"

      forgePact
        .between("My Consumer")
        .and("Their Provider Service")
        .describing("a simple get example with a header")
        .addInteraction(
          interaction
            .description("Fetch a greeting")
            .uponReceiving(GET, endPoint, Map("fish" -> "chips"), None)
            .willRespondWith(200, "Hello there!")
        )
        .runConsumerTest { mockConfig =>

          val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map("fish" -> "chips"))

          result.status should equal(200)
          result.body should equal("Hello there!")

        }

    }

    it("Should be able to create a contract for a GET request for JSON data") {

      val data = Person("Joe", 25, "London", List("Fishing", "Karate"))

      val endPoint = "/json"

      forgePact
        .between("My Consumer")
        .and("Their Provider Service")
        .describing("Get json example")
        .addInteraction(
          interaction
            .description("Request for some json")
            .uponReceiving(endPoint)
            .willRespondWith(200, Map("Content-Type" -> "application/json"), Option(write(data)))
        )
        .runConsumerTest { mockConfig =>

          val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map())

          withClue("Status mismatch") {
            result.status should equal(200)
          }

          withClue("Headers did not match") {
            result.headers.exists(h => h._1 == "Content-Type" && h._2 == "application/json")
          }

          withClue("Body did not match") {
            (parse(result.body).extract[Person] == data) should equal(true)
          }

        }

    }

    it("Should be able to create a contract for posting json and getting an xml response") {

      val requestData = Person("Joe", 25, "London", List("Fishing", "Karate"))

      val responseXml = <a><b>Error in your json</b></a>

      val endPoint = "/post-json"

      val headers = Map("Content-Type" -> "application/json")

      forgePact
        .between("My Consumer")
        .and("Their Provider Service")
        .describing("POST JSON receive XML example")
        .addInteraction(
          interaction
            .description("JSON Request for XML")
            .uponReceiving(POST, endPoint, headers, Option(write(requestData)))
            .willRespondWith(400, Map("Content-Type" -> "text/xml"), Option(responseXml.toString()))
        )
        .runConsumerTest { mockConfig =>

          val result = SimpleClient.doPostRequest(mockConfig.baseUrl, endPoint, headers, write(requestData))

          withClue("Status mismatch") {
            result.status should equal(400)
          }

          withClue("Headers did not match") {
            result.headers.exists(h => h._1 == "Content-Type" && h._2 == "text/xml")
          }

          withClue("Response body xml did not match") {
            XML.loadString(result.body) should equal(responseXml)
          }

        }

    }

  }

}

case class Person(name: String, age: Int, location: String, hobbies: List[String])

/**
  * The point of these tests are that you create your test using a piece of client code
  * that does the real call to a mocked endpoint. This is a dummy client used here as an
  * example of what that might look like.
  */
object SimpleClient {

  def doGetRequest(baseUrl: String, endPoint: String, headers: Map[String, String]): SimpleResponse = {
    val request = Http(baseUrl + endPoint).headers(headers)

    doRequest(request)
  }

  def doPostRequest(baseUrl: String, endPoint: String, headers: Map[String, String], body: String): SimpleResponse = {
    val request = Http(baseUrl + endPoint)
      .headers(headers)
      .postData(body)

    doRequest(request)
  }

  def doRequest(request: HttpRequest): SimpleResponse = {
    try {
      val response = request.asString

//      if(!response.is2xx) {
//        println("Request: \n" +  request)
//        println("Response: \n" + response)
//      }

      SimpleResponse(response.code, response.headers, response.body)
    } catch {
      case e: Throwable =>
        SimpleResponse(500, Map(), e.getMessage)
    }
  }

  case class SimpleResponse(status: Int, headers: Map[String, String], body: String)

}