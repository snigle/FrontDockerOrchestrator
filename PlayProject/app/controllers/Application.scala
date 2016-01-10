package controllers

import javax.inject.Inject

import actors.{DeleteActor, DeployVMActor, Init}
import akka.actor.ActorSystem
import models.{Vapp, VappFactory}
import play.api.Mode
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Application @Inject() (ws: WSClient, system: ActorSystem) extends Controller {

  /*
   * Return XML WSResponse describing the Vapp from Vcloud director API
   */
  def reqXml(): Future[WSResponse] = ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-" + current.configuration.getString("vapp.id").get).withHeaders(
    "Cookie" -> getCookie(),
    "Accept" -> "application/*+xml;version=1.5").withRequestTimeout(5000).get()

  /*
   * Get Json WSResponse from the docker swarm api
   */
  def reqJson = current.mode match {
    case Mode.Dev  => ws.url("https://" + current.configuration.getString("vapp.swarm-master.ip").get + ":8080/containers/àjson?all=1").withRequestTimeout(5000).get
    case Mode.Prod => ws.url("https://192.168.2.100:3376/containers/json?all=1").withRequestTimeout(5000).get
  }

  /*
   * Display Vapp informations using vcloud director api and docker swarm api
   */
  def dashboard = Action.async { implicit request =>
    {
      val vapp: Future[Vapp] = for {
        repXML <- reqXml()
        repJson <- reqJson
      } yield (VappFactory(repXML.xml, Some(repJson.json)))

      vapp.map { vapp => Ok(views.html.index(vapp)) }

    }
  }

  /*
   * Display the XML of a vapp returned by the vcloud director api
   */
  def vappXml = Action.async { implicit request =>
    {
      reqXml().map(response => Ok(response.xml))
    }
  }

  /*
   * Authentification to the vcloud director api
   */
  def getCookie() = {
    val req =
      ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/login").withHeaders("Accept" -> "application/*+xml;version=5.1").withAuth("user1@icc-02", "eisti0002", WSAuthScheme.BASIC).get().map {
        response =>
          {
            response.cookies
          }
      }
    Await.ready(req, Duration(10000, MILLISECONDS)).value.get match {
      case Success(x) => x.head.toString
      case Failure(x) => throw new Exception(x.getMessage)
    }
  }

  /*
   * Init creation of a VM by sending "start". Return informations of
   */
  def newVM = WebSocket.acceptWithActor[String, String] { request =>
    out =>
      DeployVMActor.props(out, ws, reqXml, getCookie)
  }

  /*
   * To delete a VM send object Init with the vm id
   */
  implicit val InitFormat = Json.format[Init]
  implicit val InitFormatter = FrameFormatter.jsonFrame[Init]
  def deleteVM = WebSocket.acceptWithActor[Init, String] { request =>
    out =>
      DeleteActor.props(out, ws, reqXml, getCookie)
  }


}









