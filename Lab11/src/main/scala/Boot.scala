import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{delete, put, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import model.Path
import response.{ErrorResponse, SuccessfulResponse}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("amazon-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  private val client = AmazonS3Config.client

  private val amazonManager = system.actorOf(AmazonManager.props(client), "amazon-manager")

  createBucketIfDoesNotExist()

  private val route =
    pathPrefix("lab11") {
      path("file") {
        concat(
          get {
            parameters("filename".as[String]) { filename =>
              complete {
                (amazonManager ? AmazonManager.GetObject(filename)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
              }
            }
          },
          post {
            entity(as[Path]) { path =>
              complete {
                (amazonManager ? AmazonManager.CreateObject(path.filename)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
              }
            }
          })
      }
    }

  private val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  private def createBucketIfDoesNotExist(): Unit = {
    if (!client.doesBucketExistV2(AmazonS3Config.bucketName)) {
      client.createBucket(AmazonS3Config.bucketName)
    }
    if (!client.doesBucketExistV2(AmazonS3Config.bucketNameV2)) {
      client.createBucket(AmazonS3Config.bucketNameV2)
    }
  }
}