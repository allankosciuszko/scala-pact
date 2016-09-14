package com.itv.scalapactcore.common

import scala.language.implicitConversions
import scala.xml.{Elem, Node}

import scalaz._
import Scalaz._

object PermissiveXmlEquality {

  implicit def toXmlEqualityWrapper(json: Elem): XmlEqualityWrapper = XmlEqualityWrapper(json)

  case class XmlEqualityWrapper(xml: Elem) {
    def =~(to: Elem): Boolean = PermissiveXmlEqualityHelper.areEqual(xml, to)
    def =<>=(to: Elem): Boolean => Boolean = beSelectivelyPermissive => StrictXmlEqualityHelper.areEqual(beSelectivelyPermissive, xml, to)
  }
}

object StrictXmlEqualityHelper {

  def areEqual(beSelectivelyPermissive: Boolean, expected: Elem, received: Elem): Boolean =
    (expected.headOption |@| received.headOption) { (e, r) => compareNodes(beSelectivelyPermissive)(e)(r) } match {
      case Some(bool) => bool
      case None => false
    }

  lazy val mapContainsMap: Map[String, String] => Map[String, String] => Boolean = e => r =>
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1 && rr._2 == ee._2)
    }

  lazy val compareNodes: Boolean => Node => Node => Boolean = beSelectivelyPermissive => expected => received => {
    lazy val prefixEqual = expected.prefix == received.prefix
    lazy val labelEqual = expected.label == received.label
    lazy val attributesLengthOk = expected.attributes.length == received.attributes.length
    lazy val attributesEqual = mapContainsMap(expected.attributes.asAttrMap)(received.attributes.asAttrMap)
    lazy val childLengthOk = expected.child.length == received.child.length

    lazy val childrenEqual =
      if(expected.child.isEmpty) expected.text == received.text
      else expected.child.forall { eN => received.child.exists(rN => compareNodes(beSelectivelyPermissive)(eN)(rN)) }

    prefixEqual && labelEqual && attributesLengthOk && attributesEqual && childLengthOk && childrenEqual
  }
}

object PermissiveXmlEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(expected: Elem, received: Elem): Boolean =
    (expected.headOption |@| received.headOption) { (e, r) => compareNodes(e)(r) } match {
      case Some(bool) => bool
      case None => false
    }

  lazy val mapContainsMap: Map[String, String] => Map[String, String] => Boolean = e => r =>
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1 && rr._2 == ee._2)
    }

  lazy val compareNodes: Node => Node => Boolean = expected => received => {
    lazy val prefixEqual = expected.prefix == received.prefix
    lazy val labelEqual = expected.label == received.label
    lazy val attributesEqual = mapContainsMap(expected.attributes.asAttrMap)(received.attributes.asAttrMap)
    lazy val childLengthOk = expected.child.length <= received.child.length

    lazy val childrenEqual =
      if(expected.child.isEmpty) expected.text == received.text
      else expected.child.forall { eN => received.child.exists(rN => compareNodes(eN)(rN)) }

    prefixEqual && labelEqual && attributesEqual && childLengthOk && childrenEqual
  }


}
