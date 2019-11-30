package com.example.model

/**
  * Model of a director
  * @param id unique ID of a director
  */
case class Director(id: String, firstName: String, middleName: Option[String], lastName: String)