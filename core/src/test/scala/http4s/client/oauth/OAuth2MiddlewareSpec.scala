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

//import quasar.EffectfulQSpec
//import quasar.contrib.scalaz.MonadError_
//import quasar.connector.{ByteStore, MonadResourceErr, ResourceError}
//import quasar.connector.ByteStore.ops._

import cats.MonadError
import cats.Functor
import cats.effect.IO
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._

import java.lang.Exception

import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.dsl._
import org.http4s.implicits._

import org.specs2.mutable.Specification
import scalaz.-\/
import shims.applicativeToScalaz
import cats.ApplicativeError


object OAuth2MiddlewareSpec extends Specification
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  val AuthzUri = Uri.uri("http://example.com/oauth/authorization")
  val TokenUri = Uri.uri("http://example.com/oauth/token")
  val TestId = "tester"
  val TestSecret = "testersecret"

  val store = OAuth2State.ephemeral[IO]

//  implicit val ioMonadResourceErr: MonadResourceErr[IO] =
//    MonadError_.facet[IO](ResourceError.throwableP)

implicit val ioMonadResourceErr = ApplicativeError
  implicit def monadErrorFunctorRaise[F[_], E <: Throwable](
        implicit monadError: MonadError[F, Throwable]
    ): FunctorRaise[F, E] = new FunctorRaise[F, E] {
      override val functor: Functor[F]  = monadError
      override def raise[A](e: E): F[A] = monadError.raiseError(e)
    }

  def isAuthd(tok: String, req: Request[IO]): Boolean =
    req.headers.get(Authorization).exists(_.credentials == Credentials.Token("Bearer".ci, tok))

  def config(code: AuthorizationCode): OAuth2Config =
    OAuth2Config(OAuth2GrantConfig.AuthorizationCodeConfig(
      AuthorizationEndpoint(AuthzUri),
      Map.empty,
      TokenEndpoint(TokenUri),
      code,
      None,
      ClientId(TestId),
      ClientSecret(TestSecret),
      ClientAuthenticationMethod.RequestBody,
      None))

  "request succeeds with valid cached access token" >> {
    val token = "secret"

    val routes = HttpRoutes.of[IO] {
      case r @ GET -> Root / "foo" if isAuthd(token, r) => Ok("bar")
    }

  val client = for {
      bs <- store
      _ <- bs.putHttpAuthentication(HttpAuthentication.bearerToken(token))
      underlying = Client.fromHttpApp(routes.orNotFound)
    } yield OAuth2Middleware(bs, config(AuthorizationCode("1234")))(underlying)

    client.flatMap(_.fetch(GET(Uri.uri("http://example.com/foo"))) { res =>
      IO.pure(res.status.isSuccess)
    })

    // val x = client.map(c => { 
    //   val t = c.fetch(GET(Uri.uri("http://example.com/foo")))(res => IO.pure(res.status))
    //   t
    // } ) //.unsafeRunSync()
    // x.flatten.unsafeRunSync() must_=== Status.Ok
  
  }

  "request succeeds with access token obtained on first request" >> {
    val accessToken = "tok1234"

    val authCode = AuthorizationCode("8749def")

    val expectedParams =
      UrlForm.empty
        .updateFormField("grant_type", "authorization_code")
        .updateFormField("code", authCode.value)
        .updateFormField("client_id", TestId)
        .updateFormField("client_secret", TestSecret)

    val routes = HttpRoutes.of[IO] {
      case r @ GET -> Root / "baz" if isAuthd(accessToken, r) =>
        Ok("data")

      case r @ GET -> Root / "baz" =>
        Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "OAuth")))

      case r @ POST -> Root / "oauth" / "token" =>
        r.as[UrlForm] flatMap { params =>
          if (params eqv expectedParams)
            Ok(AccessTokenSuccess(
              AccessToken(accessToken),
              Some(AccessTokenType.Bearer),
              None, None, None))
          else
            BadRequest("Unexpected params")
        }
    }

    for {
      bs <- store

      underlying = Client.fromHttpApp(routes.orNotFound)
      client = OAuth2Middleware(bs, config(authCode))(underlying)

      data <- client.fetch(GET(Uri.uri("http://example.com/baz")))(_.as[String])
      cachedAuthn <- bs.getHttpAuthentication
    } yield {
      data must_=== "data"
      cachedAuthn must beSome(HttpAuthentication.BearerToken(accessToken))
    }
  }

  "request fails with AccessDenied when access token not obtained" >> {
    val authCode = AuthorizationCode("9749def")

    val routes = HttpRoutes.of[IO] {
      case r @ GET -> Root / "quux" =>
        Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "OAuth")))

      case r @ POST -> Root / "oauth" / "token" =>
        BadRequest("Invalid credentials")
    }

    for {
      bs <- store

      underlying = Client.fromHttpApp(routes.orNotFound)
      client = OAuth2Middleware(bs, config(authCode))(underlying)

      res <- MonadResourceErr[IO].attempt(client.fetch(GET(Uri.uri("http://example.com/baz")))(_.as[String]))
      cachedAuthn <- bs.getHttpAuthentication
    } yield {
      res must beLike {
        case -\/(ResourceError.AccessDenied(_, _, Some(AccessTokenError.Unknown(Some(msg))))) =>
          msg must_=== "Invalid credentials"
      }

      cachedAuthn must beNone
    }
  }

  "access token is refreshed when request fails initially and refresh token exists" >> {
    val accessToken = "tok2468"
    val refreshToken = "ref4321"

    val expectedParams =
      UrlForm.empty
        .updateFormField("grant_type", "refresh_token")
        .updateFormField("refresh_token", refreshToken)
        .updateFormField("client_id", TestId)
        .updateFormField("client_secret", TestSecret)

    val routes = HttpRoutes.of[IO] {
      case r @ GET -> Root / "quux" if isAuthd(accessToken, r) =>
        Ok("data")

      case r @ GET -> Root / "quux" =>
        Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "OAuth")))

      case r @ POST -> Root / "oauth" / "token" =>
        r.as[UrlForm] flatMap { params =>
          if (params eqv expectedParams)
            Ok(AccessTokenSuccess(
              AccessToken(accessToken),
              Some(AccessTokenType.Bearer),
              None, None, None))
          else
            BadRequest("Unexpected params")
        }
    }

    for {
      bs <- store
      _ <- bs.putHttpAuthentication(HttpAuthentication.BearerToken("invalid-token"))
      _ <- bs.insertString("oauth2.refresh-token", refreshToken)

      underlying = Client.fromHttpApp(routes.orNotFound)
      client = OAuth2Middleware(bs, config(AuthorizationCode("expired")))(underlying)

      data <- client.fetch(GET(Uri.uri("http://example.com/quux")))(_.as[String])
      cachedAuthn <- bs.getHttpAuthn
    } yield {
      data must_=== "data"
      cachedAuthn must beSome(HttpAuthentication.BearerToken(accessToken))
    }
  }

  "AccessDenied when request fails, refresh token exists but refresh doesn't succeed" >> {
    val routes = HttpRoutes.of[IO] {
      case r @ GET -> Root / "baat" =>
        Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "OAuth")))

      case r @ POST -> Root / "oauth" / "token" =>
        BadRequest("Invalid credentials")
    }

    for {
      bs <- store
      _ <- bs.putHttpAuthentication(HttpAuthentication.BearerToken("invalid-token"))
      _ <- bs.insertString("oauth2.refresh-token", "a-refresh-token")

      underlying = Client.fromHttpApp(routes.orNotFound)
      client = OAuth2Middleware(bs, config(AuthorizationCode("expired")))(underlying)

      res <- FunctorRaise[IO, AccessToken].attempt(client.fetch(GET(Uri.uri("http://example.com/baat")))(_.as[String]))
      cachedRefresh <- bs.lookupString("oauth2.refresh-token")
    } yield {
      res must beLike {
        case -\/(ResourceError.AccessDenied(_, _, Some(AccessTokenError.Unknown(Some(msg))))) =>
          msg must_=== "Invalid credentials"
      }

      cachedRefresh must beNone
    }
  }
}