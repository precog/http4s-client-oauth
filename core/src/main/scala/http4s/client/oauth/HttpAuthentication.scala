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

import cats.Eq
import cats.instances.string._
import cats.syntax.eq._

import org.http4s._
import org.http4s.headers.Authorization

import scodec.Codec
import scodec.codecs._

sealed trait HttpAuthentication extends Product with Serializable

object HttpAuthentication {
  final case class BearerToken(token: String) extends HttpAuthentication

  def apply[F[_]](authn: HttpAuthentication, req: Request[F]): Request[F] =
    authn match {
      case BearerToken(tok) =>
        req.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, tok)))
    }

  def bearerToken(token: String): HttpAuthentication =
    BearerToken(token)

  implicit val binaryCodec: Codec[HttpAuthentication] =
    discriminated[HttpAuthentication].by(uint8)
      .| (0) { case BearerToken(t) => t } (BearerToken.apply) (utf8)

  implicit val eq: Eq[HttpAuthentication] =
    Eq instance {
      case (BearerToken(t1), BearerToken(t2)) => t1 === t2
    }
}
