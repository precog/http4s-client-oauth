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

import org.http4s.Uri
import org.http4s.argonaut._

sealed trait OAuth2GrantConfig extends Product with Serializable {
  def sanitized: OAuth2GrantConfig
}

object OAuth2GrantConfig {
  final case class AuthorizationCodeConfig(
      authzEndpoint: AuthorizationEndpoint,
      extraAuthzParams: Map[String, String],
      tokenEndpoint: TokenEndpoint,
      authorizationCode: AuthorizationCode,
      scope: Option[Scope],
      clientId: ClientId,
      clientSecret: ClientSecret,
      clientAuthnMethod: ClientAuthenticationMethod,
      redirectUri: Option[Uri])
      extends OAuth2GrantConfig {

    val RedactedStr: String = "<REDACTED>"

    def sanitized = copy(
      authorizationCode = AuthorizationCode(RedactedStr),
      clientSecret = ClientSecret(RedactedStr))
  }

  implicit def decodeJson: DecodeJson[OAuth2GrantConfig] =
    DecodeJson(c => (c --\ "grantType").as[String] flatMap {
      case "code" => authorizationCodeCodecJson.decode(c).map(x => x: OAuth2GrantConfig)
      case other => DecodeResult.fail(s"Unsupported grant type: '$other'", c.history)
    })

  implicit def encodeJson: EncodeJson[OAuth2GrantConfig] =
    EncodeJson {
      case cfg: AuthorizationCodeConfig =>
        ("grantType" := "code") ->: authorizationCodeCodecJson.encode(cfg)
    }

  ////

  private val authorizationCodeCodecJson: CodecJson[AuthorizationCodeConfig] =
    casecodec9(AuthorizationCodeConfig.apply, AuthorizationCodeConfig.unapply)(
      "authorizationEndpoint",
      "extraAuthorizationParams",
      "tokenEndpoint",
      "authorizationCode",
      "scope",
      "clientId",
      "clientSecret",
      "clientAuthenticationMethod",
      "redirectUri")
}
