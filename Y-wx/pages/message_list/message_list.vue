<template>
	<view class="page">
		<uniList>
			<uni-list-chat
				v-for="one in list"
				:title="one.senderName"
				:avatar="one.senderPhoto"
				:note="one.msg"
				badgePositon="left"
				:badgeText="one.readFlag ? '' : 'dot'"
				:key="one.id"
				link="navigateTo"
				:to="'../message/message?id=' + one.id + '&readFlag=' + one.readFlag + '&refId=' + one.refId"
			>
				<view class="chat-custom-right">
					<text class="chat-custom-text">{{ one.sendTime }}</text>
				</view>
			</uni-list-chat>
		</uniList>
	</view>
</template>

<script>
import uniList from '../../components/uni-list/uni-list.vue';
import uniListItem from '../../components/uni-list-item/uni-list-item.vue';

export default {
	components: {
		uniList,
		uniListItem
	},
	data() {
		return {
			page: 1,
			length: 20,
			list: [],
			isLastPage: false
		};
	},
	// 声明onShow回调函数，显示页面的时候加载第一页数据
	onShow:function(){
		let that=this
		that.page=1
		that.isLastPage=false
		// 滚动到顶部
		uni.pageScrollTo({
			scrollTop:"0"
		})
		// 在开始就加载分页数据
		that.loadMessageList(that)
	},
	// 当用户上划页面触底之后，触发翻页事件的回调函
	onReachBottom:function(){
		let that=this
		// 如果是最后一页，就不用发送ajax请求了
		if(that.isLastPage){
			return
		}
		that.page=that.page+1
		// 在开始就加载分页数据
		that.loadMessageList(that)
	},
	methods: {
		// 定义加载分页数据
		loadMessageList: function(ref) {
			let data = {
				page: ref.page,
				length: ref.length
			};
			ref.ajax(ref.url.searchMessageByPage, 'POST', data, function(resp) {
				let result = resp.data.result;
				if (result == null || result.length == 0) {
					ref.isLastPage = true;
					ref.page = ref.page - 1;
					uni.showToast({
						icon: 'none',
						title: '已经到底了'
					});
				} else {
					// 如果从其他页面切回来，也设置page为1 list为空
					if (ref.page == 1) {
						ref.list = [];
					}
					ref.list = ref.list.concat(result);
					if (ref.page > 1) {
						uni.showToast({
							icon: 'none',
							title: '又加载了' + result.length + '条消息'
						});
					}
				}
			});
		}
	}
};
</script>

<style lang="less">
@import url('message_list.less');
</style>
