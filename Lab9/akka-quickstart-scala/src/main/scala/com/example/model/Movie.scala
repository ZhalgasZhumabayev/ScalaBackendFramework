package com.example.model

/**
  * Model of a movie
  * @param id unique ID of a movie
  */
case class Movie(id: String, title: String, director: Director, yearOfRelease: Int)