package com.example

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import spray.json._
import scala.util.Try

trait ElasticSerializer extends SprayJsonSerializer {

  implicit object TicketIndexable extends Indexable[Ticket] {
    override def json(ticket: Ticket): String = ticket.toJson.compactPrint
  }

  implicit object TicketHitReader extends HitReader[Ticket] {
    override def read(hit: Hit): Either[Throwable, Ticket] = {
      Try {
        val json = hit.sourceAsString.parseJson
        json.convertTo[Ticket]
      }.toEither
    }
  }
}
