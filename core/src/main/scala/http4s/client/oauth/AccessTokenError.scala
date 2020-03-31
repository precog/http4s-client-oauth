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

import argonaut.{DecodeResult => _, _}, Argonaut._

import cats.data.EitherT
import cats.effect.Sync
import cats.instances.string._

import fs2._

import org.http4s._
import org.http4s.argonaut._

sealed trait AccessTokenError extends Exception

object AccessTokenError {
  final case class Standard(
      code: ErrorCode,
      description: Option[String],
      uri: Option[Uri]) extends AccessTokenError {

    override def getMessage: String = {
      val descPart = description.fold("")(": " + _)
      val uriPart = uri.fold("")(u => s" (see ${u.renderString})")

      s"${ErrorCode.label(code)}${descPart}${uriPart}"
    }
  }

  final case class Unknown(detail: Option[String]) extends AccessTokenError {
    override def getMessage: String =
      s"Failed to obtain an OAuth2 access token${detail.fold("")(": " + _)}"
  }

  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, AccessTokenError] =
    new EntityDecoder[F, AccessTokenError] {
      val consumes = Set(MediaRange.`*/*`)

      def decode(media: Media[F], strict: Boolean) =
        EitherT.right[DecodeFailure](media.body.compile.to(Array)) flatMap { bytes =>
          val staticMedia = Media[F](Stream.chunk(Chunk.array(bytes)), media.headers)

          standardEntityDecoder[F]
            .decode(staticMedia, strict)
            .orElse(unknownEntityDecoder[F].widen[AccessTokenError].decode(staticMedia, strict))
        }
    }

  ////

  private implicit val standardCodecJson: CodecJson[Standard] =
    casecodec3(Standard.apply, Standard.unapply)(
      "error", "error_description", "error_uri")

  private def standardEntityDecoder[F[_]: Sync]: EntityDecoder[F, Standard] =
    jsonOf[F, Standard]

  private def unknownEntityDecoder[F[_]: Sync]: EntityDecoder[F, Unknown] =
    new EntityDecoder[F, Unknown] {
      val consumes = Set(MediaRange.`*/*`)

      def decode(media: Media[F], strict: Boolean): DecodeResult[F, Unknown] = {
        EitherT.right(media.bodyAsText.compile.foldMonoid)
          .map(d => Unknown(Some(d)))
      }
    }
}
