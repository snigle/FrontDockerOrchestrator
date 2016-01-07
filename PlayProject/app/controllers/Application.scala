package controllers

import javax.inject.Inject
import actors.{ Delete, DeleteActor, DeployVMActor }
import akka.actor.{ ActorSystem, Props }
import models.{ Container, Vapp, VappFactory, Vm }
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ Json, Writes }
import play.api.libs.ws._
import play.api.mvc._
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.sys.process.Process
import scala.util.{ Failure, Success }
import scala.sys.process._
import actors.Init
import play.api.mvc.WebSocket.FrameFormatter
import play.api.Mode
import scala.concurrent.duration._


class Application @Inject() (ws: WSClient, system: ActorSystem) extends Controller {



  def index = Action.async {
    implicit request =>
      {
        //val cookie = getCookie
        //Future(Ok(cookie))
        reqJson.map(response => {
          Ok(response.json)
        })
      }
  }
  //Vapp id : 9dd013e3-3f51-4cde-a19c-f96b4ad2e350"
  def reqXml() = ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-"+current.configuration.getString("vapp.id").get).withHeaders(
    "Cookie" -> getCookie(),
    "Accept" -> "application/*+xml;version=1.5").withRequestTimeout(5000).get()

  def reqJson = current.mode match {
          case Mode.Dev => ws.url("https://"+current.configuration.getString("vapp.swarm-master.ip").get+":8080/containers/json?all=1").withRequestTimeout(5000).get
          case Mode.Prod => ws.url("https://192.168.2.100:2376/containers/json?all=1").withRequestTimeout(5000).get
  }

  def dashboard = Action.async { implicit request =>
    {
      /*implicit val timeout = Timeout(Duration(60,SECONDS))
       val result = Await.result(vm_deploy_actor ? VMDeployed("swarm-agent-" + (4)),timeout.duration).asInstanceOf[String]
       println(result)*/
      //       println("test");
      //TODO Faire en sorte que lorsque repJson ne fonctionne pas on peut envoyer None
      val vapp: Future[Vapp] = for {
        repXML <- reqXml()
        repJson <- reqJson
      } yield (VappFactory(repXML.xml, Some(repJson.json)))

      vapp.map { vapp => Ok(views.html.index(vapp)) }

    }
  }

  def vappXml = Action.async { implicit request =>
    {
      reqXml().map(response => Ok(response.xml))
    }
  }

  def getCookie() = {
    val req =
      ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/login").withHeaders("Accept" -> "application/*+xml;version=5.1").withAuth("user1@icc-02", "eisti0002", WSAuthScheme.BASIC).get().map {
        response =>
          {
            response.cookies
          }
      }
    Await.ready(req,Duration(10000, MILLISECONDS)).value.get match {
      case Success(x) => x.head.toString
      case Failure(x) => throw new Exception(x.getMessage)
    }
  }

  def newVM = WebSocket.acceptWithActor[String, String] { request =>
    out =>
      DeployVMActor.props(out, ws, reqXml, getCookie)
  }

  implicit val InitFormat = Json.format[Init]
  implicit val InitFormatter = FrameFormatter.jsonFrame[Init]
  def deleteVM = WebSocket.acceptWithActor[Init, String] { request =>
    out =>
      DeleteActor.props(out, ws, reqXml, getCookie)
  }

  def getVapp = Action.async {
    implicit request =>
      {
        val vapp: Future[Vapp] = for {
          repXML <- reqXml()
          repJson <- reqJson
        } yield (VappFactory(repXML.xml, Some(repJson.json)))
        vapp.map { vapp => Ok(Json.toJson(vapp)) }
      }
  }

  //Json descriptors :
  //id : String, name : String, image : String, ports : Seq[Int], active : Boolean
  implicit val ContainerToJson = new Writes[Container] {
    def writes(container: Container) = Json.obj(
      "id" -> container.id,
      "name" -> container.name,
      //"ports" -> container.ports,
      "image" -> container.image,
      "active" -> container.active)
  }

  //id : String, name : String, ipLocal : String, ipExternal : String, active : Boolean = false, containers : Seq[Container] = Nil
  implicit val VmToJson = new Writes[Vm] {
    def writes(vm: Vm) = Json.obj(
      "id" -> vm.id,
      "name" -> vm.name,
      "ipLocal" -> vm.ipLocal,
      "ipExternal" -> vm.ipExternal,
      "active" -> vm.active,
      "containers" -> vm.containers)
  }

  //id : String, vms : Seq[Vm], indice : Int
  implicit val VappToJson = new Writes[Vapp] {
    def writes(vapp: Vapp) = Json.obj(
      "id" -> vapp.id,
      "vms" -> vapp.vms,
      "indice" -> vapp.indice)
  }

}









