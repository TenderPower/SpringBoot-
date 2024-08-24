<template>
	<view>
		<!-- 调用前置摄像头；不打开闪光灯； 当这个摄像头打开失败时，触发error回调函数；-->
		<camera device-position="front" flash="off" class="camera" @error="error" v-if="showCamera"></camera>
		<!--  规定取景框下方的图片 ；-->
		<image mode="widthFix" class="image" :src="photoPath" v-if="showImage"></image>
		<!-- 声明两个按钮 -->
		<view class="operate-container">
			<!-- 当点击该按钮时，调用clickBtn方法； -->
			<button type="primary" class="btn" @tap="clickBtn" :disabled="!canCheckin">{{ btnText }}</button>
			<button type="warn" class="btn" @tap="afresh" :disabled="!canCheckin">重拍</button>
		</view>
		<!-- 声明提示文字 -->
		<view class="notice-container">
			<text class="notice">注意事项</text>
			<text class="desc">拍照签到的时候，必须要拍摄自己的正面照片，侧面照片会导致无法识别。另外，拍照的时候不要戴墨镜或者帽子，避免影响拍照签到的准确度。</text>
		</view>
	</view>
</template>

<script>
// 引入一下sdk文件
var QQMapWX = require('../../lib/qqmap-wx-jssdk.min.js');
var qqmapsdk;
export default {
	data() {
		return {
			canCheckin: true,
			photoPath: '',
			btnText: '拍照',
			showCamera: true,
			showImage: false
		};
	},
	// 该函数是声明周期函数，不能写在methods里面
	onLoad: function() {
		// 对调用sdk的变量进行初始化
		qqmapsdk = new QQMapWX({
			key: 'TBSBZ-ABLKG-QV7QX-QN2OE-MCNZS-54FBQ'
		});
	},
	// 监听页面显示
	onShow: function() {
		let that = this;
		that.ajax(that.url.validCanCheckIn, 'GET', null, function(resp) {
			let msg = resp.data.msg;
			if (msg != '可以考勤') {
				setTimeout(function() {
					uni.showToast({
						title: msg,
						icon: 'none'
					});
				}, 1000);
				that.canCheckin = false;
			}
		});
	},
	methods: {
		clickBtn: function() {
			// 先声明一下VUE对象
			let that = this;
			// 判断按钮上的文字是否为”拍照“
			if (that.btnText == '拍照') {
				// 获得摄像头上下文的对象
				let ctx = uni.createCameraContext();
				// 进行拍照
				ctx.takePhoto({
					// 拍照质量
					quality: 'high',
					// 拍照成功
					success: function(resp) {
						// 拍照图片的路径
						// 保存路径
						that.photoPath = resp.tempImagePath;
						// 关闭camera组件
						that.showCamera = false;
						// 打开图片组件
						that.showImage = true;
						// 更改按钮文字
						that.btnText = '签到';
					}
				});
			} else {
				// 执行签到任务

				// 由于用户照片和数据库人脸模型做匹配，需要消耗时间
				// 所以让用户耐心等待一下
				uni.showLoading({
					title: '签到中请稍后'
				});
				// 等待30秒关闭提示信息
				setTimeout(function() {
					uni.hideLoading();
				}, 30000);
				// 由于获取地址位置的额度用完 默认把
				// 设置地址的默认信息
				let address = '山东省济南市历下区新兴街道';
				let addressComponent = undefined;
				let nation = '中国';
				let province = '山东省';
				let city = '济南市';
				let district = '历下区';
				// 进行签到
				uni.uploadFile({
					url: that.url.checkin,
					filePath: that.photoPath,
					name: 'photo',
					header: {
						token: uni.getStorageSync('token')
					},
					// 上传的数据
					formData: {
						address: address,
						country: nation,
						province: province,
						city: city,
						district: district
					},
					success: function(resp) {
						// 判断后台的返回值
						if (resp.statusCode == 500 && resp.data == '不存在人脸模型') {
							uni.hideLoading();
							uni.showModal({
								title: '提示信息',
								content: 'EMOS系统中不存在你的人脸识别模型，是否用当前这张照片作为人脸识别模型？',
								// 确认用户是点击确认按钮还是取消按钮呀
								success: function(res) {
									if (res.confirm) {
										// 上传头像图片
										uni.uploadFile({
											url: that.url.createFaceModel,
											filePath: that.photoPath,
											name: 'photo',
											header: {
												token: uni.getStorageSync('token')
											},
											success: function(resp) {
												if (resp.statusCode == 500) {
													uni.showToast({
														title: resp.data,
														icon: 'none'
													});
												} else if (resp.statusCode == 200) {
													uni.showToast({
														title: '人脸建模成功',
														icon: 'none'
													});
												}
											}
										});
									}
								}
							});
						}
						// 上传图片与服务器中的图片匹配成功
						else if (resp.statusCode == 200) {
							// 保存解析的数据
							let data = JSON.parse(resp.data);
							// 提取业务状态码 和 code
							let code = data.code;
							let msg = data.msg;
							// 签到成功
							if (code == 200) {
								uni.hideLoading();
								uni.showToast({
									title: '签到成功',
									complete: function() {
										// TODO 跳转到签到结果统计页面
				
										uni.navigateTo({
											url: '../checkin_result/checkin_result'
										});
									}
								});
							}
						} else if (resp.statusCode == 500) {
							uni.showToast({
								title: resp.data,
								icon: 'none'
							});
						}
					}
				});
				// 获取地理位置信息
				// uni.getLocation({
				// 	type: 'wgs84',
				// 	success: function(resp) {
				// 		let statue = '默认';
				// 		let latitude = resp.latitude;
				// 		let longitude = resp.longitude;
				// 		console.log('当前位置的经度：' + resp.longitude);
				// 		console.log('当前位置的维度：' + resp.latitude);

				// 		// 设置地址的默认信息
				// 		let address = '山东省济南市历下区新兴街道';
				// 		let addressComponent = undefined;
				// 		let nation = '中国';
				// 		let province = '山东省';
				// 		let city = '济南市';
				// 		let district = '历下区';

				// 		// 利用声明好的qqmapsdk变量去使用
				// 		// 通过定位获取位置信息
				// 		qqmapsdk.reverseGeocoder({
				// 			location: {
				// 				latitude: latitude,
				// 				longitude: longitude
				// 			},
				// 			// 该第三方sdk 有使用次数限制
				// 			success: function(resp) {
				// 				statue = '第三方sdk';
				// 				console.log(resp.result);
				// 				address = resp.result.address;
				// 				addressComponent = resp.result.address_component;
				// 				nation = addressComponent.nation;
				// 				province = addressComponent.province;
				// 				city = addressComponent.city;
				// 				district = addressComponent.district;
				// 			}
				// 		});
				// 		console.log(statue);
				// 		// 进行签到
				// 		uni.uploadFile({
				// 			url: that.url.checkin,
				// 			filePath: that.photoPath,
				// 			name: 'photo',
				// 			header: {
				// 				token: uni.getStorageSync('token')
				// 			},
				// 			// 上传的数据
				// 			formData: {
				// 				address: address,
				// 				country: nation,
				// 				province: province,
				// 				city: city,
				// 				district: district
				// 			},
				// 			success: function(resp) {
				// 				// 判断后台的返回值
				// 				if (resp.statusCode == 500 && resp.data == '不存在人脸模型') {
				// 					uni.hideLoading();
				// 					uni.showModal({
				// 						title: '提示信息',
				// 						content: 'EMOS系统中不存在你的人脸识别模型，是否用当前这张照片作为人脸识别模型？',
				// 						// 确认用户是点击确认按钮还是取消按钮呀
				// 						success: function(res) {
				// 							if (res.confirm) {
				// 								// 上传头像图片
				// 								uni.uploadFile({
				// 									url: that.url.createFaceModel,
				// 									filePath: that.photoPath,
				// 									name: 'photo',
				// 									header: {
				// 										token: uni.getStorageSync('token')
				// 									},
				// 									success: function(resp) {
				// 										if (resp.statusCode == 500) {
				// 											uni.showToast({
				// 												title: resp.data,
				// 												icon: 'none'
				// 											});
				// 										} else if (resp.statusCode == 200) {
				// 											uni.showToast({
				// 												title: '人脸建模成功',
				// 												icon: 'none'
				// 											});
				// 										}
				// 									}
				// 								});
				// 							}
				// 						}
				// 					});
				// 				}
				// 				// 上传图片与服务器中的图片匹配成功
				// 				else if (resp.statusCode == 200) {
				// 					// 保存解析的数据
				// 					let data = JSON.parse(resp.data);
				// 					// 提取业务状态码 和 code
				// 					let code = data.code;
				// 					let msg = data.msg;
				// 					// 签到成功
				// 					if (code == 200) {
				// 						uni.hideLoading();
				// 						uni.showToast({
				// 							title: '签到成功',
				// 							complete: function() {
				// 								// TODO 跳转到签到结果统计页面

				// 								uni.navigateTo({
				// 									url: '../checkin_result/checkin_result'
				// 								});
				// 							}
				// 						});
				// 					}
				// 				} else if (resp.statusCode == 500) {
				// 					uni.showToast({
				// 						title: resp.data,
				// 						icon: 'none'
				// 					});
				// 				}
				// 			}
				// 		});
				// 	}
				// });
			}
		},

		afresh: function() {
			let that = this;
			that.showCamera = true;
			that.showImage = false;
			that.btnText = '拍照';
		}
	}
};
</script>

<style lang="less">
@import url('checkin.less');
</style>
