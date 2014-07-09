package uk.co.revsys.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.co.revsys.utils.http.HttpClient;
import uk.co.revsys.utils.http.HttpClientImpl;
import uk.co.revsys.utils.http.HttpMethod;
import uk.co.revsys.utils.http.HttpRequest;
import uk.co.revsys.utils.http.HttpResponse;

public class ProxyServlet extends HttpServlet {

    private ProxyMap proxies;
    private HttpClient httpClient;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        proxies = new ProxyMap();
        Map<String, ProxyMap> proxyMaps = webApplicationContext.getBeansOfType(ProxyMap.class);
        Map<String, ProxyMapFactoryBean> proxyMapFactories = webApplicationContext.getBeansOfType(ProxyMapFactoryBean.class);
        for(Entry<String, ProxyMapFactoryBean> entry: proxyMapFactories.entrySet()){
            try {
                proxyMaps.put(entry.getKey(), entry.getValue().getObject());
            } catch (Exception ex) {
                throw new ServletException("Could not load proxy maps", ex);
            }
        }
        for(ProxyMap proxyMap: proxyMaps.values()){
            proxies.putAll(proxyMap);
        }
        httpClient = new HttpClientImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpRequest request = getHttpRequest(req, HttpMethod.GET);
        HttpResponse response = httpClient.invoke(request);
        processHttpResponse(response, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpRequest request = getHttpRequest(req, HttpMethod.POST);
        HttpResponse response = httpClient.invoke(request);
        processHttpResponse(response, resp);
    }

    private HttpRequest getHttpRequest(HttpServletRequest req, HttpMethod method) throws ServletException{
        System.out.println("req.getRequestURI() = " + req.getRequestURI());
        System.out.println("req.getContextPath() + req.getServletPath() = " + req.getContextPath() + req.getServletPath());
        String requestUrl = req.getRequestURI().substring((req.getContextPath() + req.getServletPath()).length() + 1);
        System.out.println("requestUrl = " + requestUrl);
        String proxyKey = requestUrl;
        String requestPath = "";
        if(requestUrl.contains("/")){
            proxyKey = requestUrl.substring(0, requestUrl.indexOf("/"));
            requestPath = requestUrl.substring(requestUrl.indexOf("/") + 1);
        }
        System.out.println("proxyKey = " + proxyKey);
        String proxyUrl = proxies.get(proxyKey);
        System.out.println("proxyUrl = " + proxyUrl);
        if(proxyUrl == null){
            throw new ServletException("No proxy found for " + proxyKey);
        }
        proxyUrl = proxyUrl + requestPath;
        System.out.println("proxyUrl = " + proxyUrl);
        HttpRequest request = new HttpRequest(proxyUrl);
        request.setMethod(method);
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
        return request;
    }

    private void processHttpResponse(HttpResponse response, HttpServletResponse resp) throws IOException {
        resp.setStatus(response.getStatusCode());
        resp.setContentType(response.getContentType());
        resp.setContentLength(response.getContentLength());
        for (Entry<String, String> header : response.getHeaders().entrySet()) {
            if (!header.getKey().equals("Transfer-Encoding")) {
                resp.addHeader(header.getKey(), header.getValue());
            }
        }
        resp.setHeader("Access-Control-Allow-Origin", "*");
        InputStream inputStream = response.getInputStream();
        if (inputStream != null) {
            IOUtils.copy(inputStream, resp.getOutputStream());
        }
    }

}
