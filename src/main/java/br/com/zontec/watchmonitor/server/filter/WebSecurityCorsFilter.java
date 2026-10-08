package br.com.zontec.watchmonitor.server.filter;


import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class WebSecurityCorsFilter implements Filter {

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    final HttpServletResponse res = (HttpServletResponse) response;
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
    res.setHeader("Access-Control-Max-Age", "1");
    res.setHeader("Access-Control-Allow-Headers",
        "Authorization, Content-Type, Accept, x-requested-with, Cache-Control, Sistema-Origem");
    res.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type, Location, Retry-After");
    chain.doFilter(request, res);
  }

  @Override
  public void destroy() {
    // noop
  }
}

