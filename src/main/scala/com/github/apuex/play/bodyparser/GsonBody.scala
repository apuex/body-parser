package com.github.apuex.play.bodyparser

import java.io._
import java.lang.reflect.Method
import java.util.concurrent._

import akka.stream._
import akka.util._
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat.{Parser, Printer}
import javax.inject._
import play.api.http.Status._
import play.api.http._
import play.api.libs.Files._
import play.api.mvc.Results._
import play.api.mvc._

case class GsonConfig(val parser: Parser, val printer: Printer)

@Singleton
class GsonBody @Inject()(val gson: GsonConfig,
                         val config: ParserConfiguration,
                         val errorHandler: HttpErrorHandler,
                         val materializer: Materializer,
                         val temporaryFileCreator: TemporaryFileCreator) extends PlayBodyParsers {

  private val methodCache = new ConcurrentHashMap[Class[_], Method]
  private val writable = (Writeable((a: String) => ByteString.fromArrayUnsafe(a.getBytes("utf-8")), Some("application/json; charset=utf-8")))

  def print[T <: Message](o: T) = Ok(gson.printer.print(o))(writable)

  def parser[T <: Message](clazz: Class[T]): BodyParser[T] = parser[T](clazz, DefaultMaxTextLength)

  def parser[T <: Message](clazz: Class[T], maxLength: Long): BodyParser[T] = when(
    _.contentType.exists(m => m.equalsIgnoreCase("text/json") || m.equalsIgnoreCase("application/json")),
    tolerantGson[T](clazz, maxLength),
    createBadResult("Expecting text/json or application/json body", UNSUPPORTED_MEDIA_TYPE)
  )

  def tolerantGson[T <: Message](clazz: Class[T], maxLength: Long): BodyParser[T] =
    tolerantBodyParser[T]("json", maxLength, "Invalid Json") { (request, bytes) =>

      val builder = getMessageBuilder(clazz)
      val reader = new InputStreamReader(bytes.iterator.asInputStream, request.charset.getOrElse("utf-8"))

      gson.parser.merge(reader, builder)
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
