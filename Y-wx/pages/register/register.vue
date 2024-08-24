<template>
	<view>
		<image src="../../static/logo-2.png" mode="widthFix" class="logo"></image>
		<view class="register-container">
			<input placeholder="请输入你的邀请码" class="register-code" maxlength="6" v-model="registerCode" />
			<view class="register-desc">管理员创建员工证账号之后，你可以从你的个人邮箱中获得注 册邀请码</view>
			<!--  我发现不加这个open-type="getUserInfo" 并不影响程序 -->
			<button class="register-btn" open-type="getUserInfo" @tap="register()">执行注册</button>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			registerCode: ''
		};
	},
	methods: {
		register: function() {
			// 在向后端提交数据之前，我们先要做好前端的数据验证
			let that = this; // 此刻this代表VUE对象
			console.log(that.registerCode);
			if (that.registerCode == null || that.registerCode.length == 0) {
				uni.showToast({
					title: '邀请码不能为空',
					icon: 'none'
				});
				return;
			} else if (/^[0-9]{6}$/.test(that.registerCode) == false) {
				uni.showToast({
					title: '邀请码必须是6位数字',
					icon: 'none'
				});
				return;
			}
			// 前端验证完成之后，在进行注册，先香开发者服务器后台获取code、nickname等信息
			// 然后将信息进行封装成后端能够接受的数据格式，传给后端
			uni.login({
				provider: 'weixin',
				success: function(resp) {
					// 获取小程序专有，用户登录凭证
					// 开发者需要在开发者服务器后台，使用 code换取 openid 和 session_key 等信息
					let code = resp.code; //临时授权字符串
					console.log('临时授权字符串：' + code);
					// 获取用户信息
					uni.getUserInfo({
						provider: 'weixin',
						success: function(resp) {
							let nickName = resp.userInfo.nickName;
							let avatarUrl = resp.userInfo.avatarUrl;
							console.log('用户昵称：' + nickName);
							// data的格式类型，要符合后端RegisterForm类中的格式
							let data = {
								code: code,
								nickname: nickName,
								photo: avatarUrl,
								registerCode: that.registerCode
							};
							// 向后端发出ajax请求
							that.ajax(that.url.register, 'POST', data, function(resp) {
								console.log('发送请求');
								// 将权限存放到前端本地
								let permission = resp.data.permission;
								uni.setStorageSync('permission', permission);
								// 跳转到index页面
								// 跳转到Tab导航页面（使用uni中的switchTab)
								uni.switchTab({
									url: '../index/index'
								});
							});
						}
					});
				}
			});
		}
	}
};
</script>

<style lang="less">
@import url('register.less');
</style>
