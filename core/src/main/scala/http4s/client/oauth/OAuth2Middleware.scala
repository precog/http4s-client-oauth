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

import cats.MonadError
import cats.data.{EitherT, OptionT}
import cats.effect.{Resource, Sync}
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._

import org.http4s._
import org.http4s.client.Client
import org.http4s.headers.Authorization

object OAuth2Middleware {
  import OAuth2GrantConfig._

  def apply[F[_]: Sync](
      store: OAuth2State[F],
      config: OAuth2Config)(
      client: Client[F])(implicit F: FunctorRaise[F, AccessDenied])
      : Client[F] = {

    def accessDenied[A](detail: String, cause: Option[Throwable]): F[A] =
      FunctorRaise[F, AccessDenied].raise(AccessDenied(detail, cause))

    def attemptRequest(req: Request[F]): Resource[F, Response[F]] =
      for {
        authn <- Resource.liftF(config.grant match {
          case codeCfg: AuthorizationCodeConfig =>
            EitherT(authorizationCode(store, codeCfg, client))
              .valueOrF(t => accessDenied[HttpAuthentication]("Authentication via OAuth2 'authorization_code' failed", Some(t)))
        })

        initialRes <- client.run(HttpAuthentication(authn, req))

        // TODO: May need to add configurable error conditions to configuration, rather than relying on 401
        res <- Resource liftF {
          if (initialRes.status === Status.Unauthorized)
            initialRes.bodyAsText.compile.foldMonoid flatMap { body =>
              accessDenied[Response[F]]("Invalid OAuth access token: " + body, None)
            }
          else
            store.putHttpAuthentication(authn).as(initialRes)
        }
      } yield res

    def attemptCached(req: Request[F]): Resource[F, Response[F]] =
      Resource.liftF(store.getHttpAuthentication) flatMap {
        case Some(authn) =>
          client.run(HttpAuthentication(authn, req)) flatMap { res =>
            if (res.status === Status.Unauthorized)
              Resource.liftF(store.deleteHttpAuthentication) >> attemptRequest(req)
            else
              res.pure[Resource[F, ?]]
          }

        case None =>
          attemptRequest(req)
      }


    Client[F](attemptCached)
  }

  ///

  private val AuthorizationCodeType = "authorization_code"
  private val RefreshTokenType = "refresh_token"

  private val GrantTypeParam = "grant_type"
  private val CodeParam = "code"
  private val RedirectUriParam = "redirect_uri"
  private val ClientIdParam = "client_id"
  private val ClientSecretParam = "client_secret"
  private val RefreshTokenParam = "refresh_token"

  private def authorizationCode[F[_]: Sync: MonadError[?[_], Throwable]](
      store: OAuth2State[F],
      config: AuthorizationCodeConfig,
      client: Client[F])
      : F[Either[Throwable, HttpAuthentication]] = {

    def accessTokenAuthn(params: UrlForm): F[Either[Throwable, HttpAuthentication]] = {
      val req0 =
        Request[F](Method.POST, config.tokenEndpoint.value)

      val req = config.clientAuthnMethod match {
        case ClientAuthenticationMethod.Basic =>
          val creds = BasicCredentials(config.clientId.value, config.clientSecret.value)

          req0
            .putHeaders(Authorization(creds))
            .withEntity(params)

        case ClientAuthenticationMethod.RequestBody =>
          val authParams =
            UrlForm(
              ClientIdParam -> config.clientId.value,
              ClientSecretParam -> config.clientSecret.value)

          req0.withEntity(params |+| authParams)
      }

      client.fetch(req) { res =>
        if (res.status.isSuccess)
          for {
            success <- res.as[AccessTokenSuccess]

            authn = (success.tokenType getOrElse AccessTokenType.Bearer) match {
              case AccessTokenType.Bearer => HttpAuthentication.bearerToken(success.accessToken.value)
            }

            _ <- success.refreshToken.traverse(t => store.putRefreshToken(t))
          } yield authn.asRight[Throwable]
        else
          res.as[AccessTokenError].map(err => (err: Throwable).asLeft[HttpAuthentication])
      }
    }

    def fromCode: F[Either[Throwable, HttpAuthentication]] = {
      val params =
        UrlForm.empty
          .updateFormField(GrantTypeParam, AuthorizationCodeType)
          .updateFormField(CodeParam, config.authorizationCode.value)
          .updateFormField(RedirectUriParam, config.redirectUri)

      accessTokenAuthn(params)
    }

    def fromRefresh(refreshToken: String): F[Either[Throwable, HttpAuthentication]] = {
      val params =
        UrlForm.empty
          .updateFormField(GrantTypeParam, RefreshTokenType)
          .updateFormField(RefreshTokenParam, refreshToken)

      accessTokenAuthn(params)
    }

    OptionT(store.getRefreshToken)
      .semiflatMap(fromRefresh)
      .semiflatMap(_.leftTraverse(err => store.deleteHttpAuthentication.as(err)))
      .getOrElseF(fromCode)
  }

}
