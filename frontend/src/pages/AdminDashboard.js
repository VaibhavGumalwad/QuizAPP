import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../services/api';
import KnowledgeEnhancer from '../components/KnowledgeEnhancer';
import './Dashboard.css';

function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadUsers();
    loadQuizzes();
  }, []);

  const loadUsers = async () => {
    try {
      const response = await adminService.getAllUsers();
      setUsers(response.data);
    } catch (err) {
      console.error('Failed to load users', err);
    }
  };

  const loadQuizzes = async () => {
    try {
      const response = await adminService.getAllQuizzes();
      setQuizzes(response.data);
    } catch (err) {
      console.error('Failed to load quizzes', err);
    }
  };

  const handleDeleteUser = async (userId) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        await adminService.deleteUser(userId);
        loadUsers();
      } catch (err) {
        alert('Failed to delete user');
      }
    }
  };

  const handleApproveQuiz = async (quizId) => {
    try {
      await adminService.approveQuiz(quizId);
      loadQuizzes();
    } catch (err) {
      alert('Failed to approve quiz');
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/');
  };

  const students = users.filter(u => u.role === 'STUDENT');
  const instructors = users.filter(u => u.role === 'INSTRUCTOR');

  return (
    <div className="dashboard">
      <div className="header">
        <h1>Admin Dashboard</h1>
        <button onClick={handleLogout} className="logout-btn">Logout</button>
      </div>

      <KnowledgeEnhancer />

      <div className="quizzes-section">
        <h2>Students</h2>
        <div className="quiz-list">
          {students.map((student) => (
            <div key={student.id} className="quiz-card">
              <h3>{student.username}</h3>
              <p>Email: {student.email}</p>
              <button onClick={() => handleDeleteUser(student.id)} className="delete-btn">
                Delete
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="quizzes-section">
        <h2>Instructors</h2>
        <div className="quiz-list">
          {instructors.map((instructor) => (
            <div key={instructor.id} className="quiz-card">
              <h3>{instructor.username}</h3>
              <p>Email: {instructor.email}</p>
              <button onClick={() => handleDeleteUser(instructor.id)} className="delete-btn">
                Delete
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="quizzes-section">
        <h2>Quizzes Pending Approval</h2>
        <div className="quiz-list">
          {quizzes.filter(q => !q.approved).map((quiz) => (
            <div key={quiz.id} className="quiz-card">
              <h3>{quiz.title}</h3>
              <p>Topic: {quiz.topic}</p>
              <p>Instructor: {quiz.instructor.username}</p>
              <button onClick={() => handleApproveQuiz(quiz.id)} className="approve-btn">
                Approve
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default AdminDashboard;
