import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { studentService } from '../services/api';
import WeakTopics from '../components/WeakTopics';
import AITutorChat from '../components/AITutorChat';
import DetailedFeedback from '../components/DetailedFeedback';
import './Dashboard.css';

function StudentDashboard() {
  const [quizzes, setQuizzes] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [showWeakTopics, setShowWeakTopics] = useState(false);
  const [showAITutor, setShowAITutor] = useState(false);
  const [showDetailedFeedback, setShowDetailedFeedback] = useState(false);
  const [selectedAttemptId, setSelectedAttemptId] = useState(null);
  const [weakTopicsList, setWeakTopicsList] = useState([]);
  const [recentFeedback, setRecentFeedback] = useState(null);
  const navigate = useNavigate();
  const studentId = localStorage.getItem('userId');

  useEffect(() => {
    loadQuizzes();
    loadAttempts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadQuizzes = async () => {
    try {
      const response = await studentService.getAllQuizzes();
      setQuizzes(response.data);
    } catch (err) {
      console.error('Failed to load quizzes', err);
    }
  };

  const loadAttempts = async () => {
    try {
      const response = await studentService.getMyAttempts(studentId);
      setAttempts(response.data);
      
      // Load weak topics for AI tutor
      if (response.data.length > 0) {
        const weakTopicsResponse = await studentService.getWeakTopics(studentId);
        const weakTopics = weakTopicsResponse.data.weakTopics
          .filter(topic => topic.isWeak)
          .map(topic => topic.topic);
        setWeakTopicsList(weakTopics);
      }
    } catch (err) {
      console.error('Failed to load attempts', err);
    }
  };

  const handleAttemptQuiz = (quizId) => {
    const alreadyAttempted = attempts.some(a => a.quiz.id === quizId);
    if (alreadyAttempted) {
      alert('You have already attempted this quiz!');
      return;
    }
    navigate(`/student/quiz/${quizId}`);
  };

  const handleViewDetailedFeedback = async (attemptId) => {
    try {
      const feedbackResponse = await studentService.getDetailedFeedback(attemptId);
      setRecentFeedback(feedbackResponse.data);
      setSelectedAttemptId(attemptId);
      setShowDetailedFeedback(true);
    } catch (error) {
      console.error('Error loading feedback:', error);
      setSelectedAttemptId(attemptId);
      setShowDetailedFeedback(true);
    }
  };

  const handleCloseDetailedFeedback = () => {
    setShowDetailedFeedback(false);
    setSelectedAttemptId(null);
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/');
  };

  return (
    <div className="dashboard">
      <div className="header">
        <h1>Student Dashboard</h1>
        <div className="header-buttons">
          <button onClick={() => {
            setShowWeakTopics(false);
            setShowAITutor(!showAITutor);
          }} className="ai-tutor-btn">
            {showAITutor ? '📚 View Dashboard' : '🤖 AI Tutor'}
          </button>
          <button onClick={handleLogout} className="logout-btn">Logout</button>
        </div>
      </div>

      {showDetailedFeedback && (
        <DetailedFeedback 
          attemptId={selectedAttemptId} 
          onClose={handleCloseDetailedFeedback} 
        />
      )}

      {showAITutor ? (
        <AITutorChat 
          studentWeakTopics={weakTopicsList} 
          recentFeedback={recentFeedback}
        />
      ) : showWeakTopics ? (
        <div>
          <button onClick={() => setShowWeakTopics(false)} className="back-to-quizzes-btn">
            📚 Back to Dashboard
          </button>
          <WeakTopics />
        </div>
      ) : (
        <>
          <div className="dashboard-actions">
            <button onClick={() => setShowWeakTopics(true)} className="view-weak-topics-btn">
              📊 View Performance Analysis
            </button>
          </div>

      <div className="quizzes-section">
        <h2>Available Quizzes</h2>
        <div className="quiz-list">
          {quizzes.map((quiz) => {
            const attempted = attempts.some(a => a.quiz.id === quiz.id);
            return (
              <div key={quiz.id} className="quiz-card">
                <h3>{quiz.title}</h3>
                <p>Topic: {quiz.topic}</p>
                <p>Instructor: {quiz.instructor?.username || 'N/A'}</p>
                <p>Questions: {quiz.questions?.length || 0}</p>
                <button 
                  onClick={() => handleAttemptQuiz(quiz.id)}
                  disabled={attempted}
                  className={attempted ? 'attempted' : ''}
                >
                  {attempted ? 'Already Attempted' : 'Attempt Quiz'}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      <div className="attempts-section">
        <h2>My Attempts</h2>
        <div className="attempts-list">
          {attempts.map((attempt) => {
            const totalQuestions = attempt.totalQuestions || attempt.quiz.questions?.length || 0;
            const percentage = totalQuestions > 0 ? Math.round((attempt.score / totalQuestions) * 100) : 0;
            return (
              <div key={attempt.id} className="attempt-card">
                <h3>{attempt.quiz.title}</h3>
                <p>Score: <span style={{color: 'green', fontWeight: 'bold'}}>{percentage}%</span></p>
                <p>Date: {new Date(attempt.attemptedAt).toLocaleString()}</p>
                <button 
                  onClick={() => navigate(`/student/attempt/${attempt.id}/analysis`)}
                  className="view-analysis-btn"
                >
                  📊 View Analysis
                </button>
                <button 
                  onClick={() => handleViewDetailedFeedback(attempt.id)}
                  className="view-feedback-btn"
                >
                  🤖 AI Feedback
                </button>
              </div>
            );
          })}
        </div>
      </div>
        </>
      )}
    </div>
  );
}

export default StudentDashboard;
