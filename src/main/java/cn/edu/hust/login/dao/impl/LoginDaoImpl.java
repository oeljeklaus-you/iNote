package cn.edu.hust.login.dao.impl;

import cn.edu.hust.login.dao.LoginDao;
import cn.edu.hust.utils.constants.Constants;
import cn.edu.hust.note.service.jedis.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;



@Repository
public class LoginDaoImpl implements LoginDao {

	@Autowired
	private RedisUtils redisUtils;
	@Override
	public boolean getLoginInfo(String userName, String password) throws Exception {
		boolean flag = false;
		String userInfo =(String)redisUtils.get(userName);
		if (userInfo!=null) {
			String[] split = userInfo.split("\\"+ Constants.STRING_SEPARATOR);
			if (password.equals(split[0])) {
				flag=true;
			}
		}
		return flag;
	}

}
