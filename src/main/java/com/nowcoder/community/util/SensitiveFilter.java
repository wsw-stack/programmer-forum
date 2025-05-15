package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// use Trie to search sensitive words
@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    // 敏感词替换符号
    private static final String REPLACEMENT = "***";
    // 根节点初始化
    private TrieNode rootNode = new TrieNode();

    @PostConstruct // 当容器实例化后自动调用
    public void init() {
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                InputStreamReader inputStreamReader = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                ) {
            String keyword;
            while((keyword = reader.readLine()) != null) {
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败");
        }
    }

    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for(int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if(subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            tempNode = subNode;

            // 设置结束标识
            if(i == keyword.length()-1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词，返回过滤后的文本
     * @param text
     * @return
     */
    public String filter(String text) {
        if(StringUtils.isBlank(text)) {
            return null;
        }
        // 分别对应3个指针
        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;
        StringBuilder stringBuilder = new StringBuilder();

        while(position < text.length()) {
            char c = text.charAt(position);
            // 跳过符号字符
            if(isSymbol(c)) {
                // 指针1处于根节点，此符号计入结果，先让指针2走一步
                if(tempNode == rootNode) {
                    stringBuilder.append(c);
                    begin++;
                }
                position++;
                continue;
            }
            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null) { // 当前不存在敏感词
                // begin开头的字符串不是敏感词
                stringBuilder.append(text.charAt(begin));
                // 下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = rootNode;
            } else if(tempNode.isKeywordEnd()) {
                // 发现敏感词，将begin~position替换
                stringBuilder.append(REPLACEMENT);
                // 并进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = rootNode;
            } else {
                // 检查下一个字符
                position++;
            }
        }
        // 最后一批字符计入结果
        stringBuilder.append(text.substring(begin));
        return stringBuilder.toString();
    }

    private boolean isSymbol(Character c) {
        // 东亚文字范围保留
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }


    // 初始化敏感词前缀树
    private class TrieNode {
        // 关键词结束标识
        private boolean isKeywordEnd;
        // 子节点(key是下级节点字符，value是节点）
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }


}
