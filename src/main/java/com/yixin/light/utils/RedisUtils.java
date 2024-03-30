package com.yixin.light.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yixin.light.mapper.UserLikesMapper;
import com.yixin.light.models.dto.UserLikCountDTO;
import com.yixin.light.models.dto.UserLikesDto;
import com.yixin.light.models.entity.UserLikes;
import com.yixin.light.models.eum.LikedStatusEum;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Mwx
 * Date: 2022/8/15
 * Time: 21:45
 */
@Component
public class RedisUtils {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private UserLikesMapper userLikesDao;

    public void likes(String infoId, String likeUserId) {
        String likedKey = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        redisTemplate.opsForHash().increment(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, infoId, 1);
        redisTemplate.opsForHash().put(RedisKeyUtils.MAP_KEY_USER_LIKED, likedKey, LikedStatusEum.LIKE.getCode());
    }


    public void unLikes(String infoId, String likeUserId) {
        String likedKey = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        redisTemplate.opsForHash().increment(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, infoId, -1);
        redisTemplate.opsForHash().delete(RedisKeyUtils.MAP_KEY_USER_LIKED, likedKey);
    }

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


}
