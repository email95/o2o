package com.chen.o2o.web.wechat;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.chen.o2o.dto.ShopExecution;
import com.chen.o2o.dto.WechatAuthExecution;
import com.chen.o2o.entity.PersonInfo;
import com.chen.o2o.entity.WechatAuth;
import com.chen.o2o.enums.WechatAuthStateEnum;
import com.chen.o2o.service.PersonInfoService;
import com.chen.o2o.service.ShopAuthMapService;
import com.chen.o2o.service.ShopService;
import com.chen.o2o.service.WechatAuthService;
import com.chen.o2o.util.weixin.WeiXinUser;
import com.chen.o2o.util.weixin.WeiXinUserUtil;
import com.chen.o2o.util.weixin.message.pojo.UserAccessToken;

@Controller
@RequestMapping("wechatlogin")
/**
 * 从微信菜单点击后调用的接口，可以在url里增加参数（role_type）来表明是从商家还是从玩家按钮进来的，依次区分登陆后跳转不同的页面
 * 玩家会跳转到index.html页面
 * 商家如果没有注册，会跳转到注册页面，否则跳转到任务管理页面
 * 如果是商家的授权用户登陆，会跳到授权店铺的任务管理页面
 * @author lixiang
 *
 */

/**
 * 获取关注公众号之后的微信用户信息的接口，如果在微信浏览器里访问
 * https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx760254d8d5c97fe1&redirect_uri=http://120.79.204.164/o2o/wechatlogin/logincheck&role_type=1&response_type=code&scope=snsapi_userinfo&state=1#wechat_redirect
 * 则这里将会获取到code，之后再可以通过code获取access_token进而获取到用户信息
 */
public class WeiXinLoginController {

	private static Logger log = LoggerFactory
			.getLogger(WeiXinLoginController.class);

	@Resource
	private PersonInfoService personInfoService;
	@Resource
	private WechatAuthService WechatAuthService;

	@Resource
	private ShopService shopService;

//	@Resource
//	private ShopAuthMapService shopAuthMapService;

	private static final String FRONTEND = "1";
	private static final String SHOPEND = "2";

	@RequestMapping(value = "/logincheck", method = { RequestMethod.GET })
	public String doGet(HttpServletRequest request, HttpServletResponse response) {
		log.debug("weixin login get...");
		//获取微信公众号传输过来的code,通过code可获取access_token，进而获取用户信息
		String code = request.getParameter("code");
		//这个state可以用来传我们自定义的信息，方便程序调用，这里也可以不用
		//String roleType = request.getParameter("state");
		String roleType = request.getParameter("state");
		log.debug("weixin login code:" + code);
		WechatAuth auth = null;
		WeiXinUser user = null;
		String openId = null;
		if (null != code) {
			UserAccessToken token;
			try {
				//通过code获取access_token
				token = WeiXinUserUtil.getUserAccessToken(code);
				log.debug("weixin login token:" + token.toString());
				//通过token获取accessToken
				String accessToken = token.getAccessToken();
				//通过token获取openId
				openId = token.getOpenId();
				//通过access_token和openId获取用户昵称等信息
				user = WeiXinUserUtil.getUserInfo(accessToken, openId);
				log.debug("weixin login user:" + user.toString());
				request.getSession().setAttribute("openId", openId);
				auth = WechatAuthService.getWechatAuthByOpenId(openId);
			} catch (IOException e) {
				log.error("error in getUserAccessToken or getUserInfo or findByOpenId: "
						+ e.toString());
				e.printStackTrace();
			}
		}

		log.debug("weixin login success.");
		log.debug("login role_type:" + roleType);
		if (FRONTEND.equals(roleType)) {
			PersonInfo personInfo = WeiXinUserUtil
					.getPersonInfoFromRequest(user);
			if (auth == null) {
				personInfo.setCustomerFlag(1);
				auth = new WechatAuth();
				auth.setOpenId(openId);
				auth.setPersonInfo(personInfo);
				WechatAuthExecution we = WechatAuthService.register(auth, null);
				if (we.getState() != WechatAuthStateEnum.SUCCESS.getState()) {
					return null;
				}
			}
			personInfo = personInfoService.getPersonInfoById(auth.getUserId());
			request.getSession().setAttribute("user", personInfo);
			return "frontend/index";
		}
		if (SHOPEND.equals(roleType)) {
			PersonInfo personInfo = null;
			WechatAuthExecution we = null;
			if (auth == null) {
				auth = new WechatAuth();
				auth.setOpenId(openId);
				personInfo = WeiXinUserUtil.getPersonInfoFromRequest(user);
				personInfo.setShopOwnerFlag(1);
				auth.setPersonInfo(personInfo);
				we = WechatAuthService.register(auth, null);
				if (we.getState() != WechatAuthStateEnum.SUCCESS.getState()) {
					return null;
				}
			}
			personInfo = personInfoService.getPersonInfoById(auth.getUserId());
			request.getSession().setAttribute("user", personInfo);
			ShopExecution se = shopService.getByEmployeeId(personInfo
					.getUserId());
			request.getSession().setAttribute("user", personInfo);

			if (se.getShopList() == null || se.getShopList().size() <= 0) {
				return "shop/registershop";
			} else {
				request.getSession().setAttribute("shopList", se.getShopList());
				return "shop/shoplist";
			}
		}
		return null;
	}
}
