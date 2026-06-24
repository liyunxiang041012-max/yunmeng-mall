package com.liyun.remark.service;

import com.liyun.common.utils.PageDTO;
import com.liyun.remark.domain.dto.CommentFormDTO;
import com.liyun.remark.domain.po.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liyun.remark.domain.vo.CommentVO;

public interface ICommentService extends IService<Comment> {

    void saveComment(CommentFormDTO dto);

    PageDTO<CommentVO> pageQueryComments(Long bizId, String bizType, int pageNo, int pageSize);
}
