import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { instructorService } from '../services/api';
import './Dashboard.css';

function InstructorDashboard() {
  const [topic, setTopic] = useState('');
  const [numberOfQuestions, setNumberOfQuestions] = useState(5);
  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedQuizResults, setSelectedQuizResults] = useState(null);
  const navigate = useNavigate();
  const instructorId = localStorage.getItem('userId');

  useEffect(() => {
    loadQuizzes();
  }, []);

  const loadQuizzes = async () => {
    try {
      const response = await instructorService.getMyQuizzes(instructorId);
      setQuizzes(response.data);
    } catch (err) {
      console.error('Failed to load quizzes', err);
    }
  };

  const handleCreateQuiz = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await instructorService.createQuiz(topic, numberOfQuestions, instructorId);
      alert('Quiz created successfully!');
      setTopic('');
      setNumberOfQuestions(5);
      loadQuizzes();
    } catch (err) {
      alert('Failed to create quiz');
    }
    setLoading(false);
  };

  const handleDeleteQuiz = async (quizId) => {
    if (window.confirm('Are you sure you want to delete this quiz?')) {
      try {
        await instructorService.deleteQuiz(quizId, instructorId);
        alert('Quiz deleted successfully!');
        loadQuizzes();
      } catch (err) {
        alert('Failed to delete quiz');
      }
    }
  };

  const handleViewResults = async (quizId) => {
    try {
      const response = await instructorService.getQuizResults(quizId);
      console.log('Quiz Results Response:', response.data);
      setSelectedQuizResults(response.data);
    } catch (err) {
      console.error('Error loading results:', err);
      alert('Failed to load quiz results');
    }
  };

  const closeResults = () => {
    setSelectedQuizResults(null);
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/');
  };

  return (
    <div className="dashboard">
      <div className="header">
        <h1>Instructor Dashboard</h1>
        <button onClick={handleLogout} className="logout-btn">Logout</button>
      </div>

      <div className="create-quiz-section">
        <h2>Create New Quiz</h2>
        <form onSubmit={handleCreateQuiz}>
          <input
            type="text"
            placeholder="Quiz Topic"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            required
          />
          <input
            type="number"
            placeholder="Number of Questions"
            value={numberOfQuestions}
            onChange={(e) => setNumberOfQuestions(e.target.value)}
            min="1"
            max="20"
            required
          />
          <button type="submit" disabled={loading}>
            {loading ? 'Generating Quiz...' : 'Create Quiz with AI'}
          </button>
        </form>
      </div>

      <div className="quizzes-section">
        <h2>My Quizzes</h2>
        <div className="quiz-list">
          {quizzes.map((quiz) => (
            <div key={quiz.id} className="quiz-card">
              <h3>{quiz.title}</h3>
              <p>Topic: {quiz.topic}</p>
              <p>Questions: {quiz.questions?.length || 0}</p>
              <p>Status: {quiz.approved ? '✓ Approved' : '⏳ Pending Approval'}</p>
              <p>Created: {new Date(quiz.createdAt).toLocaleDateString()}</p>
              <button 
                onClick={() => handleViewResults(quiz.id)} 
                className="quiz-card button"
              >
                View Results
              </button>
              <button 
                onClick={() => handleDeleteQuiz(quiz.id)} 
                className="delete-btn"
              >
                Delete Quiz
              </button>
            </div>
          ))}
        </div>
      </div>

      {selectedQuizResults && (
        <div className="modal-overlay" onClick={closeResults}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Quiz Results: {selectedQuizResults.quizTitle}</h2>
              <button onClick={closeResults} className="close-btn">&times;</button>
            </div>
            <div className="modal-body">
              {!selectedQuizResults.attempts || selectedQuizResults.attempts.length === 0 ? (
                <p>No students have attempted this quiz yet.</p>
              ) : (
                <div className="results-grid">
                  {selectedQuizResults.attempts.map((attempt) => {
                    const totalQuestions = attempt.totalQuestions || 0;
                    const score = attempt.score || 0;
                    const percentage = totalQuestions > 0 
                      ? Math.round((score / totalQuestions) * 100) 
                      : 0;
                    return (
                      <div key={attempt.id} className="result-card">
                        <h3>{attempt.studentName}</h3>
                        <p className="score-text">Score: <span style={{color: 'green', fontWeight: 'bold'}}>{percentage}%</span></p>
                        <p>Marks: {score} / {totalQuestions}</p>
                        <p className="date-text">Date: {new Date(attempt.attemptedAt).toLocaleString()}</p>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default InstructorDashboard;
