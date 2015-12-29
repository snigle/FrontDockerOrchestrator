package controllers

import javax.inject.Inject
import actors.{DeployVMActor, VMDeployed}
import akka.actor.{ActorSystem, Props}
import models.Vapp
import play.api.Play.current
import play.api.libs.ws._
import play.api.mvc._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import models.Vapp
import models.VappFactory
import actors.Delete
import actors.DeleteActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import actors.ContainersActor
import play.api.libs.json.Writes
import models.Vapp
import play.api.libs.json.Json
import models.Vm
import models.Container






class Application @Inject() (ws: WSClient, system: ActorSystem) extends Controller {
  
  
//  var cookie: Seq[WSCookie] = Seq[WSCookie]()
//  val vm_deploy_actor =  system.actorOf(Props(new DeployActor(reqXml,getCookie)))
   val vm_delete_actor =  system.actorOf(Props(new DeleteActor(ws,reqXml,getCookie)))




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

  def reqXml() = ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/").withHeaders(
    "Cookie" -> getCookie(),
    "Accept" -> "application/*+xml;version=1.5").get()
    
  def reqJson = ws.url("https://192.168.30.53:8080/containers/json?all=1").get

  def dashboard = Action.async { implicit request =>
    {

       /*implicit val timeout = Timeout(Duration(60,SECONDS))
       val result = Await.result(vm_deploy_actor ? VMDeployed("swarm-agent-" + (4)),timeout.duration).asInstanceOf[String]
       println(result)*/
//       println("test");
      //TODO Faire en sorte que lorsque repJson ne fonctionne pas on peut envoyer None
       val vapp : Future[Vapp] = for{
         repXML <- reqXml()
         repJson <- reqJson
       }yield (VappFactory(repXML.xml, Some(repJson.json)))
       

        vapp.map { vapp => Ok(views.html.index(vapp)) }
        


    
  }
  }

  def vappXml = Action.async { implicit request =>
    {
      reqXml().map(response => Ok(response.xml))
    }
  }


  // Pas encore testée
//  def getVMs =  {
//    val req =
//  {
//    //"Accept:application/*+xml;version=1.5" -X GET https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-cb109e5b-e457-450e-aff9-322cdd6181f6/productSections
//    ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/").withHeaders(
//      "Cookie" -> getCookie,
//      "Accept" -> "application/*+xml;version=1.5").get().map(response => {
//      val ids = response.xml \ "Children" \ "Vm"
//      println(ids.isEmpty)
//      println(ids.map(id=>id.attribute("id"))mkString("\n"))
//      val liste_vm = ids.map(id=>id.attribute("id"))mkString
//    })
//
//  }
//  }


  def getCookie() = {
    val req =
      ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/login").withHeaders("Accept" -> "application/*+xml;version=5.1").withAuth("user1@icc-02", "eisti0002", WSAuthScheme.BASIC).get().map {
        response =>
          {
            response.cookies
          }
      }
    Await.ready(req, Duration.Inf).value.get match {
      case Success(x) => x.head.toString
      case Failure(x) => throw new Exception(x.getMessage)
    }
  }

  def newVM = WebSocket.acceptWithActor[String, String] { request => out =>
    DeployVMActor.props(out, ws, reqXml, getCookie)
  }
  
  def deleteVM_action(id_vm : String) = Action.async {
    val cookie = getCookie()
    
    //PowerOff
    val data = <UndeployVAppParams xmlns="http://www.vmware.com/vcloud/v1.5">
    <UndeployPowerAction>powerOff</UndeployPowerAction>
</UndeployVAppParams>
          
    ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + id_vm +"/action/undeploy").withHeaders(
    "Cookie" -> cookie,
    "Accept" -> "application/*+xml;version=1.5",
    "Content-Type" -> "application/vnd.vmware.vcloud.undeployVAppParams+xml"
  ).post(data).map( response => {
      vm_delete_actor ! Delete(id_vm)
      Redirect("/dashboard").withSession(("delete",id_vm))
    })
        
    
  }


  
  
  
  def getVapp = Action.async { 
    implicit request =>
    {
       val vapp : Future[Vapp] = for{
         repXML <- reqXml()
         repJson <- reqJson
       }yield (VappFactory(repXML.xml, Some(repJson.json)))
        vapp.map { vapp => Ok(Json.toJson(vapp)) }
    }
  }
  
  
  //Json descriptors :
  //id : String, name : String, image : String, ports : Seq[Int], active : Boolean
  implicit val ContainerToJson = new Writes[Container] {
    def writes(container: Container) = Json.obj(
      "id"  -> container.id,
      "name" -> container.name,
      //"ports" -> container.ports,
      "image" -> container.image,
      "active" -> container.active
    )
  }
  
  //id : String, name : String, ipLocal : String, ipExternal : String, active : Boolean = false, containers : Seq[Container] = Nil
  implicit val VmToJson = new Writes[Vm] {
    def writes(vm: Vm) = Json.obj(
      "id" -> vm.id,
      "name" -> vm.name,
      "ipLocal" -> vm.ipLocal,
      "ipExternal" -> vm.ipExternal,
      "active" -> vm.active,
      "containers" -> vm.containers
    )
  }
  
  //id : String, vms : Seq[Vm], indice : Int
  implicit val VappToJson = new Writes[Vapp] {
    def writes(vapp: Vapp) = Json.obj(
      "id"  -> vapp.id,
      "vms" -> vapp.vms,
      "indice" -> vapp.indice
    )
  }

  

}









