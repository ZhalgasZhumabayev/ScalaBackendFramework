package com.example

case class TicketResponse(recipe: Ticket,
                          isSuccessful: Boolean,
                          statusCode: Int,
                          message: String)
