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

sealed trait ErrorCode extends Product with Serializable

object ErrorCode {
  case object InvalidRequest extends ErrorCode
  case object InvalidClient extends ErrorCode
  case object InvalidGrant extends ErrorCode
  case object UnauthorizedClient extends ErrorCode
  case object UnsupportedGrantType extends ErrorCode
  case object InvalidScope extends ErrorCode

  def label(ec: ErrorCode): String =
    ec match {
      case InvalidRequest => "Invalid request"
      case InvalidClient => "Invalid client"
      case InvalidGrant => "Invalid grant"
      case UnauthorizedClient => "Unauthorized client"
      case UnsupportedGrantType => "Unsupported grant type"
      case InvalidScope => "Invalid scope"
    }

  implicit val decodeJson: DecodeJson[ErrorCode] =
    DecodeJson.optionDecoder(
      _.string collect {
        case "invalid_request" => InvalidRequest
        case "invalid_client" => InvalidClient
        case "invalid_grant" => InvalidGrant
        case "unauthorized_client" => UnauthorizedClient
        case "unsupported_grant_type" => UnsupportedGrantType
        case "invalid_scope" => InvalidScope
      },
      "Unknown error code")

  implicit val encodeJson: EncodeJson[ErrorCode] =
    EncodeJson {
      case InvalidRequest => jString("invalid_request")
      case InvalidClient => jString("invalid_client")
      case InvalidGrant => jString("invalid_grant")
      case UnauthorizedClient => jString("unauthorized_client")
      case UnsupportedGrantType => jString("unsupported_grant_type")
      case InvalidScope => jString("invalid_scope")
    }
}
