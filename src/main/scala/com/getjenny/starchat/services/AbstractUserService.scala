package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 4/12/17.
  */

import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.auth.AbstractStarChatAuthenticator

import scala.concurrent.Future

trait AbstractUserService {
  def create(user: User): Future[IndexDocumentResult]
  def update(user: UserUpdate): Future[UpdateDocumentResult]
  def delete(user: UserId): Future[DeleteDocumentResult]
  def read(user: UserId): Future[User]
  def genUser(user: UserUpdate, authenticator: AbstractStarChatAuthenticator): Future[User]
}
