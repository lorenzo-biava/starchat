package com.getjenny.analyzer.interfaces

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

trait OperatorFactoryTrait[T, V] {
  val operations = Set[String]()
  def get(name: String, argument: T): V
}

trait AtomicFactoryTrait[T, V, Z] {
  val operations = Set[String]()
  def get(name: String, argument: T, restricted_args: Z): V
}
