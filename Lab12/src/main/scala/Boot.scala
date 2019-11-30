import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{delete, put, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import response.{ErrorResponse, SuccessfulResponse}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("photo-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  private val client = Config.client

  private val photoManager = system.actorOf(PhotoManager.props(client), "photo-manager")

  createBucketIfDoesNotExist()

  private val route =
    pathPrefix("photos") {
      path(Segment) { id =>
        get {
          complete {
            (photoManager ? PhotoManager.GetPhoto(id)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
          }
        }
      }
    }

  Http().bindAndHandle(route, "0.0.0.0", 8080)

  private def createBucketIfDoesNotExist(): Unit = {
    if (!client.doesBucketExistV2(Config.bucketName)) {
      client.createBucket(Config.bucketName)
    }
  }
}
