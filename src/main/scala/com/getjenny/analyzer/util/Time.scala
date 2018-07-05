package com.getjenny.analyzer.util

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/11/17
  */

import java.time._

object Time {

  /** current timestamp in seconds
    *
    * @return the timestamp in seconds
    */
  def timestampEpoc: Long = {
    Instant.now.getEpochSecond
  }

  /** current timestamp in milliseconds
    *
    * @return the timestamp in milliseconds
    */
  def timestampMillis: Long = {
    Instant.now().toEpochMilli
  }

  /** get the current day of the week
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the day of week: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    */
  def dayOfTheWeek(zone: String = "CET"): DayOfWeek = {
    ZonedDateTime.ofInstant(Instant.now, ZoneId.of(zone)).getDayOfWeek
  }

  /** get the current day of the week as String
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the string with day of week: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    */
  def dayOfWeekString(zone: String = "CET"): String = {
    dayOfTheWeek(zone).toString
  }

  /** get the current day of the week as String
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the number of the day of week: 1 - MONDAY, 2 - TUESDAY, 3 - WEDNESDAY, 4 - THURSDAY,
    *         5 - FRIDAY, 6 - SATURDAY, 7 - SUNDAY
    */
  def dayOfWeekInt(zone: String = "CET"): Int = {
    dayOfTheWeek(zone).getValue
  }

  /** convert a string MONDAY .. SUNDAY to Int
    *
    * @param weekDay a string with the day of the week
    * @return an integer 1 - MONDAY .. 7 - SUNDAY
    */
  def dayOfWeekToInt(weekDay: String): Int = {
    weekDay match {
      case "MONDAY" => 1
      case "TUESDAY" => 2
      case "WEDNESDAY" => 3
      case "THURSDAY" => 4
      case "FRIDAY" => 5
      case "SATURDAY" => 6
      case "SUNDAY" => 7
      case _ => 0
    }
  }

  /** get the current day of the month
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the day of month: an integer between 1 and 31
    */
  def dayOfMonth(zone: String = "CET"): Int = {
    ZonedDateTime.ofInstant(Instant.now, ZoneId.of(zone)).getDayOfMonth
  }

  /** get the current month
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the month: JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST,
    *         SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER
    */
  def month(zone: String = "CET"): Month = {
      ZonedDateTime.ofInstant(Instant.now, ZoneId.of(zone)).getMonth
  }

  /** get the current month as String
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the month: JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST,
    *         SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER
    */
  def monthString(zone: String = "CET"): String = {
    month(zone).toString
  }

  /** get the current day of the week as String
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the month's number: 1 - JANUARY, 2 - FEBRUARY, 3 - MARCH, 4 - APRIL, 5 - MAY,
    *         6 - JUNE, 7 - JULY, 8 - AUGUST, 9 - SEPTEMBER, 10 - OCTOBER, 11 - NOVEMBER, 12 - DECEMBER
    */
  def monthInt(zone: String = "CET"): Int = {
    month(zone).getValue
  }

  /** get the current hour
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the current hour
    */
  def hour(zone: String = "CET"): Int = {
    ZonedDateTime.ofInstant(Instant.now, ZoneId.of(zone)).getHour
  }

  /** get the current minute
    *
    * @param zone UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>, GMT-<N>, UT+<N> or UT-<N> where N is a number
    *             between -18 and +18. Default is CET
    * @return the current minute
    */
  def minutes(zone: String = "CET"): Int = {
    ZonedDateTime.ofInstant(Instant.now, ZoneId.of(zone)).getMinute
  }
}
