package controllers.actions

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.requests.BPMNDiagramRequest
import daos.{BPMNDiagramDAO, MongoDBUtil}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import scaldi.{Injectable, Injector}
import util.DefaultEnv

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * An ActionRefiner that takes a SecuredRequest and returns, in case of success, a BPMNDiagramRequest which contains the
  * requested BPMNDiagram. In case of failure a corresponding Result is returned. These types are wrapped in a
  * Future of Either.
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/18/2016
  */
case class DiagramAction(id: String)
                        (implicit inj: Injector)
//  extends ActionBuilder[BPMNDiagramRequest]
  extends ActionRefiner[({type R[A] = SecuredRequest[DefaultEnv, A]})#R, BPMNDiagramRequest] //abstract type
    with MongoDBUtil
    with Injectable
    with Results
    with I18nSupport {
  val diagramDAO = inject[BPMNDiagramDAO]
  val messagesApi = inject[MessagesApi]

  /**
    * Look at declaration
    * @param input request
    * @tparam A content type
    *
    * @return Either a Result or a BPMNDiagramRequest
    */
  override def refine[A](input: SecuredRequest[DefaultEnv, A]): Future[Either[Result, BPMNDiagramRequest[A]]] = {
    ObjectID(id) match {
      case Success(identifier) =>
        diagramDAO.find(identifier).map({
          case Some(diagram) => Right(BPMNDiagramRequest(diagram, input))
          case None => Left(BadRequest(Json.obj("message" -> Messages("bpmn.id.not.found"))))
        })
      case Failure(ex) => Future.successful(Left(BadRequest(Json.obj("message" -> Messages("bpmn.id.invalid.format")))))
    }
  }
}