package controllers

import javax.inject.Inject

import models.Vapp
import play.api.Play.current
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Application @Inject() (ws: WSClient) extends Controller {
//  var cookie: Seq[WSCookie] = Seq[WSCookie]()

  def index = Action.async {
    implicit request =>
      {
        val cookie = getCookie
        copieVM(cookie,"https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-bb168665-8203-4edc-9ff8-dab64e754620","tata")
        Future(Ok(cookie))
      }
  }

  def reqXml = ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/").withHeaders(
    "Cookie" -> getCookie,
    "Accept" -> "application/*+xml;version=1.5").get()

  def dashboard = Action.async { implicit request =>
    {
      reqXml.map(response => {
        val vapp = new Vapp(response.xml)
        Ok(views.html.index(vapp))
      })

    }
  }

  def vappXml = Action.async { implicit request =>
    {
      reqXml.map(response => Ok(response.xml))
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


  def copieVM(cookie : String, source_vm : String, name_new_vm : String) = {
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









