package com.example

import com.example.model.{Director, ErrorResponse, Movie, SuccessfulResponse}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait SprayJsonSerializer extends DefaultJsonProtocol {
  implicit val directorFormat: RootJsonFormat[Director] = jsonFormat4(Director)
  implicit val movieFormat: RootJsonFormat[Movie] = jsonFormat4(Movie)
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}
