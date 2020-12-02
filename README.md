# feign-no-register

调用方式：http://localhost:8080/file/list

在将老的系统（主要是SSM开发的）改造为SpringCloud的过程中，因为直接改造工作量比较大，还有很多的jsp要转thymeleaf，因此我这里的改造方案是：

不改造原有的单个系统，只是系统直接的调用改成feign的方式，逐渐去掉double，改成SpringCloud。

然而，SSM的工程需要将调用方式改为feign，而且不依赖注册中心和配置中心，这样改造工作量相对会比较小。总结起来就是，接口之间的调用方式
改为了feign，后台都是restful提供的微服务接口。改造的过程是这样：

### 1 项目中添加依赖：

主要是添加了feign和fast-classpath-scanner的依赖

```xml
        <!--feign-->
        <dependency>
            <groupId>com.netflix.feign</groupId>
            <artifactId>feign-core</artifactId>
            <version>8.18.0</version>
        </dependency>
        <dependency>
            <groupId>com.netflix.feign</groupId>
            <artifactId>feign-jackson</artifactId>
            <version>8.18.0</version>
        </dependency>

        <dependency>
            <groupId>io.github.lukehutch</groupId>
            <artifactId>fast-classpath-scanner</artifactId>
            <version>3.1.9</version>
        </dependency>
```

### 2 自定义注解

通过自定义FeignApi注解，用来标识指定url的service

```java
package com.a360inhands.feign.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeignApi {
    /**
     * 调用的服务地址
     * @return
     */
    String serviceUrl();
}
```

### 3 重写feign客户端注册

重写feign客户端注册，主要是利用咱们自己定义的这个FeignApi注解，因为在FeignApi里面咱们定义了访问的微服务的url。
```java
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

```

### 4 定义接口
定义接口，这个和普通的openfeign是一致的。只不过，这里用了咱们自己定义的FeignApi注解标注。

```java
package com.a360inhands.feign.api;

import com.a360inhands.feign.annotation.FeignApi;
import feign.Headers;
import feign.RequestLine;

@FeignApi(serviceUrl = "http://api.360inhands.com:8080")
public interface IFileService {

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("GET /qiniu_token/file/list")
    public Object get();
}

```

### 5 调用
```java
package com.a360inhands.feign.controller;

import com.a360inhands.feign.api.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private IFileService fileService;

    @RequestMapping("/list")
    public Object list() {
        return fileService.get();
    }
}

```

完整的例子如下：https://github.com/jishaofeng89/feign-no-register