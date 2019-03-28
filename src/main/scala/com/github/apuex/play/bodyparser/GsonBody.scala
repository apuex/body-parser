package com.github.apuex.play.bodyparser

import java.io.{InputStreamReader, Reader}
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat.{Parser, Printer}
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.mvc.{BodyParser, PlayBodyParsers}


object GsonBody {
  def apply()(implicit conf: GsonConfig): GsonBody = new GsonBody(conf)
  def apply(conf: GsonConfig): GsonBody = new GsonBody(conf)
}

case class GsonConfig(val parser: Parser, val printer: Printer)
class GsonBody(val conf: GsonConfig) extends PlayBodyParsers {
  private val methodCache = new ConcurrentHashMap[Class[_], Method]

  def print[T <: Message](o: T) = conf.printer.print(o)

  def parser[T <: Message]: BodyParser[T] = parser[T](DefaultMaxTextLength)

  def parser[T <: Message](maxLength: Long): BodyParser[T] = when(
    _.contentType.exists(m => m.equalsIgnoreCase("text/json") || m.equalsIgnoreCase("application/json")),
    tolerantGson[T](maxLength),
    createBadResult("Expecting text/json or application/json body", UNSUPPORTED_MEDIA_TYPE)
  )

  def tolerantGson[T <: Message](maxLength: Long): BodyParser[T] =
    tolerantBodyParser[T]("json", maxLength, "Invalid Json") { (request, bytes) =>
      val builder = getMessageBuilder(classOf[T])
      val reader = new InputStreamReader(bytes.iterator.asInputStream, request.charset.getOrElse("utf-8"))

      conf.parser.merge(reader, builder)
      reader.close()

      builder.build().asInstanceOf[T]
    }

  private def getMessageBuilder(clazz: Class[_ <: Message]) = {
    var method = methodCache.get(clazz)
    if (method == null) {
      method = clazz.getMethod("newBuilder")
      methodCache.put(clazz, method)
    }
    method.invoke(clazz).asInstanceOf[Message.Builder]
  }
}
