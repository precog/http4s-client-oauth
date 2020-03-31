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

import argonaut._, Argonaut._

final case class OAuth2Config(grant: OAuth2GrantConfig) {
  def sanitized: OAuth2Config = OAuth2Config(grant.sanitized)
}

object OAuth2Config {
  implicit val codecJson: CodecJson[OAuth2Config] =
    casecodec1(OAuth2Config.apply, OAuth2Config.unapply)("grant")
}
