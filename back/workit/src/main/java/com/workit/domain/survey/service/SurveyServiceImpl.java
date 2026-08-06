package com.workit.domain.survey.service;

import com.workit.domain.survey.dto.request.SurveyAnswerItemRequestDTO;
import com.workit.domain.survey.dto.request.SurveyCreateRequestDTO;
import com.workit.domain.survey.dto.response.SurveyCreateResponseDTO;
import com.workit.domain.survey.dto.response.SurveyModifyResponseDTO;
import com.workit.domain.survey.dto.response.SurveyQuestionItemResponseDTO;
import com.workit.domain.survey.dto.response.SurveyQuestionListResponseDTO;
import com.workit.domain.survey.dto.response.SurveyQuestionOptionResponseDTO;
import com.workit.domain.survey.dto.response.SurveyResultOptionResponseDTO;
import com.workit.domain.survey.dto.response.SurveyResultQuestionResponseDTO;
import com.workit.domain.survey.dto.response.SurveyResultResponseDTO;
import com.workit.domain.survey.exception.SurveyErrorCode;
import com.workit.domain.survey.mapper.SurveyMapper;
import com.workit.domain.survey.vo.QuestionType;
import com.workit.domain.survey.vo.SurveyAnswerVO;
import com.workit.domain.survey.vo.SurveyOptionVO;
import com.workit.domain.survey.vo.SurveyQuestionVO;
import com.workit.domain.survey.vo.UserSurveyVO;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyServiceImpl implements SurveyService {

    private final SurveyMapper surveyMapper;
    private final WorkationMapper workationMapper;

    @Override
    @Transactional(readOnly = true)
    public SurveyQuestionListResponseDTO findQuestionList(Long userId) {
        validateUserId(userId);

        List<SurveyQuestionVO> questions = surveyMapper.selectSurveyQuestions();
        return buildQuestionListResponse(questions);
    }

    @Override
    @Transactional
    public SurveyCreateResponseDTO createSurvey(Long userId, SurveyCreateRequestDTO request) {
        validateUserId(userId);
        validateSubmitRequest(request);

        WorkationVO activeWorkation = findActiveWorkation(userId);
        if (activeWorkation == null) {
            throw new BusinessException(SurveyErrorCode.SURVEY_ACCESS_DENIED);
        }

        if (surveyMapper.countSurveyByUserAndWorkation(userId, activeWorkation.getId()) > 0) {
            throw new BusinessException(SurveyErrorCode.SURVEY_ALREADY_EXISTS);
        }

        List<SurveyQuestionVO> questions = surveyMapper.selectSurveyQuestions();
        Map<Long, SurveyQuestionVO> questionMap = toQuestionMap(questions);
        Map<Long, List<Long>> answerMap = toAnswerMap(request);

        validateQuestionAnswers(questionMap, answerMap);

        UserSurveyVO userSurvey = new UserSurveyVO();
        userSurvey.setUserId(userId);
        userSurvey.setWorkationId(activeWorkation.getId());
        surveyMapper.insertUserSurvey(userSurvey);

        List<Long> selectedOptionIds = flattenSelectedOptionIds(answerMap);
        surveyMapper.insertSurveyAnswers(userSurvey.getSurveyId(), selectedOptionIds);

        UserSurveyVO savedSurvey = surveyMapper.selectSurveyById(userSurvey.getSurveyId());
        LocalDateTime createdAt = savedSurvey != null ? savedSurvey.getCreatedAt() : LocalDateTime.now();
        return new SurveyCreateResponseDTO(savedSurvey.getSurveyId(), createdAt);
    }

    @Override
    @Transactional(readOnly = true)
    public SurveyResultResponseDTO getMySurveyResult(Long userId) {
        validateUserId(userId);

        WorkationVO activeWorkation = findActiveWorkation(userId);
        if (activeWorkation == null) {
            throw new BusinessException(SurveyErrorCode.SURVEY_ACCESS_DENIED);
        }

        UserSurveyVO survey = surveyMapper.selectLatestSurveyByUserAndWorkation(userId, activeWorkation.getId());
        if (survey == null) {
            throw new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND);
        }

        return buildResultResponse(survey);
    }

    @Override
    @Transactional
    public SurveyModifyResponseDTO modifySurvey(Long userId, Long surveyId, SurveyCreateRequestDTO request) {
        validateUserId(userId);
        validateSurveyId(surveyId);
        validateSubmitRequest(request);

        UserSurveyVO targetSurvey = surveyMapper.selectSurveyById(surveyId);
        if (targetSurvey == null) {
            throw new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND);
        }
        if (!userId.equals(targetSurvey.getUserId())) {
            throw new BusinessException(SurveyErrorCode.SURVEY_ACCESS_DENIED);
        }

        List<SurveyQuestionVO> questions = surveyMapper.selectSurveyQuestions();
        Map<Long, SurveyQuestionVO> questionMap = toQuestionMap(questions);
        Map<Long, List<Long>> answerMap = toAnswerMap(request);

        validateQuestionAnswers(questionMap, answerMap);

        surveyMapper.deleteSurveyAnswers(surveyId);
        List<Long> selectedOptionIds = flattenSelectedOptionIds(answerMap);
        surveyMapper.insertSurveyAnswers(surveyId, selectedOptionIds);

        UserSurveyVO updated = surveyMapper.selectSurveyById(surveyId);
        LocalDateTime updatedAt = updated != null ? updated.getUpdatedAt() : LocalDateTime.now();
        return new SurveyModifyResponseDTO(surveyId, updatedAt);
    }

    private WorkationVO findActiveWorkation(Long userId) {
        return workationMapper.selectActiveWorkation(userId);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(SurveyErrorCode.SURVEY_ACCESS_DENIED);
        }
    }

    private void validateSurveyId(Long surveyId) {
        if (surveyId == null || surveyId < 1) {
            throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_REQUEST);
        }
    }

    private void validateSubmitRequest(SurveyCreateRequestDTO request) {
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
        }
    }

    private Map<Long, List<Long>> toAnswerMap(SurveyCreateRequestDTO request) {
        Map<Long, List<Long>> answerMap = new LinkedHashMap<>();
        for (SurveyAnswerItemRequestDTO item : request.getAnswers()) {
            if (item == null) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
            if (item.getQuestionId() == null) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
            if (item.getOptionIds() == null || item.getOptionIds().isEmpty()) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
            List<Long> optionIds = item.getOptionIds().stream()
                    .filter(optionId -> optionId != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (optionIds.isEmpty()) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
            if (answerMap.containsKey(item.getQuestionId())) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }

            answerMap.put(item.getQuestionId(), optionIds);
        }
        return answerMap;
    }

    private Map<Long, SurveyQuestionVO> toQuestionMap(List<SurveyQuestionVO> questions) {
        Map<Long, SurveyQuestionVO> map = new LinkedHashMap<>();
        for (SurveyQuestionVO question : questions) {
            map.put(question.getQuestionId(), question);
        }
        return map;
    }

    private void validateQuestionAnswers(Map<Long, SurveyQuestionVO> questionMap,
                                        Map<Long, List<Long>> answerMap) {

        for (SurveyQuestionVO question : questionMap.values()) {
            List<Long> selectedOptionIds = answerMap.get(question.getQuestionId());

            if (question.getMinSelection() == null || question.getMaxSelection() == null) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
            if (selectedOptionIds == null) {
                if (question.getMinSelection() > 0) {
                    throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
                }
                continue;
            }

            int selectedCount = selectedOptionIds.size();
            if (selectedCount < question.getMinSelection() || selectedCount > question.getMaxSelection()) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }

            validateQuestionTypeConstraint(question, selectedCount, selectedOptionIds);
            validateOptionIds(question, selectedOptionIds);
        }

        Set<Long> requiredQuestionIds = questionMap.values().stream()
                .filter(question -> question.getMinSelection() > 0)
                .map(SurveyQuestionVO::getQuestionId)
                .collect(Collectors.toSet());

        Set<Long> answeredQuestionIds = answerMap.keySet();
        if (!answeredQuestionIds.containsAll(requiredQuestionIds)) {
            throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
        }

        for (Long key : answerMap.keySet()) {
            if (!questionMap.containsKey(key)) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
        }
    }

    private void validateQuestionTypeConstraint(SurveyQuestionVO question,
                                               int selectedCount,
                                               List<Long> selectedOptionIds) {
        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE && selectedCount != 1) {
            throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
        }
        Set<Long> uniqueSet = new LinkedHashSet<>(selectedOptionIds);
        if (uniqueSet.size() != selectedOptionIds.size()) {
            throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
        }
    }

    private void validateOptionIds(SurveyQuestionVO question, List<Long> selectedOptionIds) {
        if (question.getOptions() == null) {
            throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
        }

        Set<Long> available = question.getOptions().stream()
                .map(SurveyOptionVO::getOptionId)
                .collect(Collectors.toSet());

        for (Long optionId : selectedOptionIds) {
            if (!available.contains(optionId)) {
                throw new BusinessException(SurveyErrorCode.INVALID_SURVEY_ANSWER);
            }
        }
    }

    private SurveyQuestionListResponseDTO buildQuestionListResponse(List<SurveyQuestionVO> questions) {
        int questionOrder = 1;
        List<SurveyQuestionItemResponseDTO> items = new ArrayList<>();

        for (SurveyQuestionVO question : questions) {
            List<SurveyQuestionOptionResponseDTO> optionItems = new ArrayList<>();
            int optionOrder = 1;
            if (question.getOptions() != null) {
                for (SurveyOptionVO option : question.getOptions()) {
                    optionItems.add(new SurveyQuestionOptionResponseDTO(
                            option.getOptionId(),
                            option.getOptionCode(),
                            option.getOptionName(),
                            optionOrder++
                    ));
                }
            }
            items.add(new SurveyQuestionItemResponseDTO(
                    question.getQuestionId(),
                    question.getQuestionCode(),
                    question.getQuestionText(),
                    question.getQuestionType(),
                    question.getMinSelection() != null && question.getMinSelection() > 0,
                    question.getMinSelection(),
                    question.getMaxSelection(),
                    questionOrder++,
                    optionItems
            ));
        }

        return SurveyQuestionListResponseDTO.of(items);
    }

    private SurveyResultResponseDTO buildResultResponse(UserSurveyVO survey) {
        List<SurveyQuestionVO> questions = surveyMapper.selectSurveyQuestions();
        Map<Long, Set<Long>> selectedOptionIdsByQuestion = loadSelectedOptionIdsByQuestion(survey.getSurveyId());

        List<SurveyResultQuestionResponseDTO> questionItems = questions.stream()
                .map(question -> buildResultQuestionResponse(question,
                        selectedOptionIdsByQuestion.getOrDefault(question.getQuestionId(),
                                Collections.emptySet())))
                .collect(Collectors.toList());

        return SurveyResultResponseDTO.of(
                survey.getSurveyId(),
                survey.getCreatedAt(),
                survey.getUpdatedAt(),
                questionItems
        );
    }

    private Map<Long, Set<Long>> loadSelectedOptionIdsByQuestion(Long surveyId) {
        List<SurveyAnswerVO> answers = surveyMapper.selectAnswersBySurveyId(surveyId);
        Map<Long, Set<Long>> selectedMap = new LinkedHashMap<>();
        for (SurveyAnswerVO answer : answers) {
            selectedMap.computeIfAbsent(answer.getQuestionId(), key -> new LinkedHashSet<>())
                    .add(answer.getOptionId());
        }
        return selectedMap;
    }

    private SurveyResultQuestionResponseDTO buildResultQuestionResponse(SurveyQuestionVO question,
                                                                       Set<Long> selectedOptionIds) {
        List<SurveyResultOptionResponseDTO> optionItems = question.getOptions().stream()
                .map(option -> new SurveyResultOptionResponseDTO(option.getOptionId(), option.getOptionName()))
                .collect(Collectors.toList());

        List<Long> orderedSelectedOptionIds = optionItems.stream()
                .map(SurveyResultOptionResponseDTO::getOptionId)
                .filter(selectedOptionIds::contains)
                .collect(Collectors.toList());

        return new SurveyResultQuestionResponseDTO(
                question.getQuestionId(),
                question.getQuestionCode(),
                question.getQuestionText(),
                convertCategoryForApi(question.getCategory()),
                question.getQuestionType(),
                optionItems,
                orderedSelectedOptionIds
        );
    }

    private String convertCategoryForApi(String dbCategory) {
        if (dbCategory == null || dbCategory.isBlank()) {
            return "COMMON";
        }
        if ("RESTAURANT".equals(dbCategory)) {
            return "MEAL";
        }
        if ("ACCOMMODATION".equals(dbCategory)) {
            return "COMMON";
        }
        return dbCategory;
    }

    private List<Long> flattenSelectedOptionIds(Map<Long, List<Long>> answerMap) {
        return answerMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
