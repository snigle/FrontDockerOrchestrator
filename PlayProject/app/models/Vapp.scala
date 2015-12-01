package models

import scala.xml.Elem

case class Vapp(id : String, vms : Seq[Vm]) {
  
  def this(xml : Elem) {
	  this(xml.head.attribute("id").get.text.split(":").last,
	      (xml \ "Children" \ "Vm").map(node => new Vm(node)))
  }
}