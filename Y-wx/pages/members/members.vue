<template>
	<view class="page">
		<checkbox-group @change="selected">
			<block v-for="dept in list" :key="dept.id">
				<view class="list-title">{{ dept.deptName }}（{{ dept.count }}人）</view>
				<view class="item" v-for="member in dept.members" :key="member.userId">
					<view class="key">{{ member.name }}</view>
					<checkbox class="value" :value="member.userId" :checked="member.checked"></checkbox>
				</view>
			</block>
		</checkbox-group>
	</view>
</template>

<script>
export default {
	data() {
		return {
			list: [],
			members: []
		};
	},
	// 可以进行多次show
	onShow: function() {
		this.loadData(this);
	},
	// 只加载一次
	onLoad: function(options) {
		// 如果用户在修改会议成员时候,小程序跳转到成员列表页面,需要向该页面传递先前选中的用户
		if (options.hasOwnProperty('members')) {
			let members = options.members;
			this.members = members.split(',');
			console.log(members);
		}
	},
	methods: {
		// 加载员工信息
		loadData: function(ref) {
			ref.ajax(ref.url.searchUserGroupByDept, 'POST', { keyword: ref.keyword }, function(resp) {
				let result = resp.data.result;
				ref.list = result;
				// 对绑定后的ref.list进行处理
				for (let dept of ref.list) {
					for (let member of dept.members) {
						// 判断成员已经选择了,就在页面中一直显示
						if (ref.members.indexOf(member.userId + '') != -1) {
							
							member.checked = true;
						} else {
							member.checked = false;
						}
					}
				}
			});
		},
		// 把当前页面选中的成员绑定到上一页,返回上一页的时候就能得到选择了那些成员
		selected: function(e) {
			let that = this;
			that.members = e.detail.value;//当前所有选中的成员（成员的user_id）
			let pages = getCurrentPages();//当前页面栈
			let prevPage = pages[pages.length - 2];//上一个页面
			// 向上一个页面传递数据
			prevPage.members = that.members;
			prevPage.finishMembers = true;
		}
	}
};
</script>

<style lang="less">
@import url('members.less');
</style>
