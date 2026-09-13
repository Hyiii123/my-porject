import request from "@/utils/request.js"
const USER_API_PREFIX = "/us"
const AUTH_API_PREFIX = "/as"
const PHONE_LOGIN_TYPE = 2;
const PW_LOGIN_TYPE = 1;
// 邮箱验证码登录
export const emailLogin = (data) => {
	return request({
		url: `${AUTH_API_PREFIX}/accounts/login`,
		method: "post",
		data: {
			username: data.email,
			email: data.email,
			password: data.code,
			code: data.code,
			type: "email"
		},
		withCredentials: true
	});
}
// 手机号/邮箱验证码登录兼容
export const phoneLogins = (params) => {
	if (params.email || (params.cellPhone && params.cellPhone.includes('@'))) {
		return emailLogin({
			email: params.email || params.cellPhone,
			code: params.code || params.password
		});
	}
	params.type = PHONE_LOGIN_TYPE;
	return request({
		url: `${AUTH_API_PREFIX}/accounts/login`,
		method: "post",
		params,
		withCredentials: true
	});
}
// 账号登录
export const userLogins = (data) => {
	data.type = PW_LOGIN_TYPE;
	return request({
		url: `${AUTH_API_PREFIX}/accounts/login`,
		method: "post",
		data,
		withCredentials: true
	});
}
// 发送验证码
export const verifycode = (params) =>
request({
	url: `${AUTH_API_PREFIX}/code/verifycode`,
	method: 'post',
	params
})
// 账号注册
export const userRegist = (params) =>
request({
	url: `${AUTH_API_PREFIX}/users/register`,
	method: 'post',
	data:params
})
// 获取用户信息
export const getUserInfo = (params) =>
	request({
		url: `${USER_API_PREFIX}/users/me`,
		method: 'get',
		params
	})	
// 更改用户信息
export const updateUserInfo = (params) =>
	request({
		url: `${USER_API_PREFIX}/students`,
		method: 'put',
		data:params
	})
// 更改学生账号密码
export const updateStudentPassword = (data) =>
	request({
		url: `${USER_API_PREFIX}/students/password`,
		method: 'put',
		data
	})
// 账号退出登录
export const userLogout = () => {
	return request({
		url: `${AUTH_API_PREFIX}/accounts/logout`,
		method: "post",
		withCredentials: true
	});
}