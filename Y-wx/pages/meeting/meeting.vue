<template>
	<!-- 前提是需要权限 认证 -->
	<view class="page" v-if="checkPermission(['ROOT', 'MEETING:INSERT', 'MEETING:UPDATE'])">
		<view class="header">
			<input type="text" class="title" v-model="title" placeholder="输入会议标题" placeholder-class="title-placeholder" />
			<image src="../../static/icon-18.png" mode="widthFix" class="edit-icon"></image>
		</view>
		<view class="attr">
			<view class="list">
				<view class="item">
					<view class="key">日期</view>
					<picker v-if="canEdit" mode="date" :value="date" @change="dateChange">
						<!-- //定义change事件的捕获 -->
						<view class="uni-input">{{ date }}</view>
					</picker>
					<text v-if="!canEdit" class="value">{{ date }}</text>
				</view>
				<view class="item">
					<view class="key">开始时间</view>
					<picker v-if="canEdit" mode="time" :value="start" @change="startChange">
						<!-- //定义change事件的捕获 -->
						<view class="uni-input">{{ start }}</view>
					</picker>
					<text v-if="!canEdit" class="value">{{ start }}</text>
				</view>
				<view class="item">
					<view class="key">结束时间</view>
					<picker v-if="canEdit" mode="time" :value="end" @change="endChange">
						<!-- //定义change事件的捕获 -->
						<view class="uni-input">{{ end }}</view>
					</picker>
					<text v-if="!canEdit" class="value">{{ end }}</text>
				</view>
				<view class="item">
					<view class="key">会议类型</view>
					<!-- //定义change事件的捕获 -->
					<picker v-if="canEdit" :value="typeIndex" :range="typeArray" @change="typeChange">{{ typeArray[typeIndex] }}</picker>
					<text v-if="!canEdit" class="value">{{ typeArray[typeIndex] }}</text>
				</view>
				<view class="item" v-if="typeArray[typeIndex] == '线下会议'" @tap="editPlace">
					<view class="key">地点</view>
					<view class="value">{{ place }}</view>
				</view>
			</view>
			<view @tap="editDesc">
				<text class="desc">{{ desc }}</text>
			</view>
		</view>
		<view class="members">
			<view class="number">参会者（{{ members.length }}人）</view>
			<view class="member">
				<view class="user" v-for="one in members" :key="one.id" @longpress="deleteMember(one.id)">
					<image :src="one.photo" mode="widthFix" class="photo"></image>
					<text class="name">{{ one.name }}</text>
				</view>
				<view class="add">
					<image src="../../static/icon-19.png" mode="widthFix" class="add-btn" @tap="toMembersPage()"></image>
					<!-- //给图标绑定点击事件 -->
				</view>
			</view>
		</view>
		<button class="btn" @tap="save">保存</button>
		<!-- 对话框 控件 -->
		<uni-popup ref="popupPlace" type="dialog">
			<uni-popup-dialog mode="input" title="编辑文字内容" placeholder="输入会议地点" :value="place" @confirm="finishPlace"></uni-popup-dialog>
		</uni-popup>
		<uni-popup ref="popupDesc" type="dialog">
			<uni-popup-dialog mode="input" title="编辑文字内容" placeholder="输入会议内容" :value="desc" @confirm="finishDesc"></uni-popup-dialog>
		</uni-popup>
	</view>
</template>

<script>
import uniPopup from '@/components/uni-popup/uni-popup.vue';
import uniPopupMessage from '@/components/uni-popup/uni-popup-message.vue';
import uniPopupDialog from '@/components/uni-popup/uni-popup-dialog.vue';
export default {
	components: {
		uniPopup,
		uniPopupMessage,
		uniPopupDialog
	},
	data() {
		return {
			opt: null,
			id: null,
			uuid: null,
			canEdit: true,
			title: '',
			date: '',
			start: '',
			end: '',
			typeArray: ['线上会议', '线下会议'],
			typeIndex: 0,
			place: '',
			desc: '会议内容',
			members: [],
			instanceId: null
		};
	},
	onShow: function() {
		
		let that = this;
		let pages = getCurrentPages(); //获取页面的执行栈
		let currPage = pages[pages.length - 1]; //当前页面的对象
		// 判断是不是从会议列表页面进入的
		// 判断当前页面是否绑定数据 currPage.hasOwnProperty("finishMembers")
		if (!currPage.hasOwnProperty('finishMembers') || !currPage.finishMembers) {
			// 首先判断，进入会议详情页面 是进行的新建会议 还是进行的编辑会议
			if (that.opt == 'insert') {
				// 新建会议的话，就生成默认参数时间
				let now = new Date(); //创建日期对象
				now.setTime(now.getTime() + 30 * 60 * 1000); //把当前时刻取出，然后加入30分钟，往后偏移30分钟，作为会议的起始时间
				that.date = now.format('yyyy-MM-dd');
				that.start = now.format('hh:mm');
				now.setTime(now.getTime() + 60 * 60 * 1000); //设这结束时间的默认值，再次延后60分钟
				that.end = now.format('hh:mm');
			} else if (that.opt == 'edit') {
				// 获取数据
				that.ajax(that.url.searchMeetingById, 'POST', { id: that.id }, function(resp) {
					
					let result = resp.data.result;
					that.uuid = result.uuid;
					that.title = result.title;
					that.date = result.date;
					that.start = result.start;
					that.end = result.end;
					that.typeIndex = result.type - 1;
					that.place = result.place;
					let desc = result.desc;
					if (desc != null && desc != '') {
						that.desc = desc;
					}
					that.members = result.members;
					that.instanceId = result.instanceId;
				});
			}
		} else {
			// 从members页面 返回到 meeting页面中
			let members = [];
			// 把数组中的字符串转为数字
			// 这里的currPage.members 不是that.members
			// 而是从members页面 返回到 meeting页面时，当前页面对象所绑定的members
			for (let one of currPage.members) {
				members.push(Number(one));
			}
			// 因为members存放的是id，所以根据id 找到members的详细信息
			// 查询数据
			that.ajax(that.url.searchMembers, 'POST', { members: JSON.stringify(members) }, function(resp) {
				let result = resp.data.result;
				that.members = result;
			});
		}
	},
	// 当页面meeting_list 跳转到 meeting页面时传入的参数
	onLoad: function(options) {
		this.id = options.id;
		this.opt = options.opt;
	},
	methods: {
		// 跳转到成员列表函数
		toMembersPage: function() {
			// 将数组转换成字符串
			let array = [];

			for (let one of this.members) {
				array.push(one.id);
			}
			// 跳转到成员列表 ?传参 参数名members
			uni.navigateTo({
				url: '../members/members?members=' + array.join(',')
			});
		},
		dateChange: function(e) {
			// 得到弹出窗口的改变的value e.detail.value
			this.date = e.detail.value;
		},
		startChange: function(e) {
			this.start = e.detail.value;
		},
		endChange: function(e) {
			this.end = e.detail.value;
		},
		typeChange: function(e) {
			this.typeIndex = e.detail.value;
		},
		editPlace: function() {
			if (!this.canEdit) {
				return;
			}
			this.$refs.popupPlace.open();
		},
		finishPlace: function(done, value) {
			// value表示 你在对话框中填写的内容
			if (value != null && value != '') {
				this.place = value;
				done();
			} else {
				uni.showToast({
					icon: 'none',
					title: '地点不能为空'
				});
			}
		},
		editDesc: function() {
			if (!this.canEdit) {
				return;
			}
			this.$refs.popupDesc.open();
		},
		finishDesc: function(done, value) {
			// value表示 你在对话框中填写的内容
			if (value != null && value != '') {
				this.desc = value;
				done();
			} else {
				uni.showToast({
					icon: 'none',
					title: '内容不能为空'
				});
			}
		},
		save: function() {
			let that = this;
			let array = [];
			// that.members 参会人的信息
			for (let one of that.members) {
				array.push(one.id);
			}
			// 验证数据
			if (
				that.checkBlank(that.title, '会议题目') ||
				that.checkValidStartAndEnd(that.start, that.end) ||
				(that.typeIndex == '1' && that.checkBlank(that.place, '会议地点')) ||
				that.checkBlank(that.desc, '会议内容') ||
				array.length == 0
			) {
				return;
			}
			// 验证数据集正确的话，就发请求
			let data = {
				title: that.title,
				date: that.date,
				start: that.start,
				end: that.end,
				type: Number(that.typeIndex) + 1,
				members: JSON.stringify(array),
				desc: that.desc,
				id: that.id,
				instanceId: that.instanceId
			};
			if (that.typeIndex == '1') {
				//线下会议
				data.place = that.place;
			}
			let url;
			if (that.opt == 'insert') {
				//创建会议
				url = that.url.insertMeeting;
			} else if (that.opt == 'edit') {
				//编辑会议
				url = that.url.updateMeetingInfo;
			}
			that.ajax(url, 'POST', data, function(resp) {
				uni.showToast({
					icon: 'success',
					title: '保存成功',
					complete: function() {
						setTimeout(function() {
							uni.navigateBack({});//两秒钟之后 跳转到会议列表界面
						}, 2000);
					}
				});
			});
		},
		// 长按删除参会员工
		deleteMember: function(id) {
			let that = this;
			uni.vibrateShort({});
			uni.showModal({
				title: '提示信息',
				content: '删除该名参会人员？',
				success: function(resp) {
					let postion;
					for (let i = 0; i < that.members.length; i++) {
						let one = that.members[i];
						if (one.id == id) {
							postion = i;
							break;
						}
					}
					// 从members列表中删除 指定员工
					that.members.splice(postion, 1);
				}
			});
		}
	}
};
</script>

<style lang="less">
@import url('meeting.less');
</style>
