import response.{ErrorResponse, SuccessfulResponse}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait SprayJsonSerializer extends DefaultJsonProtocol {
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat4(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}
