import Vue from 'vue'
import App from './App'

Vue.config.productionTip = false

App.mpType = 'app'

const app = new Vue({
	...App
})
// 后端设置好的，可以查看controlpackage中的control类(Web层)
let baseUrl = "http://127.0.0.1:8080/emos-wx-api"
Vue.prototype.url = {
	register: baseUrl + "/user/register",
	// 设置login的url 
	login: baseUrl + "/user/login",

	checkin: baseUrl + "/checkin/checkin",
	createFaceModel: baseUrl + "/checkin/createFaceModel",
	validCanCheckIn: baseUrl + "/checkin/validCanCheckIn",
	
	searchTodayCheckin: baseUrl + "/checkin/searchTodayCheckin",
	
	searchUserSummary: baseUrl + "/user/searchUserSummary",
	
	searchMonthCheckin:baseUrl+"/checkin/searchMonthCheckin",
	
	refreshMessage: baseUrl + "/message/refreshMessage",
	
	searchMessageByPage: baseUrl + "/message/searchMessageByPage",
	
	searchMessageById: baseUrl + "/message/searchMessageById",
	
	updateUnreadMessage: baseUrl + "/message/updateUnreadMessage",
		
	deleteMessageRefById: baseUrl + "/message/deleteMessageRefById",
	
	searchMyMeetingListByPage: baseUrl + "/meeting/searchMyMeetingListByPage",
	
	searchUserGroupByDept: baseUrl + "/user/searchUserGroupByDept",
	
	searchMembers: baseUrl+"/user/searchMembers",
	
	insertMeeting: baseUrl+"/meeting/insertMeeting",
	
	searchMeetingById:baseUrl+"/meeting/searchMeetingById",
	
	updateMeetingInfo: baseUrl + "/meeting/updateMeetingInfo",
	
	deleteMeetingById:baseUrl+"/meeting/deleteMeetingById",
	
	// searchUserTaskListByPage:workflow+"/workflow/searchUserTaskListByPage",
	// approvalMeeting:workflow+"/workflow/approvalMeeting",
	selectUserPhotoAndName:baseUrl+"/user/selectUserPhotoAndName",
	
	genUserSig: baseUrl + "/user/genUserSig",
	
	searchRoomIdByUUID: baseUrl + "/meeting/searchRoomIdByUUID",
	
	searchUserMeetingInMonth:baseUrl+"/meeting/searchUserMeetingInMonth"

}
//移动端通过Ajax向服务端提交请求 然后将接收到的响应进行封装
// 1. 如果用户没有登陆系统，就跳转到登陆页面。
// 2. 如果用户权限不够，就显示提示信息。
// 3. 如果后端出现异常，就提示异常信息。
// 4. 如果后端验证令牌不正确，就提示信息。
// 5. 如果后端正常处理请求，还要判断响应中是否有Token。如果令牌刷新了，还要在本地存储Token。

Vue.prototype.ajax = function(url, method, data, fun) {
	// 向服务器发出请求
	uni.request({
		"url": url,
		"method": method,
		"header": {
			token: uni.getStorageSync('token')
		},
		"data": data,
		// 接受响应
		success: function(resp) {
			// 1. 如果用户没有登陆系统，就跳转到登陆页面。
			if (resp.statusCode == 401) {
				uni.redirectTo({
					url: '../login/login'
				});
			} else if (resp.statusCode == 200 && resp.data.code == 200) {
				// resp.data 是后端响应的数据
				let data = resp.data
				console.log(resp.data)
				// 5. 如果后端正常处理请求，还要判断响应中是否有Token。如果令牌刷新了，还要在本地存储Token。
				if (data.hasOwnProperty("token")) {
					console.log("token:", resp.data.token)
					uni.setStorageSync("token", data.token)
				}
				// 将resp传给设置好的匿名函数，调用匿名函数
				fun(resp)

			} else {
				uni.showToast({
					icon: 'none',
					title: resp.data
				});
			}
		}
	});
}


// 定义前端权限验证函数
Vue.prototype.checkPermission = function(perms){
	let permission = uni.getStorageSync("permission")
	let result = false;
	for(let one of perms){
		if(permission.indexOf(one) != -1){
			result=true;
			break;
		}
	}
	return result;
}
app.$mount()


// 声明日期格式化的函数

Date.prototype.format = function(fmt) {
	var o = {
		"M+": this.getMonth() + 1, //月份 
		"d+": this.getDate(), //日 
		"h+": this.getHours(), //小时 
		"m+": this.getMinutes(), //分 
		"s+": this.getSeconds(), //秒 
		"q+": Math.floor((this.getMonth() + 3) / 3), //季度 
		"S": this.getMilliseconds() //毫秒 
	};
	if (/(y+)/.test(fmt)) {
		fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
	}
	for (var k in o) {
		if (new RegExp("(" + k + ")").test(fmt)) {
			fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
		}
	}
	return fmt;
}

// 提交数据的时候，验证内容是不是为空
// 验证空值的
Vue.prototype.checkNull = function(data, name) {
	if (data == null) {
		uni.showToast({
			icon: "none",
			title: name + "不能为空"
		})
		return true
	}
	return false
}
// 验证空字符串的
Vue.prototype.checkBlank = function(data, name) {
	if (data == null || data == "") {
		uni.showToast({
			icon: "none",
			title: name + "不能为空"
		})
		return true
	}
	return false
}
// 验证姓名的
Vue.prototype.checkValidName = function(data, name) {
	if (data == null || data == "") {
		uni.showToast({
			icon: "none",
			title: name + "不能为空"
		})
		return true
	} else if (!/^[\u4e00-\u9fa5]{2,15}$/.test(data)) {
		uni.showToast({
			icon: "none",
			title: name + "内容不正确"
		})
		return true
	}
	return false
}
// 验证电话号码的
Vue.prototype.checkValidTel = function(data, name) {
	if (data == null || data == "") {
		uni.showToast({
			icon: "none",
			title: name + "不能为空"
		})
		return true
	} else if (!/^1[0-9]{10}$/.test(data)) {
		uni.showToast({
			icon: "none",
			title: name + "内容不正确"
		})
		return true
	}
	return false
}
// 验证email
Vue.prototype.checkValidEmail = function(data, name) {
	if (data == null || data == "") {
		uni.showToast({
			icon: "none",
			title: name + "不能为空"
		})
		return true
	} else if (!/^([a-zA-Z]|[0-9])(\w|\-)+@[a-zA-Z0-9]+\.([a-zA-Z]{2,4})$/.test(data)) {
		uni.showToast({
			icon: "none",
			title: name + "内容不正确"
		})
		return true
	}
	return false
}
// 验证时间的
Vue.prototype.checkValidStartAndEnd = function(start, end) {
	let d1 = new Date("2000/01/01 " + start + ":00");
	let d2 = new Date("2000/01/01 " + end + ":00");
	if (d2.getTime() <= d1.getTime()) {
		uni.showToast({
			icon: "none",
			title: "结束时间必须大于开始时间"
		})
		return true
	}
	return false
}

