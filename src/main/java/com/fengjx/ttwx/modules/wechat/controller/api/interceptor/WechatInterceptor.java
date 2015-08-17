
package com.fengjx.ttwx.modules.wechat.controller.api.interceptor;

import com.fengjx.ttwx.common.plugin.db.Record;
import com.fengjx.ttwx.common.utils.AesUtil;
import com.fengjx.ttwx.common.utils.LogUtil;
import com.fengjx.ttwx.modules.common.constants.AppConfig;
import com.fengjx.ttwx.modules.wechat.model.PublicAccount;
import com.fengjx.ttwx.modules.wechat.process.bean.WechatContext;
import com.fengjx.ttwx.modules.wechat.process.utils.WxMpUtil;

import me.chanjar.weixin.mp.api.WxMpConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WechatInterceptor implements HandlerInterceptor {

    @Autowired
    private PublicAccount publicAccount;

    private static final Logger LOG = LoggerFactory.getLogger(WechatInterceptor.class);

    public void afterCompletion(HttpServletRequest request,
            HttpServletResponse response, Object handler, Exception e)
            throws Exception {
        WechatContext.removeAll();
    }

    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler, ModelAndView arg3)
            throws Exception {

    }

    /**
     * 如果返回false 从当前拦截器往回执行所有拦截器的afterCompletion方法，再退回拦截器链 如果返回true
     * 执行下一个拦截器，直到所有拦截器都执行完毕 再运行被拦截的Controller
     * 然后进入拦截器链从最后一个拦截器往回运行所有拦截器的postHandle方法
     * 接着依旧是从最后一个拦截器往回执行所有拦截器的afterCompletion方法
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        // 在request.getParameter之后读取InputStream会导致流没有读取完整，所以这里临时保存
        ByteArrayInputStream in = new ByteArrayInputStream(IOUtils.toByteArray(request
                .getInputStream()));
        try {
            String ticket = request.getParameter("ticket");
            if (StringUtils.isBlank(ticket)) {
                LogUtil.info(LOG, "请求无效，ticket为空");
                return false;
            }
            // 解密
            String id =  AesUtil.decrypt(ticket); 
            String signature = request.getParameter("signature");
            String nonce = request.getParameter("nonce");
            String timestamp = request.getParameter("timestamp");
            // 将公众号配置信息放到微信请求上下文
            Record record = publicAccount.findById(id);
            if (record.isEmpty()) {
                LogUtil.info(LOG, "ticket无效，找不到对应公众号信息");
                return false;
            }
            WechatContext.setInMessageRecord(record);
            WxMpConfigStorage wxMpConfig = WxMpUtil.buildConfigStorage(record);
            // 非测试环境做签名校验
            if (!AppConfig.isTest()) {
                LogUtil.debug(LOG, "进入签名校验");
                WxMpService wxMpService = WxMpUtil.getWxMpServiceByConfig(wxMpConfig);
                if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
                    // 消息签名不正确，说明不是公众平台发过来的消息
                    LogUtil.error(LOG, "消息签名不正确，非法请求");
                    return false;
                }
            }
            String echostr = request.getParameter("echostr");
            if (StringUtils.isNotBlank(echostr)) {
                // 说明是一个仅仅用来验证的请求
                return true;
            }
            String encryptType = StringUtils.isBlank(request.getParameter("encrypt_type")) ?
                    "raw" :
                    request.getParameter("encrypt_type");
            WxMpXmlMessage inMessage = null;
            if ("raw".equals(encryptType)) {
                // 明文传输的消息
                inMessage = WxMpXmlMessage.fromXml(in);
            } else if ("aes".equals(encryptType)) {
                // 是aes加密的消息
                String msgSignature = request.getParameter("msg_signature");
                inMessage = WxMpXmlMessage.fromEncryptedXml(in, wxMpConfig, timestamp, nonce,
                        msgSignature);
            }
            WechatContext.setInMessage(inMessage);
            WechatContext.setWxMpConfigStorage(wxMpConfig);
            WechatContext.setEncryptType(encryptType);
            return true;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
