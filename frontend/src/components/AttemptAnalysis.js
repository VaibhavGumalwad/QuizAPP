import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { studentService } from '../services/api';
import './AttemptAnalysis.css';

const AttemptAnalysis = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAnalysis();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadAnalysis = async () => {
    try {
      setLoading(true);
      const response = await studentService.getAttemptAnalysis(attemptId);
      setAnalysis(response.data);
    } catch (error) {
      console.error('Error loading analysis:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading analysis...</div>;
  }

  if (!analysis) {
    return <div className="error">Failed to load analysis</div>;
  }

  return (
    <div className="analysis-container">
      <button onClick={() => navigate('/student/dashboard')} className="back-btn">
        ← Back to Dashboard
      </button>

      <div className="analysis-header">
        <h1>📊 Quiz Analysis</h1>
        <div className="quiz-info">
          <h2>{analysis.quizTitle}</h2>
          <p className="topic">Topic: {analysis.topic}</p>
          <p className="date">Attempted: {new Date(analysis.attemptedAt).toLocaleString()}</p>
        </div>
      </div>

      <div className="score-summary">
        <div className="score-card">
          <div className="score-circle" style={{
            background: analysis.percentage >= 70 ? '#27ae60' : analysis.percentage >= 50 ? '#f39c12' : '#e74c3c'
          }}>
            <span className="percentage">{analysis.percentage}%</span>
          </div>
          <p className="score-text">{analysis.score} / {analysis.totalQuestions} Correct</p>
        </div>

        <div className="ai-feedback">
          <h3>🤖 AI Tutor Feedback</h3>
          <p>{analysis.aiExplanation}</p>
        </div>
      </div>

      <div className="questions-review">
        <h3>Question-by-Question Review</h3>
        {analysis.questionAnalysis.map((q, index) => (
          <div key={q.questionId} className={`question-card ${q.isCorrect ? 'correct' : 'incorrect'}`}>
            <div className="question-header">
              <span className="question-number">Question {index + 1}</span>
              <span className={`status-badge ${q.isCorrect ? 'correct' : 'incorrect'}`}>
                {q.isCorrect ? '✓ Correct' : '✗ Incorrect'}
              </span>
            </div>
            
            <p className="question-text">{q.question}</p>
            
            <div className="options-list">
              {q.options.map((option, idx) => {
                const isYourAnswer = option === q.yourAnswer;
                const isCorrectAnswer = option === q.correctAnswer;
                
                return (
                  <div 
                    key={idx} 
                    className={`option ${isCorrectAnswer ? 'correct-option' : ''} ${isYourAnswer && !isCorrectAnswer ? 'wrong-option' : ''}`}
                  >
                    {option}
                    {isCorrectAnswer && <span className="badge correct-badge">✓ Correct Answer</span>}
                    {isYourAnswer && !isCorrectAnswer && <span className="badge your-badge">Your Answer</span>}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AttemptAnalysis;
