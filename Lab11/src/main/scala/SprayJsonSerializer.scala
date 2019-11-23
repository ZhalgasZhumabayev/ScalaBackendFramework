import model.Path
import response.{ErrorResponse, SuccessfulResponse}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait SprayJsonSerializer extends DefaultJsonProtocol {
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
  implicit val pathFormat: RootJsonFormat[Path] = jsonFormat1(Path)
}
