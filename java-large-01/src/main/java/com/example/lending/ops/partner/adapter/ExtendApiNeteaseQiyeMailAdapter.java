package com.example.lending.ops.partner.adapter;

import com.example.lending.ops.common.StringUtils;
import com.example.lending.ops.partner.Apps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

/** SSO into the partner enterprise mail suite used by branch staff. */
public class ExtendApiNeteaseQiyeMailAdapter extends AbstractAuthorizeAdapter {
    static final Logger _logger = LoggerFactory.getLogger(ExtendApiNeteaseQiyeMailAdapter.class);

    static final String REDIRECT_PARAMETER = "domain=%s&account_name=%s&time=%s&enc=%s&lang=%s";

    @Override
    public ModelAndView authorize(ModelAndView modelAndView) {

        Apps details=(Apps)app;
        StringBuffer redirect_uri = new StringBuffer(details.getLoginUrl());
        if(StringUtils.isNotBlank(redirect_uri)) {
            if(redirect_uri.indexOf("?")>-1) {
                redirect_uri.append("").append( REDIRECT_PARAMETER);
            }else {
                redirect_uri.append("?").append( REDIRECT_PARAMETER);
            }
        }
        //extraAttrs from App
        ExtraAttrs extraAttrs=null;
        if(details.getIsExtendAttr() == 1){
            extraAttrs = new ExtraAttrs(details.getExtendAttr());
            for(ExtraAttr attr : extraAttrs.getExtraAttrs()) {
                redirect_uri.append("&").append(attr.getAttr()).append("=").append(attr.getValue());
            }
        }

        String time = System.currentTimeMillis() + "";
        //域名，请使用企业自己的域名
        String domain = details.getPrincipal();

        String account_name = this.userInfo.getEmail().substring(0, this.userInfo.getEmail().indexOf("@"));

        String lang = "0";
        String src = account_name + domain + time;

        String privateKey = details.getCredentials();
        _logger.debug("Private Key {} " , privateKey);

        String enc = new NeteaseRSATool().generateSHA1withRSASigature(src, privateKey);
        String loginUrl = String.format(redirect_uri.toString(), domain,account_name,time,enc,lang);

        _logger.debug("LoginUrl {} " , loginUrl);
        modelAndView.addObject("redirect_uri", loginUrl);

        return modelAndView;
    }
}
