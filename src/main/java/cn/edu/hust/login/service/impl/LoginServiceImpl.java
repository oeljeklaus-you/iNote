package cn.edu.hust.login.service.impl;


import javax.annotation.Resource;

import cn.edu.hust.login.dao.LoginDao;
import cn.edu.hust.login.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

	@Autowired(required = false)
	private LoginDao loginDao;
	
	@Override
	public boolean login(String userName, String password) throws Exception{
		return loginDao.getLoginInfo(userName,password);
		
	}
}
