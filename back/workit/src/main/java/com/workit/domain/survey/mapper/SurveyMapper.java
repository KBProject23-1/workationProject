package com.workit.domain.survey.mapper;

import com.workit.domain.survey.vo.SurveyAnswerVO;
import com.workit.domain.survey.vo.SurveyQuestionVO;
import com.workit.domain.survey.vo.UserSurveyVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SurveyMapper {

    List<SurveyQuestionVO> selectSurveyQuestions();

    int countSurveyByUserAndWorkation(@Param("userId") Long userId,
                                     @Param("workationId") Long workationId);

    int insertUserSurvey(UserSurveyVO survey);

    UserSurveyVO selectSurveyById(@Param("surveyId") Long surveyId);

    UserSurveyVO selectLatestSurveyByUserAndWorkation(@Param("userId") Long userId,
                                                     @Param("workationId") Long workationId);

    void deleteSurveyAnswers(@Param("surveyId") Long surveyId);

    void insertSurveyAnswers(@Param("surveyId") Long surveyId,
                            @Param("optionIds") List<Long> optionIds);

    List<SurveyAnswerVO> selectAnswersBySurveyId(@Param("surveyId") Long surveyId);
}
