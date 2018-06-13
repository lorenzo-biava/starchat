package com.getjenny.starchat.tools

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/11/17
  */

import java.time.Instant

object Time {

  def timestampEpoc: Long = {
    Instant.now.getEpochSecond
  }

  def timestampMillis: Long = {
    Instant.now().toEpochMilli
  }
}
