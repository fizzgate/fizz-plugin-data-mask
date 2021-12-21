/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package we.plugin.mask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.plugin.FizzPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.mask.rules.MaskingRule;
import we.util.JacksonUtils;
import we.util.NettyDataBufferUtils;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * response body masking plugin
 * @author pstoneone
 * @date 2021/9/16 18:12
 */
@Component(MaskingPluginFilter.MASKINGPLUGIN)
public class MaskingPluginFilter implements FizzPluginFilter {

    private final Logger log = LoggerFactory.getLogger(MaskingPluginFilter.class);

    static final String MASKINGPLUGIN = "maskingPlugin";
    private static final String SOURCETYPE = "sourceType";
    private static final String MASKINGRULE = "maskingRule";
    private static final String MASKINGTEMPLATE = "maskingTemplate";

    /**
     * 掩码规则bean实例集合
     */
    @Autowired(required = false)
    private Map<String, MaskingRule> mrMap;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        ServerHttpRequest request = exchange.getRequest();
        String sourceType = request.getHeaders().getFirst(SOURCETYPE);
        if(StringUtils.isEmpty(sourceType)) {
            log.warn("request header sourceType is empty, masking ignore!");
            return FizzPluginFilterChain.next(exchange);
        }
        // actual is Map<String, String>
        Object mRule = config.get(MASKINGRULE);
        Object mTmpl = config.get(MASKINGTEMPLATE);
        if(mRule == null || mTmpl == null) {
            log.warn("maskingRule or maskingTemplate is null, masking ignore! sourceType: {}, mRule: {}, mTmpl: {}", sourceType, mRule, mTmpl);
            return FizzPluginFilterChain.next(exchange);
        }
        MaskingRule mr = getMaskingRule(sourceType, mRule);
        if(mr == null) {
            log.warn("can not get a maskingRule, masking ignore! sourceType: {}", sourceType);
            return FizzPluginFilterChain.next(exchange);
        }

        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        // decorate response to process masking
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Mono<DataBuffer> modifiedBody = NettyDataBufferUtils.join(body)
                            .map(dataBuffer -> {
                                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer());
                                String origBody = charBuffer.toString();
                                String response = maskingResponse(origBody, mTmpl, mr);
                                // process failed, return originalResponse
                                if(response == null) {
                                    return dataBuffer.asByteBuffer().array();
                                }
                                NettyDataBufferUtils.release(dataBuffer);
                                byte[] responseBytes = response.getBytes();
                                // modify originalResponse's Content-Length
                                originalResponse.getHeaders().setContentLength(responseBytes.length);
                                return responseBytes;
                            })
                            .map(bufferFactory::wrap);

                    return super.writeWith(modifiedBody);
                }
                return super.writeWith(body);
            }
        };

        return FizzPluginFilterChain.next(exchange.mutate().response(decoratedResponse).build());
    }

    private String maskingResponse(String origResp, Object mTmpl, MaskingRule mr) {
        if(StringUtils.isEmpty(origResp)) {
            log.warn("origResp is empty, there is no need masking! origResp:{}", origResp);
            return null;
        }
        try {
            Map<String, String> mTmplMap = (Map<String, String>) mTmpl;
            JsonNode respNode = JacksonUtils.getObjectMapper().readTree(origResp);
            jsonLeafMask(respNode, mTmplMap, mr);
            return respNode.toString();
        }catch (Exception e) {
            log.error("masking exception!", e);
        }
        return null;
    }

    /**
     * response body fields masking process
     * @param respNode response jsonNode
     * @param mTmplMap fields template to be masked
     * @param rule masking rule
     */
    private static void jsonLeafMask(JsonNode respNode, Map<String, String> mTmplMap, MaskingRule rule) {
        if (respNode.isValueNode()) {
            return ;
        }

        if (respNode.isObject()) {
            ObjectNode objNode = (ObjectNode) respNode;
            Iterator<Map.Entry<String, JsonNode>> it = respNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String filedRuleStr = mTmplMap.get(entry.getKey());
                if (StringUtils.isEmpty(filedRuleStr)){
                    filedRuleStr = "X";
                }
                switch (filedRuleStr) {
                    case MaskingRule.DEFAULT_STR:
                        objNode.put(entry.getKey(), rule.defaultOut(entry.getValue().textValue()));
                        break;
                    case MaskingRule.IDCARD_STR:
                        objNode.put(entry.getKey(), rule.idCard(entry.getValue().textValue()));
                        break;
                    case MaskingRule.NAME_STR:
                        objNode.put(entry.getKey(), rule.name(entry.getValue().textValue()));
                        break;
                    case MaskingRule.PHONE_STR:
                        objNode.put(entry.getKey(), rule.phone(entry.getValue().textValue()));
                        break;
                    case MaskingRule.BLANK_STR:
                        objNode.put(entry.getKey(), rule.blank(entry.getValue().textValue()));
                        break;
                    default:
                        break;
                }
                jsonLeafMask(entry.getValue(), mTmplMap, rule);
            }
        }

        if (respNode.isArray()) {
            Iterator<JsonNode> it = respNode.iterator();
            while (it.hasNext()) {
                jsonLeafMask(it.next(), mTmplMap, rule);
            }
        }
    }

    /**
     * according sourceType to match masking rule,sourceType is prefer,if not try to matching use *
     * @param sourceType sourceType
     * @param mRule maskingRule map string
     * @return actual MaskingRule, may be null
     */
    private MaskingRule getMaskingRule(String sourceType, Object mRule) {
        if(CollectionUtils.isEmpty(mrMap)) {
            log.warn("get MaskingRule failure, MaskingRule instance is not exists!");
            return null;
        }
        try {
            Map<String, String> ruleMap = (Map<String, String>) mRule;
            Optional<String> ruleId = ruleMap.entrySet()
                    .stream()
                    .filter(me -> contains(me.getKey(), sourceType))
                    .map(Map.Entry::getValue)
                    .findFirst();
            if(!ruleId.isPresent()) {
                ruleId = ruleMap.entrySet()
                        .stream()
                        .filter(me -> contains(me.getKey(), "*"))
                        .map(Map.Entry::getValue)
                        .findFirst();
            }
            if(ruleId.isPresent()) {
                return mrMap.get(ruleId.get());
            }
        }catch (Exception e) {
            log.error("get MaskingRule according to sourceType failed! sourceType:{},maskingRule:{}", sourceType, mRule, e);
        }
        return null;
    }

    /**
     * check val is contains target or not, replace chinese comma into en comma
     * @param val
     * @param target
     * @return
     */
    private Boolean contains(String val, String target) {
        if(StringUtils.isEmpty(val) || StringUtils.isEmpty(target)) {
            return Boolean.FALSE;
        }
        return Arrays.asList(val.replace('，', ',').split(",")).contains(target);
    }
}

