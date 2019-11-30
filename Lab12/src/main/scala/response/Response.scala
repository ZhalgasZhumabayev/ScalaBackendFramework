package response

sealed trait Response

case class SuccessfulResponse(status: Int, message: String, contentType: String, bytes: Array[Byte]) extends Response
case class ErrorResponse(status: Int, message: String) extends Response
