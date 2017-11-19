package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 17/11/17.
  */

object Permissions extends Enumeration {
  type Permission = Value
  val read, write = Value
}

case class User(
                 id: String, /** user id */
                 password: String, /** user password */
                 salt: String, /** salt for password hashing */
                 permissions: Map[
                   String, /** index name */
                   Set[Permissions.Value] /** permissions granted for the index */
                   ]
               )
