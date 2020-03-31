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

sealed trait AccessTokenType extends Product with Serializable

object AccessTokenType {
  case object Bearer extends AccessTokenType

  implicit val decodeJson: DecodeJson[AccessTokenType] =
    DecodeJson.optionDecoder(
      _.string collect {
        case "bearer" => Bearer
      },
      "Unknown access token type")

  implicit val encodeJson: EncodeJson[AccessTokenType] =
    EncodeJson {
      case Bearer => jString("bearer")
    }
}
