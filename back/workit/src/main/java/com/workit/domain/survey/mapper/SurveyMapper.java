package com.workit.domain.survey.mapper;

import com.workit.domain.survey.vo.SurveyAnswerVO;
import com.workit.domain.survey.vo.SurveyQuestionVO;
import com.workit.domain.survey.vo.UserSurveyVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SurveyMapper {

    List<SurveyQuestionVO> selectSurveyQuestions();

    // 설문은 사용자당 1개다. 워케이션이 바뀌어도 취향은 이어진다
    int countSurveyByUser(@Param("userId") Long userId);

    int insertUserSurvey(UserSurveyVO survey);

    UserSurveyVO selectSurveyById(@Param("surveyId") Long surveyId);

    UserSurveyVO selectLatestSurveyByUser(@Param("userId") Long userId);

    void deleteSurveyAnswers(@Param("surveyId") Long surveyId);

    void insertSurveyAnswers(@Param("surveyId") Long surveyId,
                            @Param("optionIds") List<Long> optionIds);

    List<SurveyAnswerVO> selectAnswersBySurveyId(@Param("surveyId") Long surveyId);
}
