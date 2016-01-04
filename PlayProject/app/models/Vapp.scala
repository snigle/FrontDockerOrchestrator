package models

import scala.xml.Elem
import play.api.libs.json.JsValue

case class Vapp(id : String, vms : Seq[Vm], indice : Int) {
  
	
}

object VappFactory {

  def apply(xml : Elem, json : Option[JsValue] = None) : Vapp = {
    Vapp(xml.head.attribute("id").get.text.split(":").last,
	      (xml \ "Children" \ "Vm").map(node => VmFactory(node, json)).sortBy { vm => vm.name },
	      (xml \ "Children" \ "Vm").map(node => node.attribute("name").get.text.split("-").last).
	      filter(i => i forall(Character.isDigit)).map(i => i.toInt) match {
	        case Nil => 0 
	        case m => m.max
	      }
	      )
  }
}