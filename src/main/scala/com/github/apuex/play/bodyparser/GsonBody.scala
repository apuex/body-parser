package com.github.apuex.play.bodyparser

import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

import akka.stream.Materializer
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat.{Parser, Printer}
import javax.inject.{Inject, Singleton}
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.http.{HttpErrorHandler, ParserConfiguration}
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{BodyParser, PlayBodyParsers}


case class GsonConfig(val parser: Parser, val printer: Printer)

@Singleton
class GsonBody @Inject()(val gson: GsonConfig,
                         val config: ParserConfiguration,
                         val errorHandler: HttpErrorHandler,
                         val materializer: Materializer,
                         val temporaryFileCreator: TemporaryFileCreator) extends PlayBodyParsers {

  private val methodCache = new ConcurrentHashMap[Class[_], Method]

  def print[T <: Message](o: T) = gson.printer.print(o)

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
