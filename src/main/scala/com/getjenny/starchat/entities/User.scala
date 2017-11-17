package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 17/11/17.
  */

object Permissions extends Enumeration {
  type Permission = Value
  val create, read, update, delete = Value
}

case class User(
                 id: String, /** user id */
                 password: String, /** user password */
                 salt: Long, /** salt for password hashing */
                 permissions: Map[
                   String, /** index name */
                   Set[Permissions.Value] /** permissions granted for the index */
                   ]
               )
