package com.examplatform.agent;

import com.examplatform.model.KnowledgeDocument;
import com.examplatform.service.RAGService;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class EducationalAgent {
    
    private final RAGService ragService;
    private final Map<String, AgentTool> tools;
    
    public EducationalAgent(RAGService ragService) {
        this.ragService = ragService;
        this.tools = initializeTools();
    }
    
    private Map<String, AgentTool> initializeTools() {
        Map<String, AgentTool> toolMap = new HashMap<>();
        toolMap.put("analyze_performance", new PerformanceAnalyzer());
        toolMap.put("generate_study_plan", new StudyPlanGenerator());
        toolMap.put("create_assessment", new AssessmentCreator());
        toolMap.put("provide_tutoring", new IntelligentTutor());
        return toolMap;
    }
    
    public AgentResponse processRequest(AgentRequest request) {
        String intent = determineIntent(request.getQuery());
        AgentTool tool = tools.get(intent);
        
        if (tool == null) {
            return new AgentResponse("I'm not sure how to help with that. Try asking about performance analysis, study plans, assessments, or tutoring.", null);
        }
        
        return tool.execute(request, ragService);
    }
    
    private String determineIntent(String query) {
        query = query.toLowerCase();
        if (query.contains("performance") || query.contains("analyze") || query.contains("weak")) {
            return "analyze_performance";
        } else if (query.contains("study plan") || query.contains("schedule") || query.contains("learn")) {
            return "generate_study_plan";
        } else if (query.contains("test") || query.contains("quiz") || query.contains("assessment")) {
            return "create_assessment";
        } else {
            return "provide_tutoring";
        }
    }
    
    interface AgentTool {
        AgentResponse execute(AgentRequest request, RAGService ragService);
    }
    
    static class PerformanceAnalyzer implements AgentTool {
        @Override
        public AgentResponse execute(AgentRequest request, RAGService ragService) {
            Map<String, Object> analysis = new HashMap<>();
            List<String> weakAreas = identifyWeakAreas(request.getStudentData());
            List<String> recommendations = generateRecommendations(weakAreas);
            
            analysis.put("weak_areas", weakAreas);
            analysis.put("recommendations", recommendations);
            analysis.put("confidence_score", calculateConfidence(request.getStudentData()));
            
            return new AgentResponse("Performance analysis complete. Found " + weakAreas.size() + " areas for improvement.", analysis);
        }
        
        private List<String> identifyWeakAreas(Map<String, Object> studentData) {
            List<String> weakAreas = new ArrayList<>();
            Map<String, Double> scores = (Map<String, Double>) studentData.getOrDefault("topic_scores", new HashMap<>());
            
            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                if (entry.getValue() < 0.7) {
                    weakAreas.add(entry.getKey());
                }
            }
            return weakAreas;
        }
        
        private List<String> generateRecommendations(List<String> weakAreas) {
            return weakAreas.stream()
                .map(area -> "Focus on " + area + " with additional practice and review")
                .toList();
        }
        
        private double calculateConfidence(Map<String, Object> studentData) {
            Map<String, Double> scores = (Map<String, Double>) studentData.getOrDefault("topic_scores", new HashMap<>());
            return scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }
    
    static class StudyPlanGenerator implements AgentTool {
        @Override
        public AgentResponse execute(AgentRequest request, RAGService ragService) {
            List<String> weakTopics = (List<String>) request.getStudentData().getOrDefault("weak_topics", List.of());
            Map<String, Object> studyPlan = createPersonalizedPlan(weakTopics, ragService);
            
            return new AgentResponse("Generated personalized study plan with " + weakTopics.size() + " focus areas.", studyPlan);
        }
        
        private Map<String, Object> createPersonalizedPlan(List<String> weakTopics, RAGService ragService) {
            Map<String, Object> plan = new HashMap<>();
            List<Map<String, Object>> dailyTasks = new ArrayList<>();
            
            for (int day = 1; day <= 7; day++) {
                Map<String, Object> dayPlan = new HashMap<>();
                String topic = weakTopics.get((day - 1) % weakTopics.size());
                
                List<KnowledgeDocument> resources = ragService.retrieveRelevantDocuments(topic, 3);
                
                dayPlan.put("day", day);
                dayPlan.put("focus_topic", topic);
                dayPlan.put("study_duration", "45 minutes");
                dayPlan.put("resources", resources.stream().map(KnowledgeDocument::getTitle).toList());
                dayPlan.put("activities", List.of("Read concepts", "Practice problems", "Self-assessment"));
                
                dailyTasks.add(dayPlan);
            }
            
            plan.put("duration", "7 days");
            plan.put("daily_tasks", dailyTasks);
            plan.put("total_topics", weakTopics.size());
            
            return plan;
        }
    }
    
    static class AssessmentCreator implements AgentTool {
        @Override
        public AgentResponse execute(AgentRequest request, RAGService ragService) {
            String topic = request.getQuery().replaceAll(".*assessment.*?on\\s+", "").trim();
            Map<String, Object> assessment = generateAdaptiveAssessment(topic, ragService);
            
            return new AgentResponse("Created adaptive assessment for " + topic, assessment);
        }
        
        private Map<String, Object> generateAdaptiveAssessment(String topic, RAGService ragService) {
            List<KnowledgeDocument> docs = ragService.retrieveRelevantDocuments(topic, 5);
            List<Map<String, Object>> questions = new ArrayList<>();
            
            for (int i = 0; i < Math.min(3, docs.size()); i++) {
                KnowledgeDocument doc = docs.get(i);
                Map<String, Object> question = new HashMap<>();
                question.put("id", i + 1);
                question.put("topic", doc.getTopic());
                question.put("difficulty", doc.getDifficultyLevel());
                question.put("question", "Explain the key concepts of " + doc.getTitle());
                question.put("context", doc.getContent().substring(0, Math.min(200, doc.getContent().length())));
                questions.add(question);
            }
            
            Map<String, Object> assessment = new HashMap<>();
            assessment.put("title", "Adaptive Assessment: " + topic);
            assessment.put("questions", questions);
            assessment.put("estimated_time", "15 minutes");
            assessment.put("adaptive", true);
            
            return assessment;
        }
    }
    
    static class IntelligentTutor implements AgentTool {
        @Override
        public AgentResponse execute(AgentRequest request, RAGService ragService) {
            String response = ragService.generateRAGResponse(
                request.getQuery(),
                (List<String>) request.getStudentData().getOrDefault("weak_topics", List.of()),
                request.getStudentData()
            );
            
            Map<String, Object> tutorData = new HashMap<>();
            tutorData.put("explanation", response);
            tutorData.put("follow_up_questions", generateFollowUpQuestions(request.getQuery()));
            tutorData.put("related_topics", findRelatedTopics(request.getQuery(), ragService));
            
            return new AgentResponse(response, tutorData);
        }
        
        private List<String> generateFollowUpQuestions(String query) {
            return List.of(
                "Can you give me an example of this concept?",
                "How does this relate to other topics?",
                "What are common mistakes to avoid?"
            );
        }
        
        private List<String> findRelatedTopics(String query, RAGService ragService) {
            return ragService.retrieveRelevantDocuments(query, 3)
                .stream()
                .map(KnowledgeDocument::getTopic)
                .distinct()
                .toList();
        }
    }
}