<template>
	<view>
		<image src="../../static/logo-1.png" mode="widthFix" class="logo"></image>
		<view class="logo-title">企业在线办公系统</view>
		<view class="logo-subtitile">Version 2030.5</view>
		<!--  我发现不加这个open-type="getUserInfo" 并不影响程序 -->
		<button class="login-btn" open-type="getUserInfo" @tap="login()">登录系统</button>
		<view class="register-container">
			没有账号？
			<text class="register" @tap="toRegister()">立即注册</text>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {};
	},
	methods: {
		toRegister: function() {
			//跳转到注册页面
			uni.navigateTo({
				url: '../register/register'
			});
		},
		login: function() {
			// 先获取当前的VUE对象
			let that = this;
			// 进行login操作
			uni.login({
				provider: 'weixin',
				// 登录成功
				success: function(resp) {
					// 从微信服务器端获取临时字符串
					let code = resp.code;
					that.ajax(that.url.login, 'POST', { code: code }, function(resp) {
						// 从后端web层中的login函数所返回的R对象
						let permission = resp.data.permission;
						uni.setStorageSync('permission', permission);
						// 跳转到登录页面
						console.log('success');
						// 跳转到Tab导航页面（使用uni中的switchTab)
						uni.switchTab({
							url: '../index/index'
						});
					});
				},
				fail: function(e) {
					console.log(e);
					uni.showToast({
						icon: 'none',
						title: '执行异常'
					});
				}
			});
		}
	}
};
</script>

<style lang="less">
@import url('login.less');
</style>
