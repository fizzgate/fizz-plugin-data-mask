package we.plugin.mask;

import org.springframework.context.annotation.Configuration;
import we.config.ManualApiConfig;
import we.plugin.PluginConfig;
import we.plugin.auth.ApiConfig;
import we.plugin.requestbody.RequestBodyPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 定义 MaskingApiConfig 继承 ManualApiConfig，并注解为 Configuration，然后实现 setApiConfigs 方法，在方法中添加路由配置；
 * 本类仅为方便开发和测试，正式环境应该通过管理后台配置路由
 */
@Configuration
public class MaskingApiConfig extends ManualApiConfig {

    @Override
    public List<ApiConfig> setApiConfigs() {

        List<ApiConfig> apiConfigs = new ArrayList<>();

        // 一个路由配置
        ApiConfig ac = new ApiConfig();
        // 路由 id，建议从 1000 开始
        ac.id = 10000;
        // 前端服务名
        ac.service = "xservice";
        // 前端路径
        ac.path = "/trip-mini";
        // 路由类型，此处为反向代理
        ac.type = ApiConfig.Type.REVERSE_PROXY;
        // 被代理接口的地址
        ac.httpHostPorts = Collections.singletonList("http://127.0.0.1:7094");
        // 被代理接口的路径
        ac.backendPath = "/trip-mini";
        ac.pluginConfigs = new ArrayList<>();

        // 如果你的插件需要访问请求体，则首先要把 RequestBodyPlugin.REQUEST_BODY_PLUGIN 加到 ac.pluginConfigs 中，就像下面这样
        PluginConfig pc1 = new PluginConfig();
        pc1.plugin = RequestBodyPlugin.REQUEST_BODY_PLUGIN;
        ac.pluginConfigs.add(pc1);

        PluginConfig pc2 = new PluginConfig();
        // 应用 id 为 maskingPlugin 的插件
        pc2.plugin = MaskingPluginFilter.MASKINGPLUGIN;
        pc2.setConfig("{\"maskingRule\":{\"ch1\":\"aliRule\",\"ch2\":\"aliRule\"}}");
        pc2.setConfig("{\"maskingTemplate\":{\"testUserName\":\"name\",\"testIdCard\":\"idcard\",\"testMobile\":\"phone\",\"testBlank\":\"blank\",\"testContent\":\"default\"}}");
        ac.pluginConfigs.add(pc2);

        apiConfigs.add(ac);

        log.info("set api configs end");
        // 返回路由配置
        return apiConfigs;
    }
}
