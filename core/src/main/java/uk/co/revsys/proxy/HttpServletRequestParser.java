package uk.co.revsys.proxy;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import uk.co.revsys.utils.http.HttpMethod;
import uk.co.revsys.utils.http.HttpRequest;

public class HttpServletRequestParser {
    
    public HttpRequest parse(HttpServletRequest req) throws IOException{
        String requestUrl = req.getRequestURI().substring((req.getContextPath() + req.getServletPath()).length() + 1);
        HttpRequest request = new HttpRequest(requestUrl);
        request.setMethod(HttpMethod.valueOf(req.getMethod()));
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = req.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                request.getHeaders().put(headerName, headerValues.nextElement());
            }
        }
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            String parameterValue = req.getParameter(parameterName);
            request.getParameters().put(parameterName, parameterValue);
        }
        request.setBody(req.getInputStream());
        return request;
    }
    
}
