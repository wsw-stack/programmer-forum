package com.nowcoder.community;

import com.nowcoder.community.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SensitiveTest {
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter01() {
        String text = "这里可以赌博，可以吸毒";
        System.out.println(sensitiveFilter.filter(text));
    }

    @Test
    public void testSensitiveFilter02() {
        String text = "这里可以赌~博，可以吸~毒";
        System.out.println(sensitiveFilter.filter(text));
    }

    @Test
    public void testSensitiveFilter03() {
        String text = "这里可以交流编程";
        System.out.println(sensitiveFilter.filter(text));
    }
}
