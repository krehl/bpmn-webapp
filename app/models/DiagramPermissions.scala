package models

/**
  * Singleton objects that describe different permission levels of users regarding diagram access
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/3/2016
  */

sealed trait Permission

object CanView extends Permission

object CanEdit extends Permission

object Owns extends Permission