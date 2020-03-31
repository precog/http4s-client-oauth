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

import cats.effect.Sync

import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.argonaut._

final case class AccessTokenSuccess(
    accessToken: AccessToken,
    // Not optional per the spec, but implementations apparently don't care
    tokenType: Option[AccessTokenType],
    expiresIn: Option[ExpiresIn],
    refreshToken: Option[RefreshToken],
    scope: Option[Scope])

object AccessTokenSuccess {
  implicit val codecJson: CodecJson[AccessTokenSuccess] =
    casecodec5(AccessTokenSuccess.apply, AccessTokenSuccess.unapply)(
      "access_token", "token_type", "expires_in", "refresh_token", "scope")

  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, AccessTokenSuccess] =
    jsonOf[F, AccessTokenSuccess]

  implicit def entityEncoder[F[_]: Sync]: EntityEncoder[F, AccessTokenSuccess] =
    jsonEncoderOf[F, AccessTokenSuccess]
}
