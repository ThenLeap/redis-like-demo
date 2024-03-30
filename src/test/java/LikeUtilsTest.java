import com.yixin.light.LightApplication;
import com.yixin.light.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author: Mwx
 * Date: 2022/8/15
 * Time: 22:10
 */
@SpringBootTest(classes = LightApplication.class)

public class LikeUtilsTest {
    @Resource
    private RedisUtils redisUtils;
    @Test
    public void test(){
        redisUtils.unLikes("1111","123");
    }
}
