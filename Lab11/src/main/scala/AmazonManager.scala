import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import Boot.client
import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3ClientBuilder}
import response.{ErrorResponse, SuccessfulResponse}

import scala.util.{Failure, Success, Try}

object AmazonManager {

  case class GetObject(objectKey: String)

  case class CreateObject(filename: String)

  def props(client: AmazonS3) = Props(new AmazonManager(client))
}

class AmazonManager(client: AmazonS3) extends Actor with ActorLogging {

  import AmazonManager._

  override def receive: Receive = {
    case GetObject(objectKey) =>
      if (client.doesObjectExist(AmazonS3Config.bucketName, objectKey)) {
        Try(getObject(objectKey)) match {
          case Success(_) =>
            sender() ! Right(200, s"Object with key $objectKey successfully downloaded")
          case Failure(_) =>
            sender() ! Left(500, s"Error while downloading object with key $objectKey")
        }
      } else {
        sender() ! Left(404, s"Object does not exist")
      }
    case CreateObject(filename) => {
      val file = new File(s"${AmazonS3Config.pathS3}/$filename")
      if (file.exists()) {
        val request = new PutObjectRequest(
          AmazonS3Config.bucketName,
          filename,
          new File(s"${AmazonS3Config.pathS3}/$filename"))
        val metadata = new ObjectMetadata()
        metadata.setContentType("plain/text")
        metadata.addUserMetadata("user-type", "customer")
        request.setMetadata(metadata)

        Try(client.putObject(request)) match {
          case Success(_) =>
            sender() ! Right(SuccessfulResponse(200, s"Object with key $filename successfully uploaded"))
          case Failure(_) =>
            sender() ! Left(ErrorResponse(500, s"Error while uploading object with key $filename"))
        }
        client.putObject(request)
      } else {
        sender() ! Left(404, s"Object cannot be found")
      }
    }
  }

  private def getObject(objectKey: String): Unit = {
    val fullObject = client.getObject(new GetObjectRequest(AmazonS3Config.bucketName, objectKey))
    val objectStream = fullObject.getObjectContent

    val file = new File(s"${AmazonS3Config.pathS3}/$objectKey")
    if (!file.exists()) file.mkdirs()
    file.createNewFile()

    Files.copy(objectStream, file.toPath, StandardCopyOption.REPLACE_EXISTING)
    objectStream.close()
  }
}
