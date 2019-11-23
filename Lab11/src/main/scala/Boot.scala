import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{delete, put, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.amazonaws.services.s3.model.{GetObjectRequest, ListObjectsRequest, ListObjectsV2Request, ObjectMetadata, PutObjectRequest}
import model.Path
import org.slf4j.LoggerFactory
import response.{ErrorResponse, SuccessfulResponse}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("amazon-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

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
      } ~
        pathPrefix("task2") {
          path("in") {
            get {
              complete {
                downloadIn()
                "DownloadIn"
              }
            }
          } ~
            path("out") {
              get {
                complete {
                  uploadOut(new File(AmazonS3Config.pathResOut))
                  "UploadOut"
                }
              }
            }
        }
    }

  Http().bindAndHandle(route, "0.0.0.0", 8080)

  private def uploadOut(root: File): Unit = {
    val files = root.listFiles().filter(!_.getName.equals(".DS_Store"))
    for (file <- files) {
      if (file.isDirectory) uploadOut(new File(file.getPath))
      else uploadFile(file)
    }
  }

  private def uploadFile(file: File): Unit = {
    val request = new PutObjectRequest(
      AmazonS3Config.bucketNameV2,
      file.getPath.replace(s"${AmazonS3Config.pathResOut}/", ""),
      file)
    val metadata = new ObjectMetadata()
    metadata.setContentType("plain/text")
    metadata.addUserMetadata("user-type", "customer")
    request.setMetadata(metadata)
    client.putObject(request)
  }

  private def downloadIn(): Unit = {
    val request = new ListObjectsV2Request().withBucketName(AmazonS3Config.bucketNameV2)
    val objectsList = client.listObjectsV2(request)
    objectsList.getObjectSummaries.forEach(obj => createFile(obj.getKey))
  }

  private def createFile(objectKey: String): Unit = {
    val fullObject = client.getObject(new GetObjectRequest(AmazonS3Config.bucketNameV2, objectKey))
    val objectStream = fullObject.getObjectContent

    val file = new File(s"${AmazonS3Config.pathResIn}/$objectKey")
    if (!file.isDirectory) {
      if (!file.exists()) file.mkdirs()
      file.createNewFile()

      Files.copy(objectStream, file.toPath, StandardCopyOption.REPLACE_EXISTING)
    }
    objectStream.close()
  }

  private def createBucketIfDoesNotExist(): Unit = {
    if (!client.doesBucketExistV2(AmazonS3Config.bucketName)) {
      client.createBucket(AmazonS3Config.bucketName)
    }
    if (!client.doesBucketExistV2(AmazonS3Config.bucketNameV2)) {
      client.createBucket(AmazonS3Config.bucketNameV2)
    }
  }
}