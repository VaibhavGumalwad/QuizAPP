import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { studentService } from '../services/api';
import './QuizAttempt.css';

function QuizAttempt() {
  const { quizId } = useParams();
  const [quiz, setQuiz] = useState(null);
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [score, setScore] = useState(null);
  const [totalQuestions, setTotalQuestions] = useState(0);
  const navigate = useNavigate();
  const studentId = localStorage.getItem('userId');

  useEffect(() => {
    loadQuiz();
  }, [quizId]);

  const loadQuiz = async () => {
    try {
      const response = await studentService.getQuiz(quizId);
      setQuiz(response.data);
      setTotalQuestions(response.data.questions?.length || 0);
    } catch (err) {
      console.error('Failed to load quiz', err);
    }
  };

  const handleAnswerChange = (questionId, answer) => {
    setAnswers({ ...answers, [questionId]: answer });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await studentService.submitQuiz(quizId, studentId, answers);
      setScore(response.data.score);
      setSubmitted(true);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to submit quiz');
    }
  };

  const handleBackToDashboard = () => {
    navigate('/student/dashboard');
  };

  if (!quiz) return <div className="loading">Loading quiz...</div>;
  if (!quiz.questions || quiz.questions.length === 0) {
    return <div className="loading">No questions available for this quiz.</div>;
  }

  if (submitted) {
    return (
      <div className="quiz-attempt">
        <div className="score-card">
          <h1>Quiz Completed! 🎉</h1>
          <div className="score-display">
            <h2>Your Score</h2>
            <div className="score-number" style={{color: 'green'}}>{score} / {totalQuestions}</div>
            <div className="score-percentage">
              {Math.round((score / totalQuestions) * 100)}%
            </div>
          </div>
          <div className="quiz-info">
            <p><strong>Quiz:</strong> {quiz.title}</p>
            <p><strong>Topic:</strong> {quiz.topic}</p>
            <p><strong>Correct Answers:</strong> {score}</p>
            <p><strong>Wrong Answers:</strong> {totalQuestions - score}</p>
          </div>
          <button onClick={handleBackToDashboard} className="back-btn">
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="quiz-attempt">
      <h1>{quiz.title}</h1>
      <p className="quiz-topic">Topic: {quiz.topic}</p>
      <p className="quiz-info-text">Answer all {totalQuestions} questions and submit</p>
      <form onSubmit={handleSubmit}>
        {quiz.questions.map((question, index) => (
          <div key={question.id} className="question-card">
            <h3>Question {index + 1}</h3>
            <p>{question.questionText}</p>
            <div className="options">
              {question.options.map((option, optIndex) => (
                <label key={optIndex}>
                  <input
                    type="radio"
                    name={`question-${question.id}`}
                    value={option}
                    onChange={() => handleAnswerChange(question.id, option)}
                    required
                  />
                  {option}
                </label>
              ))}
            </div>
          </div>
        ))}
        <button type="submit" className="submit-btn">Submit Quiz</button>
      </form>
    </div>
  );
}

export default QuizAttempt;
