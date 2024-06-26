package com.houzizhen.home.mapper;

import com.houzizhen.home.model.AnalyzeBlue;
import com.houzizhen.home.model.AnalyzeRed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface AnalyzeBlueMapper {
    void deleteAll();

    void batchInsert(List<AnalyzeBlue> analyzeBlueList);

    String selectByIssue(@Param("issue") Integer issue);

}
