

## 1. 流程图

### 流程图

![在这里插入图片描述](https://img-blog.csdnimg.cn/20023529d475454fa5b024b9cd098a3b.png?x-oss-process=image/watermark,type_ZHJvaWRzYW5zZmFsbGJhY2s,shadow_50,text_Q1NETiBAQ2hyaXNpdGluZVRY,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


### 实现思路

### 由于点赞属于一种频繁的提交操作，如果直接选用数据库做存储，对于数据库的压力比较大。这里考虑使用缓存作为中间层，然后定时的将数据持久化数据库，降低数据库的读写压力。缓存选用的是redis。

# 2. 具体实现

## 2.1 表设计

### 点赞表

```sql
CREATE TABLE `user_likes` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '点赞信息ID',
  `info_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '点赞对象id',
  `create_time` datetime DEFAULT NULL COMMENT '时间',
  `like_user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '点赞人ID',
  `update_time` datetime DEFAULT NULL,
  `status` int DEFAULT '0' COMMENT '0 取消 1 点赞',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `agdkey` (`like_user_id`,`info_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='点赞记录表';
```

### 点赞的内容表

```sql
CREATE TABLE `video` (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `likes_number` int DEFAULT NULL COMMENT '点赞数',
  `comments_number` int DEFAULT NULL COMMENT '评论数',
  `share_number` int DEFAULT NULL COMMENT '分享数',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '创建者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_user` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
```

## 2.2 工具类及枚举类

### RedisKeyUtils

```java
public class RedisKeyUtils {
    /**
     *
     保存用户点赞数据的key
     * @date 2021/9/26 14:44
     */
    public static final String MAP_KEY_USER_LIKED = "MAP_USER_LIKED";
    /**
    *
     保存用户被点赞数量的key
    * @date 2021/9/26 14:44
    */
    public static final String MAP_KEY_USER_LIKED_COUNT = "MAP_USER_LIKED_COUNT";

    /**
     * 拼接被点赞的用户id和点赞的人的id作为key。格式 222222::333333
     * @param likedUserId 被点赞的人id
     * @param likedPostId 点赞的人的id
     * @return
     */
    public static String getLikedKey(String likedUserId, String likedPostId){
            return likedUserId +
                "::" +
                likedPostId;
    }
```

### RedisConfig

```java
@Configuration
public class RedisConfig {
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory)  {

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(jackson2JsonRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }


    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory)  {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
```

### UserLikesDto

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLikesDto {
    private String infoId;
    private String likeUserId;
    private Integer status;

}
```

### UserLikCountDTO

```java
@Data
public class UserLikCountDTO implements Serializable {

    private String infoId;
    private Integer value;

    public UserLikCountDTO(String infoId, Integer value) {
        this.infoId = infoId;
        this.value = value;
    }
}
```



## 2.3 代码实现

#### 使用redisTemplate.opsForHash()方法，创建2个hash对象，一个存储点赞信息，一个存储点赞数。点赞信息的key是通过内容id拼接点赞者id拼接而成，value则为点赞状态。例如(1::2，0)

#### likeStatus方法

```java
public Object likeStatus(String infoId, String likeUserId) {
    if (redisTemplate.opsForHash().hasKey(RedisKeyUtils.MAP_KEY_USER_LIKED, RedisKeyUtils.getLikedKey(infoId, likeUserId))) {
        String o = redisTemplate.opsForHash().get(RedisKeyUtils.MAP_KEY_USER_LIKED, RedisKeyUtils.getLikedKey(infoId, likeUserId)).toString();
        if ("1".equals(o)) {
            unLikes(infoId, likeUserId);
            return LikedStatusEum.UNLIKE;
        }
        if ("0".equals(o)) {
            likes(infoId, likeUserId);
            return LikedStatusEum.LIKE;
        }
    }
    UserLikes userLikes = userLikesDao.selectOne(new QueryWrapper<UserLikes>().eq("info_id", infoId).eq("like_user_id", likeUserId));
    if (userLikes == null) {
        UserLikes userLikes1 = new UserLikes();
        userLikes1.setInfoId(infoId);
        userLikes1.setLikeUserId(likeUserId);
        userLikesDao.insert(userLikes1);
        likes(infoId, likeUserId);
        return LikedStatusEum.LIKE;
    }
    if (userLikes.getStatus() == 1) {
        unLikes(infoId, likeUserId);
        return LikedStatusEum.UNLIKE;
    }

    if (userLikes.getStatus() == 0) {
        likes(infoId, likeUserId);
        return LikedStatusEum.LIKE;
    }
    return "";
}
```

#### like方法

```java
    public void likes(String infoId, String likeUserId) {
        String likedKey = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        redisTemplate.opsForHash().increment(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, infoId, 1);
        redisTemplate.opsForHash().put(RedisKeyUtils.MAP_KEY_USER_LIKED, likedKey, LikedStatusEum.LIKE.getCode());
    }
```

#### unlike方法

```java
    public void unLikes(String infoId, String likeUserId) {
        String likedKey = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        redisTemplate.opsForHash().increment(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, infoId, -1);
            redisTemplate.opsForHash().delete(RedisKeyUtils.MAP_KEY_USER_LIKED, likedKey);
    }
```

#### 统计点赞变化情况的getLikedDataFromRedis方法

```java
 public List<UserLikesDto> getLikedDataFromRedis() {
        Cursor<Map.Entry<Object, Object>> scan = redisTemplate.opsForHash().scan(RedisKeyUtils.MAP_KEY_USER_LIKED, ScanOptions.NONE);
        List<UserLikesDto> list = new ArrayList<>();
        while (scan.hasNext()) {
            Map.Entry<Object, Object> entry = scan.next();
            String key = (String) entry.getKey();
            String[] split = key.split("::");
            String infoId = split[0];
            String likeUserId = split[1];
            Integer value = (Integer) entry.getValue();
            //组装成 UserLike 对象
            UserLikesDto userLikeDetail = new UserLikesDto(infoId, likeUserId, value);
            list.add(userLikeDetail);
            //存到 list 后从 Redis 中删除
            redisTemplate.opsForHash().delete(RedisKeyUtils.MAP_KEY_USER_LIKED, key);
        }
        return list;
    }
```

#### 统计点赞数量的getLikedCountFromRedis方法

```java
public List<UserLikCountDTO> getLikedCountFromRedis() {
        Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, ScanOptions.NONE);
        List<UserLikCountDTO> list = new ArrayList<>();
        while (cursor.hasNext()) {
            Map.Entry<Object, Object> map = cursor.next();
            String key = (String) map.getKey();
            Integer value = (Integer) map.getValue();
            UserLikCountDTO userLikCountDTO = new UserLikCountDTO(key, value);
            list.add(userLikCountDTO);
            redisTemplate.opsForHash().delete(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, key);
        }
        return list;
    }
```

