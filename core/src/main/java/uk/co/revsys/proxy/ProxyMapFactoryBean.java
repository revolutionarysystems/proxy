package uk.co.revsys.proxy;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class ProxyMapFactoryBean extends AbstractFactoryBean<ProxyMap> implements ResourceLoaderAware{

    private ResourceLoader resourceLoader;
    private String location;
    
    @Override
    public void setResourceLoader(ResourceLoader rl) {
        this.resourceLoader = rl;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public Class<?> getObjectType() {
        return LinkedHashMap.class;
    }

    @Override
    protected ProxyMap createInstance() throws Exception {
        Properties properties = new Properties();
        InputStream inputStream = resourceLoader.getResource(location).getInputStream();
        properties.load(inputStream);
        ProxyMap proxyMap = new ProxyMap();
        for(String key: properties.stringPropertyNames()){
            proxyMap.put(key, properties.getProperty(key));
        }
        return proxyMap;
    }

}
