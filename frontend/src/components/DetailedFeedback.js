import React, { useState, useEffect } from 'react';
import { studentService } from '../services/api';
import './DetailedFeedback.css';

const DetailedFeedback = ({ attemptId, onClose }) => {
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchDetailedFeedback();
  }, [attemptId]);

  const fetchDetailedFeedback = async () => {
    try {
      setLoading(true);
      const response = await studentService.getDetailedFeedback(attemptId);
      setFeedback(response.data);
    } catch (err) {
      setError('Failed to load detailed feedback');
      console.error('Error fetching feedback:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="feedback-modal">
        <div className="feedback-content">
          <div className="loading-spinner">
            <div className="spinner"></div>
            <p>Generating AI feedback...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="feedback-modal">
        <div className="feedback-content">
          <div className="error-message">
            <h3>Error</h3>
            <p>{error}</p>
            <button onClick={onClose} className="close-btn">Close</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="feedback-modal">
      <div className="feedback-content">
        <div className="feedback-header">
          <h2>🤖 AI Tutor Feedback</h2>
          <button onClick={onClose} className="close-button">×</button>
        </div>

        <div className="feedback-summary">
          <div className="score-card">
            <h3>Quiz Results</h3>
            <div className="score-display">
              <span className="score">{feedback.score}/{feedback.totalQuestions}</span>
              <span className="percentage">({feedback.percentage}%)</span>
            </div>
          </div>
          
          <div className="overall-feedback">
            <h4>Overall Assessment</h4>
            <p>{feedback.overallFeedback}</p>
          </div>
        </div>

        {feedback.weakTopics && feedback.weakTopics.length > 0 && (
          <div className="weak-topics-section">
            <h4>📚 Topics to Focus On</h4>
            <div className="weak-topics-list">
              {feedback.weakTopics.map((topic, index) => (
                <span key={index} className="topic-tag">{topic}</span>
              ))}
            </div>
          </div>
        )}

        {feedback.wrongAnswers && feedback.wrongAnswers.length > 0 && (
          <div className="wrong-answers-section">
            <h4>❌ Detailed Analysis of Wrong Answers</h4>
            {feedback.wrongAnswers.map((wrongAnswer, index) => (
              <div key={index} className="wrong-answer-card">
                <div className="question-header">
                  <h5>Question {index + 1}</h5>
                  <span className="topic-badge">{wrongAnswer.topic}</span>
                </div>
                
                <div className="question-text">
                  <p><strong>Question:</strong> {wrongAnswer.questionText}</p>
                </div>
                
                <div className="answer-comparison">
                  <div className="answer-row your-answer">
                    <span className="label">Your Answer:</span>
                    <span className="answer wrong">{wrongAnswer.studentAnswer}</span>
                  </div>
                  <div className="answer-row correct-answer">
                    <span className="label">Correct Answer:</span>
                    <span className="answer correct">{wrongAnswer.correctAnswer}</span>
                  </div>
                </div>
                
                <div className="ai-explanation">
                  <h6>🧠 AI Explanation</h6>
                  <div className="explanation-text">
                    {wrongAnswer.explanation.split('\n').map((line, lineIndex) => (
                      <p key={lineIndex}>{line}</p>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {feedback.studyRecommendations && feedback.studyRecommendations.length > 0 && (
          <div className="recommendations-section">
            <h4>💡 Study Recommendations</h4>
            {feedback.studyRecommendations.map((rec, index) => (
              <div key={index} className="recommendation-card">
                <h6>{rec.topic}</h6>
                <p><strong>Action:</strong> {rec.action}</p>
                <p><strong>Resources:</strong> {rec.resources}</p>
              </div>
            ))}
          </div>
        )}

        <div className="feedback-footer">
          <button onClick={onClose} className="close-btn">Close Feedback</button>
        </div>
      </div>
    </div>
  );
};

export default DetailedFeedback;