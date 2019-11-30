import java.nio.file.Files

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectRequest, S3Object}

import scala.util.{Failure, Success, Try}

object PhotoManager {

  case class GetPhoto(id: String)

  def props(client: AmazonS3) = Props(new PhotoManager(client))
}

class PhotoManager(client: AmazonS3) extends Actor with ActorLogging {

  import PhotoManager._

  override def receive: Receive = {
    case GetPhoto(id) =>
      if (client.doesObjectExist(Config.bucketName, id)) {
        Try(getObject(id)) match {
          case Success(objectS3) =>
            val contentType = objectS3.getObjectMetadata.getContentType
            val bytes = Stream.continually(objectS3.getObjectContent.read)
              .takeWhile(-1 !=)
              .map(_.toByte)
              .toArray
            sender() ! Right(200, s"Image id = $id", contentType, bytes)
          case Failure(_) =>
            sender() ! Left(500, "Error while retrieving the photo")
        }
      } else sender() ! Left(404, s"Image $id does not exist")
  }

  private def getObject(id: String): S3Object = {
    client.getObject(new GetObjectRequest(Config.bucketName, id))
  }
}
