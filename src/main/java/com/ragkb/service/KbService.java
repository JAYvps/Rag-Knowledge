// ============ service/KbService.java ============
package com.ragkb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ragkb.entity.YuqueRepo;
import com.ragkb.entity.YuqueDocSync;
import com.ragkb.mapper.YuqueRepoMapper;
import com.ragkb.mapper.YuqueDocSyncMapper;
import com.ragkb.yuque.YuqueClient;
import com.ragkb.yuque.dto.YuqueRepoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbService {

    private final YuqueRepoMapper yuqueRepoMapper;
    private final YuqueDocSyncMapper yuqueDocSyncMapper;

    /**
     * 获取所有启用的知识库列表
     */
    public List<YuqueRepo> listRepos() {
        return yuqueRepoMapper.selectEnabled();
    }

    /**
     * 获取某个知识库的文档列表
     */
    public List<YuqueDocSync> listDocs(Long repoId) {
        return yuqueDocSyncMapper.selectList(
                new LambdaQueryWrapper<YuqueDocSync>()
                        .eq(YuqueDocSync::getYuqueRepoId, repoId)
                        .eq(YuqueDocSync::getSyncStatus, 1)
                        .orderByAsc(YuqueDocSync::getYuqueDocId)
        );
    }
    /**
     * 获取所有知识库（含禁用的，管理页面用）
     */
    public List<YuqueRepo> listAllRepos() {
        return yuqueRepoMapper.selectList(
                new LambdaQueryWrapper<YuqueRepo>()
                        .orderByAsc(YuqueRepo::getSortOrder)
                        .orderByDesc(YuqueRepo::getCreatedAt)
        );
    }

}
