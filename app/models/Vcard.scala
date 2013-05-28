package models

import ezvcard.{ VCard => Card, Ezvcard }
import scala.collection.JavaConversions._

object Vcard {

    class Vcard(vcard: Card) {
        lazy val formattedName = vcard.getFormattedNames().map(_.getValue())
        lazy val nickname = vcard.getNicknames().flatMap(_.getValues())
    }

    def apply(text: String) = Option(Ezvcard.parse(text).first()).map(vcard => new Vcard(vcard))
}