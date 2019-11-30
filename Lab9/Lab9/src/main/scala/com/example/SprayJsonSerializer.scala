package com.example

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait SprayJsonSerializer extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val ticketFormat: RootJsonFormat[Ticket] = jsonFormat4(Ticket)
  implicit val messageFormat: RootJsonFormat[TelegramMessage] = jsonFormat2(TelegramMessage)
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}
