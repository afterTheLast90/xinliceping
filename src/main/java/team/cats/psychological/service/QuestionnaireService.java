package team.cats.psychological.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.yitter.idgen.YitIdHelper;
import org.springframework.stereotype.Service;
import team.cats.psychological.base.BasePageParam;
import team.cats.psychological.base.PageResult;
import team.cats.psychological.entity.*;
import team.cats.psychological.mapper.*;
import team.cats.psychological.param.QuestionnaireParams;
import team.cats.psychological.vo.QuestionnaireIdAndStudentIdView;
import team.cats.psychological.vo.QuestionnaireView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionnaireService {

    @Resource
    private QuestionnaireMapper questionnaireMapper;
    @Resource
    private UsersMapper usersMapper;
    @Resource
    private QuestionnaireDetailsMapper questionnaireDetailsMapper;
    @Resource
    private PublishMapper publishMapper;
    @Resource
    private UserQuestionnaireMapper userQuestionnaireMapper;

    public PageResult<QuestionnaireView> selectQuestionnaire(BasePageParam basePageParam,String value){
        PageHelper.startPage(basePageParam.getPageNum(), basePageParam.getPageSize());
        List<QuestionnaireView> questionnaires = questionnaireMapper.selectQuestionnaire(value);
        for (QuestionnaireView questionnaire : questionnaires) {
            questionnaire.setCreatorName(usersMapper.selectById(questionnaire.getCreator()).getUserName());
        }
        return new PageResult<QuestionnaireView>(questionnaires);
    }
    public Questionnaire selectById(Long id){
        return questionnaireMapper.selectById(id);
    }

    public void InsertQuestionnaire(String name,String introduction){
        long userId = StpUtil.getLoginIdAsLong();
        Questionnaire questionnaire= new Questionnaire();
        questionnaire.setQuestionnaireId(YitIdHelper.nextId());
        questionnaire.setQuestionnaireName(name);
        questionnaire.setQuestionnaireIntroduction(introduction);
        questionnaire.setCreator(userId);
        questionnaireMapper.insert(questionnaire);
    }

    public void DelQuestionnaire(Long id){
        questionnaireMapper.deleteById(id);
        QueryWrapper<Publish> publishQueryWrapper=new QueryWrapper<>();
        publishQueryWrapper.eq("questionnaire_id",id);
        publishQueryWrapper.eq("state",0);
        publishMapper.delete(publishQueryWrapper);
        QueryWrapper<UserQuestionnaire> userQuestionnaireQueryWrapper = new QueryWrapper<>();
        userQuestionnaireQueryWrapper.eq("questionnaire",id);
        userQuestionnaireMapper.delete(userQuestionnaireQueryWrapper);
        QueryWrapper<QuestionnaireDetails> questionnaireDetailsQueryWrapper = new QueryWrapper<>();
        questionnaireDetailsQueryWrapper.eq("questionnaire_id",id);
        questionnaireDetailsMapper.delete(questionnaireDetailsQueryWrapper);

    }

    public void ModifyQuestionnaire(QuestionnaireParams questionnaireParams){
        System.out.println(questionnaireParams);
        Questionnaire questionnaire = questionnaireMapper.selectById(questionnaireParams.getQuestionnaireId());
        questionnaire.setQuestionnaireName(questionnaireParams.getQuestionnaireName());
        questionnaire.setQuestionnaireIntroduction(questionnaireParams.getQuestionnaireIntroduction());
        questionnaire.setVariables(questionnaireParams.getVariables());
        questionnaire.setCalculation(questionnaireParams.getCalculation());
        questionnaire.setResults(questionnaireParams.getResults());
        questionnaire.setQuestionnaireState(questionnaireParams.getQuestionnaireState());
        questionnaire.setTopicTemplate(questionnaireParams.getTopicTemplate());
        questionnaire.setOrderNo(questionnaireParams.getOrderNo());
        questionnaireMapper.updateById(questionnaire);
    }

    public List<QuestionnaireView> getUserQuestionnaire(List<QuestionnaireIdAndStudentIdView> questionnaireIds){
        List<QuestionnaireView> questionnaireViews= new ArrayList<>();
        for (QuestionnaireIdAndStudentIdView questionnaireId : questionnaireIds) {
            if (questionnaireId.getState()){

                QueryWrapper<UserQuestionnaire> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("questionnaire",questionnaireId.getQuestionnaireId());
                queryWrapper.eq("user_id",questionnaireId.getStudentId());
                queryWrapper.eq("publish_id",questionnaireId.getPublishId());
                List<UserQuestionnaire> userQuestionnaires = userQuestionnaireMapper.selectList(queryWrapper);
                if (userQuestionnaires.size()!=0){
                    continue;
                }
            }
            QuestionnaireView questionnaireView = questionnaireMapper.selectUserQuestionnaire(questionnaireId.getQuestionnaireId());
            Publish publish = publishMapper.selectById(questionnaireId.getPublishId());
            questionnaireView.setPublishId(questionnaireId.getPublishId());
            questionnaireView.setPublisherId(publish.getPublisherId());
            questionnaireView.setPublisherName(usersMapper.selectById(publish.getPublisherId()).getUserName());
            questionnaireView.setReleaseTime(publish.getReleaseTime());
            questionnaireView.setDeadLine(publish.getDeadline());
            questionnaireView.setCreatorName(usersMapper.selectById(questionnaireView.getCreator()).getUserName());
            questionnaireView.setStudentName(usersMapper.selectById(questionnaireId.getStudentId()).getUserName());
            questionnaireView.setStudentId(questionnaireId.getStudentId());
            questionnaireView.setState(questionnaireId.getState());
            questionnaireViews.add(questionnaireView);
        }
        return questionnaireViews;
    }

    public void confirm(Long questionnaireId){
        boolean student = false;
        boolean teacher = false;
        boolean parent = false;
        List<QuestionnaireDetails> questionnaireDetails = questionnaireDetailsMapper.selectByQuestionnaireId(questionnaireId);
        for (QuestionnaireDetails questionnaireDetail : questionnaireDetails) {
            if (questionnaireDetail.getChosePeople() == 0) {
                student = true;
            } else if (questionnaireDetail.getChosePeople() == 1) {
                parent = true;
            } else {
                teacher = true;
            }
        }
        Questionnaire questionnaire = questionnaireMapper.selectById(questionnaireId);
        if (student && teacher && parent) {
            questionnaire.setChoosePeople(6);//学生教师家长6
        } else if (parent && teacher) {
            questionnaire.setChoosePeople(5);//家长教师5
        } else if (student && teacher) {
            questionnaire.setChoosePeople(4);//学生教师4
        } else if (student && parent) {
            questionnaire.setChoosePeople(3);//学生家长3
        } else if (teacher) {
            questionnaire.setChoosePeople(2);//教师2
        } else if (parent) {
            questionnaire.setChoosePeople(1);//家长1
        } else {
            questionnaire.setChoosePeople(0);//学生0
        }
        questionnaire.setQuestionnaireState(1);
        questionnaireMapper.updateById(questionnaire);
    }

    public void copy(Long questionnaireId){
        long userId = StpUtil.getLoginIdAsLong();
        Questionnaire questionnaire = questionnaireMapper.selectById(questionnaireId);
        Questionnaire questionnaire1 = new Questionnaire();
        questionnaire1.setQuestionnaireId(YitIdHelper.nextId());
        questionnaire1.setQuestionnaireName(questionnaire.getQuestionnaireName()+"副本");
        questionnaire1.setQuestionnaireIntroduction(questionnaire.getQuestionnaireIntroduction());
        questionnaire1.setVariables(questionnaire.getVariables());
        questionnaire1.setCalculation(questionnaire.getCalculation());
        questionnaire1.setResults(questionnaire.getResults());
        questionnaire1.setCreator(userId);
        questionnaire1.setTopicTemplate(questionnaire.getTopicTemplate());
        questionnaireMapper.insert(questionnaire1);
        QueryWrapper<QuestionnaireDetails> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("questionnaire_id",questionnaire.getQuestionnaireId());
        List<QuestionnaireDetails> questionnaireDetails = questionnaireDetailsMapper.selectList(queryWrapper);
        for (QuestionnaireDetails questionnaireDetail : questionnaireDetails) {
            questionnaireDetail.setQuestionId(YitIdHelper.nextId());
            questionnaireDetail.setQuestionnaireId(questionnaire1.getQuestionnaireId());
            questionnaireDetailsMapper.insert(questionnaireDetail);
        }
    }
}
