package controllers

import play.api.Play.current
import play.api.libs.ws._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import javax.inject.Inject

class Application @Inject() (ws: WSClient) extends Controller {
  var cookie: Seq[WSCookie] = Seq[WSCookie]()

  def index = Action.async {
    implicit request =>
      {
        val cookie = getCookie
        Future(Ok(cookie))
      }
  }

  def dashboard = Action.async { implicit request =>
    {
      //"Accept:application/*+xml;version=1.5" -X GET https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-cb109e5b-e457-450e-aff9-322cdd6181f6/productSections
      ws.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/").withHeaders(
        "Cookie" -> getCookie,
        "Accept" -> "application/*+xml;version=1.5").get().map(response => {
          val ids = response.xml \ "Children" \ "Vm" 
          println(ids.isEmpty)
          println(ids.map(id=>id.attribute("id"))mkString("\n"))
          Ok(response.xml)
        })
        
    }
  }


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
  /*def copieVM(cookie : String, source_vm : String, name_new_vm : String) = {
    val vm_copy_xml =  <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                           <RecomposeVAppParams
                            xmlns="http://www.vmware.com/vcloud/v1.5"
                            xmlns:ns2="http://schemas.dmtf.org/ovf/envelope/1"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1"
                            xmlns:environment_1="http://schemas.dmtf.org/ovf/environment/1">
                              <Description> "api deployed vm to ol-vapp-04" </Description>
                              <SourcedItem sourceDelete="false">
                                <Source name="tata" href="https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vm-4af381bb-f4d1-4784-8fc2-9f0db72411c9"/>
                              </SourcedItem>
                              <AllEULAsAccepted>true</AllEULAsAccepted>
                          </RecomposeVAppParams>


    WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/vApp/vapp-9dd013e3-3f51-4cde-a19c-f96b4ad2e350/action/recomposeVApp").withHeaders("Cookie" -> cookie).post(vm_copy_xml)
  } */

}









