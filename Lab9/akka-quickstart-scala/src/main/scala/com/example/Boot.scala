package com.example

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{delete, put, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.example.actor.MovieManager
import com.example.model.{ErrorResponse, Movie, SuccessfulResponse}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("movie-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  val movieManager = system.actorOf(MovieManager.props(), "movie-manager")

  val route =
    pathPrefix("kbtu-cinema") {
      path("movie" / Segment) { movieId =>
        concat(
          get {
            complete {
              (movieManager ? MovieManager.ReadMovie(movieId)).mapTo[Either[ErrorResponse, Movie]]
            }
          },
          delete {
            complete {
              (movieManager ? MovieManager.DeleteMovie(movieId)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
            }
          }
        )
      } ~
        path("movie") {
          concat(
            post {
              entity(as[Movie]) { movie =>
                complete {
                  (movieManager ? MovieManager.CreateMovie(movie)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                }
              }
            },
            put {
              entity(as[Movie]) { movie =>
                complete {
                  (movieManager ? MovieManager.UpdateMovie(movie)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                }
              }
            }
          )
        }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
}
