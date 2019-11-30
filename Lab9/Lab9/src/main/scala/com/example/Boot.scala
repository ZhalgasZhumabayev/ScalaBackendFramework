package com.example

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{delete, put, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("ticket-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  val ticketManager = system.actorOf(TicketManager.props(), "ticket-manager")

  val route =
    pathPrefix("v1") {
      path("Ticket" / Segment) { TicketId =>
        concat(
          get {
            complete {
              (TicketManager ? TicketManager.ReadTicket(TicketId)).mapTo[Either[ErrorResponse, Ticket]]
            }
          },
          delete {
            complete {
              (TicketManager ? TicketManager.DeleteTicket(TicketId)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
            }
          }
        )
      } ~
        path("Ticket") {
          concat(
            post {
              entity(as[Ticket]) { Ticket =>
                complete {
                  (TicketManager ? TicketManager.CreateTicket(Ticket)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                }
              }
            },
            put {
              entity(as[Ticket]) { Ticket =>
                complete {
                  (TicketManager ? TicketManager.UpdateTicket(Ticket)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                }
              }
            }
          )
        }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
}
