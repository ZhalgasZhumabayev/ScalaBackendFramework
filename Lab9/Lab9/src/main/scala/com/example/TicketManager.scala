package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.{ActorMaterializer, Materializer}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object TicketManager {

  case class CreateTicket(recipe: Ticket)

  case class ReadTicket(id: String)

  case class UpdateTicket(ticket: Ticket)

  case class DeleteTicket(id: String)

  def props() = Props(new TicketManager)
}

class TicketManager extends Actor with ActorLogging with ElasticSerializer {

  import TicketManager._

  implicit val system: ActorSystem = ActorSystem("telegram-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val client = HttpClient(ElasticsearchClientUri("localhost", 9200))

  val token = "try to guess it"
  val url = s"https://api.telegram.org/bot$token/sendMessage"

  override def receive: Ticket = {
    case CreateTicket(ticket) =>
      val cmd = client.execute(indexInto("tickets" / "_doc").id(ticket.id).doc(ticket))
      val replyTo = sender()

      cmd.onComplete {
        case Success(_) =>
          val ticketResponse = TicketResponse(null, isSuccessful = true, 201, s"Ticket with ID = ${ticket.id} successfully created. Ticket = [$ticket]")
          handleResponse(replyTo, ticketResponse)

          val httpRequest = Marshal(TelegramMessage(text = s"Ticket with ID = ${ticket.id} successfully created. Ticket = [$ticket]"))
            .to[RequestEntity].flatMap { entity =>
            val request = HttpRequest(HttpMethods.POST, url, Nil, entity)
            log.debug("Request: {}", request)
            Http().singleRequest(request)
          }

          httpRequest.onComplete {
            case Success(value) =>
              log.info(s"Response: $value")
              value.discardEntityBytes()

            case Failure(exception) =>
              log.error("error")
          }

        case Failure(_) =>
          val ticketResponse = TicketResponse(null, isSuccessful = false, 409, s"Bad response")
          handleResponse(replyTo, ticketResponse)
      }

    case ReadTicket(id) =>
      val cmd = client.execute(get(id).from("tickets" / "_doc"))
      val replyTo = sender()

      cmd.onComplete {
        case Success(either) => either match {
          case Right(right) =>
            if (right.result.found) {
              either.map(e => e.result.to[Ticket]).foreach { ticket =>
                val ticketResponse = TicketResponse(ticket, isSuccessful = true, 201, null)
                handleResponse(replyTo, ticketResponse)
              }
            } else {
              val ticketResponse = TicketResponse(null, isSuccessful = false, 404, s"Movie with id = $id not found")
              handleResponse(replyTo, ticketResponse)
            }

          case Left(_) =>
            val ticketResponse = TicketResponse(null, isSuccessful = false, 500, s"Elastic Search internal error")
            handleResponse(replyTo, ticketResponse)
        }

        case Failure(exception) =>
          val ticketResponse = TicketResponse(null, isSuccessful = false, 401, exception.getMessage)
          handleResponse(replyTo, ticketResponse)
      }

    case UpdateRecipe(recipe) =>
      val cmd = client.execute(update(ticket.id).in("tickets" / "_doc").docAsUpsert(recipe))
      val replyTo = sender()

      cmd.onComplete {
        case Success(_) =>
          val ticketResponse = TicketResponse(null, isSuccessful = true, 201, s"Ticket with ID = ${recipe.id} successfully updated. Ticket = [$recipe]")
          handleResponse(replyTo, ticketResponse)

        case Failure(_) =>
          val ticketResponse = TicketResponse(null, isSuccessful = false, 409, s"Bad response")
          handleResponse(replyTo, ticketResponse)
      }

    case DeleteTicket(id) =>
      val cmd = client.execute(delete(id).from("tickets" / "_doc"))
      val replyTo = sender()

      cmd.onComplete {
        case Success(either) => either match {
          case Right(right) =>
            if (right.result.found) {
              val ticketResponse = TicketResponse(null, isSuccessful = true, 201, s"Ticket with ID = $id successfully deleted.")
              handleResponse(replyTo, ticketResponse)

              val httpRequest = Marshal(TelegramMessage(text = s"Ticket with ID = $id successfully deleted. "))
                .to[RequestEntity].flatMap { entity =>
                val request = HttpRequest(HttpMethods.POST, url, Nil, entity)
                log.debug("Request: {}", request)
                Http().singleRequest(request)
              }

              httpRequest.onComplete {
                case Success(value) =>
                  log.info(s"Response: $value")
                  value.discardEntityBytes()

                case Failure(exception) =>
                  log.error("error")
              }
            } else {
              val ticketResponse = TicketResponse(null, isSuccessful = false, 409, s"Ticket  with id = $id not found.")
              handleResponse(replyTo, ticketResponse)
            }
          case Left(_) =>
            val ticketResponse = TicketResponse(null, isSuccessful = false, 500, s"Elastic Search internal error")
            handleResponse(replyTo, ticketResponse)
        }
        case Failure(exception) =>
          val ticketResponse = TicketResponse(null, isSuccessful = false, 401, exception.getMessage)
          handleResponse(replyTo, ticketResponse)
      }
  }

  private def handleResponse(replyTo: ActorRef, ticketResponse: TicketResponse): Unit = {
    if (ticketResponse.isSuccessful) {
      if (ticketResponse.ticket != null) {
        replyTo ! Right(ticketResponse.ticket)
      } else {
        replyTo ! Right(SuccessfulResponse(ticketResponse.statusCode, ticketResponse.message))
      }
    } else {
      replyTo ! Left(ErrorResponse(ticketResponse.statusCode, ticketResponse.message))
    }
  }
}