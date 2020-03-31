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

import slamdata.Predef.{Option, Unit, None, String, ???}

import cats.effect.Sync
import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.implicits._
import slamdata.Predef

trait OAuth2State[F[_]] {
  def getHttpAuthentication: F[Option[HttpAuthentication]]
  def putHttpAuthentication(value: HttpAuthentication): F[Unit]
  def deleteHttpAuthentication: F[Unit]

  def getRefreshToken: F[Option[String]]
  def putRefreshToken(value: RefreshToken): F[Unit]
  def deleteRefreshToken: F[Unit]
}

object OAuth2State {
  def ephemeral[F[_]: Sync]: F[OAuth2State[F]] =
    for {
      ref <- Ref.of[F, (Option[RefreshToken], Option[HttpAuthentication])]((None, None))
      store = new OAuth2State[F] {
          override def putHttpAuthentication(value: HttpAuthentication): F[Unit] = ???
          override def putRefreshToken(value: RefreshToken): F[Unit] = ???
          override def getRefreshToken: F[Option[Predef.String]] = ???
          override def getHttpAuthentication: F[Option[HttpAuthentication]] = ???
          override def deleteRefreshToken: F[Unit] = ???
          override def deleteHttpAuthentication: F[Unit] = ???
      }
    } yield store
}