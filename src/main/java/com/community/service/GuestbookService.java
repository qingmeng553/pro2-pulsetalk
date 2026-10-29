package com.community.service;

import com.community.vo.GuestbookVO;
import com.community.vo.PageVO;

/** 主页留言板。 */
public interface GuestbookService {

    PageVO<GuestbookVO> list(long ownerId, long page, long size);

    GuestbookVO add(long ownerId, long fromUserId, String content);

    void delete(long guestbookId, long currentUserId, boolean admin);
}
