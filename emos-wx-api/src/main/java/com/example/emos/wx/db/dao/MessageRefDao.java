package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRefDao {
    @Autowired
    private MongoTemplate mongoTemplate;

//    数据保存
    public String insert(MessageRefEntity entity) {
        entity = mongoTemplate.save(entity);
        return entity.get_id();
    }

//    查询未读数据的数量
    public long searchUnreadCount(int userId) {
        Query query = new Query();
//        构造查询条件
        query.addCriteria(Criteria.where("readFlag").is(false).and("receiverId").is(userId));
//        做统计
        long count = mongoTemplate.count(query, MessageRefEntity.class);
        return count;
    }
//  最新消息的统计
    public long searchLastCount(int userId) {
        Query query = new Query();
//        构造查询条件
        query.addCriteria(Criteria.where("lastFlag").is(true).and("receiverId").is(userId));
//        进行修改
        Update update = new Update();
        update.set("lastFlag", false);
//        将lastFlag 为true的 更改为 false （说明该消息不是新消息了）
        UpdateResult result = mongoTemplate.updateMulti(query, update, "message_ref");
//        获取更改后数据的数量
        long rows = result.getModifiedCount();
        return rows;
    }

//    修改消息记录，把未读改为已读
    public long updateUnreadMessage(String id){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("readFlag", true);
//        updateFirst 只修改一个数据
        UpdateResult result = mongoTemplate.updateFirst(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }
// 根据主键值id 删除消息记录
    public long deleteMessageRefById(String id){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
//        调用remove方法
        DeleteResult result=mongoTemplate.remove(query,"message_ref");
        long rows=result.getDeletedCount();
        return rows;
    }

//    删除userid 删除用户所有的消息
    public long deleteUserMessageRef(int userId){
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(userId));
        DeleteResult result=mongoTemplate.remove(query,"message_ref");
        long rows=result.getDeletedCount();
        return rows;
    }
}
