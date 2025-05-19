package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        if(user == null) { // 尚未登录
            return CommunityUtil.getJsonString(403, "您尚未登录！"); // 没有权限
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0, "发布成功");
    }

    @RequestMapping(path = "/detail/{id}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("id") int id, Model model, Page page) {
        // 帖子
        DiscussPost post = discussPostService.findDiscussPostById(id);
        model.addAttribute("post", post);
        // 查询作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        // 点赞
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, id);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态，取决于是否登录
        int likeStatus = hostHolder.getUser() == null ? 0 :likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, id);
        model.addAttribute("likeStatus", likeStatus);

        // 显示评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + id);
        page.setRows(post.getCommentCount());

        // 帖子评论和针对评论的回复
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        // 评论Vo列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        for (Comment comment : commentList) {
            Map<String, Object> commentVo = new HashMap<>();
            // 添加评论及其作者
            commentVo.put("comment", comment);
            commentVo.put("user", userService.findUserById(comment.getUserId()));
            // 点赞
            likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
            commentVo.put("likeCount", likeCount);
            // 点赞状态，取决于是否登录
            likeStatus = hostHolder.getUser() == null ? 0 :likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
            commentVo.put("likeStatus", likeStatus);
            // 评论的回复也需要查询，此处回复不进行分页，故设置为最大值
            List<Comment> replyList = commentService.findCommentsByEntity(
                    ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
            // 回复VO列表
            List<Map<String, Object>> replyVOList = new ArrayList<>();
            if(replyList != null) {
                for (Comment reply : replyList) {
                    Map<String, Object> replyVo = new HashMap<>();
                    replyVo.put("reply", reply);
                    replyVo.put("user", userService.findUserById(reply.getUserId()));
                    // 点赞
                    likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                    replyVo.put("likeCount", likeCount);
                    // 点赞状态，取决于是否登录
                    likeStatus = hostHolder.getUser() == null ? 0 :likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                    replyVo.put("likeStatus", likeStatus);
                    // 查询被该评论回复的对象
                    User target = reply.getTargetId() == 0 ? null: userService.findUserById(reply.getTargetId());
                    replyVo.put("target", target);
                    replyVOList.add(replyVo);
                }
            }
            commentVo.put("replys", replyVOList);
            // 回复数量
            int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
            commentVo.put("replyCount", replyCount);
            commentVoList.add(commentVo);
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }
}
