
package com.fengjx.modules.wechat.controller.admin;

import com.fengjx.commons.plugin.db.page.AdapterPage;
import com.fengjx.commons.utils.WebUtil;
import com.fengjx.modules.common.controller.MyController;
import com.fengjx.modules.wechat.model.ReqMsgLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fengjx.
 * @date：2015/5/19 0019
 */
@RequestMapping("${adminPath}/wechat/msglog")
@Controller
public class ReqMsglogController extends MyController {

    @Autowired
    private ReqMsgLog reqMsgLog;

    @RequestMapping(value = "")
    public ModelAndView view(String openid) {
        ModelAndView mv = new ModelAndView("wechat/admin/msg_log");
        mv.addObject("openid", openid);
        return mv;
    }

    @RequestMapping(value = "/pageList")
    @ResponseBody
    public AdapterPage pageList(HttpServletRequest request) {
        return reqMsgLog.pageList(WebUtil.getRequestParams(request), getLoginSysUserId());
    }

}
