/*
 * Copyright 2014–2020 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package http4s.client.oauth

import slamdata.Predef._

import argonaut._, Argonaut._
import argonaut.ArgonautCats._

import cats.data.NonEmptyList

import org.http4s.Uri
import org.http4s.argonaut._

import scala.AnyVal
import scala.concurrent.duration._

sealed trait ClientAuthenticationMethod extends Product with Serializable

object ClientAuthenticationMethod {
  case object Basic extends ClientAuthenticationMethod
  case object RequestBody extends ClientAuthenticationMethod

  implicit val decodeJson: DecodeJson[ClientAuthenticationMethod] =
    DecodeJson.optionDecoder(
      _.string collect {
        case "basic" => Basic
        case "request-body" => RequestBody
      },
      "ClientAuthenticationMethod")

  implicit val encodeJson: EncodeJson[ClientAuthenticationMethod] =
    EncodeJson {
      case Basic => jString("basic")
      case RequestBody => jString("request-body")
    }
}

final case class AuthorizationEndpoint(value: Uri) extends AnyVal

object AuthorizationEndpoint {
  implicit val codecJson: CodecJson[AuthorizationEndpoint] =
    CodecJson.derived[Uri].xmap(AuthorizationEndpoint(_))(_.value)
}

final case class TokenEndpoint(value: Uri) extends AnyVal

object TokenEndpoint {
  implicit val codecJson: CodecJson[TokenEndpoint] =
    CodecJson.derived[Uri].xmap(TokenEndpoint(_))(_.value)
}

final case class AuthorizationCode(value: String) extends AnyVal

object AuthorizationCode {
  implicit val codecJson: CodecJson[AuthorizationCode] =
    CodecJson.derived[String].xmap(AuthorizationCode(_))(_.value)
}

final case class ClientId(value: String) extends AnyVal

object ClientId {
  implicit val codecJson: CodecJson[ClientId] =
    CodecJson.derived[String].xmap(ClientId(_))(_.value)
}

final case class ClientSecret(value: String) extends AnyVal

object ClientSecret {
  implicit val codecJson: CodecJson[ClientSecret] =
    CodecJson.derived[String].xmap(ClientSecret(_))(_.value)
}

final case class ScopeToken(value: String) extends AnyVal

object ScopeToken {
  implicit val codecJson: CodecJson[ScopeToken] =
    CodecJson.derived[String].xmap(ScopeToken(_))(_.value)
}

final case class Scope(tokens: NonEmptyList[ScopeToken]) extends AnyVal

object Scope {
  def apply(t: String, ts: String*): Scope =
    Scope(NonEmptyList(ScopeToken(t), ts.map(ScopeToken(_)).toList))

  implicit val codecJson: CodecJson[Scope] =
    CodecJson.derived[NonEmptyList[ScopeToken]].xmap(Scope(_))(_.tokens)
}

final case class AccessToken(value: String) extends AnyVal

object AccessToken {
  implicit val codecJson: CodecJson[AccessToken] =
    CodecJson.derived[String].xmap(AccessToken(_))(_.value)
}

final case class RefreshToken(value: String) extends AnyVal

object RefreshToken {
  implicit val codecJson: CodecJson[RefreshToken] =
    CodecJson.derived[String].xmap(RefreshToken(_))(_.value)
}

final case class ExpiresIn(seconds: Long) extends AnyVal {
  def toDuration: FiniteDuration = seconds.seconds
}

object ExpiresIn {
  implicit val codecJson: CodecJson[ExpiresIn] =
    CodecJson.derived[Long].xmap(ExpiresIn(_))(_.seconds)
}
