package util

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
//TODO play.http.filters = "filters.MyFilters" in config
class SecurityFilter @Inject()(csrfFilter: CSRFFilter, securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter)
}
