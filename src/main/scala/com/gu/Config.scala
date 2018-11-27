package com.gu

import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load
  object Zuora {
    val client_id = conf.getString("zuora.client_id")
    val client_secret = conf.getString("zuora.client_secret")
  }
}
