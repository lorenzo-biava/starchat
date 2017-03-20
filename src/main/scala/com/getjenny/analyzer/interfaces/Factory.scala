package com.getjenny.analyzer.interfaces

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

trait Factory[T, V] {
  val operations = Set[String]()
  def get(name: String, argument: T): V
}