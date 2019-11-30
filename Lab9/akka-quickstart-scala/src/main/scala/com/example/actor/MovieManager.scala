package com.example.actor

import akka.actor.{Actor, ActorLogging, Props}
import com.example.model.{ErrorResponse, Movie, SuccessfulResponse}

object MovieManager {
  case class CreateMovie(movie: Movie)
  case class ReadMovie(id: String)
  case class UpdateMovie(movie: Movie)
  case class DeleteMovie(id: String)

  def props() = Props(new MovieManager)
}

class MovieManager extends Actor with ActorLogging {
  import MovieManager._

  var movies: Map[String, Movie] = Map()

  override def receive: Receive = {
    case CreateMovie(movie) => movies.get(movie.id) match {
      case Some(_) =>
        log.warning(s"Cannot create movie with ID = ${movie.id} because it already exists.")
        sender() ! Left(ErrorResponse(409, s"Movie with ID = ${movie.id} already exists"))
      case None =>
        movies += (movie.id -> movie)
        log.info(s"Movie with ID = ${movie.id} successfully created. Movie = [$movie]")
        sender() ! Right(SuccessfulResponse(201, s"Movie with ID = ${movie.id} successfully created. Movie = [$movie]"))
    }

    case ReadMovie(id) => movies.get(id) match {
      case Some(movie) =>
        log.info(s"Movie with ID = $id was read.")
        sender() ! Right(movie)
      case None => wasNotFoundError(id)
    }

    case UpdateMovie(movie) => movies.get(movie.id) match {
      case Some(_) =>
        movies += (movie.id -> movie)
        log.info(s"Movie with ID = ${movie.id} successfully updated. Movie: [$movie]")
        sender() ! Right(SuccessfulResponse(204, s"Movie with ID = ${movie.id} successfully updated. Movie: [$movie]"))
      case None => wasNotFoundError(movie.id)
    }

    case DeleteMovie(id) => movies.get(id) match {
      case Some(_) =>
        movies -= id
        log.info(s"Movie with ID = $id successfully deleted.")
        sender() ! Right(SuccessfulResponse(202, s"Movie with ID = $id successfully deleted."))
      case None => wasNotFoundError(id)
    }
  }

  private def wasNotFoundError(id: String): Unit = {
    log.warning(s"Movie with ID = $id was not found")
    sender() ! Left(ErrorResponse(404, s"Movie with ID = $id was not found."))
  }
}