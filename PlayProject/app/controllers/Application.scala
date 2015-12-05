package controllers

import javax.inject.Inject

import actors.{DeployActor, VMDeployed}
import akka.actor.{ActorSystem, Props}
import models.Vapp
import play.api.Play.current
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}






class Application @Inject() (ws: WSClient) extends Controller {
//  var cookie: Seq[WSCookie] = Seq[WSCookie]()
   val system = ActorSystem("VM")
   val vm_deploy_actor =  system.actorOf(Props(new DeployActor(reqXml,getCookie)))




  def index = Action.async {
    implicit request =>
      {
        val cookie = getCookie
        Future(Ok(cookie))
      }
  }

  def reqXml() = ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/").withHeaders(
    "Cookie" -> getCookie,
    "Accept" -> "application/*+xml;version=1.5").get()

  def dashboard = Action.async { implicit request =>
    {

       /*implicit val timeout = Timeout(Duration(60,SECONDS))
       val result = Await.result(vm_deploy_actor ? VMDeployed("swarm-agent-" + (4)),timeout.duration).asInstanceOf[String]
       println(result)*/



      reqXml().map(response => {
        val vapp = new Vapp(response.xml)
//        println(vapp)
        Ok(views.html.index(vapp))
      })

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


  def getCookie = {
    val req =
      ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/login").withHeaders("Accept" -> "application/*+xml;version=5.1").withAuth("user1@icc-02", "eisti0002", WSAuthScheme.BASIC).get().map {
        response =>
          {
            response.cookies
          }
      }
    Await.ready(req, Duration.Inf).value.get match {
      case Success(x) => x.head.toString
      case Failure(x) => throw new Exception
    }
  }

  def copieVM_action = Action.async {
    reqXml().map(response => {
      val vapp = new Vapp(response.xml)
      process_copie(getCookie,"https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-bb168665-8203-4edc-9ff8-dab64e754620","swarm-agent-"+ (vapp.indice+1))
      vm_deploy_actor ! VMDeployed("swarm-agent-" + (vapp.indice+1))
      Redirect("/dashboard")
    })
  }

  def deleteVM_action(id_vm : String) = Action {
    process_deleteVM(getCookie,id_vm)
//    Thread.sleep(3000)
    Redirect("/dashboard")
  }

  def process_deleteVM(cookie: String,id_vm : String): Unit = {

    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + id_vm + "/power/action/suspend").withHeaders(
      "Cookie" -> cookie,
      "Accept" -> "application/*+xml;version=1.5"
    ).post("")


    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-" + id_vm).withHeaders(
      "Cookie" -> cookie,
      "Accept" -> "application/*+xml;version=1.5"
    ).delete
  }

  def process_copie(cookie : String, source_vm : String, name_new_vm : String) {
    val vm_copy_xml =     <RecomposeVAppParams
                            xmlns="http://www.vmware.com/vcloud/v1.5"
                            xmlns:ns2="http://schemas.dmtf.org/ovf/envelope/1"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1"
                            xmlns:environment_1="http://schemas.dmtf.org/ovf/environment/1">
                              <Description> "api deployed vm to ol-vapp-04" </Description>
                              <SourcedItem sourceDelete="false">
                                <Source name={name_new_vm} href={source_vm}/>
                              </SourcedItem>
                              <AllEULAsAccepted>true</AllEULAsAccepted>
                          </RecomposeVAppParams>

      WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/action/recomposeVApp").withHeaders(
        "Cookie" -> cookie,
        "Accept" -> "application/*+xml;version=1.5",
        "Content-Type" -> "application/vnd.vmware.vcloud.recomposeVAppParams+xml"
      ).post(vm_copy_xml)



  }

}









