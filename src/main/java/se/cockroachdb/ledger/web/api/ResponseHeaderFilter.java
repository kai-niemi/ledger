package se.cockroachdb.ledger.web.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@WebFilter("/api")
public class ResponseHeaderFilter extends OncePerRequestFilter {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String HEADER_NAME = "X-Application-Context";

    public static final String HEADER_VERSION = "X-Application-Version";

//    @Autowired
//    private BuildProperties buildProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
//        response.setHeader(HEADER_NAME, buildProperties.getName());
//        response.setHeader(HEADER_VERSION, buildProperties.getVersion());
        filterChain.doFilter(request, response);
    }
}
