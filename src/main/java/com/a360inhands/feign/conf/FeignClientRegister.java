package com.a360inhands.feign.conf;

import com.a360inhands.feign.annotation.FeignApi;
import feign.Feign;
import feign.Request;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeignClientRegister implements BeanFactoryPostProcessor {

    //扫描的接口路径
    private String  scanPath="com.a360inhands.feign.api";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        List<String> classes = scan(scanPath);
        if(classes==null){
            return ;
        }
//        System.out.println(classes);
        Feign.Builder builder = getFeignBuilder();
        if(classes.size()>0){
            for (String claz : classes) {
                Class<?> targetClass = null;
                try {
                    targetClass = Class.forName(claz);
                    String url=targetClass.getAnnotation(FeignApi.class).serviceUrl();
                    if(url.indexOf("http://")!=0){
                        url="http://"+url;
                    }
                    Object target = builder.target(targetClass, url);
                    beanFactory.registerSingleton(targetClass.getName(), target);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    public Feign.Builder getFeignBuilder(){
        Feign.Builder builder = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))
                .retryer(new Retryer.Default(5000, 5000, 3));
        return builder;
    }

    public List<String> scan(String path){
        ScanResult result = new FastClasspathScanner(path).matchClassesWithAnnotation(FeignApi.class, (Class<?> aClass) -> {
        }).scan();
        if(result!=null){
            return result.getNamesOfAllInterfaceClasses();
        }
        return  null;
    }
}
